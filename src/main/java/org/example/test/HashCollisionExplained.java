package org.example.test;

import java.util.HashMap;
import java.util.Map;

/**
 * 哈希碰撞（Hash Collision）详解
 *
 * 为什么会发生哈希碰撞？
 * 因为哈希值的范围是有限的，而对象的数量是无限的
 */
public class HashCollisionExplained {
    public static void main(String[] args) {
        System.out.println("========== 哈希碰撞（Hash Collision）详解 ==========\n");

        // 1. 什么是哈希碰撞
        System.out.println("【1. 什么是哈希碰撞？】");
        demoWhatIsHashCollision();

        // 2. 为什么会发生哈希碰撞
        System.out.println("\n【2. 为什么会发生哈希碰撞？】");
        demoWhyHashCollision();

        // 3. 数学原理
        System.out.println("\n【3. 数学原理：鸽笼原理】");
        demoPigeonholePrinciple();

        // 4. Java 中的哈希碰撞例子
        System.out.println("\n【4. Java 中的实际例子】");
        demoRealHashCollisionInJava();

        // 5. HashMap 如何处理哈希碰撞
        System.out.println("\n【5. HashMap 如何处理哈希碰撞】");
        demoHowHashMapHandlesCollision();
    }

    // ============ 1. 什么是哈希碰撞？ ============
    private static void demoWhatIsHashCollision() {
        System.out.println("哈希碰撞 = 两个不同的对象产生了相同的哈希值\n");

        System.out.println("例子：");
        String str1 = "AB";
        String str2 = "BA";

        System.out.println("str1 = \"AB\"");
        System.out.println("str2 = \"BA\"");
        System.out.println("str1.hashCode() = " + str1.hashCode());
        System.out.println("str2.hashCode() = " + str2.hashCode());
        System.out.println("str1.equals(str2) = " + str1.equals(str2) + "\n");

        if (str1.hashCode() == str2.hashCode()) {
            System.out.println("✅ 这是一个哈希碰撞！");
            System.out.println("   • 两个不同的字符串");
            System.out.println("   • 有相同的哈希值");
            System.out.println("   • 但 equals() 返回 false");
        } else {
            System.out.println("没有碰撞，让我们找一个真实的例子...\n");

            // 找一个真实的哈希碰撞例子
            String s1 = "BaDcD";
            String s2 = "aE5b8";
            System.out.println("s1 = \"" + s1 + "\"");
            System.out.println("s2 = \"" + s2 + "\"");
            System.out.println("s1.hashCode() = " + s1.hashCode());
            System.out.println("s2.hashCode() = " + s2.hashCode());
            System.out.println("s1.equals(s2) = " + s1.equals(s2));

            if (s1.hashCode() == s2.hashCode()) {
                System.out.println("\n✅ 找到了哈希碰撞！");
                System.out.println("   两个不同的字符串，相同的哈希值");
            }
        }
    }

    // ============ 2. 为什么会发生哈希碰撞？ ============
    private static void demoWhyHashCollision() {
        System.out.println("核心原因：数学上的必然性\n");

        System.out.println("事实 1：哈希值范围是有限的");
        System.out.println("  • hashCode() 返回一个 int 类型");
        System.out.println("  • int 范围：-2^31 到 2^31-1");
        System.out.println("  • 总共大约 42 亿个可能的值\n");

        System.out.println("事实 2：对象的数量是无限的");
        System.out.println("  • 可以创建无限多个对象");
        System.out.println("  • String 可以有无数种组合");
        System.out.println("  • 自定义对象可以有无数种状态\n");

        System.out.println("数学结论：");
        System.out.println("  42 亿个哈希值的空间");
        System.out.println("     vs");
        System.out.println("  无限个对象");
        System.out.println("     =");
        System.out.println("  必然发生哈希碰撞！\n");

        System.out.println("类比：");
        System.out.println("  • 50 个学生，365 个教室");
        System.out.println("  • 很少有学生在同一个教室（很少碰撞）");
        System.out.println("  • 500 个学生，365 个教室");
        System.out.println("  • 很多学生在同一个教室（频繁碰撞）");
        System.out.println("  • 无限个学生，365 个教室");
        System.out.println("  • 每个教室都有很多学生（大量碰撞）");
    }

    // ============ 3. 鸽笼原理 ============
    private static void demoPigeonholePrinciple() {
        System.out.println("鸽笼原理（Pigeonhole Principle）\n");

        System.out.println("如果有 n+1 个鸽子要放入 n 个鸽笼");
        System.out.println("那么至少有一个鸽笼里有 2 个或以上的鸽子\n");

        System.out.println("应用到哈希：");
        System.out.println("  • 鸽笼 = 哈希值（42 亿个）");
        System.out.println("  • 鸽子 = 对象（无限个）");
        System.out.println("  • 必然有多个不同的对象");
        System.out.println("    落在同一个哈希值上\n");

        System.out.println("生日悖论（Birthday Paradox）：");
        System.out.println("  房间里有 23 个人");
        System.out.println("  两个人生日相同的概率 > 50%\n");

        System.out.println("应用到哈希：");
        System.out.println("  哈希表大小为 N 时");
        System.out.println("  只需要大约 √N 个对象");
        System.out.println("  就有 50% 的概率发生碰撞");
        System.out.println("  例如：N = 2^32，√N ≈ 65536");
        System.out.println("  只需要大约 6.5 万个对象就可能发生碰撞");
    }

    // ============ 4. Java 中的实际例子 ============
    private static void demoRealHashCollisionInJava() {
        System.out.println("自定义类的哈希碰撞例子：\n");

        // 创建两个不同的 Student 对象
        Student s1 = new Student("Alice", 20, "001");
        Student s2 = new Student("Alice", 20, "001");
        Student s3 = new Student("Bob", 21, "002");

        System.out.println("定义 Student 类，hashCode() 只基于名字：\n");
        System.out.println("s1 = new Student(\"Alice\", 20, \"001\")");
        System.out.println("s2 = new Student(\"Alice\", 20, \"001\")");
        System.out.println("s3 = new Student(\"Bob\", 21, \"002\")\n");

        System.out.println("结果：");
        System.out.println("s1.hashCode() = " + s1.hashCode());
        System.out.println("s2.hashCode() = " + s2.hashCode());
        System.out.println("s3.hashCode() = " + s3.hashCode());
        System.out.println("s1.equals(s2) = " + s1.equals(s2));
        System.out.println("s1.equals(s3) = " + s1.equals(s3) + "\n");

        System.out.println("分析：");
        System.out.println("s1 和 s2：");
        System.out.println("  • hashCode() 相同（都是 Alice 的哈希值）");
        System.out.println("  • equals() 也相同（内容相同）");
        System.out.println("  • 完全相等，没有问题 ✅\n");

        System.out.println("现在让我们定义一个有问题的 hashCode()：");
        System.out.println("只基于年龄的哈希碰撞例子：\n");

        StudentBadHash s4 = new StudentBadHash("Alice", 20, "001");
        StudentBadHash s5 = new StudentBadHash("Bob", 20, "002");
        StudentBadHash s6 = new StudentBadHash("Charlie", 21, "003");

        System.out.println("s4 = new StudentBadHash(\"Alice\", 20, \"001\")");
        System.out.println("s5 = new StudentBadHash(\"Bob\", 20, \"002\")");
        System.out.println("s6 = new StudentBadHash(\"Charlie\", 21, \"003\")\n");

        System.out.println("结果：");
        System.out.println("s4.hashCode() = " + s4.hashCode() + " (年龄: 20)");
        System.out.println("s5.hashCode() = " + s5.hashCode() + " (年龄: 20)");
        System.out.println("s6.hashCode() = " + s6.hashCode() + " (年龄: 21)");
        System.out.println("s4.equals(s5) = " + s4.equals(s5) + "\n");

        System.out.println("✅ 这是一个完美的哈希碰撞例子：");
        System.out.println("  s4 和 s5 有相同的 hashCode（20）");
        System.out.println("  但 s4.equals(s5) = false（不同的学生）");
        System.out.println("  这是完全合法的 ✅");
    }

    // ============ 5. HashMap 如何处理碰撞 ============
    private static void demoHowHashMapHandlesCollision() {
        System.out.println("HashMap 处理哈希碰撞的方式：链表法（Chaining）\n");

        Map<StudentBadHash, String> map = new HashMap<>();

        StudentBadHash s1 = new StudentBadHash("Alice", 20, "001");
        StudentBadHash s2 = new StudentBadHash("Bob", 20, "002");

        System.out.println("创建两个 hashCode 相同但 equals 不同的对象：");
        System.out.println("s1 = new StudentBadHash(\"Alice\", 20, \"001\")");
        System.out.println("s2 = new StudentBadHash(\"Bob\", 20, \"002\")");
        System.out.println("s1.hashCode() = " + s1.hashCode());
        System.out.println("s2.hashCode() = " + s2.hashCode());
        System.out.println("s1.equals(s2) = " + s1.equals(s2) + "\n");

        System.out.println("将两个对象都放入 HashMap：");
        map.put(s1, "Engineer");
        map.put(s2, "Designer");
        System.out.println("map.put(s1, \"Engineer\");");
        System.out.println("map.put(s2, \"Designer\");");
        System.out.println("map.size() = " + map.size() + "\n");

        System.out.println("查询：");
        System.out.println("map.get(s1) = " + map.get(s1));
        System.out.println("map.get(s2) = " + map.get(s2) + "\n");

        System.out.println("工作原理：");
        System.out.println("1. 计算 s1 的 hashCode = 20");
        System.out.println("   → 找到哈希桶 20");
        System.out.println("   → 用链表存储该桶中的所有对象");
        System.out.println("   → 链表: [s1 -> value=\"Engineer\"]\n");

        System.out.println("2. 计算 s2 的 hashCode = 20");
        System.out.println("   → 也找到哈希桶 20");
        System.out.println("   → 检查链表中是否有 equals() 相等的对象");
        System.out.println("   → 没有找到，添加到链表");
        System.out.println("   → 链表: [s1 -> \"Engineer\"] → [s2 -> \"Designer\"]\n");

        System.out.println("3. 查询 map.get(s2)：");
        System.out.println("   → 计算 s2 的 hashCode = 20");
        System.out.println("   → 在哈希桶 20 的链表中查找");
        System.out.println("   → 遍历链表，用 equals() 比较每个对象");
        System.out.println("   → 找到 s2 equals 相等的对象");
        System.out.println("   → 返回对应的值 \"Designer\" ✅");
    }
}

// ============ 学生类（基于名字的 hashCode）============
class Student {
    private String name;
    private int age;
    private String studentId;

    public Student(String name, int age, String studentId) {
        this.name = name;
        this.age = age;
        this.studentId = studentId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Student)) return false;
        Student other = (Student) obj;
        return name.equals(other.name) &&
               age == other.age &&
               studentId.equals(other.studentId);
    }

    @Override
    public int hashCode() {
        // 基于名字的 hashCode
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Student{" + name + ", " + age + ", " + studentId + "}";
    }
}

// ============ 学生类（基于年龄的 hashCode - 容易碰撞）============
class StudentBadHash {
    private String name;
    private int age;
    private String studentId;

    public StudentBadHash(String name, int age, String studentId) {
        this.name = name;
        this.age = age;
        this.studentId = studentId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StudentBadHash)) return false;
        StudentBadHash other = (StudentBadHash) obj;
        return name.equals(other.name) &&
               age == other.age &&
               studentId.equals(other.studentId);
    }

    @Override
    public int hashCode() {
        // 只基于年龄的 hashCode - 容易碰撞！
        // 所有 20 岁的学生有相同的哈希值
        return age;  // ⚠️ 这是个坏的 hashCode 实现
    }

    @Override
    public String toString() {
        return "Student{" + name + ", " + age + ", " + studentId + "}";
    }
}

