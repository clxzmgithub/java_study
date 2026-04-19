package org.example.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Java 泛型演示 - 综合示例
 */
public class FanxingDemo {
    public static void main(String[] args) {
        System.out.println("========== Java 泛型演示 ==========\n");

        // 1. 泛型类示例
        System.out.println("【1. 泛型类示例 - Box】");
        demoGenericClass();

        // 2. 泛型接口示例
        System.out.println("\n【2. 泛型接口示例 - Container】");
        demoGenericInterface();

        // 3. 泛型方法示例
        System.out.println("\n【3. 泛型方法示例】");
        demoGenericMethod();

        // 4. 泛型上界限制
        System.out.println("\n【4. 泛型上界限制 - NumberBox】");
        demoNumberBox();

        // 5. DAO 泛型应用
        System.out.println("\n【5. DAO 泛型应用】");
        demoDAO();
    }

    // ============ 1. 泛型类演示 ============
    private static void demoGenericClass() {
        // 存储字符串
        Box<String> stringBox = new Box<>();
        stringBox.setValue("Hello World");
        String str = stringBox.getValue();
        System.out.println("String Box: " + stringBox);

        // 存储整数
        Box<Integer> intBox = new Box<>();
        intBox.setValue(42);
        int num = intBox.getValue();
        System.out.println("Integer Box: " + intBox);

        // 存储自定义对象
        Box<Person> personBox = new Box<>();
        personBox.setValue(new Person("张三", 25));
        Person person = personBox.getValue();
        System.out.println("Person Box: " + personBox);
    }

    // ============ 2. 泛型接口演示 ============
    private static void demoGenericInterface() {
        // 字符串容器
        Container<String> stringContainer = new GenericList<>();
        stringContainer.add("Apple");
        stringContainer.add("Banana");
        stringContainer.add("Orange");
        System.out.println("String Container: " + stringContainer);
        System.out.println("第一个元素: " + stringContainer.get(0));
        System.out.println("容器大小: " + stringContainer.size());

        // 整数容器
        Container<Integer> intContainer = new GenericList<>();
        intContainer.add(10);
        intContainer.add(20);
        intContainer.add(30);
        System.out.println("\nInteger Container: " + intContainer);
        System.out.println("第一个元素: " + intContainer.get(0));
        System.out.println("容器大小: " + intContainer.size());
    }

    // ============ 3. 泛型方法演示 ============
    private static void demoGenericMethod() {
        // 打印数组
        System.out.println("--- 打印整数数组 ---");
        Integer[] numbers = {1, 2, 3, 4, 5};
        GenericMethod.printArray(numbers);

        System.out.println("\n--- 打印字符串数组 ---");
        String[] words = {"Hello", "World", "Java", "Generics"};
        GenericMethod.printArray(words);

        // 获取第一个元素
        System.out.println("\n--- 获取第一个元素 ---");
        String first = GenericMethod.getFirstElement(words);
        System.out.println("第一个单词: " + first);

        // 打印 Map
        System.out.println("\n--- 打印 Map ---");
        Map<String, Integer> scoreMap = new HashMap<>();
        scoreMap.put("Alice", 95);
        scoreMap.put("Bob", 87);
        scoreMap.put("Charlie", 92);
        GenericMethod.printMap(scoreMap);

        // 交换数组元素
        System.out.println("\n--- 交换数组元素 ---");
        String[] fruits = {"Apple", "Banana", "Orange"};
        System.out.println("交换前: " + Arrays.toString(fruits));
        GenericMethod.swap(fruits, 0, 2);
        System.out.println("交换后: " + Arrays.toString(fruits));

        // 查找元素
        System.out.println("\n--- 查找元素 ---");
        int index = GenericMethod.indexOf(words, "Java");
        System.out.println("'Java' 的索引: " + index);
    }

    // ============ 4. NumberBox 演示 ============
    private static void demoNumberBox() {
        // Integer Box
        NumberBox<Integer> intBox = new NumberBox<>();
        intBox.setValue(42);
        System.out.println("Integer Box: " + intBox);
        System.out.println("Double 值: " + intBox.doubleValue());
        System.out.println("Long 值: " + intBox.longValue());

        // Double Box
        System.out.println();
        NumberBox<Double> doubleBox = new NumberBox<>();
        doubleBox.setValue(3.14);
        System.out.println("Double Box: " + doubleBox);
        System.out.println("Double 值: " + doubleBox.doubleValue());
        System.out.println("Int 值: " + doubleBox.intValue());

        // Long Box
        System.out.println();
        NumberBox<Long> longBox = new NumberBox<>();
        longBox.setValue(999999L);
        System.out.println("Long Box: " + longBox);
        System.out.println("Double 值: " + longBox.doubleValue());

        // String Box
        System.out.println();
        NumberBox<Float> stringBox = new NumberBox<>();
        longBox.setValue(999999L);
        System.out.println("Long Box: " + longBox);
        System.out.println("Double 值: " + longBox.doubleValue());
    }

    // ============ 5. DAO 泛型应用 ============
    private static void demoDAO() {
        UserDao userDao = new UserDao();

        // 测试 CRUD 操作
        User user1 = new User(1L, "张三", "zhangsan@email.com");
        User user2 = new User(2L, "李四", "lisi@email.com");

        System.out.println("--- 插入用户 ---");
        userDao.insert(user1);
        userDao.insert(user2);

        System.out.println("\n--- 查询用户 ---");
        User user = userDao.selectById(1L);
        userDao.selectAll();

        System.out.println("\n--- 更新用户 ---");
        user1.setEmail("zhangsan_new@email.com");
        userDao.update(user1);

        System.out.println("\n--- 删除用户 ---");
        userDao.delete(1L);
    }
}

