package org.example.basetest;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型接口实现类 - GenericList
 * 实现了 Container 接口
 */
public class GenericList<T> implements Container<T> {
    private List<T> items = new ArrayList<>();

    @Override
    public void add(T item) {
        items.add(item);
    }

    @Override
    public T get(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public List<T> getAll() {
        return new ArrayList<>(items);
    }

    @Override
    public String toString() {
        return "GenericList{" +
                "items=" + items +
                '}';
    }
}

