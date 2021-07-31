package org.coryjk.wikispider.core.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WebUtils {

    private static final String LINKS_CSS_QUERY = "a[href]";
    private static final String ABSOLUTE_PATH_ATTR = "abs:href";

    public static List<String> getAllURLs(final String url) throws IOException {
        return getAllURLs(url, null);
    }

    public static List<String> getAllURLs(final String url, final Consumer<Document> documentMutator) throws IOException {
        final Document document = Jsoup.connect(url).get();
        if (documentMutator != null) {
            documentMutator.accept(document);
        }
        final Elements links = document.select(LINKS_CSS_QUERY);
        return links.stream()
                .map(link -> link.attr(ABSOLUTE_PATH_ATTR))
                .collect(Collectors.toList());
    }
}
