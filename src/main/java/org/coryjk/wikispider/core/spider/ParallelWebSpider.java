package org.coryjk.wikispider.core.spider;

import org.coryjk.wikispider.core.web.wiki.WikiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ParallelWebSpider extends BreadthFirstSpider<String> {

    private static final Logger log = LoggerFactory.getLogger(ParallelWebSpider.class);

    private static final long IDLE_KEEP_ALIVE_MILLIS = 10000;
    private static final long MIN_THREADS = 1;

    protected final AtomicInteger activeThreads;
    protected final ThreadPoolExecutor executor;
    protected final BlockingQueue<Runnable> workQueue;
    protected final Set<String> visitedNodes;

    private final int threadCount;
    private final int maxConnections;
    private final int maxChances;
    private final long connectionGracePeriod;

    private Predicate<String> urlFilter = url -> true;

    ParallelWebSpider(final int threadCount,
                      final int maxConnections,
                      final int maxChances,
                      final long connectionGracePeriod) {
        if (threadCount < MIN_THREADS) {
            throw new IllegalArgumentException("Cannot have less than " + MIN_THREADS + " threads!");
        }
        // keep half the thread count alive at all times
        final int corePoolSize = threadCount > 1 ? threadCount/2 : threadCount;
        this.threadCount = threadCount;
        this.maxConnections = maxConnections;
        this.maxChances = maxChances;
        this.connectionGracePeriod = connectionGracePeriod;

        this.activeThreads = new AtomicInteger(0);

        this.workQueue = new LinkedBlockingQueue<>();
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                threadCount,
                IDLE_KEEP_ALIVE_MILLIS,
                TimeUnit.MILLISECONDS,
                workQueue);
        this.visitedNodes = ConcurrentHashMap.newKeySet();
    }

    ParallelWebSpider(final int threadCount,
                      final int maxConnections,
                      final int maxChances,
                      final long connectionGracePeriod,
                      final Predicate<String> urlFilter) {
        this(threadCount, maxConnections, maxChances, connectionGracePeriod);
        this.urlFilter = urlFilter;
    }

    @Override
    protected List<String> visit(final String node, final List<String> currentPath) {
        final Set<String> adjacentNodes = new LinkedHashSet<>();
        rest();
        try {
            executor.submit(() -> {
                activeThreads.incrementAndGet();
                log.info("Visiting node: [{}], active threads: [{}]", node, activeThreads.get());
                getAdjacentNodes(node).stream()
                        .filter(urlFilter
                                .and(adjacentNode -> !visitedNodes.contains(adjacentNode)))
                        .collect(Collectors.toCollection(() -> adjacentNodes));
                visitedNodes.add(node);
                log.info("Nodes adjacent to [{}]: [{}]", node, adjacentNodes);
                activeThreads.decrementAndGet();
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Got exception during execution", e);
        }
        return new LinkedList<>(adjacentNodes);
    }

    @Override
    protected boolean inTerminalState(final String node, final String destination) {
        return destination.equals(node) || (maxChances > 0 && visitedNodes.size() > maxChances);
    }

    @Override
    protected void terminate() {
        executor.shutdownNow();
    }

    @Override
    protected void updateState() {

    }

    @Override
    protected boolean isValidNode(final String node) {
        return !visitedNodes.contains(node);
    }

    private int getThreadCount() {
        return threadCount;
    }

    private int getMaxConnections() {
        return maxConnections;
    }

    private long getConnectionGracePeriod() {
        return connectionGracePeriod;
    }

    private void rest() {
        while (activeThreads.get() > maxConnections) { }
        try {
            Thread.sleep(connectionGracePeriod);
        } catch (InterruptedException __) { }
    }

    private List<String> getAdjacentNodes(final String node) {
        final List<String> adjacentNodes;
        try {
            adjacentNodes = WikiUtils.getAllURLSWithoutExternalLinks(node);
            return adjacentNodes;
        } catch (IOException __) { }
        return new LinkedList<>();
    }
}
