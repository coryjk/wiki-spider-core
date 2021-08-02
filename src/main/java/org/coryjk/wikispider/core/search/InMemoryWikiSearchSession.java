package org.coryjk.wikispider.core.search;

import org.coryjk.wikispider.core.message.State;
import org.coryjk.wikispider.core.message.Status;
import org.coryjk.wikispider.core.spider.Spider;
import org.coryjk.wikispider.core.spider.SpiderFactory;
import org.coryjk.wikispider.core.spider.WikiSpider;
import org.coryjk.wikispider.core.spider.controller.Controller;
import org.coryjk.wikispider.core.util.wiki.WikiUtils;
import org.coryjk.wikispider.core.web.WebNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class InMemoryWikiSearchSession implements SearchSession<WebNode> {

    private static final Logger log = LoggerFactory.getLogger(InMemoryWikiSearchSession.class);

    final int numThreads;
    final int maxChancesPerEpoch;
    final long connectionGracePeriodPerWorker;

    final AtomicBoolean errorDuringSearch;

    final ExecutorService searchExecutor;
    final CyclicBarrier workerBarrier;
    final Semaphore workerPermit;

    final Controller<WikiSpider> controller;
    final Queue<WebNode> frontier;
    final Lock workQueueLock = new ReentrantLock();

    private Exception causeOfError;
    private List<WebNode> solution;

    InMemoryWikiSearchSession(final int numThreads,
                              final int maxChancesPerEpoch,
                              final long connectionGracePeriodPerWorker) {
        this.numThreads = numThreads;
        this.maxChancesPerEpoch = maxChancesPerEpoch;
        this.connectionGracePeriodPerWorker = connectionGracePeriodPerWorker;

        this.errorDuringSearch = new AtomicBoolean(false);

        this.searchExecutor = Executors.newFixedThreadPool(numThreads);
        this.workerBarrier = new CyclicBarrier(numThreads + 1);
        this.workerPermit = new Semaphore(numThreads);

        this.controller = Controller.create();
        this.frontier = new LinkedList<>();
    }

    @Override
    public List<WebNode> getSolution() {
        return new LinkedList<>(solution);
    }

    @Override
    public void search(final WebNode start, final WebNode target) {
        if (!frontier.isEmpty()) {
            throw new IllegalStateException("Work has already started for this session instance");
        }

        // next set of nodes to search, or "work queue"
        frontier.addAll(WikiSpider.getAdjacentNodes(start));

        /*
         * Main working loop executing each epoch of the overall search task.
         *
         * During each epoch, each available worker is assigned a sub-search from the next frontier node
         * to the overall target node. Each worker will perform its sub-search and await for all other
         * workers once a terminal state is reached.
         *
         * Each terminal state also consists of the next set of nodes to be enqueued into the frontier.
         * The search continues until no more nodes are left in the frontier, or if another terminating
         * state is met.
         */
        while (!frontier.isEmpty() && shouldContinueSearch()) {
            // answer pool from workers for each round of attempts
            final List<Future<WebNode>> candidateAnswers = new LinkedList<>();

            // keep assigning tasks until all workers have work to do
            for (int i = 0; i < numThreads; i++) {
                candidateAnswers.add(
                        performSubSearchAsync(frontier.poll(), target));
            }

            // join on all working threads (from parent thread)
            workerAwait();

            // see if solution was found
            evaluateCandidateAnswers(candidateAnswers)
                    .ifPresent(candidate -> { solution = candidate.getPathToRoot(); } );

            // return all used permits to semaphore
            workerPermit.release(numThreads - workerPermit.availablePermits());
        }

        // no more work to do, terminate
        searchExecutor.shutdownNow();
    }

    public boolean solutionFound() {
        return solution != null && solution.size() > 0;
    }

    public boolean searchExceptionOccurred() {
        return causeOfError != null;
    }

    public Exception getCauseOfError() {
        return causeOfError;
    }

    public boolean inProgress() {
        return !searchExecutor.isShutdown();
    }

    private boolean shouldContinueSearch() {
        return !solutionFound() && !searchExceptionOccurred();
    }

    private Future<WebNode> performSubSearchAsync(final WebNode current, final WebNode target) {
        return CompletableFuture
                .supplyAsync(doWork(current, target), searchExecutor)
                .thenApply(
                        state -> {
                            final boolean collision = state.status() == Status.COLLISION;
                            final boolean foundTarget = state.status() == Status.FOUND_RESULT;

                            if (!collision) {
                                workerAwait();
                            } else {
                                workerPermit.release();
                            }

                            if (!foundTarget) {
                                enqueueWorkFromTerminalState(state);
                            }

                            return foundTarget
                                    ? state.value().get(state.value().size()-1)
                                    : null;
                        });
    }

    private Supplier<State<List<WebNode>>> doWork(final WebNode current, final WebNode target) {
        return () -> acquireWorkPermit() ? spawnSpider().crawl(current, target) : createFailureState();
    }

    private boolean acquireWorkPermit() {
        try {
            workerPermit.acquire();
            return true;
        } catch (Exception exception) {
            log.error("Failed to acquire work permit.", exception);
            flagError(exception);
        }
        return false;
    }

    private State<List<WebNode>> createFailureState() {
        return new State<>(new LinkedList<>(), Status.ERROR);
    }

    private Optional<WebNode> evaluateCandidateAnswers(final List<Future<WebNode>> candidates) {
        return candidates.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception exception) {
                        log.error("Got exception while evaluating candidate answer.", exception);
                        flagError(exception);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private void workerAwait() {
        try {
            workerBarrier.await();
        } catch (Exception exception) {
            log.error("Got exception during worker barrier await.", exception);
            flagError(exception);
        }
    }

    private void enqueueWorkFromTerminalState(final State<List<WebNode>> state) {
        workQueueLock.lock();
        frontier.addAll(state.value());
        workQueueLock.unlock();
    }

    private void flagError(final Exception exception) {
        causeOfError = exception;
        errorDuringSearch.set(true);
    }

    private Spider<WebNode> spawnSpider() {
        return SpiderFactory.createWikiSpider(
                controller, maxChancesPerEpoch, connectionGracePeriodPerWorker, WikiUtils::isValidURL);
    }
}
