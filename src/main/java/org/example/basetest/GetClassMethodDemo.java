package org.example.basetest;

/**
 * Object.getClass() 方法深度分析
 * <p>
 * 方法签名：public final native Class<?> getClass()
 * <p>
 * 解释：
 * 1. public - 公开方法，任何地方都可以调用
 * 2. final - 不允许子类重写
 * 3. native - 本地方法，由 C++ 实现
 * 4. Class<?> - 返回 Class 对象
 */
public class GetClassMethodDemo {
    public static void main(String[] args) {
        System.out.println("========== getClass() 方法详解 ==========\n");

        String str = new String("Hello");
        String str1 = new String("Hello");
        System.out.println("str == str1: " + (str.equals(str1))); // true
        GetClassMethodDemo demo = new GetClassMethodDemo();
        System.out.println("【1. 基本概念】demo:" + demo);
        System.out.println("demo hashcode:" + demo.hashCode());
        demoBasicConcept();

        System.out.println("\n【2. native 关键字】");
        demoNativeKeyword();

        System.out.println("\n【3. final 关键字】");
        demoFinalKeyword();

        System.out.println("\n【4. 实际应用】");
        demoRealScenarios();
    }

    private static void demoBasicConcept() {
        System.out.println("getClass() 的作用：返回对象所属类的 Class 对象\n");

        // 例子 1：String
        String str = "Hello";
        Class<?> strClass = str.getClass();
        System.out.println("String 对象：");
        System.out.println("  str = \"" + str + "\"");
        System.out.println("  str.getClass() = " + strClass);
        System.out.println("  str.getClass().getName() = " + strClass.getName());

        // 例子 2：Integer
        Integer num = 42;
        Class<?> numClass = num.getClass();
        System.out.println("\nInteger 对象：");
        System.out.println("  num = " + num);
        System.out.println("  num.getClass() = " + numClass);
        System.out.println("  num.getClass().getName() = " + numClass.getName());

        // 例子 3：数组
        int[] arr = {1, 2, 3};
        Class<?> arrClass = arr.getClass();
        System.out.println("\n数组对象：");
        System.out.println("  arr = [1, 2, 3]");
        System.out.println("  arr.getClass() = " + arrClass);
        System.out.println("  arr.getClass().getName() = " + arrClass.getName());
        System.out.println("  arr.getClass().isArray() = " + arrClass.isArray());

        // 例子 4：获取 Class 对象的各种方式
        System.out.println("\n获取 Class 对象的三种方式：");
        System.out.println("  1. 对象.getClass() = " + str.getClass());
        System.out.println("  2. 类名.class = " + String.class);
        System.out.println("  3. Class.forName(\"...\") = " + (try_forName()));
    }

    private static String try_forName() {
        try {
            return Class.forName("java.lang.String").toString();
        } catch (Exception e) {
            return "异常";
        }
    }

    private static void demoNativeKeyword() {
        System.out.println("native 关键字的含义：\n");

        System.out.println("• native 表示本地方法（由 C/C++ 实现）");
        System.out.println("• Java 中只有方法签名，没有方法体");
        System.out.println("• 实现在 JVM 底层\n");

        System.out.println("为什么 getClass() 是 native？");
        System.out.println("  1. 需要直接访问 JVM 内存中的对象信息");
        System.out.println("  2. 需要获取对象头数据（标记该对象属于哪个类）");
        System.out.println("  3. Java 无法直接访问内存");
        System.out.println("  4. 必须由 JVM 的 C++ 代码实现\n");

        System.out.println("其他常见的 native 方法：");
        System.out.println("  • System.currentTimeMillis() - 获取系统时间");
        System.out.println("  • Object.clone() - 克隆对象");
        System.out.println("  • Object.notify() / wait() - 线程通信");
        System.out.println("  • Array.get(Object, int) - 反射获取数组元素\n");
    }

    private static void demoFinalKeyword() {
        System.out.println("final 修饰方法的含义：\n");

        System.out.println("• final 表示不允许子类重写该方法");
        System.out.println("• getClass() 是 final 的，子类无法重写");
        System.out.println("• 这是合理的，因为必须准确返回对象的真实类型\n");

        System.out.println("为什么不允许重写？");
        System.out.println("  1. getClass() 必须准确反映对象的真实类型");
        System.out.println("  2. 如果允许重写，子类可能返回错误的类型");
        System.out.println("  3. 会破坏 Java 的类型系统和类型检查的可靠性\n");

        System.out.println("❌ 不允许的代码：");
        System.out.println("class MyClass extends Object {");
        System.out.println("    public final Class<?> getClass() {  // ❌ 编译错误");
        System.out.println("        return null;");
        System.out.println("    }");
        System.out.println("}\n");

        System.out.println("编译器报错：Cannot override final method\n");
    }

    private static void demoRealScenarios() {
        System.out.println("--- 应用 1：类型检查 ---\n");

        Object obj1 = "Hello";
        Object obj2 = 42;

        System.out.println("检查对象的真实类型：");
        System.out.println("obj1 的类型：" + obj1.getClass().getSimpleName());
        System.out.println("obj2 的类型：" + obj2.getClass().getSimpleName());

        System.out.println("\n--- 应用 2：多态中获取真实类型 ---\n");

        // 使用 Person 和 Student
        System.out.println("Person person = new Student(...);");
        System.out.println("person.getClass() 返回：Student.class（真实类型）");
        System.out.println("而不是：Person.class（声明类型）\n");

        System.out.println("--- 应用 3：安全的类型检查 ---\n");

        String str = "Hello";
        System.out.println("if (str.getClass() == String.class) {");
        System.out.println("    // 类型匹配，可以安全地进行操作");
        System.out.println("}");

        if (str.getClass() == String.class) {
            System.out.println("✅ 判断成功：str 的类型就是 String\n");
        }

        System.out.println("--- 应用 4：equals() 方法中的类型检查 ---\n");

        System.out.println("通常在 equals() 中使用 getClass()：");
        System.out.println("```java");
        System.out.println("@Override");
        System.out.println("public boolean equals(Object obj) {");
        System.out.println("    if (obj == null) return false;");
        System.out.println("    if (this.getClass() != obj.getClass()) return false;");
        System.out.println("    // 继续比较其他属性...");
        System.out.println("    return true;");
        System.out.println("}");
        System.out.println("```\n");

        System.out.println("--- 应用 5：反射操作 ---\n");

        String str2 = "Test";
        Class<?> clazz = str2.getClass();

        System.out.println("获取类的详细信息：");
        System.out.println("  1. 类名：" + clazz.getName());
        System.out.println("  2. 简名：" + clazz.getSimpleName());
        System.out.println("  3. 父类：" + clazz.getSuperclass().getSimpleName());
        System.out.println("  4. 方法数：" + clazz.getMethods().length);
        System.out.println("  5. 包名：" + clazz.getPackage());
    }
}

