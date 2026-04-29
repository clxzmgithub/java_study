package org.example.basetest;

/**
 * 静态方法 vs 非静态成员的深度分析
 * 解释为什么静态方法不能调用非静态成员
 */
public class StaticVsNonStatic {
    public static void main(String[] args) {
        System.out.println("========== 静态方法 vs 非静态成员 ==========\n");

        // 1. 内存模型理解
        System.out.println("【1. 内存模型：为什么不能调用】");
        demoMemoryModel();

        // 2. 具体的编译错误示例
        System.out.println("\n【2. 编译错误示例】");
        demoCompilationError();

        // 3. 正确的调用方式
        System.out.println("\n【3. 正确的调用方式】");
        demoCorrectWays();

        // 4. 实际场景对比
        System.out.println("\n【4. 实际应用场景】");
        demoRealScenario();
    }

    // ============ 1. 内存模型 ============
    private static void demoMemoryModel() {
        System.out.println("--- 内存分配位置不同 ---\n");

        System.out.println("当类被加载时：\n");

        System.out.println("JVM 内存布局：");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│          方法区（Method Area）      │");
        System.out.println("│ ┌──────────────────────────────────┐│");
        System.out.println("│ │ 静态变量（类变量）                  ││");
        System.out.println("│ │   ✓ className (一份，所有对象共享) ││");
        System.out.println("│ │   ✓ classVersion (一份，所有对象共享)││");
        System.out.println("│ └──────────────────────────────────┘│");
        System.out.println("│ ┌──────────────────────────────────┐│");
        System.out.println("│ │ 静态方法（类方法）                  ││");
        System.out.println("│ │   ✓ staticMethod() (一份)         ││");
        System.out.println("│ └──────────────────────────────────┘│");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println("           ↑ 类装载时就存在");
        System.out.println("           ↑ 不依赖任何对象\n");

        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│          堆内存（Heap）             │");
        System.out.println("│ 对象 1          对象 2         对象 3  │");
        System.out.println("│ ┌──────┐   ┌──────┐    ┌──────┐    │");
        System.out.println("│ │name  │   │name  │    │name  │    │");
        System.out.println("│ │age   │   │age   │    │age   │    │");
        System.out.println("│ │email │   │email │    │email │    │");
        System.out.println("│ └──────┘   └──────┘    └──────┘    │");
        System.out.println("│ （非静态成员）                       │");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println("           ↑ new 对象时才存在");
        System.out.println("           ↑ 每个对象一份\n");

        System.out.println("关键点：");
        System.out.println("  • 静态方法在类装载时就存在");
        System.out.println("  • 静态方法与具体对象无关");
        System.out.println("  • 非静态成员只有在创建对象时才存在");
        System.out.println("  • 静态方法执行时，可能还没有任何对象被创建\n");
    }

    // ============ 2. 编译错误示例 ============
    private static void demoCompilationError() {
        System.out.println("--- 代码例子 ---\n");

        System.out.println("定义一个 Person 类：\n");
        System.out.println("class Person {");
        System.out.println("    // 非静态成员变量");
        System.out.println("    private String name;");
        System.out.println("    private int age;\n");

        System.out.println("    // 非静态成员方法");
        System.out.println("    public void setName(String name) {");
        System.out.println("        this.name = name;");
        System.out.println("    }\n");

        System.out.println("    // 静态方法");
        System.out.println("    public static void printInfo() {");
        System.out.println("        // ❌ 编译错误：不能访问非静态成员");
        System.out.println("        System.out.println(this.name);      // ← 错误！");
        System.out.println("        System.out.println(age);            // ← 错误！");
        System.out.println("        setName(\"Tom\");                    // ← 错误！");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("为什么会报错？");
        System.out.println("  • 静态方法中没有 this 指针（代表对象）");
        System.out.println("  • name 和 age 属于对象，不属于类");
        System.out.println("  • setName() 是非静态方法，需要对象才能调用");
        System.out.println("  • 静态方法执行时，可能没有任何 Person 对象存在\n");

        System.out.println("类比：");
        System.out.println("  • 静态方法 = 工厂总部的公共电话");
        System.out.println("    → 任何人都能拨打（不需要创建工厂实例）");
        System.out.println("    → 不能获取具体员工的信息");
        System.out.println("");
        System.out.println("  • 非静态成员 = 某个员工的工号");
        System.out.println("    → 只有创建了员工才有工号");
        System.out.println("    → 不能在公共电话（静态方法）里查询\n");
    }

    // ============ 3. 正确的调用方式 ============
    private static void demoCorrectWays() {
        System.out.println("--- 正确方式 1：非静态方法调用非静态成员 ---\n");

        System.out.println("class Person {");
        System.out.println("    private String name;");
        System.out.println("    private int age;\n");

        System.out.println("    // 非静态方法（有 this）");
        System.out.println("    public void setName(String name) {");
        System.out.println("        this.name = name;  // ✅ 可以");
        System.out.println("    }\n");

        System.out.println("    public void printInfo() {");
        System.out.println("        System.out.println(name);  // ✅ 可以");
        System.out.println("        System.out.println(age);    // ✅ 可以");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("为什么可以？");
        System.out.println("  • 非静态方法有 this 指针");
        System.out.println("  • this 代表调用这个方法的对象");
        System.out.println("  • 可以通过 this 访问该对象的所有成员\n");

        System.out.println("使用：");
        System.out.println("Person person = new Person();");
        System.out.println("person.setName(\"Alice\");");
        System.out.println("person.printInfo();  // ✅ 正常输出\n");

        System.out.println("--- 正确方式 2：静态方法调用静态成员 ---\n");

        System.out.println("class Person {");
        System.out.println("    // 静态成员");
        System.out.println("    private static int totalCount = 0;");
        System.out.println("    private static String species = \"Human\";\n");

        System.out.println("    // 静态方法");
        System.out.println("    public static void printSpecies() {");
        System.out.println("        System.out.println(species);      // ✅ 可以");
        System.out.println("        System.out.println(totalCount);   // ✅ 可以");
        System.out.println("    }\n");

        System.out.println("    public static int getTotalCount() {");
        System.out.println("        return totalCount;  // ✅ 可以");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("为什么可以？");
        System.out.println("  • 静态成员和静态方法都属于类");
        System.out.println("  • 都在方法区，都不需要对象");
        System.out.println("  • 可以直接访问\n");

        System.out.println("使用：");
        System.out.println("Person.printSpecies();   // ✅ 无需创建对象");
        System.out.println("int count = Person.getTotalCount();  // ✅ 无需创建对象\n");

        System.out.println("--- 正确方式 3：静态方法通过对象访问非静态成员 ---\n");

        System.out.println("class Person {");
        System.out.println("    private String name;");
        System.out.println("    private int age;\n");

        System.out.println("    // 静态方法中通过对象参数访问非静态成员");
        System.out.println("    public static void printPersonInfo(Person person) {");
        System.out.println("        // ✅ 可以（但需要先有对象）");
        System.out.println("        System.out.println(person.name);");
        System.out.println("        System.out.println(person.age);");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("为什么可以？");
        System.out.println("  • 通过参数传入了对象");
        System.out.println("  • 现在有了具体的对象引用");
        System.out.println("  • 可以访问该对象的成员\n");

        System.out.println("使用：");
        System.out.println("Person person = new Person();");
        System.out.println("Person.printPersonInfo(person);  // ✅ 通过参数传入对象\n");
    }

    // ============ 4. 实际应用场景 ============
    private static void demoRealScenario() {
        System.out.println("--- 实际场景 1：工具类（全是静态方法）---\n");

        System.out.println("class MathUtils {");
        System.out.println("    // 这些都是静态方法，可以直接调用");
        System.out.println("    public static int add(int a, int b) {");
        System.out.println("        return a + b;  // ✅ 只使用参数");
        System.out.println("    }\n");

        System.out.println("    public static int multiply(int a, int b) {");
        System.out.println("        return a * b;  // ✅ 只使用参数");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("使用：");
        System.out.println("int result = MathUtils.add(5, 3);  // ✅ 无需创建对象\n");

        System.out.println("--- 实际场景 2：业务类（需要对象状态）---\n");

        System.out.println("class ShoppingCart {");
        System.out.println("    private List<Item> items;  // 非静态成员");
        System.out.println("    private double totalPrice;  // 非静态成员\n");

        System.out.println("    // ❌ 错误：静态方法不能访问非静态成员");
        System.out.println("    public static void checkout() {");
        System.out.println("        // System.out.println(totalPrice);  // ← 错误！");
        System.out.println("        // items.stream().forEach(...);     // ← 错误！");
        System.out.println("    }\n");

        System.out.println("    // ✅ 正确：非静态方法可以访问");
        System.out.println("    public void checkout() {");
        System.out.println("        System.out.println(totalPrice);  // ✅ 可以");
        System.out.println("        items.stream().forEach(...);     // ✅ 可以");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("为什么？");
        System.out.println("  • 购物车的信息（items、totalPrice）属于具体的购物车对象");
        System.out.println("  • 不能在静态方法中使用");
        System.out.println("  • 必须是非静态方法才能访问对象的信息\n");

        System.out.println("--- 实际场景 3：main 方法是静态的 ---\n");

        System.out.println("public class Main {");
        System.out.println("    private String title = \"程序\";  // 非静态\n");

        System.out.println("    // ❌ 这里为什么可以直接运行？");
        System.out.println("    public static void main(String[] args) {");
        System.out.println("        // System.out.println(title);  // ← 错误！");
        System.out.println("        // 因为 main 是静态方法，没有对象！\n");

        System.out.println("        // 正确方式：创建对象后使用");
        System.out.println("        Main app = new Main();");
        System.out.println("        System.out.println(app.title);  // ✅");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("总结：");
        System.out.println("  • main 是静态方法，JVM 启动时直接调用");
        System.out.println("  • 此时还没有 Main 对象");
        System.out.println("  • 所以不能访问非静态成员");
        System.out.println("  • 但可以在 main 中创建对象后使用\n");
    }
}

