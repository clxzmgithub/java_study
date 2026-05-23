package org.example.java_base_test;

/**
 * 深度理解："返回当前运行时对象的 Class 对象" 是什么意思
 *
 * 关键词解析：
 * • "运行时" - 程序正在执行时（不是编译时）
 * • "对象" - 真实存在的对象实例
 * • "Class 对象" - 代表这个对象所属类的信息
 */
public class RuntimeClassObjectExplained {
    public static void main(String[] args) {
        System.out.println("========== 深入理解 getClass() ==========\n");

        // 1. 什么是"运行时"？
        System.out.println("【1. 什么是运行时（Runtime）？】");
        demoRuntime();

        // 2. 什么是 Class 对象？
        System.out.println("\n【2. 什么是 Class 对象？】");
        demoClassObject();

        // 3. getClass() 返回的是什么？
        System.out.println("\n【3. getClass() 返回的是什么？】");
        demoWhatGetClassReturns();

        // 4. 编译时 vs 运行时
        System.out.println("\n【4. 编译时 vs 运行时的区别】");
        demoCompileTimeVsRuntime();

        // 5. 实际应用
        System.out.println("\n【5. 实际应用场景】");
        demoRealApplications();
    }

    // ============ 1. 什么是运行时？ ============
    private static void demoRuntime() {
        System.out.println("运行时（Runtime）= 程序正在执行的时刻\n");

        System.out.println("编译时（Compile Time）：");
        System.out.println("  • 源代码写好了");
        System.out.println("  • 编译器在检查和编译代码");
        System.out.println("  • 还没有运行程序\n");

        System.out.println("↓ 编译完成 ↓\n");

        System.out.println("运行时（Runtime）：");
        System.out.println("  • 程序在 JVM 中执行");
        System.out.println("  • 对象在内存中真实存在");
        System.out.println("  • 可以通过 getClass() 获取对象的真实信息\n");

        System.out.println("例子：");
        System.out.println("编译时：");
        System.out.println("  String str = \"Hello\";");
        System.out.println("  // 编译器看到一个 String 类型的变量\n");

        String str = "Hello";
        System.out.println("运行时（现在）：");
        System.out.println("  String 对象已创建，在内存中真实存在");
        System.out.println("  str.getClass() = " + str.getClass());
        System.out.println("  str.getClass().getName() = " + str.getClass().getName());
    }

    // ============ 2. 什么是 Class 对象？ ============
    private static void demoClassObject() {
        System.out.println("Class 对象 = 代表一个类的元信息（类的信息）\n");

        System.out.println("类比：");
        System.out.println("  • 对象实例 = 一个真实的人");
        System.out.println("    例如：小明（一个具体的人）");
        System.out.println("");
        System.out.println("  • Class 对象 = 这个人的身份证");
        System.out.println("    例如：小明的身份证上记录了");
        System.out.println("         - 姓名：小明");
        System.out.println("         - 性别：男");
        System.out.println("         - 出生日期：2000-01-01");
        System.out.println("         - 身份证号：123456...\n");

        System.out.println("Class 对象中包含什么信息？");
        String str = "Hello";
        Class<?> clazz = str.getClass();

        System.out.println("  1. 类的名字");
        System.out.println("     " + clazz.getName());

        System.out.println("\n  2. 类的所有方法");
        System.out.println("     共有 " + clazz.getMethods().length + " 个方法");
        System.out.println("     例如：" + clazz.getMethods()[0].getName() + "(), " +
                          clazz.getMethods()[1].getName() + "(), ...");

        System.out.println("\n  3. 类的所有属性");
        System.out.println("     共有 " + clazz.getFields().length + " 个公开属性");

        System.out.println("\n  4. 类的父类");
        System.out.println("     " + clazz.getSuperclass().getName());

        System.out.println("\n  5. 类是否是接口");
        System.out.println("     " + (clazz.isInterface() ? "是接口" : "不是接口"));

        System.out.println("\n  6. 类是否是数组");
        System.out.println("     " + (clazz.isArray() ? "是数组" : "不是数组"));
    }

    // ============ 3. getClass() 返回的是什么？ ============
    private static void demoWhatGetClassReturns() {
        System.out.println("getClass() 返回的是「对象所属类的 Class 对象」\n");

        System.out.println("具体意思：");
        System.out.println("  • getClass() 查看这个对象属于哪个类");
        System.out.println("  • 然后返回那个类的 Class 对象\n");

        System.out.println("例子 1：String 对象");
        String str = "Hello";
        System.out.println("  对象：str = \"Hello\"");
        System.out.println("  所属类：String 类");
        System.out.println("  str.getClass() 返回：String 类的 Class 对象");
        System.out.println("  结果：" + str.getClass() + "\n");

        System.out.println("例子 2：Integer 对象");
        Integer num = 42;
        System.out.println("  对象：num = 42");
        System.out.println("  所属类：Integer 类");
        System.out.println("  num.getClass() 返回：Integer 类的 Class 对象");
        System.out.println("  结果：" + num.getClass() + "\n");

        System.out.println("例子 3：数组对象");
        int[] arr = {1, 2, 3};
        System.out.println("  对象：arr = [1, 2, 3]");
        System.out.println("  所属类：int[] 数组类型");
        System.out.println("  arr.getClass() 返回：int[] 的 Class 对象");
        System.out.println("  结果：" + arr.getClass() + "\n");

        System.out.println("重点：");
        System.out.println("  每个对象都属于某个类");
        System.out.println("  getClass() 就是告诉你这个对象属于哪个类");
    }

    // ============ 4. 编译时 vs 运行时 ============
    private static void demoCompileTimeVsRuntime() {
        System.out.println("这个例子展示编译时和运行时的区别：\n");

        System.out.println("代码：");
        System.out.println("  Object obj = \"Hello\";  // 声明为 Object 类型\n");

        System.out.println("编译时（Compile Time）：");
        System.out.println("  编译器看到：Object obj = ...;");
        System.out.println("  编译器认为：obj 的类型是 Object");
        System.out.println("  没有实际的对象，只有类型信息\n");

        Object obj = "Hello";

        System.out.println("运行时（Runtime）：现在程序正在执行");
        System.out.println("  实际情况：obj 指向一个真实的 String 对象");
        System.out.println("  obj.getClass() 返回：" + obj.getClass());
        System.out.println("  obj.getClass().getName() 返回：" + obj.getClass().getName());
        System.out.println("");
        System.out.println("  ⭐ 关键点：");
        System.out.println("    • 声明类型（编译时）= Object");
        System.out.println("    • 实际类型（运行时）= String");
        System.out.println("    • getClass() 返回的是实际类型（String）\n");

        System.out.println("再看一个例子：");
        System.out.println("  Object num = 42;");
        System.out.println("  num.getClass() 返回的是 Integer，不是 Object\n");

        Object num = 42;
        System.out.println("结果：" + num.getClass());
    }

    // ============ 5. 实际应用 ============
    private static void demoRealApplications() {
        System.out.println("--- 应用 1：判断对象的真实类型 ---\n");

        Object[] objects = {
            "Hello",      // String 对象
            42,           // Integer 对象
            3.14,         // Double 对象
            true          // Boolean 对象
        };

        System.out.println("有一个对象数组，类型都是 Object");
        System.out.println("但实际的对象类型不同。使用 getClass() 判断：\n");

        for (Object obj : objects) {
            String type = obj.getClass().getSimpleName();
            System.out.println("  对象：" + obj + " → 类型：" + type);
        }

        System.out.println("\n--- 应用 2：多态中获取真实类型 ---\n");

        System.out.println("场景：所有动物都可以发出声音\n");

        // 使用 Object 代替具体的动物类来演示
        Object animal1 = "这是一个狗";
        Object animal2 = "这是一个猫";

        System.out.println("animal1 的真实类型：" + animal1.getClass().getSimpleName());
        System.out.println("animal2 的真实类型：" + animal2.getClass().getSimpleName());

        System.out.println("\n--- 应用 3：安全的类型转换 ---\n");

        System.out.println("场景：需要转换对象类型前先检查\n");

        Object obj = "Hello";

        System.out.println("代码：");
        System.out.println("  if (obj.getClass() == String.class) {");
        System.out.println("      String str = (String) obj;  // 安全转换");
        System.out.println("      System.out.println(str);");
        System.out.println("  }");
        System.out.println("");

        if (obj.getClass() == String.class) {
            String str = (String) obj;
            System.out.println("✅ 转换成功：" + str);
        }

        System.out.println("\n--- 应用 4：序列化和反序列化 ---\n");

        System.out.println("场景：保存对象到文件，然后读取\n");

        System.out.println("保存时：");
        String data = "Hello World";
        System.out.println("  对象类型：" + data.getClass().getName());
        System.out.println("  (保存到文件：org.example.java_base_test.RuntimeClassObjectExplained...");
        System.out.println("   ...String...\\\"Hello World\\\")");

        System.out.println("\n读取时：");
        System.out.println("  先读取类型信息：String");
        System.out.println("  然后创建对应的对象");
        System.out.println("  最后恢复数据");

        System.out.println("\n--- 应用 5：instanceof vs getClass() ---\n");

        System.out.println("两种类型检查方式的区别：");
        String str = "Hello";

        System.out.println("\n使用 instanceof（包含继承）：");
        System.out.println("  str instanceof String = " + (str instanceof String));
        System.out.println("  str instanceof Object = " + (str instanceof Object));
        System.out.println("  (String 继承自 Object，所以都是 true)\n");

        System.out.println("使用 getClass()（精确匹配）：");
        System.out.println("  str.getClass() == String.class = " + (str.getClass() == String.class));
        Class<?> objClass = Object.class;
        System.out.println("  str.getClass() == Object.class = " + (str.getClass().equals(objClass) ? false : true));
        System.out.println("  (只有真实类型匹配才是 true)");
    }
}

