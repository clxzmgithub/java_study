package org.example.basetest;

/**
 * 泛型类示例 - Box
 * 可以存储任何类型的数据
 */
public class Box<T> {
    private T value;

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Box{" +
                "value=" + value +
                '}';
    }
}

