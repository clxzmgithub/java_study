package org.example.java_base_test;

import java.util.List;

/**
 * 泛型接口示例 - Container
 */
public interface Container<T> {
    void add(T item);

    T get(int index);

    int size();

    List<T> getAll();
}

