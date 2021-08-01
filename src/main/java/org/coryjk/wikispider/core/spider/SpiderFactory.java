package org.coryjk.wikispider.core.spider;

import org.coryjk.wikispider.core.spider.controller.Controller;
import org.coryjk.wikispider.core.web.WikiNode;

import java.util.function.Predicate;

public final class SpiderFactory {

    public static Spider<WikiNode> createWikiSpider(final Controller<WikiSpider> controller,
                                                    final int maxChances,
                                                    final long connectionGracePeriod,
                                                    final Predicate<String> urlFilter) {
        return new WikiSpider(controller, maxChances, connectionGracePeriod, urlFilter);
    }
}
