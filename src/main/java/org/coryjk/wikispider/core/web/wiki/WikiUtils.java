package org.coryjk.wikispider.core.web.wiki;

import org.coryjk.wikispider.core.web.WebUtils;

import java.io.IOException;
import java.util.List;

public final class WikiUtils {

    private static final String NO_WRAP_LINKS_TABLE_CSS_QUERY = "table[class^=nowraplinks]";
    private static final String EXTERNAL_LINKS_TABLE_CLASS_ATTR_REGEX = "^nowraplinks.*mw-collapsible.*autocollapse.*";

    public static List<String> getAllURLSWithoutExternalLinks(final String url) throws IOException {
        return WebUtils.getAllURLs(url, document -> {
            document.select(NO_WRAP_LINKS_TABLE_CSS_QUERY).forEach(
                    element -> {
                        if (element.attr("class").matches(EXTERNAL_LINKS_TABLE_CLASS_ATTR_REGEX)) {
                            element.remove();
                        }
                    });
        });
    }
}
