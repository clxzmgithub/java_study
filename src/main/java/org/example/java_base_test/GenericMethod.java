package org.example.java_base_test;

import java.util.Map;

/**
 * 泛型方法示例
 */
public class GenericMethod {

    /**
     * 简单的泛型方法 - 打印数组
     */
    public static <T> void printArray(T[] array) {
        for (T element : array) {
            System.out.println(element);
        }
    }

    /**
     * 返回泛型类型 - 获取数组第一个元素
     */
    public static <T> T getFirstElement(T[] array) {
        return array.length > 0 ? array[0] : null;
    }

    /**
     * 多个泛型参数 - 打印 Map
     */
    public static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    /**
     * 泛型方法 - 交换数组中的两个元素
     */
    public static <T> void swap(T[] array, int i, int j) {
        if (i >= 0 && i < array.length && j >= 0 && j < array.length) {
            T temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /**
     * 泛型方法 - 查找元素在数组中的索引
     */
    public static <T> int indexOf(T[] array, T target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && array[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }
}

