package org.coryjk.wikispider.core.web;

public class WikiNode extends WebNode {

    private static final String WIKIPEDIA_EN_BASE = "https://en.wikipedia.org/wiki";

    private final String path;

    public WikiNode(final String url) {
        this(url, null);
    }

    public WikiNode(final String url, final WebNode parent) {
        super(url, parent);
        this.path = evaluatePath(url);
    }

    public static WikiNode fromPath(final String path) {
        return new WikiNode(WIKIPEDIA_EN_BASE + path);
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof WikiNode w) && path.equals(w.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    private String evaluatePath(final String url) {
        if (url == null
                || url.length() <= WIKIPEDIA_EN_BASE.length()
                || !url.contains(WIKIPEDIA_EN_BASE)) {
            throw new IllegalArgumentException("Invalid Wikipedia URL provided for path evaluation: " + url);
        }
        return url.substring(WIKIPEDIA_EN_BASE.length());
    }
}
