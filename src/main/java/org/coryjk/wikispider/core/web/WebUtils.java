package org.coryjk.wikispider.core.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class WebUtils {

    private static final String LINKS_CSS_QUERY = "a[href]";
    private static final String ABSOLUTE_PATH_ATTRIBUTE_KEY = "abs:href";

    public static List<String> getAllURLs(final String url) throws IOException {
        final Document document = Jsoup.connect(url).get();
        final Elements links = document.select(LINKS_CSS_QUERY);
        return links.stream()
                .map(link -> link.attr(ABSOLUTE_PATH_ATTRIBUTE_KEY))
                .collect(Collectors.toList());
    }
}
