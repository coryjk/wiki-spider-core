package org.coryjk.wikispider.core.search;

import java.util.List;

public interface SearchSession<T> {

    void search(final T start, final T target);

    List<T> getSolution();
}
