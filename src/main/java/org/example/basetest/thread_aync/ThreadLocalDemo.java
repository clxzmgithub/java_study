package org.example.basetest.thread_aync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ThreadLocal 完整深度解析
 * <p>
 * 目录：
 * 第一部分：ThreadLocal 是什么 & 解决了什么问题（对比无 ThreadLocal 的混乱）
 * 第二部分：ThreadLocal 底层原理（数据结构 & 读写流程）
 * 第三部分：内存泄露是怎么产生的 & 为什么 key 设计为弱引用
 * 第四部分：真实项目场景（用户上下文 / 链路追踪 / 数据库连接）
 * 第五部分：使用规范 & 常见坑
 */
public class ThreadLocalDemo {

    public static void main(String[] args) throws InterruptedException {

        sep("第一部分：ThreadLocal 是什么 & 解决了什么问题");
        WhatIsThreadLocalSection.run();

        sep("第二部分：底层原理（数据结构 & 读写流程）");
        InternalPrincipleSection.run();

        sep("第三部分：内存泄露原因 & 弱引用设计");
        MemoryLeakSection.run();

        sep("第四部分：真实项目场景");
        RealWorldSection.run();

        sep("第五部分：使用规范 & 常见坑");
        BestPracticeSection.run();
    }

    static void sep(String title) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" " + title);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
}

// ====================================================================
// 第一部分：ThreadLocal 是什么 & 解决了什么问题
// ====================================================================
class WhatIsThreadLocalSection {

    // ---- 问题场景：多线程共享一个 userId 变量，互相干扰 ----
    static String sharedUserId = null; // 全局共享变量，危险！

    // ---- 解决方案：ThreadLocal，每个线程独享一份 ----
    static ThreadLocal<String> userIdLocal = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return "100000000000000";
        }
    };
    static ThreadLocal<String> traceIdLocal = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return "";
        }
    };

    static void run() throws InterruptedException {
        System.out.println("【生活类比：ThreadLocal 是什么】\n");
        System.out.println("  想象一家酒店，每个房间（线程）都有自己的保险箱（ThreadLocal）。");
        System.out.println("  保险箱里放的东西（变量值）只有这个房间的客人（线程）能看到，");
        System.out.println("  隔壁房间的客人打开的是他自己的保险箱，里面是他自己的东西。");
        System.out.println("  无论有多少房间，每个人只操作自己的保险箱，互不干扰。\n");

        System.out.println("【对比演示：没有 ThreadLocal 的混乱】\n");

        // 3个线程同时修改 sharedUserId，互相覆盖
        Runnable chaosTask = () -> {
            String myUserId = "user-" + Thread.currentThread().getName();
            sharedUserId = myUserId;           // 我设置了...
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 可能被别的线程覆盖了！
            System.out.println("  线程[" + Thread.currentThread().getName()
                    + "] 设置 userId=" + myUserId
                    + "，读到的却是=" + sharedUserId   // 可能是别人的！
                    + (myUserId.equals(sharedUserId) ? " ✅" : " ❌ 被其他线程污染！"));
        };

        Thread t1 = new Thread(chaosTask, "A");
        Thread t2 = new Thread(chaosTask, "B");
        Thread t3 = new Thread(chaosTask, "C");
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();

        System.out.println("\n【有 ThreadLocal 的情况：每个线程读写自己的副本】\n");

        Runnable safeTask = () -> {
            String myUserId = "user-" + Thread.currentThread().getName();
            userIdLocal.set(myUserId);          // 存到自己线程的保险箱
            traceIdLocal.set(myUserId);          // 存到自己线程的保险箱
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String read = userIdLocal.get();    // 从自己线程的保险箱取，永远是自己的
            String trace = traceIdLocal.get();    // 从自己线程的保险箱取，永远是自己的
            System.out.println("  线程[" + Thread.currentThread().getName()
                    + "] 设置=" + myUserId
                    + "，读到=" + read
                    + (myUserId.equals(read) ? " ✅ 隔离正常" : " ❌"));
            userIdLocal.remove();               // 用完必须清除！
            traceIdLocal.remove();               // 用完必须清除！
        };

        Thread s1 = new Thread(safeTask, "A");
        Thread s2 = new Thread(safeTask, "B");
        Thread s3 = new Thread(safeTask, "C");
        s1.start();
        s2.start();
        s3.start();
        s1.join();
        s2.join();
        s3.join();

        System.out.println("\n【ThreadLocal 的定义】");
        System.out.println("  ThreadLocal 为每个线程提供一个独立的变量副本。");
        System.out.println("  同一个 ThreadLocal 对象，在不同线程里 get() 到的是不同的值。");
        System.out.println("  本质：不是'共享数据 + 加锁'，而是'彻底不共享'。\n");

        System.out.println("  核心 API：");
        System.out.println("    ThreadLocal<T> local = new ThreadLocal<>();");
        System.out.println("    local.set(value)       // 把值存入当前线程的副本");
        System.out.println("    local.get()            // 从当前线程的副本取值");
        System.out.println("    local.remove()         // 清除当前线程的副本（重要！）");
        System.out.println("    ThreadLocal.withInitial(() -> defaultValue) // 设置默认值");
    }
}

// ====================================================================
// 第二部分：ThreadLocal 底层原理
// ====================================================================
class InternalPrincipleSection {

    static void run() {
        System.out.println("【底层数据结构】\n");
        System.out.println("  错误认知：ThreadLocal 内部有一个大 Map<Thread, T>，所有线程共享");
        System.out.println("           ThreadLocal.get() 以当前线程为 key 去查这个 Map\n");
        System.out.println("  真实设计：每个 Thread 对象内部有一个 ThreadLocalMap（不是 ThreadLocal 内部的！）");
        System.out.println("           ThreadLocalMap 的 key 是 ThreadLocal 对象，value 是变量值");
        System.out.println("           ThreadLocal.get() 实际是：");
        System.out.println("             1. 拿到当前线程 Thread.currentThread()");
        System.out.println("             2. 拿到该线程的 threadLocals（ThreadLocalMap）");
        System.out.println("             3. 以 this（ThreadLocal对象本身）为 key，查 value\n");

        System.out.println("  JDK 源码等价逻辑（简化）：");
        System.out.println("  ─────────────────────────────────────────────────");
        System.out.println("  // Thread 类内部");
        System.out.println("  class Thread {");
        System.out.println("      ThreadLocal.ThreadLocalMap threadLocals = null; // ← 存在 Thread 里！");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // ThreadLocal 类的 get() 方法");
        System.out.println("  public T get() {");
        System.out.println("      Thread t = Thread.currentThread();       // ① 拿当前线程");
        System.out.println("      ThreadLocalMap map = t.threadLocals;     // ② 拿这个线程的 Map");
        System.out.println("      if (map != null) {");
        System.out.println("          Entry e = map.getEntry(this);        // ③ this=ThreadLocal对象，作为 key");
        System.out.println("          if (e != null) return (T) e.value;  // ④ 返回 value");
        System.out.println("      }");
        System.out.println("      return setInitialValue();");
        System.out.println("  }");
        System.out.println("  ─────────────────────────────────────────────────\n");

        System.out.println("【数据结构图】\n");
        System.out.println("  " + getString() + "存");
        System.out.println("  ┌────────────────────────────────────────────────────────┐");
        System.out.println("  │  Thread-A                                              │");
        System.out.println("  │  ┌─────────────────────────────────────────────────┐  │");
        System.out.println("  │  │ threadLocals (ThreadLocalMap)                   │  │");
        System.out.println("  │  │  Entry[ key=userIdLocal(弱引用), value=\"user-A\" ] │  │");
        System.out.println("  │  │  Entry[ key=traceIdLocal(弱引用), value=\"T-001\" ] │  │");
        System.out.println("  │  └─────────────────────────────────────────────────┘  │");
        System.out.println("  └────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  ┌────────────────────────────────────────────────────────┐");
        System.out.println("  │  Thread-B                                              │");
        System.out.println("  │  ┌─────────────────────────────────────────────────┐  │");
        System.out.println("  │  │ threadLocals (ThreadLocalMap)                   │  │");
        System.out.println("  │  │  Entry[ key=userIdLocal(弱引用), value=\"user-B\" ] │  │");
        System.out.println("  │  │  Entry[ key=traceIdLocal(弱引用), value=\"T-002\" ] │  │");
        System.out.println("  │  └─────────────────────────────────────────────────┘  │");
        System.out.println("  └────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  同一个 userIdLocal 对象，在 Thread-A 里查到 \"user-A\"，");
        System.out.println("  在 Thread-B 里查到 \"user-B\"，互不干扰。\n");

        System.out.println("【关键认知：Map 在 Thread 上，不在 ThreadLocal 上】");
        System.out.println("  错误认知：ThreadLocal 内有 Map，Thread 是 key");
        System.out.println("            → 问题：ThreadLocal 持有所有线程的引用，线程无法被 GC");
        System.out.println("  正确设计：Map 在 Thread 内，ThreadLocal 是 key");
        System.out.println("            → 好处：线程结束时，Thread 对象被 GC，threadLocals 随之释放");
        System.out.println("            → 但这不够，还需要 key 用弱引用（见第三部分）");

        System.out.println("\n【用代码验证：同一 ThreadLocal，不同线程得到不同值】\n");
        ThreadLocal<String> local = ThreadLocal.withInitial(() -> "默认值");

        Thread ta = new Thread(() -> {
            System.out.println("  Thread-A 初始值: " + local.get());
            local.set("Thread-A 的专属数据");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  Thread-A 读取:   " + local.get());
            local.remove();
        });
        Thread tb = new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  Thread-B 初始值: " + local.get());  // 看不到 Thread-A 设置的值
            local.set("Thread-B 的专属数据");
            System.out.println("  Thread-B 读取:   " + local.get());
            local.remove();
        });
        ta.start();
        tb.start();
        try {
            ta.join();
            tb.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String getString() {
        return "JVM 内";
    }
}

// ====================================================================
// 第三部分：内存泄露原因 & 弱引用设计
// ====================================================================
class MemoryLeakSection {

    static void run() {
        System.out.println("【先理解强引用 vs 弱引用】\n");
        System.out.println("  强引用（StrongReference）：普通的 Object obj = new Object()");
        System.out.println("    只要强引用存在，GC 永远不会回收这个对象，哪怕 OOM 也不回收。\n");
        System.out.println("  弱引用（WeakReference）：new WeakReference<>(obj)");
        System.out.println("    只有弱引用指向这个对象时（无强引用），GC 下次运行就会回收它。\n");

        System.out.println("【ThreadLocalMap 的 Entry 结构】\n");
        System.out.println("  static class Entry extends WeakReference<ThreadLocal<?>> {");
        System.out.println("      Object value;  // value 是强引用");
        System.out.println("      Entry(ThreadLocal<?> k, Object v) {");
        System.out.println("          super(k);  // ← key(ThreadLocal对象)是弱引用");
        System.out.println("          value = v; // ← value 是强引用");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println("  ─────────────────────────────────────────────────");
        System.out.println("  Entry 里：key（ThreadLocal 对象）是弱引用");
        System.out.println("            value（存的数据）是强引用\n");

        System.out.println("【内存泄露的产生过程（配合图来理解）】\n");
        System.out.println("  背景：线程池中的线程会被反复复用（不会被销毁）");
        System.out.println();
        System.out.println("  步骤 1：正常使用时，内存引用链：");
        System.out.println();
        System.out.println("  栈帧 (方法内局部变量)");
        System.out.println("    userLocal ──────────────────────────► ThreadLocal 对象");
        System.out.println("    (强引用)                                      │");
        System.out.println("                                                   │ (ThreadLocalMap 的 key，弱引用)");
        System.out.println("                                                   ▼");
        System.out.println("  Thread.threadLocals  ──► Entry[ key=弱引用→ThreadLocal, value=强引用→\"user-A\" ]");
        System.out.println();
        System.out.println("  步骤 2：方法执行完，局部变量 userLocal 出栈（强引用断开）：");
        System.out.println();
        System.out.println("  栈帧                    ThreadLocal 对象");
        System.out.println("  userLocal (已出栈)  ✂️  (只剩 Entry 的弱引用指向它)");
        System.out.println();
        System.out.println("  此时 ThreadLocal 对象只有弱引用，下次 GC 时被回收：");
        System.out.println("  Entry 变成：[ key=null（弱引用指向的对象被回收了）, value=强引用→\"user-A\" ]");
        System.out.println();
        System.out.println("  步骤 3：⚠️ 内存泄露发生！");
        System.out.println("  Thread 仍然活着（线程池的线程不死）");
        System.out.println("    → threadLocals 仍然存在");
        System.out.println("      → Entry 仍然存在");
        System.out.println("        → value（\"user-A\"）被 Entry 强引用着，永远无法被 GC！");
        System.out.println();
        System.out.println("  这就是内存泄露：key 已经是 null，但 value 还在被强引用，无法释放\n");

        System.out.println("【为什么 key 要设计为弱引用？（关键！）】\n");
        System.out.println("  假设 key 也是强引用，会发生什么？");
        System.out.println("  ─────────────────────────────────────────────────");
        System.out.println("  方法结束后，局部变量 userLocal 出栈（强引用断开）。");
        System.out.println("  BUT！Entry 的 key 还是强引用指向 ThreadLocal 对象！");
        System.out.println("  → ThreadLocal 对象永远无法被 GC（两处强引用：局部变量 + Entry.key）");
        System.out.println("  → 不仅 value 泄露，ThreadLocal 对象本身也泄露了！");
        System.out.println("  → 泄露的东西更多，问题更严重。\n");
        System.out.println("  设计为弱引用的好处：");
        System.out.println("  ─────────────────────────────────────────────────");
        System.out.println("  局部变量出栈后，ThreadLocal 对象只有弱引用，GC 时被回收。");
        System.out.println("  Entry.key 变为 null，这是一个明确的信号：");
        System.out.println("  '这个 Entry 的主人（ThreadLocal）已经消失了，这个 Entry 是垃圾！'");
        System.out.println("  ThreadLocalMap 在 get/set/remove 时会扫描 key==null 的 Entry，");
        System.out.println("  主动把这些'孤儿 Entry'的 value 也置为 null，帮助 GC。\n");
        System.out.println("  总结：弱引用是'前半段'保护（保护 ThreadLocal 对象本身不泄露）");
        System.out.println("        但 value 的'后半段'仍然需要靠我们手动 remove() 来保护！\n");

        System.out.println("【引用链对比总结】\n");
        System.out.println("  ┌──────────────────────────────────────────────────────┐");
        System.out.println("  │           key 是强引用（假设）       key 是弱引用（实际）│");
        System.out.println("  ├──────────────────────────────────────────────────────┤");
        System.out.println("  │ 局部变量出栈  ThreadLocal 泄露 ✘   ThreadLocal 被 GC ✅ │");
        System.out.println("  │              value 也泄露    ✘   value 仍然泄露 ⚠️   │");
        System.out.println("  │ 调用 remove() ThreadLocal 泄露 ✘   ThreadLocal 被 GC ✅ │");
        System.out.println("  │              value 也泄露    ✘   value 被清除    ✅  │");
        System.out.println("  └──────────────────────────────────────────────────────┘\n");
        System.out.println("  结论：弱引用 + remove() 组合使用，才能彻底杜绝内存泄露");
        System.out.println("        只有弱引用，value 仍可能泄露");
        System.out.println("        只有 remove()，ThreadLocal 对象可能先泄露");
    }
}

// ====================================================================
// 第四部分：真实项目场景
// ====================================================================
class RealWorldSection {

    // ================================================================
    // 场景 1：Web 请求上下文（最经典的用法）
    // ================================================================
    static class UserContext {
        private final String userId;
        private final String userName;
        private final String token;
        private final long requestTime;

        UserContext(String userId, String userName, String token) {
            this.userId = userId;
            this.userName = userName;
            this.token = token;
            this.requestTime = System.currentTimeMillis();
        }

        String getUserId() {
            return userId;
        }

        String getUserName() {
            return userName;
        }

        String getToken() {
            return token;
        }

        @Override
        public String toString() {
            return "UserContext{userId='" + userId + "', userName='" + userName + "'}";
        }
    }

    // 全局 UserContext 持有者（工具类，供整个调用链使用）
    static class UserContextHolder {
        // ThreadLocal 通常定义为 static final（一个 JVM 全局只有一个 ThreadLocal 对象）
        private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

        static void set(UserContext ctx) {
            CONTEXT.set(ctx);
        }

        static UserContext get() {
            return CONTEXT.get();
        }

        static void clear() {
            CONTEXT.remove();
        }   // 必须在请求结束时调用

        static String getUserId() {
            UserContext ctx = CONTEXT.get();
            return ctx != null ? ctx.getUserId() : null;
        }
    }

    // 模拟三层调用：Controller → Service → DAO（不传参，直接从 ThreadLocal 取）
    static class OrderController {
        static void handleRequest(String userId, String userName) {
            // 模拟 Filter/Interceptor 在请求入口设置上下文
            UserContextHolder.set(new UserContext(userId, userName, "token-" + userId));
            System.out.println("  [Controller] 收到请求，设置用户上下文: " + UserContextHolder.get());
            try {
                OrderService.createOrder("iPhone 15");
            } finally {
                // 请求结束必须清除，防止线程池场景下数据污染下一个请求
                UserContextHolder.clear();
                System.out.println("  [Filter]     请求结束，清除 ThreadLocal\n");
            }
        }
    }

    static class OrderService {
        static void createOrder(String product) {
            // Service 层直接从 ThreadLocal 取用户信息，不需要 Controller 传参
            String userId = UserContextHolder.getUserId();
            System.out.println("  [Service]    创建订单，userId=" + userId
                    + "，商品=" + product + "（不需要从参数传入 userId！）");
            OrderDao.insertOrder(userId, product);
        }
    }

    static class OrderDao {
        static void insertOrder(String userId, String product) {
            // DAO 层同样能取到，整个调用链都在同一个线程，ThreadLocal 天然传递
            String ctxUserId = UserContextHolder.getUserId();
            System.out.println("  [DAO]        插入订单，userId=" + userId
                    + "，ThreadLocal中userId=" + ctxUserId
                    + "（DAO层无需接收参数，直接从上下文取）");
        }
    }

    // ================================================================
    // 场景 2：链路追踪（MDC / TraceId）
    // ================================================================
    static class TraceContext {
        private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

        static void setTraceId(String traceId) {
            TRACE_ID.set(traceId);
        }

        static String getTraceId() {
            return TRACE_ID.get();
        }

        static void clear() {
            TRACE_ID.remove();
        }

        // 模拟带 traceId 的日志打印
        static void log(String level, String msg) {
            String traceId = getTraceId();
            System.out.println("  [" + level + "][traceId=" + traceId + "] " + msg);
        }
    }

    // ================================================================
    // 场景 3：线程安全的 SimpleDateFormat（经典案例）
    // ================================================================
    // SimpleDateFormat 线程不安全，多线程共享会出问题
    // 解决方案：每个线程一个副本
    static final ThreadLocal<java.text.SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    static String formatDate(java.util.Date date) {
        return DATE_FORMAT.get().format(date); // 每个线程用自己的 SimpleDateFormat，线程安全
    }

    // ================================================================
    // 场景 4：数据库连接绑定（事务管理核心原理）
    // ================================================================
    static class ConnectionHolder {
        // Spring 事务管理的核心原理就是这个！
        // @Transactional 的 begin/commit/rollback 能在同一个连接上操作，
        // 靠的就是把 Connection 绑定到 ThreadLocal
        private static final ThreadLocal<String> CONN = new ThreadLocal<>();

        static void bindConnection(String conn) {
            System.out.println("    [事务] 开启事务，绑定连接=" + conn + " 到当前线程");
            CONN.set(conn);
        }

        static String getConnection() {
            return CONN.get();
        }

        static void unbindConnection() {
            System.out.println("    [事务] 事务结束，解绑连接=" + CONN.get());
            CONN.remove();
        }
    }

    static void run() throws InterruptedException {
        System.out.println("【场景 1：Web 请求上下文（解决参数透传问题）】\n");
        System.out.println("  痛点：userId 需要从 Controller 一路传到 DAO，每个方法都要加参数，非常臃肿");
        System.out.println("  方案：请求入口（Filter）存入 ThreadLocal，整个调用链直接取，无需传参\n");

        // 模拟两个并发请求，用不同线程代表
        Thread req1 = new Thread(() -> OrderController.handleRequest("U001", "张三"), "请求线程-1");
        Thread req2 = new Thread(() -> OrderController.handleRequest("U002", "李四"), "请求线程-2");

        req1.start();
        req1.join();
        req2.start();
        req2.join();

        System.out.println("  ✅ 两个请求的 userId 互不干扰，即使并发也安全\n");

        System.out.println("【场景 2：链路追踪（TraceId 全链路传递）】\n");
        System.out.println("  痛点：分布式系统中，一个请求经过多个服务/方法，日志散落各处，难以关联");
        System.out.println("  方案：请求入口生成 TraceId 存入 ThreadLocal，所有日志自动携带 TraceId\n");

        Thread traceReq = new Thread(() -> {
            String traceId = "TRACE-" + System.currentTimeMillis();
            TraceContext.setTraceId(traceId);
            try {
                TraceContext.log("INFO", "收到下单请求，商品=MacBook");
                TraceContext.log("INFO", "调用库存服务，扣减库存");
                TraceContext.log("INFO", "调用支付服务，扣款成功");
                TraceContext.log("INFO", "发送 MQ 消息，通知发货");
                TraceContext.log("INFO", "订单创建完成");
            } finally {
                TraceContext.clear();
            }
        }, "业务线程");
        traceReq.start();
        traceReq.join();
        System.out.println("  ✅ 同一请求的所有日志都有相同 TraceId，一搜即可关联全链路\n");

        System.out.println("【场景 3：线程安全的 SimpleDateFormat】\n");
        System.out.println("  痛点：SimpleDateFormat 内部有状态（Calendar 字段），多线程共享会产生错乱");
        System.out.println("  方案：ThreadLocal<SimpleDateFormat>，每个线程持有自己的实例\n");

        ExecutorService pool = Executors.newFixedThreadPool(3);
        java.util.Date now = new java.util.Date();
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {
                String formatted = formatDate(now);
                System.out.println("  线程[" + Thread.currentThread().getName()
                        + "] 格式化结果: " + formatted);
            });
        }
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("  ✅ 每个线程用自己的 SimpleDateFormat 实例，不会互相干扰\n");

        System.out.println("【场景 4：数据库连接绑定（@Transactional 的底层原理）】\n");
        System.out.println("  痛点：事务要求同一个请求的所有 SQL 在同一个 Connection 上执行");
        System.out.println("        但 Service 调 DAO 时如何保证拿到的是同一个 Connection？");
        System.out.println("  方案：开启事务时把 Connection 绑定到 ThreadLocal，DAO 层直接取\n");

        // 模拟 Spring @Transactional 的工作原理
        Thread txThread = new Thread(() -> {
            System.out.println("  [业务方法] 开始执行（@Transactional 切面介入）");
            ConnectionHolder.bindConnection("conn#12345");
            try {
                // 模拟多次 DAO 操作，都用同一个 Connection
                System.out.println("    [DAO-1] 插入订单，使用连接=" + ConnectionHolder.getConnection());
                System.out.println("    [DAO-2] 扣减库存，使用连接=" + ConnectionHolder.getConnection());
                System.out.println("    [DAO-3] 记录日志，使用连接=" + ConnectionHolder.getConnection());
                System.out.println("    [事务] 三个 SQL 在同一个 Connection 上，可以统一 commit/rollback");
            } finally {
                ConnectionHolder.unbindConnection();
            }
        }, "事务线程");
        txThread.start();
        txThread.join();
        System.out.println("  ✅ Spring @Transactional 的核心就是这个原理");
    }
}

// ====================================================================
// 第五部分：使用规范 & 常见坑
// ====================================================================
class BestPracticeSection {

    static void run() throws InterruptedException {
        System.out.println("【规范 1：必须在 finally 块中调用 remove()】\n");
        System.out.println("  ❌ 错误写法：");
        System.out.println("    threadLocal.set(value);");
        System.out.println("    doSomething(); // 如果抛异常，remove() 不会执行");
        System.out.println("    threadLocal.remove(); // 可能执行不到！\n");
        System.out.println("  ✅ 正确写法：");
        System.out.println("    threadLocal.set(value);");
        System.out.println("    try {");
        System.out.println("        doSomething();");
        System.out.println("    } finally {");
        System.out.println("        threadLocal.remove(); // 无论是否异常，都会执行");
        System.out.println("    }\n");

        System.out.println("【规范 2：线程池场景下的数据污染问题（最常见的坑！）】\n");
        System.out.println("  线程池中的线程会被复用，上一个任务没有 remove()，");
        System.out.println("  下一个任务就会读到上一个任务残留的数据！\n");

        // 演示数据污染
        ThreadLocal<String> tl = new ThreadLocal<>();
        ExecutorService pool = Executors.newSingleThreadExecutor(); // 单线程池，确保复用同一个线程

        // 第一个任务：设置了值，但没有 remove()
        pool.submit(() -> {
            tl.set("任务1的数据");
            System.out.println("  任务1 执行中，threadLocal=" + tl.get());
            // ❌ 忘记 remove() ！
        });
        Thread.sleep(100);

        // 第二个任务：没有 set()，但读到了任务1的残留数据
        pool.submit(() -> {
            System.out.println("  任务2 执行，没有set，读到的值=" + tl.get()
                    + " ← ⚠️ 这是任务1的残留数据！数据污染！");
            tl.remove(); // 这次清一下
        });
        Thread.sleep(100);

        // 第三个任务：正常，因为任务2 remove() 了
        pool.submit(() -> {
            System.out.println("  任务3 执行，没有set，读到的值=" + tl.get() + " ← ✅ 正常，null");
        });

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println();
        System.out.println("【规范 3：ThreadLocal 应定义为 static final】\n");
        System.out.println("  ❌ 错误：定义为实例变量");
        System.out.println("    class UserService {");
        System.out.println("        ThreadLocal<String> tl = new ThreadLocal<>(); // 每个 UserService 实例一个 tl");
        System.out.println("    }");
        System.out.println("    问题：每次 new UserService()，就创建一个新的 ThreadLocal，");
        System.out.println("          浪费内存，且很难做到正确 remove()\n");
        System.out.println("  ✅ 正确：定义为 static final");
        System.out.println("    class UserContextHolder {");
        System.out.println("        private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();");
        System.out.println("    }");
        System.out.println("    整个 JVM 只有一个 ThreadLocal 对象，通过它的 threadLocalHashCode");
        System.out.println("    在每个线程的 ThreadLocalMap 里找到对应 Entry\n");

        System.out.println("【规范 4：InheritableThreadLocal —— 父子线程传值】\n");
        System.out.println("  普通 ThreadLocal：子线程无法继承父线程的值");
        System.out.println("  InheritableThreadLocal：子线程创建时，自动复制父线程的所有值\n");

        ThreadLocal<String> normal = new ThreadLocal<>();
        InheritableThreadLocal<String> inheritable = new InheritableThreadLocal<>();

        normal.set("父线程的普通值");
        inheritable.set("父线程的可继承值");

        Thread child = new Thread(() -> {
            System.out.println("  子线程读 ThreadLocal:            " + normal.get()
                    + "（null，继承不了）");
            System.out.println("  子线程读 InheritableThreadLocal: " + inheritable.get()
                    + "（✅ 继承成功）");
        });
        child.start();
        child.join();

        normal.remove();
        inheritable.remove();

        System.out.println();
        System.out.println("  ⚠️ 注意：线程池中 InheritableThreadLocal 也有坑");
        System.out.println("          线程复用时，子线程是旧线程，不会重新继承新父线程的值");
        System.out.println("          解决方案：阿里开源的 TransmittableThreadLocal（TTL）");
        System.out.println("            <dependency>");
        System.out.println("                <groupId>com.alibaba</groupId>");
        System.out.println("                <artifactId>transmittable-thread-local</artifactId>");
        System.out.println("            </dependency>\n");

        System.out.println("【总结：ThreadLocal 使用场景 & 选型】\n");
        System.out.println("  ┌────────────────────┬─────────────────────────────────────────┐");
        System.out.println("  │ 场景               │ 推荐方案                                 │");
        System.out.println("  ├────────────────────┼─────────────────────────────────────────┤");
        System.out.println("  │ 请求用户上下文传递   │ ThreadLocal（最经典用法）                 │");
        System.out.println("  │ 链路追踪 TraceId    │ ThreadLocal（MDC 底层就是 ThreadLocal）  │");
        System.out.println("  │ 数据库事务连接      │ ThreadLocal（Spring @Transactional 原理）│");
        System.out.println("  │ 线程不安全工具类     │ ThreadLocal（如 SimpleDateFormat）       │");
        System.out.println("  │ 父子线程传值        │ InheritableThreadLocal                   │");
        System.out.println("  │ 线程池跨线程传值     │ TransmittableThreadLocal（阿里 TTL）      │");
        System.out.println("  └────────────────────┴─────────────────────────────────────────┘\n");

        System.out.println("  不适合用 ThreadLocal 的场景：");
        System.out.println("    ❌ 需要多个线程共享同一份数据 → 用锁 / ConcurrentHashMap");
        System.out.println("    ❌ 需要跨线程通信 → 用 BlockingQueue / Future");
        System.out.println("    ❌ 数据量很大 → ThreadLocal 副本多，内存占用翻倍");
    }
}

