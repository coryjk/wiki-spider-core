package org.coryjk.wikispider.core.spider;

import java.util.List;

public interface Spider<T> {

    List<T> crawl(final T start, final T destination);
}
