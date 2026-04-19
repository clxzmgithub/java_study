package org.example.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * equals() 和 hashCode() 的关系
 *
 * 重要规则：
 * 1. 如果两个对象 equals()，它们的 hashCode() 必须相同
 * 2. 如果两个对象 hashCode() 相同，它们 equals() 不一定相同（哈希碰撞）
 * 3. 重写 equals() 就必须重写 hashCode()
 */
public class EqualsAndHashCodeDemo {
    public static void main(String[] args) {
        System.out.println("========== equals() 和 hashCode() 的关系 ==========\n");

        // 1. 基本概念
        System.out.println("【1. 为什么需要 hashCode()？】");
        demoWhyNeedHashCode();

        // 2. equals 和 hashCode 的关系
        System.out.println("\n【2. equals() 和 hashCode() 的关系】");
        demoRelationship();

        // 3. 实际问题：不重写 hashCode
        System.out.println("\n【3. 不重写 hashCode() 的问题】");
        demoProblemWithoutHashCode();

        // 4. 正确的实现
        System.out.println("\n【4. 正确的实现】");
        demoCorrectImplementation();

        // 5. 在 HashMap 和 HashSet 中的应用
        System.out.println("\n【5. 在 HashMap 和 HashSet 中的应用】");
        demoInHashCollections();
    }

    // ============ 1. 为什么需要 hashCode()？ ============
    private static void demoWhyNeedHashCode() {
        System.out.println("hashCode() 的作用：");
        System.out.println("  • 将对象转换为一个整数（哈希值）");
        System.out.println("  • 用于快速定位对象在集合中的位置");
        System.out.println("  • 提高集合的查找性能\n");

        System.out.println("例子：");
        String str = "Hello";
        System.out.println("  str.hashCode() = " + str.hashCode());
        System.out.println("  (这是 String 对象 'Hello' 的哈希值)\n");

        System.out.println("Java 集合中的使用：");
        System.out.println("  • HashMap：用 hashCode() 决定存放位置");
        System.out.println("  • HashSet：用 hashCode() 检查重复");
        System.out.println("  • Hashtable：用 hashCode() 存储数据");
    }

    // ============ 2. equals 和 hashCode 的关系 ============
    private static void demoRelationship() {
        System.out.println("Java 规范规定：\n");

        System.out.println("规则 1：如果 a.equals(b) 为 true");
        System.out.println("       那么 a.hashCode() 必须等于 b.hashCode()\n");

        System.out.println("       例子：");
        System.out.println("       String a = \"Hello\";");
        System.out.println("       String b = \"Hello\";");
        System.out.println("       a.equals(b) = true");
        System.out.println("       a.hashCode() == b.hashCode() = true (都是 " + "Hello".hashCode() + ")\n");

        System.out.println("规则 2：如果 a.hashCode() == b.hashCode()");
        System.out.println("       a.equals(b) 不一定为 true（哈希碰撞）\n");

        System.out.println("       例子：");
        System.out.println("       对象 A 和对象 B 的哈希值都是 12345");
        System.out.println("       但它们的 equals() 可能返回 false\n");

        System.out.println("规则 3：重写 equals() 就必须重写 hashCode()");
        System.out.println("       否则会违反规则 1");
    }

    // ============ 3. 不重写 hashCode 的问题 ============
    private static void demoProblemWithoutHashCode() {
        System.out.println("问题演示：\n");

        System.out.println("定义一个 Person 类（只重写 equals，不重写 hashCode）：\n");

        // 创建两个相等的 Person 对象
        PersonWithoutHashCode p1 = new PersonWithoutHashCode("Alice", 25);
        PersonWithoutHashCode p2 = new PersonWithoutHashCode("Alice", 25);

        System.out.println("p1 = new PersonWithoutHashCode(\"Alice\", 25)");
        System.out.println("p2 = new PersonWithoutHashCode(\"Alice\", 25)\n");

        System.out.println("p1.equals(p2) = " + p1.equals(p2) + " ✅ (相等)");
        System.out.println("p1.hashCode() = " + p1.hashCode());
        System.out.println("p2.hashCode() = " + p2.hashCode());
        System.out.println("p1.hashCode() == p2.hashCode() = " + (p1.hashCode() == p2.hashCode()) + " ❌ (不相等)\n");

        System.out.println("问题：");
        System.out.println("  ❌ 两个对象 equals() 返回 true");
        System.out.println("  ❌ 但 hashCode() 不相同");
        System.out.println("  ❌ 这违反了 Java 的规范！\n");

        System.out.println("在 HashSet 中会出现问题：");
        Set<PersonWithoutHashCode> set = new HashSet<>();
        set.add(p1);
        set.add(p2);
        System.out.println("  set.add(p1);");
        System.out.println("  set.add(p2);");
        System.out.println("  set.size() = " + set.size() + " ❌ (应该是 1，但现在是 2)");
        System.out.println("  (虽然 p1.equals(p2)，但它们被视为不同的对象)\n");
    }

    // ============ 4. 正确的实现 ============
    private static void demoCorrectImplementation() {
        System.out.println("正确方法：同时重写 equals() 和 hashCode()\n");

        // 创建两个相等的 Person 对象
        PersonWithHashCode p1 = new PersonWithHashCode("Bob", 30);
        PersonWithHashCode p2 = new PersonWithHashCode("Bob", 30);

        System.out.println("p1 = new PersonWithHashCode(\"Bob\", 30)");
        System.out.println("p2 = new PersonWithHashCode(\"Bob\", 30)\n");

        System.out.println("p1.equals(p2) = " + p1.equals(p2) + " ✅");
        System.out.println("p1.hashCode() = " + p1.hashCode());
        System.out.println("p2.hashCode() = " + p2.hashCode());
        System.out.println("p1.hashCode() == p2.hashCode() = " + (p1.hashCode() == p2.hashCode()) + " ✅\n");

        System.out.println("在 HashSet 中工作正常：");
        Set<PersonWithHashCode> set = new HashSet<>();
        set.add(p1);
        set.add(p2);
        System.out.println("  set.add(p1);");
        System.out.println("  set.add(p2);");
        System.out.println("  set.size() = " + set.size() + " ✅ (正确是 1)\n");
    }

    // ============ 5. 在 HashMap 和 HashSet 中的应用 ============
    private static void demoInHashCollections() {
        System.out.println("--- HashMap 示例 ---\n");

        Map<PersonWithHashCode, String> map = new HashMap<>();

        PersonWithHashCode person1 = new PersonWithHashCode("Charlie", 28);
        PersonWithHashCode person2 = new PersonWithHashCode("Charlie", 28);

        System.out.println("创建两个相等的对象：");
        System.out.println("  person1 = new PersonWithHashCode(\"Charlie\", 28)");
        System.out.println("  person2 = new PersonWithHashCode(\"Charlie\", 28)");
        System.out.println("  person1.equals(person2) = " + person1.equals(person2) + "\n");

        System.out.println("将 person1 作为 key 存入 HashMap：");
        map.put(person1, "Engineer");
        System.out.println("  map.put(person1, \"Engineer\");");
        System.out.println("  map.size() = " + map.size() + "\n");

        System.out.println("用 person2 查找（虽然是不同的对象，但 equals() 相等）：");
        String value = map.get(person2);
        System.out.println("  map.get(person2) = " + value + " ✅");
        System.out.println("  (因为 hashCode() 相同，所以能找到)\n");

        System.out.println("--- HashSet 示例 ---\n");

        Set<PersonWithHashCode> set = new HashSet<>();

        PersonWithHashCode p1 = new PersonWithHashCode("Diana", 26);
        PersonWithHashCode p2 = new PersonWithHashCode("Diana", 26);
        PersonWithHashCode p3 = new PersonWithHashCode("Diana", 26);

        System.out.println("创建三个相等的对象：");
        System.out.println("  p1 = new PersonWithHashCode(\"Diana\", 26)");
        System.out.println("  p2 = new PersonWithHashCode(\"Diana\", 26)");
        System.out.println("  p3 = new PersonWithHashCode(\"Diana\", 26)\n");

        set.add(p1);
        set.add(p2);
        set.add(p3);

        System.out.println("set.add(p1);");
        System.out.println("set.add(p2);");
        System.out.println("set.add(p3);");
        System.out.println("set.size() = " + set.size() + " ✅ (只有 1 个元素，其他被去重了)\n");

        System.out.println("set 中包含 p2？");
        System.out.println("set.contains(p2) = " + set.contains(p2) + " ✅\n");
    }
}

// ============ 没有重写 hashCode 的实现 ============
class PersonWithoutHashCode {
    private String name;
    private int age;

    public PersonWithoutHashCode(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PersonWithoutHashCode)) {
            return false;
        }
        PersonWithoutHashCode other = (PersonWithoutHashCode) obj;
        return this.name.equals(other.name) && this.age == other.age;
    }

    // ❌ 没有重写 hashCode()
    // 会使用 Object 默认的 hashCode()，基于对象的内存地址
}

// ============ 正确的实现 ============
class PersonWithHashCode {
    private String name;
    private int age;

    public PersonWithHashCode(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PersonWithHashCode)) {
            return false;
        }
        PersonWithHashCode other = (PersonWithHashCode) obj;
        return this.name.equals(other.name) && this.age == other.age;
    }

    @Override
    public int hashCode() {
        // ✅ 重写 hashCode()
        // 基于相同的属性生成哈希值
        return name.hashCode() * 31 + age;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}

