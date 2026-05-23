package org.example.java_base_test;

/**
 * 泛型上界限制示例 - NumberBox
 * T 必须是 Number 或其子类
 */
public class NumberBox<T extends Number> {
    private T value;

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    /**
     * 可以调用 Number 的方法
     */
    public double doubleValue() {
        return value.doubleValue();
    }

    /**
     * 可以调用 Number 的方法
     */
    public long longValue() {
        return value.longValue();
    }

    /**
     * 可以调用 Number 的方法
     */
    public int intValue() {
        return value.intValue();
    }

    @Override
    public String toString() {
        return "NumberBox{" +
                "value=" + value +
                '}';
    }
}

