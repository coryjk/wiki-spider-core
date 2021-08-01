package org.coryjk.wikispider.core.web;

public interface Node<T> extends Comparable<Node<T>> {

    T getValue();
}
