package org.example.test;

import java.util.ArrayList;
import java.util.List;

/**
 * 基本类型 vs 包装类型的区别演示
 */
public class PrimitiveVsWrapper {
    public static void main(String[] args) {
        System.out.println("========== 基本类型 vs 包装类型 ==========\n");

        // 1. 存储位置和默认值
        System.out.println("【1. 存储位置和默认值】");
        demoDefaultValue();

        // 2. null 值
        System.out.println("\n【2. null 值处理】");
        demoNullValue();

        // 3. 自动装箱和拆箱
        System.out.println("\n【3. 自动装箱和拆箱】");
        demoBoxingAndUnboxing();

        // 4. 性能对比
        System.out.println("\n【4. 性能对比】");
        demoPerformance();

        // 5. 包装类缓存机制
        System.out.println("\n【5. 包装类缓存机制】");
        demoCaching();

        // 6. 在集合中的应用
        System.out.println("\n【6. 在集合中的应用】");
        demoCollection();

        // 7. 相等性比较
        System.out.println("\n【7. 相等性比较】");
        demoEquality();
    }

    // ============ 1. 默认值 ============
    private static void demoDefaultValue() {
        // 基本类型的默认值
        int num = 0;
        boolean flag = false;
        double d = 0.0;

        System.out.println("基本类型的默认值:");
        System.out.println("  int 默认值: " + num);
        System.out.println("  boolean 默认值: " + flag);
        System.out.println("  double 默认值: " + d);

        // 包装类型的默认值
        Integer objNum = new Integer(0);
        Boolean objFlag = null;

        System.out.println("\n包装类型的默认值:");
        System.out.println("  Integer 默认值: " + objNum);
        System.out.println("  Boolean 默认值: " + objFlag);
    }

    // ============ 2. null 值处理 ============
    private static void demoNullValue() {
        // 基本类型不能为 null
        // int num = null;  // ❌ 编译错误

        // 包装类型可以为 null
        Integer obj = null;
        System.out.println("Integer 可以设为 null: " + obj);

        // 这在实际应用中很有用
        // 例如：数据库查询可能返回 null
        Integer result = queryDatabase();
        if (result != null) {
            System.out.println("查询结果: " + result);
        } else {
            System.out.println("查询结果为空");
        }
    }

    private static Integer queryDatabase() {
        // 模拟数据库查询
        return null;  // 没有找到结果
    }

    // ============ 3. 自动装箱和拆箱 ============
    private static void demoBoxingAndUnboxing() {
        // 自动装箱
        int num = 10;
        Integer obj = num;  // 自动调用 Integer.valueOf(10)
        System.out.println("自动装箱: int " + num + " → Integer " + obj);

        // 自动拆箱
        Integer obj2 = 20;
        int num2 = obj2;  // 自动调用 obj2.intValue()
        System.out.println("自动拆箱: Integer " + obj2 + " → int " + num2);

        // 在运算中自动拆箱
        Integer a = 5;
        Integer b = 10;
        int sum = a + b;  // 先拆箱为 int，再做加法
        System.out.println("\n运算中的拆箱: " + a + " + " + b + " = " + sum);

        // 注意：拆箱可能导致 NullPointerException
        Integer c = null;
        try {
            int value = c;  // ❌ 会抛出 NullPointerException
        } catch (NullPointerException e) {
            System.out.println("⚠️ 拆箱 null 导致异常: NullPointerException");
        }
    }

    // ============ 4. 性能对比 ============
    private static void demoPerformance() {
        // 测试 1000 万次操作
        int iterations = 10_000_000;

        // 基本类型性能
        long startTime = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < iterations; i++) {
            sum1 += i;
        }
        long time1 = System.nanoTime() - startTime;
        System.out.println("基本类型 (int) 耗时: " + (time1 / 1_000_000) + " ms");

        // 包装类型性能
        startTime = System.nanoTime();
        Long sum2 = 0L;
        for (int i = 0; i < iterations; i++) {
            sum2 += i;  // 涉及装箱/拆箱
        }
        long time2 = System.nanoTime() - startTime;
        System.out.println("包装类型 (Long) 耗时: " + (time2 / 1_000_000) + " ms");

        System.out.println("性能差异: " + (time2 / time1) + " 倍");
    }

    // ============ 5. 包装类缓存 ============
    private static void demoCaching() {
        // Integer 缓存范围：-128 ~ 127
        Integer a = 100;
        Integer b = 100;
        System.out.println("Integer a = 100, b = 100");
        System.out.println("a == b: " + (a == b) + " (在缓存范围内，使用同一对象)");
        System.out.println("a.equals(b): " + a.equals(b));

        // 超出缓存范围
        Integer c = 200;
        Integer d = 200;
        System.out.println("\nInteger c = 200, d = 200");
        System.out.println("c == d: " + (c == d) + " (超出缓存范围，不同对象)");
        System.out.println("c.equals(d): " + c.equals(d));

        // 显式创建对象
        Integer e = new Integer(100);
        Integer f = new Integer(100);
        System.out.println("\nInteger e = new Integer(100), f = new Integer(100)");
        System.out.println("e == f: " + (e == f) + " (创建了新对象)");
        System.out.println("e.equals(f): " + e.equals(f));
    }

    // ============ 6. 在集合中的应用 ============
    private static void demoCollection() {
        // 集合中必须使用包装类型
        // List<int> list = new ArrayList<>();  // ❌ 编译错误

        // 正确方式：使用包装类型
        List<Integer> intList = new ArrayList<>();
        intList.add(10);      // 自动装箱
        intList.add(20);
        intList.add(30);
        System.out.println("Integer 集合: " + intList);

        // 从集合中取出元素
        int first = intList.get(0);  // 自动拆箱
        System.out.println("第一个元素: " + first);

        // 其他包装类型集合
        List<Double> doubleList = new ArrayList<>();
        doubleList.add(3.14);
        doubleList.add(2.71);
        System.out.println("Double 集合: " + doubleList);

        List<Boolean> boolList = new ArrayList<>();
        boolList.add(true);
        boolList.add(false);
        System.out.println("Boolean 集合: " + boolList);
    }

    // ============ 7. 相等性比较 ============
    private static void demoEquality() {
        Integer a = 10;
        Integer b = 10;
        Integer c = new Integer(10);
        int d = 10;

        System.out.println("a = 10 (自动装箱)");
        System.out.println("b = 10 (自动装箱)");
        System.out.println("c = new Integer(10)");
        System.out.println("d = 10 (基本类型)");

        System.out.println("\n== 比较 (引用相等):");
        System.out.println("a == b: " + (a == b));      // true (缓存)
        System.out.println("a == c: " + (a == c));      // false (不同对象)
        System.out.println("a == d: " + (a == d));      // true (自动拆箱)

        System.out.println("\nequals() 比较 (值相等):");
        System.out.println("a.equals(b): " + a.equals(b));    // true
        System.out.println("a.equals(c): " + a.equals(c));    // true
        System.out.println("a.equals(d): 无法比较");  // Integer 和 int 无法直接比较

        System.out.println("\n建议: 用 equals() 比较包装类型的值，用 == 比较基本类型");
    }
}

