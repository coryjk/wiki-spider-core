package org.coryjk.wikispider.core.web;

import java.util.LinkedList;
import java.util.List;

public abstract class WebNode implements Node<String> {

    protected final WebNode parent;
    protected final List<WebNode> children = new LinkedList<>();

    protected String url;

    public WebNode(final String url) {
        this(url, null);
    }

    public WebNode(final String url, final WebNode parent) {
        this.url = url;
        this.parent = parent;
    }

    @Override
    public int compareTo(final Node<String> node) {
        return getValue().compareTo(node.getValue());
    }

    @Override
    public String getValue() {
        return url;
    }

    public WebNode getParent() {
        return parent;
    }

    public List<WebNode> getChildren() {
        return children;
    }

    public List<WebNode> getPathToRoot() {
        final List<WebNode> path = new LinkedList<>();
        WebNode next = this;
        while (next != null) {
            path.add(next);
            next = next.getParent();
        }
        return path;
    }
}
