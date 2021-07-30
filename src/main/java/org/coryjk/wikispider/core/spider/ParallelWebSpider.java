package org.coryjk.wikispider.core.spider;

import org.coryjk.wikispider.core.web.WebUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelWebSpider extends BreadthFirstSpider<String> {

    private static final long IDLE_KEEP_ALIVE_MINUTES = 3;
    private static final long MIN_THREADS = 1;

    private final AtomicInteger activeThreads;

    private final ThreadPoolExecutor executor;
    private final BlockingQueue<Runnable> workQueue;
    private final Set<String> visitedNodes;

    private final int threadCount;
    private final int maxConnections;
    private final long gracePeriod;

    private ParallelWebSpider(final int threadCount, final int maxConnections, final long gracePeriod) {
        if (threadCount < MIN_THREADS) {
            throw new IllegalArgumentException("Cannot have less than " + MIN_THREADS + " threads!");
        }
        // keep half the thread count alive at all times
        final int corePoolSize = threadCount > 1 ? threadCount/2 : threadCount;
        this.threadCount = threadCount;
        this.maxConnections = maxConnections;
        this.gracePeriod = gracePeriod;

        this.activeThreads = new AtomicInteger(0);

        this.workQueue = new LinkedBlockingQueue<>();
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                threadCount,
                IDLE_KEEP_ALIVE_MINUTES,
                TimeUnit.MINUTES,
                workQueue);
        this.visitedNodes = ConcurrentHashMap.newKeySet();
    }

    @Override
    protected List<String> visit(final String node, final List<String> currentPath) {
        final List<String> adjacentNodes = new LinkedList<>();
        if (!visitedNodes.contains(node)) {
            rest();
            executor.submit(() -> {
                activeThreads.incrementAndGet();
                adjacentNodes.addAll(getAdjacentNodes(node));
                activeThreads.decrementAndGet();
            });
        }
        return adjacentNodes;
    }

    @Override
    protected boolean inTerminalState(final String node, final String destination) {
        return destination.equals(node);
    }

    @Override
    protected void updateState() {

    }

    private int getThreadCount() {
        return threadCount;
    }

    private void rest() {
        while (activeThreads.get() > maxConnections) { }
        try {
            Thread.sleep(gracePeriod);
        } catch (InterruptedException __) { }
    }

    private List<String> getAdjacentNodes(final String node) {
        final List<String> adjacentNodes;
        try {
            adjacentNodes = WebUtils.getAllURLs(node);
            return adjacentNodes;
        } catch (IOException __) { }
        return new LinkedList<>();
    }
}
