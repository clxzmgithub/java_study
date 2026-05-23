package org.example.basetest.thread_aync;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 跨线程传递 ThreadLocal 的值
 *
 * 目录：
 *   第一部分：普通 ThreadLocal —— 子线程拿不到父线程的值（问题）
 *   第二部分：InheritableThreadLocal —— 原理 & 局限性
 *   第三部分：TransmittableThreadLocal 原理（手写简化版）
 *   第四部分：三种方式对比 & 选型
 */
public class CrossThreadTransmitDemo {

    public static void main(String[] args) throws Exception {

        sep("第一部分：普通 ThreadLocal —— 子线程拿不到父线程的值");
        Part1_NormalThreadLocal.run();

        sep("第二部分：InheritableThreadLocal —— 原理 & 局限性");
        Part2_InheritableThreadLocal.run();

        sep("第三部分：TransmittableThreadLocal 原理（手写简化版）");
        Part3_TTL.run();

        sep("第四部分：三种方式对比 & 选型");
        Part4_Summary.run();

        sep("第五部分：深挖 —— InheritableThreadLocal 在线程池中为何失效（附逐帧分析）");
        Part5_DeepDive.run();
    }

    static void sep(String title) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" " + title);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
}

// ====================================================================
// 第一部分：普通 ThreadLocal，子线程拿不到父线程的值
// ====================================================================
class Part1_NormalThreadLocal {

    static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    static void run() throws InterruptedException {
        System.out.println("【场景：主线程设置 TraceId，提交异步任务到子线程打印日志】\n");

        // 主线程（模拟 HTTP 请求处理线程）
        TRACE_ID.set("TRACE-20240101-001");
        System.out.println("主线程 set traceId = " + TRACE_ID.get());

        // 提交异步任务（模拟调用异步方法记录日志）
        Thread child = new Thread(() -> {
            // 子线程是全新的线程，threadLocals 是 null，拿不到父线程的值
            System.out.println("子线程 get traceId = " + TRACE_ID.get()
                    + " ← null！父线程的 TraceId 丢失了！");
        });
        child.start();
        child.join();

        System.out.println();
        System.out.println("【问题分析】");
        System.out.println("  普通 ThreadLocal：每个线程有自己独立的 threadLocals（Map）");
        System.out.println("  子线程刚创建时：threadLocals = null，没有继承父线程的任何数据");
        System.out.println("  主线程 set 的 traceId 只存在于主线程的 threadLocals 里");
        System.out.println("  子线程的 threadLocals 是空的，get() 返回 null");
        System.out.println();
        System.out.println("【现实影响】");
        System.out.println("  请求进来 → 主线程生成 TraceId 存入 ThreadLocal");
        System.out.println("  主线程提交异步任务到线程池（发 MQ、写日志、调第三方接口）");
        System.out.println("  子线程里 TraceId 丢失 → 日志无法关联 → 链路追踪断链");

        TRACE_ID.remove();
    }
}

// ====================================================================
// 第二部分：InheritableThreadLocal —— 原理 & 局限性
// ====================================================================
class Part2_InheritableThreadLocal {

    static final InheritableThreadLocal<String> TRACE_ID = new InheritableThreadLocal<>();

    static void run() throws InterruptedException, ExecutionException {
        System.out.println("【InheritableThreadLocal 基本效果】\n");

        TRACE_ID.set("TRACE-20240101-002");
        System.out.println("主线程 set traceId = " + TRACE_ID.get());

        Thread child = new Thread(() -> {
            System.out.println("子线程 get traceId = " + TRACE_ID.get()
                    + " ← ✅ 继承成功！");
            // 子线程修改自己的副本（不影响父线程）
            TRACE_ID.set("TRACE-CHILD-MODIFIED");
            System.out.println("子线程修改后 = " + TRACE_ID.get());
        });
        child.start();
        child.join();

        System.out.println("父线程的值没变 = " + TRACE_ID.get() + " ← 子线程的修改不影响父线程");

        System.out.println();
        System.out.println("【InheritableThreadLocal 的底层原理】\n");
        System.out.println("  Thread 类内部有两个 Map 字段：");
        System.out.println("    ThreadLocal.ThreadLocalMap threadLocals = null;");
        System.out.println("    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;");
        System.out.println();
        System.out.println("  子线程创建时（Thread 构造方法里）会调用：");
        System.out.println("    private void init(ThreadGroup g, Runnable target, ...) {");
        System.out.println("        Thread parent = currentThread();  // 取父线程");
        System.out.println("        if (parent.inheritableThreadLocals != null) {");
        System.out.println("            // ★ 把父线程的 inheritableThreadLocals 复制给子线程");
        System.out.println("            this.inheritableThreadLocals =");
        System.out.println("                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println();
        System.out.println("  createInheritedMap() 的行为：");
        System.out.println("    遍历父线程 inheritableThreadLocals 里的每个 Entry");
        System.out.println("    把每个 Entry 的 value 复制到子线程的新 Map 里（浅拷贝）");
        System.out.println("    复制时调用 threadLocal.childValue(parentValue)");
        System.out.println("    默认实现直接返回 parentValue（引用传递，不是深拷贝）");
        System.out.println();
        System.out.println("  图示：");
        System.out.println("    父线程.inheritableThreadLocals:");
        System.out.println("      Entry{ key=TRACE_ID, value=\"TRACE-001\" }");
        System.out.println("                    ↓ 子线程创建时，复制一份（浅拷贝）");
        System.out.println("    子线程.inheritableThreadLocals:");
        System.out.println("      Entry{ key=TRACE_ID, value=\"TRACE-001\" }  ← 独立副本");
        System.out.println("    父子线程各自持有独立副本，互不影响");

        System.out.println();
        System.out.println("【InheritableThreadLocal 的致命缺陷：线程池场景】\n");
        System.out.println("  继承时机：子线程「创建时」从父线程复制");
        System.out.println("  线程池的线程：创建一次，反复复用");
        System.out.println("  问题：线程池的线程在「创建时」继承了当时父线程的值，");
        System.out.println("        之后被复用时，不会重新继承新请求的父线程的值！");

        ExecutorService pool = Executors.newFixedThreadPool(1);

        // 第一个请求：主线程设置 traceId = A，提交任务到线程池
        TRACE_ID.set("TRACE-REQUEST-A");
        System.out.println("\n  请求A：主线程 set = " + TRACE_ID.get());
        Future<?> f1 = pool.submit(() -> {
            System.out.println("  请求A 的线程池任务：get = " + TRACE_ID.get());
            // 线程池线程此时 inheritableThreadLocals 里的值是它「创建时」继承的
            // 如果线程在请求A之前就创建了，这里可能是 null 或者其他请求的值
        });
        f1.get();

        // 主线程更新 traceId 为 B（模拟下一个请求）
        TRACE_ID.set("TRACE-REQUEST-B");
        System.out.println("  请求B：主线程 set = " + TRACE_ID.get());
        Future<?> f2 = pool.submit(() -> {
            System.out.println("  请求B 的线程池任务：get = " + TRACE_ID.get()
                    + " ← ⚠️ 不是 REQUEST-B！线程池线程没有重新继承！");
            // 线程池里的线程在「第一次创建时」继承了 traceId
            // 之后 traceId 变了，但线程不重新创建，所以不会重新继承
        });
        f2.get();

        pool.shutdown();

        System.out.println();
        System.out.println("  根本原因：");
        System.out.println("    线程池线程（Thread-1）在第一次创建时：");
        System.out.println("      parent（提交第一个任务的线程）.inheritableThreadLocals");
        System.out.println("      → 此时复制了一份给 Thread-1");
        System.out.println("    之后 Thread-1 一直存活，不再重新继承");
        System.out.println("    请求B 提交任务时，Thread-1 里的值还是上次继承的旧值");

        TRACE_ID.remove();
    }
}

// ====================================================================
// 第三部分：TransmittableThreadLocal 原理（手写简化版）
// ====================================================================
class Part3_TTL {

    /**
     * 手写 TTL 简化版，理解其核心原理
     *
     * 核心思想：
     *   不依赖线程创建时机来传递，而是在「任务提交时」主动捕获当前线程的上下文，
     *   在「任务执行时」把捕获的上下文设置到执行线程里，
     *   任务执行完毕后恢复执行线程的原始上下文。
     */

    // ---- 简化版 TTL 实现 ----

    // 注册表：记录所有 SimpleTTL 实例（真实 TTL 用 WeakHashMap + holder 机制）
    static final java.util.Set<SimpleTTL<?>> ALL_TTL_INSTANCES =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    static class SimpleTTL<T> {
        // 底层用 InheritableThreadLocal 存值（同时支持普通父子线程继承）
        private final InheritableThreadLocal<T> holder = new InheritableThreadLocal<>();

        SimpleTTL() {
            synchronized (ALL_TTL_INSTANCES) {
                ALL_TTL_INSTANCES.add(this);  // 注册自己，方便快照时遍历
            }
        }

        public void set(T value) { holder.set(value); }
        public T    get()        { return holder.get(); }
        public void remove()     { holder.remove(); }

        // 设置指定值（恢复时用）
        void setDirect(T value)  { holder.set(value); }
    }

    // ---- 上下文快照（Snapshot）----
    // 在「提交任务时」捕获当前线程所有 TTL 的值
    @SuppressWarnings("unchecked")
    static Map<SimpleTTL<?>, Object> capture() {
        Map<SimpleTTL<?>, Object> snapshot = new HashMap<>();
        synchronized (ALL_TTL_INSTANCES) {
            for (SimpleTTL<?> ttl : ALL_TTL_INSTANCES) {
                Object value = ttl.get();
                if (value != null) {
                    snapshot.put(ttl, value);
                }
            }
        }
        return snapshot;
    }

    // ---- 在执行线程中回放快照，并返回执行前的旧值（用于恢复）----
    @SuppressWarnings("unchecked")
    static Map<SimpleTTL<?>, Object> replay(Map<SimpleTTL<?>, Object> captured) {
        Map<SimpleTTL<?>, Object> backup = new HashMap<>();
        synchronized (ALL_TTL_INSTANCES) {
            for (SimpleTTL<?> ttl : ALL_TTL_INSTANCES) {
                backup.put(ttl, ttl.get());  // 备份当前线程的旧值
                Object capturedValue = captured.get(ttl);
                if (capturedValue != null) {
                    ((SimpleTTL<Object>) ttl).setDirect(capturedValue); // 设置捕获的值
                } else {
                    ttl.remove();   // 父线程里没有这个值，清除线程池线程的残留值
                }
            }
        }
        return backup;
    }

    // ---- 恢复执行线程的原始上下文 ----
    @SuppressWarnings("unchecked")
    static void restore(Map<SimpleTTL<?>, Object> backup) {
        synchronized (ALL_TTL_INSTANCES) {
            for (SimpleTTL<?> ttl : ALL_TTL_INSTANCES) {
                Object oldValue = backup.get(ttl);
                if (oldValue != null) {
                    ((SimpleTTL<Object>) ttl).setDirect(oldValue);
                } else {
                    ttl.remove();
                }
            }
        }
    }

    // ---- TtlRunnable：包装 Runnable，在执行时自动回放和恢复上下文 ----
    static class TtlRunnable implements Runnable {
        private final Runnable runnable;
        // ★ 在「提交任务时」（构造时）捕获当前线程（提交者）的上下文快照
        private final Map<SimpleTTL<?>, Object> capturedContext;

        TtlRunnable(Runnable runnable) {
            this.runnable        = runnable;
            this.capturedContext = capture();  // 提交时捕获！
        }

        @Override
        public void run() {
            // ★ 任务执行时，把捕获的快照回放到执行线程里
            Map<SimpleTTL<?>, Object> backup = replay(capturedContext);
            try {
                runnable.run();   // 执行真实业务逻辑
            } finally {
                // ★ 任务执行完毕，恢复执行线程的原始上下文（线程池线程的原始状态）
                restore(backup);
            }
        }
    }

    // ---- 使用 TTL 的场景演示 ----

    static final SimpleTTL<String> TRACE_ID = new SimpleTTL<>();
    static final SimpleTTL<String> USER_ID  = new SimpleTTL<>();

    static void run() throws InterruptedException, ExecutionException {
        System.out.println("【TTL 核心原理：提交时捕获，执行时回放，完成后恢复】\n");

        System.out.println("  传统方案（InheritableThreadLocal）的问题：");
        System.out.println("    依赖「线程创建时机」继承，线程池线程复用时失效");
        System.out.println();
        System.out.println("  TTL 的解法：");
        System.out.println("    不依赖线程创建，而是把传递时机从「线程创建」改为「任务提交」");
        System.out.println("    提交任务时：主动拍一张「快照」（当前线程所有 TTL 的值）");
        System.out.println("    执行任务时：把快照里的值设置到执行线程里");
        System.out.println("    任务完成后：把执行线程恢复为任务前的状态");
        System.out.println();

        ExecutorService pool = Executors.newFixedThreadPool(2);

        // ---- 请求 A ----
        TRACE_ID.set("TRACE-REQUEST-A");
        USER_ID.set("user-001");
        System.out.println("主线程（请求A）set traceId=" + TRACE_ID.get() + ", userId=" + USER_ID.get());

        // 提交任务时用 TtlRunnable 包装（真实 TTL 用 TtlExecutors.getTtlExecutorService 自动包装）
        Future<?> fa = pool.submit(new TtlRunnable(() -> {
            System.out.println("  [请求A的异步任务] traceId=" + TRACE_ID.get()
                    + ", userId=" + USER_ID.get()
                    + " ← ✅ 正确拿到请求A的上下文");
        }));
        fa.get();

        TRACE_ID.remove();
        USER_ID.remove();

        // ---- 请求 B（主线程换了新的上下文）----
        TRACE_ID.set("TRACE-REQUEST-B");
        USER_ID.set("user-002");
        System.out.println("主线程（请求B）set traceId=" + TRACE_ID.get() + ", userId=" + USER_ID.get());

        Future<?> fb = pool.submit(new TtlRunnable(() -> {
            System.out.println("  [请求B的异步任务] traceId=" + TRACE_ID.get()
                    + ", userId=" + USER_ID.get()
                    + " ← ✅ 正确拿到请求B的上下文，没有被请求A污染");
        }));
        fb.get();

        TRACE_ID.remove();
        USER_ID.remove();

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println();
        System.out.println("【TTL 执行流程图】\n");
        System.out.println("  提交线程（主线程/请求线程）          执行线程（线程池 Thread-1）");
        System.out.println("  ──────────────────────────          ──────────────────────────");
        System.out.println("  TRACE_ID.set(\"TRACE-B\")");
        System.out.println("       ↓");
        System.out.println("  new TtlRunnable(task)");
        System.out.println("    └── capture()                     Thread-1 可能有残留值");
        System.out.println("        快照 = {TRACE_ID: \"TRACE-B\"}  TRACE_ID = \"TRACE-A\"(残留)");
        System.out.println("       ↓                                     ↓");
        System.out.println("  pool.submit(TtlRunnable)  ─────────► TtlRunnable.run()");
        System.out.println("                                          └── replay(快照)");
        System.out.println("                                              备份 = {TRACE_ID: \"TRACE-A\"}");
        System.out.println("                                              设置 TRACE_ID = \"TRACE-B\"");
        System.out.println("                                              ↓");
        System.out.println("                                          task.run()");
        System.out.println("                                          TRACE_ID.get() = \"TRACE-B\" ✅");
        System.out.println("                                              ↓");
        System.out.println("                                          restore(备份)");
        System.out.println("                                          恢复 TRACE_ID = \"TRACE-A\"");
        System.out.println("                                          (线程池线程原始状态恢复)");

        System.out.println();
        System.out.println("【真实项目中的 TTL 使用方式（阿里开源库）】\n");
        System.out.println("  Maven 依赖：");
        System.out.println("    <dependency>");
        System.out.println("        <groupId>com.alibaba</groupId>");
        System.out.println("        <artifactId>transmittable-thread-local</artifactId>");
        System.out.println("        <version>2.14.2</version>");
        System.out.println("    </dependency>\n");
        System.out.println("  用法 1：TTL + 手动包装 Runnable");
        System.out.println("    TransmittableThreadLocal<String> traceId = new TransmittableThreadLocal<>();");
        System.out.println("    traceId.set(\"TRACE-001\");");
        System.out.println("    pool.submit(TtlRunnable.get(() -> {");
        System.out.println("        System.out.println(traceId.get()); // TRACE-001 ✅");
        System.out.println("    }));\n");
        System.out.println("  用法 2：TTL + 包装线程池（推荐，自动包装所有任务）");
        System.out.println("    ExecutorService pool = TtlExecutors.getTtlExecutorService(");
        System.out.println("        Executors.newFixedThreadPool(10)");
        System.out.println("    );");
        System.out.println("    // 之后 pool.submit() 提交的所有任务自动传递 TTL 上下文");
        System.out.println("    pool.submit(() -> {");
        System.out.println("        System.out.println(traceId.get()); // 自动传递，无需手动包装 ✅");
        System.out.println("    });");
    }
}

// ====================================================================
// 第四部分：三种方式对比 & 选型
// ====================================================================
class Part4_Summary {
    static void run() {
        System.out.println("【三种方式核心对比】\n");
        System.out.println("  ┌──────────────────────┬─────────────────┬────────────────────┬─────────────────────┐");
        System.out.println("  │                      │ ThreadLocal     │InheritableThreadLocal│TransmittableThreadLocal│");
        System.out.println("  ├──────────────────────┼─────────────────┼────────────────────┼─────────────────────┤");
        System.out.println("  │ 父子线程继承           │      ❌         │        ✅          │         ✅          │");
        System.out.println("  │ 线程池复用场景         │      ❌         │        ❌          │         ✅          │");
        System.out.println("  │ 传递时机               │     不传递      │   线程创建时        │    任务提交时        │");
        System.out.println("  │ 执行后恢复线程池状态    │      —          │        ❌          │         ✅          │");
        System.out.println("  │ 使用复杂度             │     简单        │       简单         │    稍复杂（需包装）  │");
        System.out.println("  └──────────────────────┴─────────────────┴────────────────────┴─────────────────────┘\n");

        System.out.println("【三种方式的本质区别】\n");
        System.out.println("  ThreadLocal：");
        System.out.println("    数据只活在当前线程，从不传递");
        System.out.println("    原理：每个线程有独立的 threadLocals Map\n");

        System.out.println("  InheritableThreadLocal：");
        System.out.println("    数据在「线程创建」时从父线程拷贝到子线程");
        System.out.println("    原理：Thread 构造方法调用 createInheritedMap 浅拷贝父线程的 inheritableThreadLocals");
        System.out.println("    缺陷：线程池的线程创建一次就复用，之后不再拷贝，无法感知新请求的上下文\n");

        System.out.println("  TransmittableThreadLocal（TTL）：");
        System.out.println("    数据在「任务提交」时从提交线程捕获快照，在「任务执行」时回放到执行线程");
        System.out.println("    原理：TtlRunnable/TtlCallable 包装任务，在 run() 前后做 capture/replay/restore");
        System.out.println("    优势：线程池场景完美支持，每次任务都能拿到最新提交时的上下文\n");

        System.out.println("【选型指南】\n");
        System.out.println("  场景 1：同一线程内跨方法传递数据（Controller → Service → DAO）");
        System.out.println("          → 用 ThreadLocal ✅\n");
        System.out.println("  场景 2：通过 new Thread() 创建一次性子线程，需要传递父线程上下文");
        System.out.println("          → 用 InheritableThreadLocal ✅\n");
        System.out.println("  场景 3：使用线程池，需要把请求上下文（TraceId/UserId）传递到异步任务");
        System.out.println("          → 用 TransmittableThreadLocal ✅（阿里 TTL 库）\n");
        System.out.println("  场景 4：Spring 项目中使用 @Async 注解的异步方法需要传递 MDC");
        System.out.println("          → 自定义 TaskDecorator + TTL，或直接用 TTL 包装线程池 ✅");

        System.out.println();
        System.out.println("【TTL 在微服务中的典型应用：链路追踪 TraceId 全链路传递】\n");
        System.out.println("  HTTP 请求进来");
        System.out.println("      ↓");
        System.out.println("  Filter：TTL.set(traceId)");
        System.out.println("      ↓");
        System.out.println("  Controller → Service（同线程，TTL 正常工作）");
        System.out.println("      ↓");
        System.out.println("  Service 提交异步任务到线程池（TtlRunnable 自动传递 traceId）");
        System.out.println("      ↓");
        System.out.println("  线程池子任务里 TTL.get() 拿到正确的 traceId");
        System.out.println("      ↓");
        System.out.println("  日志系统读取 traceId，所有日志带上同一个 traceId");
        System.out.println("      ↓");
        System.out.println("  通过 TraceId 可以关联主线程 + 所有子线程的全部日志 ✅");
    }
}

// ====================================================================
// 第五部分：深挖 —— InheritableThreadLocal 在线程池场景为何失效
// ====================================================================
class Part5_DeepDive {

    static final InheritableThreadLocal<String> TRACE_ID = new InheritableThreadLocal<>();

    static void run() throws Exception {

        sep1("一、从 JDK 源码看「继承」究竟发生在哪一行");
        sourceCodeWalkthrough();

        sep1("二、线程池的线程是「谁」创建的？父线程是谁？");
        whoIsTheParent();

        sep1("三、四种典型场景的行为对比（可运行验证）");
        fourScenarios();

        sep1("四、「值被固化」的本质：一次性快照 vs 动态传递");
        snapshotVsDynamic();

        sep1("五、即使换新线程，时序问题照样翻车");
        timingProblem();
    }

    static void sep1(String s) {
        System.out.println("\n  ┄┄┄┄ " + s + " ┄┄┄┄\n");
    }

    // ------------------------------------------------------------------
    // 一、JDK 源码级别走读
    // ------------------------------------------------------------------
    static void sourceCodeWalkthrough() {
        System.out.println("  Thread 的构造链路（JDK 8）：");
        System.out.println();
        System.out.println("    new Thread(runnable)");
        System.out.println("      └─ Thread(null, runnable, \"Thread-n\", 0)");
        System.out.println("           └─ init(null, runnable, name, stackSize=0, acc=null, inheritThreadLocals=true)");
        System.out.println();
        System.out.println("  init() 关键代码（精简）：");
        System.out.println();
        System.out.println("    private void init(..., boolean inheritThreadLocals) {");
        System.out.println("        Thread parent = currentThread();            // ① 取「调用 new Thread() 的那个线程」");
        System.out.println();
        System.out.println("        // ② 只要 parent 有 inheritableThreadLocals，就复制");
        System.out.println("        if (inheritThreadLocals && parent.inheritableThreadLocals != null) {");
        System.out.println("            this.inheritableThreadLocals =");
        System.out.println("                ThreadLocal.createInheritedMap(           // ③ 浅拷贝，一次性！");
        System.out.println("                    parent.inheritableThreadLocals);");
        System.out.println("        }");
        System.out.println("    }                                                // ④ 之后再无任何继承动作");
        System.out.println();
        System.out.println("  createInheritedMap() 做的事：");
        System.out.println("    new ThreadLocalMap(parentMap)");
        System.out.println("      └─ 遍历 parentMap 的每个 Entry");
        System.out.println("           ├─ k = entry.get()          // 弱引用，取 ThreadLocal 实例");
        System.out.println("           └─ v = k.childValue(entry.value)  // 默认直接 return parentValue（引用传递）");
        System.out.println("      → 产生一个全新的 Map，与父 Map 完全独立");
        System.out.println();
        System.out.println("  ★ 关键结论：");
        System.out.println("    继承 = 构造方法里的一次性浅拷贝");
        System.out.println("    拷贝完毕后，子线程的 inheritableThreadLocals 和父线程毫无关联");
        System.out.println("    父线程之后再 set/remove，对已创建的子线程没有任何影响");
    }

    // ------------------------------------------------------------------
    // 二、线程池里「父线程」是谁
    // ------------------------------------------------------------------
    static void whoIsTheParent() throws Exception {
        System.out.println("  问题：线程池里的 Thread 是谁 new 出来的？");
        System.out.println();
        System.out.println("  ThreadPoolExecutor 内部的 addWorker() 方法：");
        System.out.println("    Worker w = new Worker(firstTask);   // Worker 实现了 Runnable");
        System.out.println("    Thread t = factory.newThread(w);    // 用 ThreadFactory 创建线程");
        System.out.println();
        System.out.println("  默认 ThreadFactory（Executors.defaultThreadFactory()）：");
        System.out.println("    public Thread newThread(Runnable r) {");
        System.out.println("        return new Thread(group, r, ...);   // ← 就是 new Thread()");
        System.out.println("    }");
        System.out.println();
        System.out.println("  那 new Thread() 在哪个线程里执行？—— 在「调用 pool.submit() 的那一刻」");
        System.out.println("  但线程的实际创建（addWorker）发生在线程数未达上限时，");
        System.out.println("  具体是「第一次提交任务」触发，或「核心线程数不够时」扩容触发。");
        System.out.println();

        // 可运行验证
        TRACE_ID.set("MAIN-VALUE-AT-POOL-CREATION");

        // 提前预热线程池，使得线程在「这个时刻」创建，继承 MAIN-VALUE-AT-POOL-CREATION
        ExecutorService pool = Executors.newFixedThreadPool(1);
        // 第一次 submit 会触发 addWorker → new Thread()，此时父线程是主线程
        // 主线程此刻的 ITL 值 = "MAIN-VALUE-AT-POOL-CREATION"
        Future<?> warmup = pool.submit(() -> {
            System.out.println("  [线程创建时刻] 继承到的值 = " + TRACE_ID.get());
            System.out.println("  ↑ 这是线程被 new 出来那一刻，父线程（主线程）的 TRACE_ID 值");
        });
        warmup.get();

        // 现在主线程改变 TRACE_ID
        TRACE_ID.set("MAIN-VALUE-CHANGED-LATER");
        System.out.println("\n  主线程将 traceId 改为：" + TRACE_ID.get());

        Future<?> f = pool.submit(() -> {
            System.out.println("  [后续任务] 得到的值 = " + TRACE_ID.get()
                    + "  ← ⚠️ 还是线程创建时继承的旧值！");
        });
        f.get();
        pool.shutdown();
        TRACE_ID.remove();

        System.out.println();
        System.out.println("  ★ 关键结论：");
        System.out.println("    线程池线程的「父线程」= 触发 addWorker 时正在运行的那个线程");
        System.out.println("    一旦线程创建完毕，它的 inheritableThreadLocals 就「冻结」了");
        System.out.println("    父线程之后怎么改 TRACE_ID，都影响不了已创建的线程池线程");
    }

    // ------------------------------------------------------------------
    // 三、四种典型场景对比
    // ------------------------------------------------------------------
    static void fourScenarios() throws Exception {
        System.out.println("  场景 A：new Thread() 每次都新建 —— ITL 正常工作");
        System.out.println("  ┌────────────────────────────────────────────────┐");
        TRACE_ID.set("REQ-A");
        new Thread(() ->
            System.out.println("  │ 场景A new Thread：" + TRACE_ID.get() + " ✅（正确继承）   │")
        ) {{ start(); join(); }};
        System.out.println("  └────────────────────────────────────────────────┘\n");
        TRACE_ID.remove();

        System.out.println("  场景 B：线程池 + 每次提交前 set —— ITL 失效（值固化在创建时）");
        System.out.println("  ┌────────────────────────────────────────────────┐");
        ExecutorService pool = Executors.newFixedThreadPool(1);
        // 线程在第一次 submit 时创建，此时主线程 ITL 为 null
        pool.submit(() -> {}).get();  // 预热，触发线程创建（此时主线程未 set 任何值）
        TRACE_ID.set("REQ-B");
        pool.submit(() ->
            System.out.println("  │ 场景B 线程池：" + TRACE_ID.get()
                    + " ← ⚠️ null！线程创建时父线程没有值 │")
        ).get();
        pool.shutdown();
        System.out.println("  └────────────────────────────────────────────────┘\n");
        TRACE_ID.remove();

        System.out.println("  场景 C：线程池创建时父线程有值，但后续请求换了值 —— 脏数据！");
        System.out.println("  ┌────────────────────────────────────────────────┐");
        TRACE_ID.set("REQUEST-1");
        ExecutorService pool2 = Executors.newFixedThreadPool(1);
        pool2.submit(() -> {}).get();  // 触发线程创建，继承 REQUEST-1
        TRACE_ID.set("REQUEST-2");     // 主线程换成 REQUEST-2
        pool2.submit(() ->
            System.out.println("  │ 场景C 第2个请求：" + TRACE_ID.get()
                    + " ← ⚠️ 还是 REQUEST-1！脏数据！│")
        ).get();
        pool2.shutdown();
        System.out.println("  └────────────────────────────────────────────────┘\n");
        TRACE_ID.remove();

        System.out.println("  场景 D：每次 new 一个单线程池 —— 线程每次重建，ITL「看似」正常");
        System.out.println("  ┌────────────────────────────────────────────────┐");
        TRACE_ID.set("REQUEST-X");
        // 注意：每次 submit 都创建新线程池（实际项目绝对不要这么做！）
        ExecutorService tmp = Executors.newSingleThreadExecutor();
        tmp.submit(() ->
            System.out.println("  │ 场景D 新线程池：" + TRACE_ID.get()
                    + " ← 虽然正确，但每次建池代价极高 │")
        ).get();
        tmp.shutdown();
        System.out.println("  └────────────────────────────────────────────────┘");
        System.out.println("  ↑ 这种写法能拿到正确值，但本质是「每次创建新线程」，");
        System.out.println("    性能灾难，实际项目中不可接受");
        TRACE_ID.remove();
    }

    // ------------------------------------------------------------------
    // 四、「值被固化」的本质：一次性快照 vs 动态传递
    // ------------------------------------------------------------------
    static void snapshotVsDynamic() {
        System.out.println("  类比现实生活：");
        System.out.println();
        System.out.println("  InheritableThreadLocal 的继承 ≈ 「拍照片」");
        System.out.println("  ──────────────────────────────────────────");
        System.out.println("  你（父线程）在 2024年1月1日（线程创建时）拍了一张全家福");
        System.out.println("  照片里的内容 = 线程创建那一刻父线程 ITL 的值（快照）");
        System.out.println("  之后你换了发型、搬了新家（父线程改了 ITL 值）");
        System.out.println("  照片不会自动变！——线程池线程里的值永远是「拍照时」的状态");
        System.out.println();
        System.out.println("  TTL 的传递 ≈ 「实时视频通话」");
        System.out.println("  ──────────────────────────────────────────");
        System.out.println("  每次提交任务时（任务提交 = 发起通话）");
        System.out.println("  对方（执行线程）看到的是「此刻你」的状态");
        System.out.println("  通话结束后（任务完成），你们各自回到自己的状态");
        System.out.println();
        System.out.println("  两者的本质差异：");
        System.out.println("  ┌─────────────────────┬───────────────────────────────────────┐");
        System.out.println("  │ ITL                 │ 线程创建时复制一次，之后「脱离」父线程   │");
        System.out.println("  │ TTL                 │ 每次任务提交时重新捕获，动态传递        │");
        System.out.println("  └─────────────────────┴───────────────────────────────────────┘");
        System.out.println();
        System.out.println("  从数据生命周期看：");
        System.out.println();
        System.out.println("  ITL（线程池线程视角）：");
        System.out.println("    t=0  线程创建，inheritableThreadLocals = { TRACE_ID: \"REQ-1\" }  ← 冻结");
        System.out.println("    t=1  执行任务1，TRACE_ID.get() = \"REQ-1\"  （正确）");
        System.out.println("    t=2  执行任务2，TRACE_ID.get() = \"REQ-1\"  （⚠️ 错误，REQ-2 的任务）");
        System.out.println("    t=3  执行任务3，TRACE_ID.get() = \"REQ-1\"  （⚠️ 错误，REQ-3 的任务）");
        System.out.println("    ↑ inheritableThreadLocals 里的值永不更新，因为没有代码去更新它");
        System.out.println();
        System.out.println("  TTL（同一线程池线程视角）：");
        System.out.println("    t=0  线程创建，inheritableThreadLocals 可能为任意值");
        System.out.println("    t=1  执行任务1，TtlRunnable.run() → replay({TRACE_ID:\"REQ-1\"})");
        System.out.println("         业务代码里 TRACE_ID.get() = \"REQ-1\" ✅");
        System.out.println("         任务完成 → restore() 清理");
        System.out.println("    t=2  执行任务2，TtlRunnable.run() → replay({TRACE_ID:\"REQ-2\"})");
        System.out.println("         业务代码里 TRACE_ID.get() = \"REQ-2\" ✅");
        System.out.println("         任务完成 → restore() 清理");
        System.out.println("    ↑ 每次任务前都重新注入正确的值，彻底解决问题");
    }

    // ------------------------------------------------------------------
    // 五、即使换新线程，时序问题照样翻车
    // ------------------------------------------------------------------
    static void timingProblem() throws Exception {
        System.out.println("  有人想到一个「临时方案」：提交任务时先 set，执行完再 remove");
        System.out.println("  这样行得通吗？—— 仍然有问题，而且问题很微妙\n");

        System.out.println("  伪代码（错误方案）：");
        System.out.println("    // 提交线程（请求A）");
        System.out.println("    String traceId = \"REQ-A\";");
        System.out.println("    pool.submit(() -> {");
        System.out.println("        TRACE_ID.set(traceId);   // 在子线程里 set");
        System.out.println("        try { doWork(); }");
        System.out.println("        finally { TRACE_ID.remove(); }");
        System.out.println("    });\n");
        System.out.println("  这实际上不是「传递」而是「在子线程里重新赋值」");
        System.out.println("  如果 doWork() 里又嵌套提交了子任务，嵌套的子任务照样拿不到值\n");

        System.out.println("  更致命的时序问题（高并发场景）：");
        System.out.println();
        System.out.println("  时间线：");
        System.out.println("    主线程                          线程池 Thread-1");
        System.out.println("    ──────────────────────────      ──────────────────────");
        System.out.println("    TRACE_ID.set(\"REQ-A\")");
        System.out.println("    pool.submit(task-A)  ──────────► 队列中等待...");
        System.out.println("    TRACE_ID.set(\"REQ-B\")           ← 主线程立刻处理下一个请求");
        System.out.println("    pool.submit(task-B)             ← 又提交任务B");
        System.out.println("                                    ► task-A 开始执行");
        System.out.println("                                      TRACE_ID.get() = ???");
        System.out.println();
        System.out.println("  如果 task-A 里直接读的是 ThreadLocal（而非快照），");
        System.out.println("  由于主线程已经把值改成 REQ-B，task-A 读到的是 REQ-B —— 完全错误！");
        System.out.println();

        // 演示：直接在 Runnable 里 capture（晚了一步），vs 在提交时 capture（TTL 的做法）
        System.out.println("  演示：提交时捕获 vs 执行时读取 的区别\n");

        TRACE_ID.set("REQ-A-ORIGINAL");
        ExecutorService pool = Executors.newFixedThreadPool(1);

        // 模拟：提交 task-A，但故意让它晚点执行
        // 用 CountDownLatch 控制 task-A 的执行时机
        CountDownLatch latch = new CountDownLatch(1);

        // 方式1：「执行时」才读 TRACE_ID（错误方式，读到的是执行那一刻的值）
        Future<?> taskA_wrong = pool.submit(() -> {
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  [执行时读取-错误方式] TRACE_ID = " + TRACE_ID.get()
                    + " ← 读到的是执行时刻的值，不是提交时的 REQ-A！");
        });

        // 方式2：「提交时」就把值固化到局部变量（模拟 TTL 的 capture）
        String capturedAtSubmitTime = TRACE_ID.get();  // ← 提交时捕获
        Future<?> taskA_correct = pool.submit(() -> {
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  [提交时捕获-正确方式] capturedValue = " + capturedAtSubmitTime
                    + " ✅ 始终是提交时的 REQ-A");
        });

        // 主线程在任务执行之前，把 TRACE_ID 改掉（模拟下一个请求到来）
        Thread.sleep(50);
        TRACE_ID.set("REQ-B-CHANGED");
        System.out.println("  主线程改变 TRACE_ID = " + TRACE_ID.get() + "（模拟下一个请求）");
        latch.countDown();  // 放开任务执行

        taskA_wrong.get();
        taskA_correct.get();
        pool.shutdown();
        TRACE_ID.remove();

        System.out.println();
        System.out.println("  ★ 最终结论：");
        System.out.println("    问题根源不只是「线程复用」，更深层是「ThreadLocal 存的是引用，");
        System.out.println("    而 ThreadLocal 本身是线程隔离的」——不同线程根本看不到彼此的值。");
        System.out.println();
        System.out.println("    解决跨线程传递的正确思路：");
        System.out.println("    1. 在「提交任务的那一刻」把当前线程的上下文值打一个快照（capture）");
        System.out.println("    2. 把快照随着 Runnable 对象一起传递（天然跟着任务走，不依赖线程）");
        System.out.println("    3. 在「执行线程」里把快照里的值写入该线程的 ThreadLocal（replay）");
        System.out.println("    4. 任务结束后还原执行线程的状态，避免污染下一个任务（restore）");
        System.out.println();
        System.out.println("    这正是 TTL（TransmittableThreadLocal）的核心设计哲学：");
        System.out.println("    「上下文跟着任务走，而不是跟着线程走」");
    }
}

