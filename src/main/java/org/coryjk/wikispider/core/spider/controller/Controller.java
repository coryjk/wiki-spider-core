package org.coryjk.wikispider.core.spider.controller;

import org.coryjk.wikispider.core.spider.Spider;
import org.coryjk.wikispider.core.web.Node;

import java.util.HashSet;
import java.util.Set;

public class Controller<T extends Spider<?>> {

    private final Set<Node<?>> visitedNodes = new HashSet<>();

    Controller() { }

    public static <T extends Spider<?>> Controller<T> create() {
        return new Controller<>();
    }

    public void reportVisited(final Node<?> node) {
        synchronized (visitedNodes) {
            visitedNodes.add(node);
        }
    }

    public boolean hasVisited(final Node<?> node) {
        synchronized (visitedNodes) {
            return visitedNodes.contains(node);
        }
    }

}
