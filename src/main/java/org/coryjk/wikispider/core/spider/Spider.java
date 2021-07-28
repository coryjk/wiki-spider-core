package org.coryjk.wikispider.core.spider;

import java.net.URL;
import java.util.List;

public interface Spider {

    List<URL> crawl(final URL start, final URL destination);
}
