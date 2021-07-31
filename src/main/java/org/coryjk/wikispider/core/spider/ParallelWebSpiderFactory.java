package org.coryjk.wikispider.core.spider;

import java.util.function.Predicate;

public final class ParallelWebSpiderFactory {

    public static Spider<String> getParallelWebSpider(final int threadCount,
                                                      final int maxConnections,
                                                      final int maxChances,
                                                      final long connectionGracePeriod,
                                                      final Predicate<String> urlFilter) {
        return new ParallelWebSpider(threadCount, maxConnections, maxChances, connectionGracePeriod, urlFilter);
    }
}
