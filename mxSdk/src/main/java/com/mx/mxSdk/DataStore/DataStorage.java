package com.mx.mxSdk.DataStore;

import java.util.List;

public interface DataStorage<E> {

    void add(E e);

    void add(int location, E e);

    void addFirst(E e);

    void addLast(E e);

    void add(List<E> e);

    boolean contains(E e);

    boolean contains(String attributeName, Object attributeValue);

    E get(String attributeName, Object attributeValue);

    List<E> get();

    E get(int location);

    void remove(int location);

    void remove(E e);

    int size();

    boolean isEmpty();

    void clear();
}
