package org.coryjk.wikispider.core.search;

import org.coryjk.wikispider.core.web.WebNode;
import org.coryjk.wikispider.core.web.WikiNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class SearchProvider {

    private static final Logger log = LoggerFactory.getLogger(SearchProvider.class);

    public static Future<List<WebNode>> inMemoryWikiSearch(final String start,
                                                           final String target,
                                                           final int numThreads,
                                                           final int maxChancesPerEpoch,
                                                           final int connectionGracePeriod) {
        final SearchSession<WebNode> searchSession
                = new InMemoryWikiSearchSession(numThreads, maxChancesPerEpoch, connectionGracePeriod);

        return new CompletableFuture<List<WebNode>>()
                    .completeAsync(() -> {
                        try {
                            CompletableFuture.runAsync(() -> searchSession.search(
                                    WikiNode.fromPath(start), WikiNode.fromPath(target))).get();
                        } catch (Exception exception) {
                            log.error("Exception thrown while evaluating search from [{}] to [{}]",
                                    start, target, exception);
                        }
                        return searchSession.getSolution();
                    });
    }
}
