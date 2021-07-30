package org.coryjk.wikispider.core.spider;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class BreadthFirstSpider<T> implements Spider<T> {

    @Override
    public List<T> crawl(final T start, final T destination) {
        // fetch first adjacent nodes to visit
        final Queue<List<T>> toVisit = new LinkedList<>();
        toVisit.add(List.of(start));

        // do BFS search until destination is reached or until terminal state is met
        T current;
        while (!toVisit.isEmpty()) {
            final List<T> path = toVisit.poll();

            // get last node from the next path
            current = path.get(toVisit.size()-1);

            // terminal state reached, return path
            if (inTerminalState(current, destination)) {
                return path;
            }
            updateState();

            // visit next nodes then return new path to queue
            toVisit.add(visit(current, path));
        }

        // no path found
        return null;
    }

    protected abstract List<T> visit(final T node, final List<T> currentPath);

    protected abstract boolean inTerminalState(final T node, final T destination);

    protected abstract void updateState();
}
