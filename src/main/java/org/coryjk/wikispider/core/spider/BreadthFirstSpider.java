package org.coryjk.wikispider.core.spider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class BreadthFirstSpider<T> implements Spider<T> {

    private static final Logger log = LoggerFactory.getLogger(BreadthFirstSpider.class);

    @Override
    public List<T> crawl(final T start, final T destination) {
        // fetch first adjacent nodes to visit
        final Queue<List<T>> toVisit = new LinkedList<>();
        toVisit.add(List.of(start));

        // do BFS search until destination is reached or until terminal state is met
        while (!toVisit.isEmpty()) {
            final List<T> path = toVisit.poll();
            log.info("Path: [{}], \nPaths left to check: [{}]", path, toVisit.size());

            // get last node from the next path
            final T current = dequeueUntilValidNode(path);
            if (current == null) {
                continue;
            }

            // terminal state reached, return path
            if (inTerminalState(current, destination)) {
                // signal termination
                terminate();
                return path;
            }
            updateState();

            // visit next nodes then return new path to queue
            final List<T> neighbors = visit(current, path);
            for (final T neighbor : neighbors) {
                if (current.equals(neighbor)) {
                    continue;
                }
                final List<T> newPath = new LinkedList<>(path);
                newPath.add(neighbor);
                toVisit.add(newPath);
            }
        }

        // no path found
        return null;
    }

    protected abstract List<T> visit(final T node, final List<T> currentPath);

    protected abstract boolean inTerminalState(final T node, final T destination);

    protected abstract void terminate();

    protected abstract void updateState();

    protected boolean isValidNode(final T node) {
        return true;
    }

    private T dequeueUntilValidNode(final List<T> nodes) {
        T current = nodes.get(nodes.size()-1);
        while (!isValidNode(current)) {
            final int N = nodes.size();
            nodes.remove(N-1);

            // no more nodes
            if (N-1 <= 0) {
                return null;
            }
            current = nodes.get(N-2);
        }
        return current;
    }

}
