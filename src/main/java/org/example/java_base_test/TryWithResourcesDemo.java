package org.example.java_base_test;

/**
 * try-with-resources 执行顺序详解
 *
 * 结论：资源关闭(close) 发生在 catch/finally 之前
 * 顺序：try 体执行 → 异常发生 → 资源 close() → catch → finally
 */
public class TryWithResourcesDemo {

    public static void main(String[] args) {
        System.out.println("========== try-with-resources 执行顺序详解 ==========\n");

        System.out.println("【1. 传统 try-finally 写法（对比用）】");
        demoOldStyle();

        System.out.println("\n【2. try-with-resources 正常执行（无异常）】");
        demoNormalFlow();

        System.out.println("\n【3. try-with-resources 有异常 + catch + finally】");
        demoWithException();

        System.out.println("\n【4. 多个资源时的关闭顺序】");
        demoMultipleResources();

        System.out.println("\n【5. close() 本身抛异常时（Suppressed 异常）】");
        demoCloseThrowsException();
    }

    // ================================================================
    // 【1. 传统写法：手动关闭资源，繁琐且容易漏关】
    // ================================================================
    private static void demoOldStyle() {
        MyResource res = null;
        try {
            res = new MyResource("文件A");
            System.out.println("  → 使用资源：读取数据");
            // 模拟异常：int x = 1 / 0;
            int x = 1 / 0;
        } catch (Exception e) {
            System.out.println("  → catch：处理异常 " + e.getMessage());
        } finally {
            // 必须手动在 finally 里关闭，否则异常时资源泄漏
            if (res != null) {
                try {
                    res.close();
                } catch (Exception e) {
                    System.out.println("  → finally：关闭资源失败 " + e.getMessage());
                }
            }
        }
        System.out.println("  问题：代码冗长，忘了写 finally 就会资源泄漏");
    }

    // ================================================================
    // 【2. 正常执行（无异常）的执行顺序】
    // ================================================================
    private static void demoNormalFlow() {
        System.out.println("  执行 try-with-resources（无异常）：\n");

        try (MyResource res = new MyResource("文件B")) {
            System.out.println("  → [try 体] 使用资源：读取数据");
            System.out.println("  → [try 体] 操作完成，try 体结束");
            // try 体正常结束
            // 此时：自动调用 res.close()，然后才执行 finally
        } catch (Exception e) {
            System.out.println("  → [catch] 不会执行（无异常）");
        } finally {
            System.out.println("  → [finally] 在 close() 之后执行");
        }

        System.out.println("\n  执行顺序：try 体 → close() → finally");
    }

    // ================================================================
    // 【3. 有异常时的执行顺序（核心演示）】
    // ================================================================
    private static void demoWithException() {
        System.out.println("  执行 try-with-resources（有异常）：\n");

        try (MyResource res = new MyResource("文件C")) {
            System.out.println("  → [try 体] 开始使用资源");
            System.out.println("  → [try 体] 发生异常！");
            throw new RuntimeException("业务处理失败");
            // 执行到这里：
            // 1. try 体因异常退出
            // 2. 自动调用 res.close()   ← 先关闭资源
            // 3. 进入 catch 块          ← 再处理异常
            // 4. 执行 finally 块        ← 最后执行 finally
        } catch (Exception e) {
            System.out.println("  → [catch] 处理异常：" + e.getMessage());
        } finally {
            System.out.println("  → [finally] 最后执行");
        }

        System.out.println("\n  执行顺序：try 体抛异常 → close() → catch → finally");
        System.out.println("  关键：close() 在 catch 之前！");
    }

    // ================================================================
    // 【4. 多个资源时，关闭顺序与声明顺序相反】
    // ================================================================
    private static void demoMultipleResources() {
        System.out.println("  多个资源的关闭顺序（与声明顺序相反）：\n");

        // 声明顺序：res1 → res2 → res3
        // 关闭顺序：res3 → res2 → res1（栈式关闭，LIFO）
        try (MyResource res1 = new MyResource("数据库连接");
             MyResource res2 = new MyResource("文件流");
             MyResource res3 = new MyResource("网络连接")) {

            System.out.println("  → [try 体] 同时使用三个资源");
            System.out.println("  → [try 体] 完成操作");
            // try 体结束，按反序关闭
        } finally {
            System.out.println("  → [finally] 所有资源都已关闭后执行");
        }

        System.out.println("\n  关闭顺序是反的原因：像栈（LIFO）一样");
        System.out.println("  最后打开的资源，最先关闭（避免依赖问题）");
        System.out.println("  例如：res3 依赖 res2，res2 依赖 res1");
        System.out.println("        必须先关 res3，再关 res2，最后关 res1");
    }

    // ================================================================
    // 【5. close() 本身抛出异常时（Suppressed 异常机制）】
    // ================================================================
    private static void demoCloseThrowsException() {
        System.out.println("  当 try 体和 close() 都抛异常时：\n");

        try (BrokenResource res = new BrokenResource()) {
            System.out.println("  → [try 体] 使用有问题的资源");
            throw new RuntimeException("try 体中的业务异常");
            // 1. try 体抛出"业务异常"
            // 2. 自动调用 res.close()，close() 又抛出"关闭异常"
            // 3. Java 会保留"业务异常"作为主异常
            //    把"关闭异常"作为 Suppressed（被抑制的）异常附加上去
        } catch (Exception e) {
            System.out.println("  → [catch] 主异常：" + e.getMessage());
            // 查看被抑制的异常
            Throwable[] suppressed = e.getSuppressed();
            if (suppressed.length > 0) {
                System.out.println("  → [catch] 被抑制的异常（来自 close()）：" + suppressed[0].getMessage());
            }
        }

        System.out.println("\n  对比传统写法（finally 里关闭）：");
        System.out.println("  如果 finally 里的 close() 抛异常");
        System.out.println("  会覆盖掉 try 体里的原始异常！原始异常就丢失了 ❌");
        System.out.println("  try-with-resources 用 Suppressed 机制两个都保留 ✅");
    }
}

// ================================================================
// 自定义资源类（实现 AutoCloseable）
// ================================================================
class MyResource implements AutoCloseable {
    private final String name;

    public MyResource(String name) {
        this.name = name;
        System.out.println("  → [构造] 打开资源：" + name);
    }

    @Override
    public void close() {
        // try-with-resources 会自动调用这个方法
        System.out.println("  → [close] 关闭资源：" + name);
    }
}

// ================================================================
// 关闭时会抛出异常的资源类
// ================================================================
class BrokenResource implements AutoCloseable {
    public BrokenResource() {
        System.out.println("  → [构造] 打开有问题的资源");
    }

    @Override
    public void close() {
        System.out.println("  → [close] 关闭时出错！抛出异常");
        throw new RuntimeException("关闭资源时发生异常");
    }
}

