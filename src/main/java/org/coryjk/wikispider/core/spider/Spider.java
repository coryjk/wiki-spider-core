package org.coryjk.wikispider.core.spider;

import org.coryjk.wikispider.core.message.State;

import java.util.List;

public interface Spider<T> {

    State<List<T>> crawl(final T start, final T target);
}
