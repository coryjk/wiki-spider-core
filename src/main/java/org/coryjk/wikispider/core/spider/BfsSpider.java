package org.coryjk.wikispider.core.spider;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class BfsSpider implements Spider {

    @Override
    public List<URL> crawl(final URL start, final URL destination) {
        // fetch first adjacent nodes to visit
        final Queue<List<URL>> toVisit = new LinkedList<>();
        toVisit.add(List.of(start));

        // do BFS search until destination is reached or until terminal state is met
        URL current;
        while (!toVisit.isEmpty()) {
            final List<URL> path = toVisit.poll();

            // get last node from the next path
            current = path.get(toVisit.size()-1);

            // terminal state reached, return path
            if (inTerminalState()) {
                return path;
            }

            // visit next nodes then return new path to queue
            toVisit.add(visit(current, path));
        }

        // no path found
        return null;
    }

    protected abstract List<URL> visit(final URL node, final List<URL> currentPath);

    protected abstract boolean inTerminalState();
}
