package org.coryjk.wikispider.core.spider;

import org.coryjk.wikispider.core.message.State;
import org.coryjk.wikispider.core.message.Status;
import org.coryjk.wikispider.core.spider.controller.Controller;
import org.coryjk.wikispider.core.util.wiki.WikiUtils;
import org.coryjk.wikispider.core.web.WikiNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WikiSpider extends BreadthFirstSpider<WikiNode> {

    private static final Logger log = LoggerFactory.getLogger(WikiSpider.class);

    private final Controller<WikiSpider> controller;
    private final int maxChances;
    private final long connectionGracePeriod;
    private final Set<WikiNode> visitedNodes;

    private Predicate<String> urlFilter = url -> true;

    WikiSpider(final Controller<WikiSpider> controller,
               final int maxChances,
               final long connectionGracePeriod) {
        this.controller = controller != null ? controller : Controller.create();
        this.maxChances = maxChances; // <- TODO: obsolete atm
        this.connectionGracePeriod = connectionGracePeriod;
        this.visitedNodes = new TreeSet<>();
    }

    WikiSpider(final Controller<WikiSpider> controller,
               final int maxChances,
               final long connectionGracePeriod,
               final Predicate<String> urlFilter) {
        this(controller, maxChances, connectionGracePeriod);
        this.urlFilter = urlFilter;
    }

    public static List<WikiNode> getAdjacentNodes(final WikiNode node) {
        return new WikiSpider(null, -1, 0, WikiUtils::isValidURL).visit(node);
    }

    @Override
    protected List<WikiNode> visit(final WikiNode node) {
        final Set<WikiNode> adjacentNodes = new LinkedHashSet<>();
        rest();

        log.info("Visiting node: [{}]", node);

        getAdjacentURLs(node.getValue()).stream()
                .filter(urlFilter)
                .map(url -> new WikiNode(url, node))
                .filter(adjacentNode -> isValidNode(adjacentNode) && !controller.hasVisited(adjacentNode))
                .collect(Collectors.toCollection(() -> adjacentNodes));
        controller.reportVisited(node);
        visitedNodes.add(node);

        log.info("Nodes adjacent to [{}]: [{}]", node, adjacentNodes);

        return new LinkedList<>(adjacentNodes);
    }

    @Override
    protected State<List<WikiNode>> getCurrentState(final WikiNode node,
                                                    final WikiNode target,
                                                    final List<WikiNode> currentPath) {
        final Status status;

        // target node found
        if (target.equals(node)) {
            status = Status.FOUND_RESULT;
        }

        // configured max chances is positive and has been surpassed
        else if (maxChances > 0 && visitedNodes.size() > maxChances) {
            status = Status.MAX_ATTEMPTS_EXHAUSTED;
        }

        // already-visited node found
        else if (visitedNodes.contains(node)) {
            status = Status.COLLISION;
        }

        // nothing of interest, still working
        else {
            status = Status.WORKING;
        }

        return new State<>(currentPath, status);
    }

    @Override
    protected boolean isValidNode(final WikiNode node) {
        return !visitedNodes.contains(node);
    }

    private long getConnectionGracePeriod() {
        return connectionGracePeriod;
    }

    private void rest() {
        try {
            Thread.sleep(connectionGracePeriod);
        } catch (InterruptedException __) { }
    }

    private List<String> getAdjacentURLs(final String url) {
        try {
            return WikiUtils.getAllURLSWithoutExternalLinks(url);
        } catch (IOException __) { }
        return new LinkedList<>();
    }
}
