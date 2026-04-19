package org.example.test;

import java.util.Iterator;

/**
 * Java 静态内部类 vs 非静态内部类（成员内部类）对比详解
 *
 * 内部类的四种类型：
 * 1. 成员内部类（非静态）  - non-static inner class
 * 2. 静态内部类           - static nested class
 * 3. 局部内部类           - local inner class（方法内部定义）
 * 4. 匿名内部类           - anonymous inner class
 *
 * 本文重点对比：静态内部类 vs 非静态内部类（成员内部类）
 */
public class InnerClassDemo {
    public static void main(String[] args) {
        System.out.println("========== 静态内部类 vs 非静态内部类（成员内部类）==========\n");

        // 1. 基本区别
        System.out.println("【1. 基本区别】");
        demoBasicDifference();

        // 2. 访问外部类成员的区别
        System.out.println("\n【2. 访问外部类成员的区别】");
        demoAccessOuterMembers();

        // 3. 实例化方式的区别
        System.out.println("\n【3. 实例化方式的区别】");
        demoInstantiation();

        // 4. 内存与生命周期的区别
        System.out.println("\n【4. 内存与生命周期的区别】");
        demoMemoryAndLifecycle();

        // 5. 实际应用场景
        System.out.println("\n【5. 实际应用场景】");
        demoRealWorldUsage();

        // 6. Builder 用静态内部类 vs 不用的对比
        System.out.println("\n【6. Builder：用静态内部类 vs 不用的对比】");
        demoBuilderComparison();

        // 7. 迭代器用非静态内部类 vs 不用的对比
        System.out.println("\n【7. 迭代器：用非静态内部类 vs 不用的对比】");
        demoIteratorComparison();
    }

    // ====================================================
    // 【1. 基本区别】
    // ====================================================
    private static void demoBasicDifference() {
        System.out.println("静态内部类：有 static 关键字修饰");
        System.out.println("  static class StaticInner { ... }\n");

        System.out.println("非静态内部类（成员内部类）：没有 static 关键字");
        System.out.println("  class NonStaticInner { ... }\n");

        System.out.println("关键区别总结：");
        System.out.println("┌─────────────────────┬──────────────────────────┬──────────────────────────┐");
        System.out.println("│ 特性                │ 静态内部类               │ 非静态内部类             │");
        System.out.println("├─────────────────────┼──────────────────────────┼──────────────────────────┤");
        System.out.println("│ 关键字              │ static class             │ class（无 static）       │");
        System.out.println("│ 是否持有外部类引用  │ ❌ 不持有                │ ✅ 持有（隐式）          │");
        System.out.println("│ 访问外部类静态成员  │ ✅ 可以                  │ ✅ 可以                  │");
        System.out.println("│ 访问外部类非静态成员│ ❌ 不能直接访问          │ ✅ 可以直接访问          │");
        System.out.println("│ 实例化是否需要外部类│ ❌ 不需要                │ ✅ 需要（先创建外部类）  │");
        System.out.println("│ 内存开销            │ 较小                     │ 较大（持有外部类引用）   │");
        System.out.println("│ 内存泄漏风险        │ ❌ 无                    │ ⚠️ 有（外部类无法被GC） │");
        System.out.println("└─────────────────────┴──────────────────────────┴──────────────────────────┘");
    }

    // ====================================================
    // 【2. 访问外部类成员的区别】
    // ====================================================
    private static void demoAccessOuterMembers() {
        System.out.println("-- 静态内部类只能访问外部类的静态成员 --");

        // 演示静态内部类访问
        Outer.StaticInner staticInner = new Outer.StaticInner();
        staticInner.showAccess();

        System.out.println("\n-- 非静态内部类可以访问外部类的所有成员（包括非静态）--");

        // 演示非静态内部类访问（必须先创建外部类实例）
        Outer outer = new Outer("外部类实例", 100);
        Outer.NonStaticInner nonStaticInner = outer.new NonStaticInner();
        nonStaticInner.showAccess();
    }

    // ====================================================
    // 【3. 实例化方式的区别】
    // ====================================================
    private static void demoInstantiation() {
        System.out.println("-- 静态内部类：直接实例化，不需要外部类对象 --");
        System.out.println("  语法：OuterClass.StaticInner obj = new OuterClass.StaticInner();");
        // ✅ 直接 new，不需要 Outer 实例
        Outer.StaticInner si = new Outer.StaticInner();
        System.out.println("  创建成功：" + si + "\n");

        System.out.println("-- 非静态内部类：必须通过外部类实例来实例化 --");
        System.out.println("  语法：OuterClass outer = new OuterClass();");
        System.out.println("         OuterClass.InnerClass obj = outer.new InnerClass();");
        // ✅ 必须先有 Outer 的实例
        Outer outer = new Outer("Tom", 25);
        Outer.NonStaticInner nsi = outer.new NonStaticInner();
        System.out.println("  创建成功：" + nsi);

        System.out.println("\n  ❌ 不能直接 new 非静态内部类：");
        System.out.println("     // Outer.NonStaticInner err = new Outer.NonStaticInner(); // 编译错误！");
        System.out.println("     原因：非静态内部类持有外部类的引用，没有外部类实例，内部类无法存在");
    }

    // ====================================================
    // 【4. 内存与生命周期的区别】
    // ====================================================
    private static void demoMemoryAndLifecycle() {
        System.out.println("-- 非静态内部类持有外部类的隐式引用 --\n");
        System.out.println("  非静态内部类的内存模型：");
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │  NonStaticInner 对象                    │");
        System.out.println("  │  ┌─────────────────────────────────┐    │");
        System.out.println("  │  │  this$0 ──────────────────────► │  Outer 对象（隐式引用）");
        System.out.println("  │  └─────────────────────────────────┘    │");
        System.out.println("  │  + 内部类自己的字段                     │");
        System.out.println("  └─────────────────────────────────────────┘");

        System.out.println("\n  ⚠️ 内存泄漏风险：");
        System.out.println("  如果非静态内部类对象的生命周期 > 外部类对象");
        System.out.println("  即使外部类对象不再使用，GC 也无法回收它");
        System.out.println("  因为非静态内部类持有其引用\n");

        System.out.println("-- 静态内部类不持有外部类引用 --\n");
        System.out.println("  静态内部类的内存模型：");
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │  StaticInner 对象                       │");
        System.out.println("  │  + 内部类自己的字段（仅此而已）         │");
        System.out.println("  │  不持有任何外部类引用 ✅                │");
        System.out.println("  └─────────────────────────────────────────┘");

        System.out.println("\n  ✅ 无内存泄漏风险：");
        System.out.println("  外部类对象和静态内部类对象是相互独立的");
        System.out.println("  各自的生命周期互不影响");

        // 实际演示
        System.out.println("\n  实际演示：");
        Outer outer = new Outer("测试用户", 30);
        Outer.NonStaticInner nsi = outer.new NonStaticInner();
        nsi.showOuterReference();

        Outer.StaticInner si = new Outer.StaticInner();
        si.showNoOuterReference();
    }

    // ====================================================
    // 【5. 实际应用场景】
    // ====================================================
    private static void demoRealWorldUsage() {
        System.out.println("-- 场景 1：Builder 模式（静态内部类的典型用法）--\n");

        // Builder 模式的典型用法（如 Android 的 AlertDialog.Builder）
        Computer computer = new Computer.Builder()
                .cpu("Intel i9")
                .memory("32GB")
                .storage("1TB SSD")
                .build();

        System.out.println("  构建的电脑: " + computer);

        System.out.println("\n-- 场景 2：迭代器模式（非静态内部类的典型用法）--\n");

        // 自定义简单集合，演示非静态内部类作为迭代器
        SimpleList list = new SimpleList();
        list.add("苹果");
        list.add("香蕉");
        list.add("橙子");

        System.out.print("  遍历 SimpleList：");
        for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
            System.out.print(it.next() + " ");
        }
        System.out.println();

        System.out.println("\n-- 为什么 Builder 用静态内部类？--");
        System.out.println("  • Builder 需要独立存在，不依赖外部类实例");
        System.out.println("  • 用户在外部类实例创建之前就要使用 Builder");
        System.out.println("  • 不需要访问外部类的非静态成员");

        System.out.println("\n-- 为什么迭代器用非静态内部类？--");
        System.out.println("  • 迭代器需要访问集合的数据（外部类的非静态字段）");
        System.out.println("  • 迭代器的生命周期与集合对象绑定");
        System.out.println("  • 需要共享外部类的状态（数组、指针等）");
    }

    // ====================================================
    // 【6. Builder：用静态内部类 vs 不用的对比】
    // ====================================================
    private static void demoBuilderComparison() {
        System.out.println("-- 方式 A：使用静态内部类 Builder（推荐）--\n");
        Computer c1 = new Computer.Builder()
                .cpu("Intel i9")
                .memory("32GB")
                .storage("1TB SSD")
                .build();
        System.out.println("  结果: " + c1);

        System.out.println("\n-- 方式 B：不用内部类，单独写一个 ComputerBuilder 外部类 --\n");
        ComputerBuilder cb = new ComputerBuilder();
        Computer2 c2 = cb.cpu("Intel i7").memory("16GB").storage("512GB SSD").build();
        System.out.println("  结果: " + c2);

        System.out.println("\n-- 方式 C：不用 Builder，直接用构造器传参 --\n");
        // 当字段很多时，调用方完全看不出每个参数的含义
        Computer3 c3 = new Computer3("Intel i5", "8GB", "256GB SSD", "NVIDIA RTX", "Windows 11", "1920x1080");
        System.out.println("  结果: " + c3);
        System.out.println("  ⚠️ 你能一眼看出第3个参数是什么吗？很难！");

        System.out.println("\n-- 对比总结 --");
        System.out.println("  方式A（静态内部类Builder）:");
        System.out.println("    ✅ Computer.Builder 语义清晰，Builder 在 Computer 命名空间下");
        System.out.println("    ✅ Computer 的构造器可以是 private，外界只能通过 Builder 创建");
        System.out.println("    ✅ Builder 类与 Computer 类封装在一起，代码内聚性强");
        System.out.println("    ✅ 不会污染包的命名空间（不会多出一个 ComputerBuilder.java）");
        System.out.println("    ✅ build() 里可以访问 Computer 的 private 构造器");
        System.out.println();
        System.out.println("  方式B（单独的外部类Builder）:");
        System.out.println("    ⚠️ 需要额外一个 ComputerBuilder.java 文件，类散落在不同文件");
        System.out.println("    ⚠️ Computer 的构造器必须是 package-private 或 public（无法 private）");
        System.out.println("    ⚠️ 包下多出了 ComputerBuilder 这个'散落'的类，命名空间污染");
        System.out.println("    ⚠️ 调用方写 new ComputerBuilder() 而不是 new Computer.Builder()，语义弱");
        System.out.println();
        System.out.println("  方式C（多参数构造器）:");
        System.out.println("    ❌ 参数多时极难阅读，容易传错位置");
        System.out.println("    ❌ 无法设置可选参数（只能加构造器重载，组合爆炸）");
        System.out.println("    ❌ 无法做参数校验链");
        System.out.println();
        System.out.println("  结论：Builder 用静态内部类不是强制的，但这是最优雅的方案。");
        System.out.println("        不用也行，但要付出'代码内聚性差'和'封装性弱'的代价。");
    }

    // ====================================================
    // 【7. 迭代器：用非静态内部类 vs 不用的对比】
    // ====================================================
    private static void demoIteratorComparison() {
        System.out.println("-- 方式 A：用非静态内部类作迭代器（推荐）--\n");
        SimpleList list1 = new SimpleList();
        list1.add("苹果"); list1.add("香蕉"); list1.add("橙子");
        System.out.print("  遍历结果: ");
        for (Iterator<String> it = list1.iterator(); it.hasNext(); ) {
            System.out.print(it.next() + " ");
        }
        System.out.println();

        System.out.println("\n-- 方式 B：用静态内部类作迭代器（要手动传引用）--\n");
        SimpleList2 list2 = new SimpleList2();
        list2.add("猫"); list2.add("狗"); list2.add("鱼");
        System.out.print("  遍历结果: ");
        for (Iterator<String> it = list2.iterator(); it.hasNext(); ) {
            System.out.print(it.next() + " ");
        }
        System.out.println();

        System.out.println("\n-- 方式 C：不用内部类，把迭代器单独写成外部类 --\n");
        SimpleList3 list3 = new SimpleList3();
        list3.add("红"); list3.add("绿"); list3.add("蓝");
        System.out.print("  遍历结果: ");
        // 外部类迭代器必须把集合传进去
        for (SimpleList3Iterator it = new SimpleList3Iterator(list3); it.hasNext(); ) {
            System.out.print(it.next() + " ");
        }
        System.out.println();

        System.out.println("\n-- 对比总结 --");
        System.out.println("  方式A（非静态内部类）:");
        System.out.println("    ✅ 迭代器直接访问集合的 private 字段（data、size），无需暴露任何方法");
        System.out.println("    ✅ 自动持有外部类引用，代码简洁，cursor < size 直接可用");
        System.out.println("    ✅ 封装性最好，data 数组不需要 getter，外部完全看不到");
        System.out.println("    ✅ Java 标准库（ArrayList、LinkedList）全部采用这种方式");
        System.out.println();
        System.out.println("  方式B（静态内部类）:");
        System.out.println("    ⚠️ 静态内部类没有外部类引用，必须在构造器里手动传入集合对象");
        System.out.println("    ⚠️ 仍然需要访问集合的数据，所以集合必须把字段改为 package 级别");
        System.out.println("       或者提供 getData()/getSize() 等方法，破坏封装性");
        System.out.println("    ⚠️ 代码更复杂，优势不明显");
        System.out.println();
        System.out.println("  方式C（外部类迭代器）:");
        System.out.println("    ❌ 集合的 private 字段必须暴露（变成 public 或加 getter）");
        System.out.println("       严重破坏封装性！data 数组被外部随意访问");
        System.out.println("    ❌ 迭代器和集合分散在两个文件，内聚性差");
        System.out.println("    ❌ 调用方需要手动 new SimpleList3Iterator(list)，不符合直觉");
        System.out.println();
        System.out.println("  结论：迭代器用非静态内部类不是强制的，但这是最能保护封装性的方案。");
        System.out.println("        不用也行，但集合的内部数据就必须对外暴露，打破封装原则。");
    }
}

// ====================================================
// 演示用的外部类
// ====================================================
class Outer {
    // 静态字段
    private static String staticField = "外部类静态字段";
    // 非静态字段
    private String name;
    private int age;

    public Outer(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // ============ 静态内部类 ============
    static class StaticInner {

        void showAccess() {
            // ✅ 可以访问外部类的静态字段
            System.out.println("  [静态内部类] 访问外部类静态字段: " + staticField);

            // ❌ 不能访问外部类的非静态字段
//             System.out.println(name);  // 编译错误！
            // System.out.println(age);   // 编译错误！
            System.out.println("  [静态内部类] 无法直接访问 name 和 age（非静态字段）");
        }

        void showNoOuterReference() {
            System.out.println("  [静态内部类] 不持有任何外部类对象的引用");
            System.out.println("  [静态内部类] 外部类对象 = null（不存在）");
        }

        @Override
        public String toString() {
            return "StaticInner{}";
        }
    }

    // ============ 非静态内部类（成员内部类）============
    class NonStaticInner {

        void showAccess() {
            // ✅ 可以访问外部类的静态字段
            System.out.println("  [非静态内部类] 访问外部类静态字段: " + staticField);

            // ✅ 可以访问外部类的非静态字段
            System.out.println("  [非静态内部类] 访问外部类非静态字段 name: " + name);
            System.out.println("  [非静态内部类] 访问外部类非静态字段 age: " + age);

            // ✅ 可以使用 Outer.this 明确引用外部类
            System.out.println("  [非静态内部类] 通过 Outer.this.name: " + Outer.this.name);
        }

        void showOuterReference() {
            // 非静态内部类持有外部类的隐式引用 this$0
            System.out.println("  [非静态内部类] 持有外部类引用，可访问: name=" + name + ", age=" + age);
            System.out.println("  [非静态内部类] 外部类对象的 hashCode: " + Outer.this.hashCode());
        }

        @Override
        public String toString() {
            return "NonStaticInner{持有 Outer(" + name + ", " + age + ") 的引用}";
        }
    }
}

// ====================================================
// 场景 1：Builder 模式（静态内部类）
// ====================================================
class Computer {
    private final String cpu;
    private final String memory;
    private final String storage;

    // private 构造器，只能通过 Builder 创建
    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.memory = builder.memory;
        this.storage = builder.storage;
    }

    // ✅ 静态内部类 Builder
    // 因为在创建 Computer 之前就需要使用 Builder
    // 所以 Builder 必须是静态的，不能依赖 Computer 实例
    static class Builder {
        private String cpu;
        private String memory;
        private String storage;

        public Builder cpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        public Builder memory(String memory) {
            this.memory = memory;
            return this;
        }

        public Builder storage(String storage) {
            this.storage = storage;
            return this;
        }

        // 最终构建 Computer 对象
        public Computer build() {
            return new Computer(this);
        }
    }

    @Override
    public String toString() {
        return "Computer{cpu='" + cpu + "', memory='" + memory + "', storage='" + storage + "'}";
    }
}

// ====================================================
// 场景 2：迭代器模式（非静态内部类）
// ====================================================
class SimpleList {
    // 存储数据的数组
    private String[] data = new String[10];
    private int size = 0;

    public void add(String item) {
        data[size++] = item;
    }

    // 返回一个迭代器
    public Iterator<String> iterator() {
        // 创建非静态内部类的实例
        return new ListIterator();
    }

    // ✅ 非静态内部类 ListIterator
    // 因为迭代器需要直接访问 SimpleList 的 data 和 size 字段
    // 所以必须是非静态的（持有外部类引用）
    class ListIterator implements Iterator<String> {
        private int cursor = 0;  // 当前指针

        @Override
        public boolean hasNext() {
            // ✅ 直接访问外部类的 size 字段
            return cursor < size;
        }

        @Override
        public String next() {
            // ✅ 直接访问外部类的 data 数组
            return data[cursor++];
        }
    }
}

// ====================================================
// 方式 B：单独的外部类 Builder（不用静态内部类）
// ====================================================
class Computer2 {
    // 构造器必须是 package-private，外部 Builder 才能访问
    String cpu;
    String memory;
    String storage;

    Computer2(String cpu, String memory, String storage) {
        this.cpu = cpu;
        this.memory = memory;
        this.storage = storage;
    }

    @Override
    public String toString() {
        return "Computer2{cpu='" + cpu + "', memory='" + memory + "', storage='" + storage + "'}";
    }
}

// 单独的外部 Builder 类（散落在包里，语义不如 Computer.Builder 清晰）
class ComputerBuilder {
    private String cpu;
    private String memory;
    private String storage;

    public ComputerBuilder cpu(String cpu)         { this.cpu = cpu; return this; }
    public ComputerBuilder memory(String memory)   { this.memory = memory; return this; }
    public ComputerBuilder storage(String storage) { this.storage = storage; return this; }

    public Computer2 build() {
        return new Computer2(cpu, memory, storage);
    }
}

// ====================================================
// 方式 C：多参数构造器（不用 Builder）
// ====================================================
class Computer3 {
    private final String cpu;
    private final String memory;
    private final String storage;
    private final String gpu;
    private final String os;
    private final String resolution;

    // ❌ 6个参数，调用方根本看不懂每个参数是什么含义
    public Computer3(String cpu, String memory, String storage,
                     String gpu, String os, String resolution) {
        this.cpu = cpu;
        this.memory = memory;
        this.storage = storage;
        this.gpu = gpu;
        this.os = os;
        this.resolution = resolution;
    }

    @Override
    public String toString() {
        return "Computer3{cpu='" + cpu + "', memory='" + memory + "', storage='" + storage
                + "', gpu='" + gpu + "', os='" + os + "', resolution='" + resolution + "'}";
    }
}

// ====================================================
// 方式 B：用静态内部类作迭代器（需要手动传引用）
// ====================================================
class SimpleList2 {
    // ✅ 静态内部类也能访问外部类的 private 字段（同一个文件内）
    // 但需要通过持有的外部类引用 list.data 来访问，而不是像非静态内部类那样直接写 data
    private String[] data = new String[10];
    private int size = 0;

    public void add(String item) { data[size++] = item; }

    public Iterator<String> iterator() {
        return new StaticListIterator(this);
    }

    // ⚠️ 静态内部类：没有外部类引用，必须手动传入
    static class StaticListIterator implements Iterator<String> {
        private final SimpleList2 list; // 手动持有引用
        private int cursor = 0;

        StaticListIterator(SimpleList2 list) {
            this.list = list;
        }

        @Override
        public boolean hasNext() { return cursor < list.size; }

        @Override
        public String next() { return list.data[cursor++]; }
    }
}

// ====================================================
// 方式 C：迭代器写成独立外部类
// ====================================================
class SimpleList3 {
    // ❌ 字段必须暴露为 public，外部迭代器才能访问，严重破坏封装！
    public String[] data = new String[10];
    public int size = 0;

    public void add(String item) { data[size++] = item; }
}

// 独立的外部迭代器类（和集合散落在不同地方）
class SimpleList3Iterator implements Iterator<String> {
    private final SimpleList3 list;
    private int cursor = 0;

    public SimpleList3Iterator(SimpleList3 list) {
        this.list = list;
    }

    @Override
    public boolean hasNext() { return cursor < list.size; }

    @Override
    // ❌ 直接访问 public 字段，封装性已经被打破
    public String next() { return list.data[cursor++]; }
}

