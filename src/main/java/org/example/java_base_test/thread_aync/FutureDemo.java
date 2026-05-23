package org.example.java_base_test.thread_aync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Callable / Future / FutureTask / CompletableFuture 深度解析
 *
 * 目录：
 *   第一部分：Callable —— 有返回值的任务
 *   第二部分：Future   —— 异步结果的句柄
 *   第三部分：FutureTask —— Callable + Future 的桥梁（可独立运行）
 *   第四部分：CompletableFuture —— 异步编排利器
 *   第五部分：四者对比与选型指南
 */
public class FutureDemo {

    public static void main(String[] args) throws Exception {
        sep("第一部分：Callable —— 有返回值的任务");
        Part1_Callable.run();

        sep("第二部分：Future —— 异步结果的句柄");
        Part2_Future.run();

        sep("第三部分：FutureTask —— Callable + Future 的桥梁");
        Part3_FutureTask.run();

        sep("第四部分：CompletableFuture —— 异步编排利器");
        Part4_CompletableFuture.run();

        sep("第五部分：四者对比与选型指南");
        Part5_Comparison.run();

        sep("第六部分：thenApply vs thenCompose 深度对比");
        Part6_ThenApplyVsCompose.run();

        sep("第七部分：CompletableFuture 深度讲解（含实际案例）");
        Part7_CompletableFutureDeep.run();

        sep("第八部分：四者对比与选型指南（层层递进）");
        Part8_Evolution.run();

        sep("第九部分：thenApply vs thenCompose 深入剖析");
        Part9_ThenApplyVsCompose.run();
    }

    static void sep(String title) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" " + title);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
}

// ====================================================================
// 第一部分：Callable
// ====================================================================
class Part1_Callable {
    static void run() throws Exception {
        System.out.println("【Callable 是什么】\n");
        System.out.println("  Runnable（老接口）：void run()        → 执行任务，无返回值，不能抛受检异常");
        System.out.println("  Callable（JDK1.5）：V    call()       → 执行任务，有返回值，可以抛受检异常\n");

        System.out.println("  接口定义对比：");
        System.out.println("  ┌─────────────────────────────────────────────────┐");
        System.out.println("  │  // Runnable                                    │");
        System.out.println("  │  public interface Runnable {                    │");
        System.out.println("  │      void run();  // 无返回值，不抛受检异常     │");
        System.out.println("  │  }                                              │");
        System.out.println("  │                                                 │");
        System.out.println("  │  // Callable<V>                                 │");
        System.out.println("  │  public interface Callable<V> {                 │");
        System.out.println("  │      V call() throws Exception; // 有返回值     │");
        System.out.println("  │  }                                              │");
        System.out.println("  └─────────────────────────────────────────────────┘\n");

        System.out.println("【Callable 基本用法演示】\n");

        // 定义一个 Callable：模拟查询数据库，返回用户名
        Callable<String> queryUser = () -> {
            System.out.println("  [Callable] 正在查询用户...");
            Thread.sleep(500); // 模拟耗时
            return "张三";   // 返回查询结果
        };

        // Callable 不能直接提交给 Thread，必须配合 FutureTask 或 ExecutorService
        // 直接调用 call() 是同步的（和普通方法调用一样）
        System.out.println("  直接调用 call()（同步，阻塞直到返回）：");
        String result = queryUser.call();
        System.out.println("  返回值：" + result);

        System.out.println();
        System.out.println("  注意：Callable 单独用价值不大，真正的威力在于配合 Future/线程池使用");
        System.out.println("  Callable 解决的核心问题：「我想知道异步任务的执行结果」");
    }
}

// ====================================================================
// 第二部分：Future
// ====================================================================
class Part2_Future {
    static void run() throws Exception {
        System.out.println("【Future 是什么】\n");
        System.out.println("  Future<V> 是一个接口，代表「一个将来某时刻才会有的结果」");
        System.out.println("  就像快递单号：快递还在路上，但你手里已经有了「凭证」，");
        System.out.println("  随时可以用它查快递状态、等快递到达、或者取消快递。\n");

        System.out.println("  Future 接口的5个方法：");
        System.out.println("  ┌────────────────────────────────────────────────────────────┐");
        System.out.println("  │  boolean cancel(boolean mayInterruptIfRunning)             │");
        System.out.println("  │      → 取消任务（如果还没开始执行，或mayInterrupt=true）   │");
        System.out.println("  │                                                            │");
        System.out.println("  │  boolean isCancelled()                                    │");
        System.out.println("  │      → 任务是否被取消                                     │");
        System.out.println("  │                                                            │");
        System.out.println("  │  boolean isDone()                                         │");
        System.out.println("  │      → 任务是否完成（正常完成/异常/取消 都算Done）         │");
        System.out.println("  │                                                            │");
        System.out.println("  │  V get()                                                  │");
        System.out.println("  │      → 阻塞等待结果，直到任务完成                         │");
        System.out.println("  │                                                            │");
        System.out.println("  │  V get(long timeout, TimeUnit unit)                       │");
        System.out.println("  │      → 最多等 timeout 时间，超时抛 TimeoutException        │");
        System.out.println("  └────────────────────────────────────────────────────────────┘\n");

        System.out.println("【Future 基本用法演示】\n");

        ExecutorService pool = Executors.newFixedThreadPool(3);

        // 提交 Callable，立即返回 Future（不阻塞）
        System.out.println("  提交3个异步任务...");
        long start = System.currentTimeMillis();

        Future<Integer> f1 = pool.submit(() -> {
            Thread.sleep(300);
            System.out.println("  [任务1] 完成，耗时300ms");
            return 100;
        });

        Future<Integer> f2 = pool.submit(() -> {
            Thread.sleep(300);
            System.out.println("  [任务2] 完成，耗时500ms");
            return 200;
        });

        Future<Integer> f3 = pool.submit(() -> {
            Thread.sleep(300);
            System.out.println("  [任务3] 完成，耗时200ms");
            return 300;
        });

        System.out.println("  任务已提交，主线程继续做其他事情...");
        System.out.println("  isDone f1=" + f1.isDone() + " f2=" + f2.isDone() + " f3=" + f3.isDone());

        // 等待并收集结果（get() 会阻塞）
        int sum = f1.get() + f2.get() + f3.get();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  3个任务总耗时：" + elapsed + "ms（并行执行，最长的那个500ms决定总耗时）");
        System.out.println("  结果之和：" + sum + "（100+200+300）");

        System.out.println();
        System.out.println("【Future 的核心缺陷】\n");
        System.out.println("  ❌ get() 是阻塞的：调用 get() 会阻塞主线程直到结果返回，无法真正异步");
        System.out.println("  ❌ 无法链式编排：任务A结束后自动触发任务B，Future 做不到");
        System.out.println("  ❌ 无法合并结果：等待多个 Future 都完成后合并，写起来很麻烦");
        System.out.println("  ❌ 异常处理不便：get() 抛 ExecutionException，需要层层 unwrap");
        System.out.println("  → 这些缺陷催生了 CompletableFuture（第四部分）");

        pool.shutdown();

        System.out.println();
        System.out.println("【Future.cancel() 演示】\n");
        demo_cancel();
    }

    static void demo_cancel() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<String> future = pool.submit(() -> {
            System.out.println("  [任务] 开始执行，预计需要3秒...");
            try {
                Thread.sleep(3000);
                return "完成";
            } catch (InterruptedException e) {
                System.out.println("  [任务] 被中断了！");
                return "被取消";
            }
        });

        Thread.sleep(100); // 等任务开始
        System.out.println("  isCancelled=" + future.isCancelled() + " isDone=" + future.isDone());

        boolean cancelled = future.cancel(true); // true = 允许中断正在执行的线程
        System.out.println("  cancel() 返回：" + cancelled);
        System.out.println("  isCancelled=" + future.isCancelled() + " isDone=" + future.isDone());

        try {
            future.get(); // 已取消，会抛 CancellationException
        } catch (CancellationException e) {
            System.out.println("  get() 抛出 CancellationException（任务已被取消）✓");
        }

        pool.shutdown();
    }
}

// ====================================================================
// 第三部分：FutureTask
// ====================================================================
class Part3_FutureTask {
    static void run() throws Exception {
        System.out.println("【FutureTask 是什么】\n");
        System.out.println("  FutureTask<V> 同时实现了 Runnable 和 Future<V>：");
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │  class FutureTask<V> implements RunnableFuture<V>            │");
        System.out.println("  │  interface RunnableFuture<V> extends Runnable, Future<V>     │");
        System.out.println("  │                                                              │");
        System.out.println("  │  所以 FutureTask 既是一个 Runnable（可以被线程/线程池执行）  │");
        System.out.println("  │              又是一个 Future（可以获取执行结果）              │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘\n");

        System.out.println("  核心价值：把 Callable 包装成可以被 Thread 直接运行的对象");
        System.out.println("  没有 FutureTask，Callable 无法直接被 Thread 执行（Thread只认Runnable）\n");

        System.out.println("【用法一：配合 Thread 使用（不用线程池）】\n");
        demo_withThread();

        System.out.println("\n【用法二：配合线程池使用】\n");
        demo_withPool();

        System.out.println("\n【用法三：缓存/幂等场景（FutureTask 的特殊价值）】\n");
        demo_cache();

        System.out.println("\n【FutureTask 内部状态机】\n");
        System.out.println("  NEW → COMPLETING → NORMAL       （正常完成）");
        System.out.println("  NEW → COMPLETING → EXCEPTIONAL  （异常完成）");
        System.out.println("  NEW → CANCELLED                 （任务取消）");
        System.out.println("  NEW → INTERRUPTING → INTERRUPTED（中断取消）\n");
        System.out.println("  get() 的阻塞原理：当状态是 NEW 或 COMPLETING 时，调用者线程进入等待队列");
        System.out.println("  任务完成后，FutureTask 唤醒所有等待的线程");
    }

    static void demo_withThread() throws Exception {
        // FutureTask 包装 Callable
        FutureTask<Integer> futureTask = new FutureTask<>(() -> {
            System.out.println("  [子线程] 计算中...");
            Thread.sleep(300);
            return 42;
        });

        // FutureTask 是 Runnable，可以直接给 Thread
        Thread t = new Thread(futureTask, "calculator");
        t.start();

        System.out.println("  [主线程] 子线程已启动，主线程继续...");
        System.out.println("  [主线程] 等待结果...");

        Integer result = futureTask.get(); // 阻塞等待
        System.out.println("  [主线程] 结果：" + result);
    }

    static void demo_withPool() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        FutureTask<String> task = new FutureTask<>(() -> {
            Thread.sleep(200);
            return "线程池执行完毕";
        });

        // 两种提交方式都可以，因为 FutureTask 是 Runnable
        pool.execute(task);       // 方式一：execute（无返回Future，用task自身的Future）
        // pool.submit(task);     // 方式二：submit（返回一个包装的Future，通常不用）

        System.out.println("  通过 execute 提交 FutureTask，用 task.get() 获取结果");
        System.out.println("  结果：" + task.get());

        pool.shutdown();
    }

    static void demo_cache() throws Exception {
        System.out.println("  场景：高并发下，同一个耗时查询只执行一次，其他线程复用结果");
        System.out.println("  FutureTask 天然支持这个场景（多个线程 get() 同一个 task，只执行一次）\n");

        AtomicInteger execCount = new AtomicInteger(0);

        // 同一个 FutureTask，多个线程共享
        FutureTask<String> sharedTask = new FutureTask<>(() -> {
            execCount.incrementAndGet();
            System.out.println("  ★ 耗时查询真正执行了（只应该出现一次）");
            Thread.sleep(300);
            return "查询结果";
        });

        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(1);

        // 5个线程同时 get() 同一个 FutureTask
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            pool.execute(() -> {
                try {
                    latch.await(); // 等所有线程就绪，一起触发
                    String result = sharedTask.get();
                    System.out.println("  线程" + id + " 得到结果：" + result);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 启动任务（只启动一次）
        new Thread(sharedTask).start();

        latch.countDown(); // 5个线程同时开始 get()
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("  耗时查询实际执行次数：" + execCount.get() + "（5个线程只触发了1次）✓");
    }
}

// ====================================================================
// 第四部分：CompletableFuture
// ====================================================================
class Part4_CompletableFuture {
    static void run() throws Exception {
        System.out.println("【CompletableFuture 是什么】\n");
        System.out.println("  JDK 8 引入，是对 Future 的全面升级，解决了 Future 的所有痛点：");
        System.out.println("  ✅ 非阻塞回调：任务完成后自动触发下一步，不需要手动 get()");
        System.out.println("  ✅ 链式编排：thenApply → thenCompose → thenAccept 流水线");
        System.out.println("  ✅ 组合多个异步：allOf（全部完成）、anyOf（任意完成）");
        System.out.println("  ✅ 异常处理：exceptionally、handle 优雅处理异常");
        System.out.println("  ✅ 手动完成：可以随时调用 complete() 手动设置结果\n");

        System.out.println("【一、基础：创建 CompletableFuture】\n");
        demo_create();

        System.out.println("\n【二、链式编排：thenApply / thenAccept / thenRun】\n");
        demo_chain();

        System.out.println("\n【三、组合：thenCompose（串行依赖）vs thenCombine（并行合并）】\n");
        demo_combine();

        System.out.println("\n【四、等待多个任务：allOf 和 anyOf】\n");
        demo_allOfAnyOf();

        System.out.println("\n【五、异常处理：exceptionally 和 handle】\n");
        demo_exception();

        System.out.println("\n【六、实际业务场景：电商下单并行查询】\n");
        demo_realWorld();
    }

    static void demo_create() throws Exception {
        // 方式一：runAsync（无返回值）
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() ->
                System.out.println("  runAsync：异步执行，无返回值，线程=" + Thread.currentThread().getName()));

        // 方式二：supplyAsync（有返回值）
        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("  supplyAsync：异步执行，有返回值，线程=" + Thread.currentThread().getName());
            return 100;
        });

        // 方式三：手动完成（测试/Mock 场景非常有用）
        CompletableFuture<String> f3 = new CompletableFuture<>();
        f3.complete("手动设置的结果");

        // 方式四：已完成的 Future（直接有值）
        CompletableFuture<String> f4 = CompletableFuture.completedFuture("已完成");

        f1.get();
        System.out.println("  f2结果=" + f2.get());
        System.out.println("  f3结果=" + f3.get());
        System.out.println("  f4结果=" + f4.get());
    }

    static void demo_chain() throws Exception {
        System.out.println("  thenApply：转换结果（有入参，有返回值）类比 Stream.map()");
        System.out.println("  thenAccept：消费结果（有入参，无返回值）类比 Stream.forEach()");
        System.out.println("  thenRun：完成后触发（无入参，无返回值）");
        System.out.println();

        // 链式：查询用户ID → 查询用户名 → 打印
        CompletableFuture.supplyAsync(() -> {
                    System.out.println("  步骤1：查询用户ID");
                    return 1001;
                })
                .thenApply(userId -> {
                    System.out.println("  步骤2：根据ID=" + userId + " 查询用户名");
                    return "张三(id=" + userId + ")";
                })
                .thenApply(userName -> {
                    System.out.println("  步骤3：格式化：" + userName);
                    return "欢迎，" + userName;
                })
                .thenAccept(msg -> System.out.println("  步骤4（消费）：" + msg))
                .thenRun(() -> System.out.println("  步骤5（thenRun）：整个流程完成"))
                .get(); // 等整条链执行完
    }

    static void demo_combine() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        System.out.println("  thenCompose：串行依赖，第二个任务依赖第一个的结果");
        System.out.println("  场景：先查订单，再根据订单ID查物流信息（有依赖关系）\n");

        CompletableFuture<String> logistics = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("  [thenCompose] 查询订单ID...");
                    return "ORDER-001";
                }, pool)
                .thenCompose(orderId ->
                        CompletableFuture.supplyAsync(() -> {
                            System.out.println("  [thenCompose] 用订单ID=" + orderId + " 查物流...");
                            return "物流状态：已发货";
                        }, pool)
                );
        System.out.println("  结果：" + logistics.get());

        System.out.println();
        System.out.println("  thenCombine：并行执行两个任务，等两个都完成后合并结果");
        System.out.println("  场景：同时查价格和库存，两个独立查询，最后合并（无依赖关系）\n");

        long start = System.currentTimeMillis();
        CompletableFuture<String> price = CompletableFuture.supplyAsync(() -> {
            System.out.println("  [thenCombine] 并行查询价格（需200ms）...");
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "价格：99元";
        }, pool);

        CompletableFuture<String> stock = CompletableFuture.supplyAsync(() -> {
            System.out.println("  [thenCombine] 并行查询库存（需300ms）...");
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "库存：100件";
        }, pool);

        CompletableFuture<String> combined = price.thenCombine(stock,
                (p, s) -> p + "，" + s + "（两个结果合并）");

        System.out.println("  合并结果：" + combined.get());
        System.out.println("  耗时：" + (System.currentTimeMillis() - start) + "ms（并行，约300ms而非500ms）");

        pool.shutdown();
    }

    static void demo_allOfAnyOf() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        System.out.println("  allOf：等所有任务都完成（类比 CountDownLatch）");
        System.out.println("  anyOf：任意一个任务完成就返回（类比赛马，取最快的）\n");

        // allOf 演示
        System.out.println("  --- allOf 演示 ---");
        long start = System.currentTimeMillis();

        CompletableFuture<Integer> t1 = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  任务A完成（200ms）");
            return 10;
        }, pool);

        CompletableFuture<Integer> t2 = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  任务B完成（400ms）");
            return 20;
        }, pool);

        CompletableFuture<Integer> t3 = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  任务C完成（300ms）");
            return 30;
        }, pool);

        // allOf 等所有任务完成，但它本身的泛型是 Void，需要单独 get 各任务结果
        CompletableFuture.allOf(t1, t2, t3).get();
        int total = t1.get() + t2.get() + t3.get();
        System.out.println("  allOf 总耗时：" + (System.currentTimeMillis() - start) + "ms");
        System.out.println("  allOf 结果之和：" + total);

        // anyOf 演示
        System.out.println("\n  --- anyOf 演示（取最快完成的那个）---");
        start = System.currentTimeMillis();

        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "服务A（300ms）";
                }, pool),
                CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "服务B（100ms）★最快";
                }, pool),
                CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "服务C（200ms）";
                }, pool)
        );

        System.out.println("  anyOf 结果：" + fastest.get() + "  耗时：" + (System.currentTimeMillis() - start) + "ms");

        pool.shutdown();
    }

    static void demo_exception() throws Exception {
        System.out.println("  exceptionally：出异常时返回一个默认值（只处理异常，类比 catch）");
        System.out.println("  handle：无论正常还是异常都会执行（类比 finally，但有返回值）\n");

        // exceptionally 演示
        System.out.println("  --- exceptionally ---");
        CompletableFuture<String> f1 = CompletableFuture
                .<String>supplyAsync(() -> {
                    System.out.println("  模拟查询异常...");
                    throw new RuntimeException("数据库连接失败");
                })
                .exceptionally(ex -> {
                    System.out.println("  捕获异常：" + ex.getMessage());
                    return "默认用户";  // 降级返回
                });
        System.out.println("  exceptionally 结果：" + f1.get());

        // handle 演示（无论成功失败都执行）
        System.out.println("\n  --- handle（正常情况）---");
        CompletableFuture<String> f2 = CompletableFuture
                .supplyAsync(() -> "正常结果")
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("  handle 捕获异常：" + ex.getMessage());
                        return "降级值";
                    }
                    System.out.println("  handle 正常执行，结果=" + result);
                    return result + "（handle处理后）";
                });
        System.out.println("  handle 结果：" + f2.get());

        System.out.println("\n  --- handle（异常情况）---");
        CompletableFuture<String> f3 = CompletableFuture
                .<String>supplyAsync(() -> {
                    throw new RuntimeException("服务超时");
                })
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("  handle 捕获异常：" + ex.getMessage());
                        return "降级值";
                    }
                    return result;
                });
        System.out.println("  handle 结果：" + f3.get());
    }

    static void demo_realWorld() throws Exception {
        System.out.println("  业务场景：用户下单后，并行查询「商品详情」「库存」「用户优惠券」，");
        System.out.println("  等三个查询都完成后，合并成订单确认页数据返回。\n");

        ExecutorService pool = Executors.newFixedThreadPool(4);
        long start = System.currentTimeMillis();

        // 三个并行查询
        CompletableFuture<String> productFuture = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  ✓ 商品详情查询完毕（300ms）");
            return "iPhone 16，价格6999元";
        }, pool);

        CompletableFuture<String> stockFuture = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  ✓ 库存查询完毕（200ms）");
            return "库存充足（500件）";
        }, pool);

        CompletableFuture<String> couponFuture = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("  ✓ 优惠券查询完毕（400ms）");
            return "满减券：满1000减100";
        }, pool).exceptionally(ex -> "暂无优惠券（查询失败降级）"); // 优惠券查询失败不影响主流程

        // 等所有查询完成，合并结果
        CompletableFuture.allOf(productFuture, stockFuture, couponFuture).get();

        System.out.println("\n  ===== 订单确认页 =====");
        System.out.println("  商品：" + productFuture.get());
        System.out.println("  库存：" + stockFuture.get());
        System.out.println("  优惠：" + couponFuture.get());
        System.out.println("  总耗时：" + (System.currentTimeMillis() - start) + "ms");
        System.out.println("  （三个查询并行，耗时取最慢的400ms，而非串行的900ms）");

        pool.shutdown();
    }
}

// ====================================================================
// 第五部分：四者对比与选型指南
// ====================================================================
class Part5_Comparison {
    static void run() {
        System.out.println("【四者本质对比】\n");
        System.out.println("  ┌──────────────────┬────────────┬──────────────────────────────────────────┐");
        System.out.println("  │ 类/接口           │ 类型       │ 核心职责                                 │");
        System.out.println("  ├──────────────────┼────────────┼──────────────────────────────────────────┤");
        System.out.println("  │ Callable<V>      │ 接口       │ 描述「有返回值的任务」，定义干什么         │");
        System.out.println("  │ Future<V>        │ 接口       │ 描述「异步结果的句柄」，定义怎么取结果    │");
        System.out.println("  │ FutureTask<V>    │ 实现类     │ Callable+Future的桥梁，既能跑又能取结果  │");
        System.out.println("  │ CompletableFuture│ 实现类     │ 异步编排利器，解决Future的所有痛点        │");
        System.out.println("  └──────────────────┴────────────┴──────────────────────────────────────────┘\n");

        System.out.println("【关系图】\n");
        System.out.println("  Callable ──────────────────────────────────────────────────────────────────┐");
        System.out.println("    「定义任务」                                                               │");
        System.out.println("                                                                              │");
        System.out.println("  Future<V>（接口）                                                           │");
        System.out.println("    「异步结果的引用」                                                         │");
        System.out.println("         ↑                                                                   │");
        System.out.println("         │ implements                          包装 Callable ←───────────────┘");
        System.out.println("         │");
        System.out.println("  FutureTask<V>  ←── 同时 implements Runnable（可被Thread/线程池执行）");
        System.out.println("         ↑");
        System.out.println("         │");
        System.out.println("  pool.submit(Callable) 内部就是把 Callable 包成 FutureTask 返回给你");
        System.out.println();
        System.out.println("  CompletableFuture<V>  ←── 独立实现 Future，不依赖 FutureTask");
        System.out.println("    「完整的异步编程框架」");
        System.out.println();

        System.out.println("【选型指南】\n");
        System.out.println("  场景一：只需要异步执行，不关心结果");
        System.out.println("     → Runnable + execute()  或  CompletableFuture.runAsync()");
        System.out.println();
        System.out.println("  场景二：需要异步执行 + 获取结果，逻辑简单");
        System.out.println("     → Callable + submit() → Future.get()");
        System.out.println("     → 适合：批量并行计算，最后汇总结果");
        System.out.println();
        System.out.println("  场景三：需要配合原生 Thread（不用线程池）");
        System.out.println("     → Callable + FutureTask + new Thread(futureTask).start()");
        System.out.println("     → 或者：FutureTask 作为幂等缓存（多线程共享同一个 FutureTask）");
        System.out.println();
        System.out.println("  场景四：需要任务编排、链式处理、多任务组合、非阻塞回调");
        System.out.println("     → CompletableFuture  ← 现代 Java 异步编程的首选");
        System.out.println("     → 适合：微服务并行调用、电商下单流程、数据聚合等");
        System.out.println();

        System.out.println("【Future.get() 的阻塞问题与解决方案】\n");
        System.out.println("  Future.get() 阻塞主线程的本质：");
        System.out.println("  主线程调用 get() → 如果结果没好，主线程进入 WAITING 状态");
        System.out.println("  任务完成 → 工作线程调用 LockSupport.unpark(等待线程) → 主线程唤醒");
        System.out.println();
        System.out.println("  解决方案：");
        System.out.println("  1. get(timeout) 设置超时，避免无限等待");
        System.out.println("  2. 用 CompletableFuture 的 thenApply/thenAccept 回调，完全不阻塞");
        System.out.println("  3. 用 isDone() 轮询（不推荐，CPU空转浪费资源）");
        System.out.println();

        System.out.println("【常见误区】\n");
        System.out.println("  ❌ 误区1：FutureTask 和 Future 是同一个东西");
        System.out.println("     正确：Future 是接口，FutureTask 是实现类，还额外实现了 Runnable");
        System.out.println();
        System.out.println("  ❌ 误区2：CompletableFuture 不需要线程池，用默认就行");
        System.out.println("     正确：默认用 ForkJoinPool.commonPool()，生产环境务必传入自定义线程池");
        System.out.println("     原因：commonPool 是全局共享的，被打满会影响所有使用它的地方");
        System.out.println();
        System.out.println("  ❌ 误区3：thenApply 和 thenCompose 一样");
        System.out.println("     thenApply：  T → U         （转换普通值）");
        System.out.println("     thenCompose：T → Future<U>  （转换为另一个异步任务，避免 Future 嵌套）");
    }
}

// ====================================================================
// 第六部分：thenApply vs thenCompose 深度对比
// ====================================================================
class Part6_ThenApplyVsCompose {

    // ---- 模拟异步数据库查询（返回 CompletableFuture）----
    static CompletableFuture<Integer> queryUserId(String name) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("    [DB] 根据名字=" + name + " 查询用户ID... 线程=" + Thread.currentThread().getName());
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return 1001;
        });
    }

    static CompletableFuture<String> queryUserDetail(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("    [DB] 根据userId=" + userId + " 查询用户详情... 线程=" + Thread.currentThread().getName());
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "用户详情{id=" + userId + ", name=张三, age=25}";
        });
    }

    static void run() throws Exception {
        System.out.println("【一、用 thenApply 接异步操作 —— 错误示范，产生嵌套】\n");
        demo_thenApply_wrong();

        System.out.println("\n【二、用 thenCompose 接异步操作 —— 正确方式，保持扁平】\n");
        demo_thenCompose_right();

        System.out.println("\n【三、thenApply 的正确使用场景 —— 纯同步数据转换】\n");
        demo_thenApply_right();

        System.out.println("\n【四、多级 thenCompose 链式调用】\n");
        demo_thenCompose_chain();

        System.out.println("\n【五、直观对比：类型签名】\n");
        printTypeSummary();
    }

    // ----------------------------------------------------------------
    // 一：thenApply 接异步操作，产生嵌套（错误示范）
    // ----------------------------------------------------------------
    static void demo_thenApply_wrong() throws Exception {
        System.out.println("  场景：根据用户名 → 查用户ID（异步）");
        System.out.println("  用 thenApply 接一个返回 CompletableFuture 的函数会怎样？\n");

        // thenApply 返回 CompletableFuture<CompletableFuture<Integer>>
        // 因为 thenApply 把 queryUserId() 的返回值（CompletableFuture<Integer>）直接包了一层
        CompletableFuture<CompletableFuture<Integer>> nested =
                CompletableFuture.supplyAsync(() -> "张三")
                        .thenApply(name -> queryUserId(name)); // 返回值是Future，又被包了一层

        System.out.println("  ⚠ thenApply 产生的类型：CompletableFuture<CompletableFuture<Integer>>");
        System.out.println("  ⚠ 需要两次 get() 才能拿到值（嵌套地狱）：");

        // 需要 .get().get() 两层才能拿到值
        Integer userId = nested.get().get();
        System.out.println("  最终结果（两次get）：" + userId);
        System.out.println("  ⚠ 而且无法继续正常链式调用，链断了！");
    }

    // ----------------------------------------------------------------
    // 二：thenCompose 接异步操作，扁平化（正确方式）
    // ----------------------------------------------------------------
    static void demo_thenCompose_right() throws Exception {
        System.out.println("  场景：根据用户名 → 查用户ID（异步）");
        System.out.println("  用 thenCompose 接一个返回 CompletableFuture 的函数：\n");

        // thenCompose 会自动解包内层的 CompletableFuture
        // 最终类型是 CompletableFuture<Integer>，干净扁平
        CompletableFuture<Integer> flat =
                CompletableFuture.supplyAsync(() -> "张三")
                        .thenCompose(name -> queryUserId(name)); // 自动解包，类型不嵌套

        System.out.println("  ✓ thenCompose 产生的类型：CompletableFuture<Integer>");
        System.out.println("  ✓ 只需一次 get()：");

        Integer userId = flat.get();
        System.out.println("  最终结果（一次get）：" + userId);
        System.out.println("  ✓ 链式调用正常，还可以继续 .thenCompose().thenApply()...");
    }

    // ----------------------------------------------------------------
    // 三：thenApply 的正确使用场景（纯同步转换）
    // ----------------------------------------------------------------
    static void demo_thenApply_right() throws Exception {
        System.out.println("  场景：查到用户ID后，同步拼接一个字符串（不需要发起新的异步操作）");
        System.out.println("  这种场景 thenApply 完全正确：\n");

        CompletableFuture<String> result =
                CompletableFuture.supplyAsync(() -> 1001)   // 异步：得到 userId
                        .thenApply(userId -> "user_" + userId)  // 同步转换：int → String（无异步调用）
                        .thenApply(String::toUpperCase)         // 同步转换：字符串处理
                        .thenApply(s -> "[" + s + "]");         // 同步转换：格式化

        System.out.println("  ✓ 纯数据转换，thenApply 完全够用");
        System.out.println("  结果：" + result.get());
        System.out.println();
        System.out.println("  原则：函数体里有没有 CompletableFuture.supplyAsync / runAsync 等异步调用？");
        System.out.println("    没有 → thenApply");
        System.out.println("    有   → thenCompose");
    }

    // ----------------------------------------------------------------
    // 四：多级 thenCompose 链：用户名 → 用户ID → 用户详情
    // ----------------------------------------------------------------
    static void demo_thenCompose_chain() throws Exception {
        System.out.println("  完整链：用户名 ──[异步]──► 用户ID ──[异步]──► 用户详情 ──[同步]──► 展示文案");
        System.out.println("  前两步用 thenCompose（接异步），最后一步用 thenApply（纯转换）\n");

        long start = System.currentTimeMillis();

        String displayText = CompletableFuture
                // 第一步：已知用户名
                .supplyAsync(() -> {
                    System.out.println("  Step1: 出发点，用户名=张三");
                    return "张三";
                })
                // 第二步：异步查用户ID（用 thenCompose，因为 queryUserId 返回 Future）
                .thenCompose(name -> {
                    System.out.println("  Step2: thenCompose 查用户ID");
                    return queryUserId(name);
                })
                // 第三步：异步查用户详情（用 thenCompose，因为 queryUserDetail 返回 Future）
                .thenCompose(userId -> {
                    System.out.println("  Step3: thenCompose 查用户详情");
                    return queryUserDetail(userId);
                })
                // 第四步：同步格式化（用 thenApply，只是字符串拼接，无异步）
                .thenApply(detail -> {
                    System.out.println("  Step4: thenApply 格式化");
                    return "展示文案: 欢迎回来！" + detail;
                })
                .get();

        System.out.println();
        System.out.println("  最终结果：" + displayText);
        System.out.println("  总耗时：" + (System.currentTimeMillis() - start) + "ms（两次异步查询串行，约400ms）");
    }

    // ----------------------------------------------------------------
    // 五：类型签名对比总结
    // ----------------------------------------------------------------
    static void printTypeSummary() {
        System.out.println("  ┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("  │  thenApply(Function<T, U> fn)                                       │");
        System.out.println("  │      fn 的签名：T → U                                               │");
        System.out.println("  │      fn 返回普通值 U，thenApply 把它包成 CompletableFuture<U>        │");
        System.out.println("  │      如果 fn 返回 CompletableFuture<U>，结果就嵌套了 ⚠              │");
        System.out.println("  ├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("  │  thenCompose(Function<T, CompletableFuture<U>> fn)                  │");
        System.out.println("  │      fn 的签名：T → CompletableFuture<U>                            │");
        System.out.println("  │      fn 返回 CompletableFuture<U>，thenCompose 自动解包，结果扁平   │");
        System.out.println("  │      等价于 Stream 的 flatMap                                       │");
        System.out.println("  └─────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  判断口诀：");
        System.out.println("    下一步函数体里有没有新的异步调用（supplyAsync/runAsync/RPC/DB）？");
        System.out.println("      没有（只是转换数据）→ thenApply");
        System.out.println("      有（返回新的Future）→ thenCompose，否则会产生 Future<Future<T>> 嵌套");
    }
}

// ====================================================================
// 第七部分：CompletableFuture 深度讲解（含实际案例）
// ====================================================================
class Part7_CompletableFutureDeep {

    // 模拟线程池（生产环境必须传自定义线程池，不能用默认的 ForkJoinPool.commonPool）
    static final java.util.concurrent.ExecutorService POOL =
            new java.util.concurrent.ThreadPoolExecutor(
                    4, 8, 60L, java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<>(100),
                    r -> new Thread(r, "cf-worker-" + new java.util.concurrent.atomic.AtomicInteger(0).incrementAndGet()),
                    new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
            );

    static void run() throws Exception {
        section("1. 核心设计思想：为什么需要 CompletableFuture");
        part1_WhyNeeded();

        section("2. 创建方式全览");
        part2_Creation();

        section("3. 转换与消费：thenApply / thenAccept / thenRun");
        part3_Transform();

        section("4. 串行依赖：thenCompose（异步接异步）");
        part4_ThenCompose();

        section("5. 并行合并：thenCombine / allOf / anyOf");
        part5_Parallel();

        section("6. 异常处理：exceptionally / handle / whenComplete");
        part6_Exception();

        section("7. 同步与异步变体：thenApply vs thenApplyAsync");
        part7_AsyncVariants();

        section("8. 手动控制：complete / completeExceptionally / cancel");
        part8_ManualControl();

        section("9. 实战案例一：电商下单页面并行数据聚合");
        part9_EcommerceCase();

        section("10. 实战案例二：超时降级 + 重试");
        part10_TimeoutFallback();

        section("11. 常见陷阱与最佳实践");
        part11_Pitfalls();

        POOL.shutdown();
    }

    static void section(String title) {
        System.out.println("\n  ── " + title + " ──\n");
    }

    // ----------------------------------------------------------------
    // 1. 为什么需要 CompletableFuture
    // ----------------------------------------------------------------
    static void part1_WhyNeeded() {
        System.out.println("  Future 的三大痛点：");
        System.out.println();
        System.out.println("  痛点一：get() 阻塞");
        System.out.println("  ┌────────────────────────────────────────────────────┐");
        System.out.println("  │  Future<String> f = pool.submit(task);             │");
        System.out.println("  │  String result = f.get();  // 主线程在这里死等 ← │");
        System.out.println("  │  // 真正的异步应该是：任务完成后自动通知我        │");
        System.out.println("  └────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  痛点二：无法链式编排");
        System.out.println("  ┌────────────────────────────────────────────────────┐");
        System.out.println("  │  // 任务A完成后自动触发任务B，Future 做不到        │");
        System.out.println("  │  // 只能手动 get() 再 submit()，代码冗长           │");
        System.out.println("  └────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  痛点三：异常处理繁琐");
        System.out.println("  ┌────────────────────────────────────────────────────┐");
        System.out.println("  │  try {                                             │");
        System.out.println("  │      result = future.get();                        │");
        System.out.println("  │  } catch (ExecutionException e) {                  │");
        System.out.println("  │      Throwable cause = e.getCause(); // 层层解包  │");
        System.out.println("  │  }                                                 │");
        System.out.println("  └────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  CompletableFuture 的解决方案：");
        System.out.println("    ✅ 回调机制（thenApply/thenAccept）：任务完成自动触发下一步，无需 get()");
        System.out.println("    ✅ 链式 API：像 Stream 一样流畅编排多个异步步骤");
        System.out.println("    ✅ 内置异常处理：exceptionally/handle，像 try-catch 一样自然");
        System.out.println("    ✅ 组合能力：allOf/anyOf/thenCombine，轻松实现并行聚合");
    }

    // ----------------------------------------------------------------
    // 2. 创建方式全览
    // ----------------------------------------------------------------
    static void part2_Creation() throws Exception {
        System.out.println("  // 方式一：runAsync —— 异步执行，无返回值");
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(
                () -> System.out.println("  runAsync 执行，线程=" + Thread.currentThread().getName()),
                POOL  // 务必传自定义线程池！
        );
        f1.get();

        System.out.println();
        System.out.println("  // 方式二：supplyAsync —— 异步执行，有返回值");
        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(
                () -> {
                    System.out.println("  supplyAsync 执行，线程=" + Thread.currentThread().getName());
                    return 42;
                },
                POOL
        );
        System.out.println("  supplyAsync 结果=" + f2.get());

        System.out.println();
        System.out.println("  // 方式三：completedFuture —— 直接返回已完成的 Future（常用于测试/Mock）");
        CompletableFuture<String> f3 = CompletableFuture.completedFuture("已完成的值");
        System.out.println("  completedFuture 结果=" + f3.get() + "  isDone=" + f3.isDone());

        System.out.println();
        System.out.println("  // 方式四：new CompletableFuture<>() —— 手动控制完成时机");
        CompletableFuture<String> f4 = new CompletableFuture<>();
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            f4.complete("手动在100ms后完成");  // 手动触发完成
        }).start();
        System.out.println("  手动complete 结果=" + f4.get());
    }

    // ----------------------------------------------------------------
    // 3. 转换与消费
    // ----------------------------------------------------------------
    static void part3_Transform() throws Exception {
        System.out.println("  三个方法的核心区别：");
        System.out.println("    thenApply(fn)  : T → U        有入参，有返回值，用于转换");
        System.out.println("    thenAccept(fn) : T → void     有入参，无返回值，用于消费（副作用）");
        System.out.println("    thenRun(fn)    : void → void  无入参，无返回值，仅触发后续动作");
        System.out.println();

        CompletableFuture
                .supplyAsync(() -> 100, POOL)
                .thenApply(n -> {
                    System.out.println("  thenApply: 收到 " + n + "，返回 " + (n * 2));
                    return n * 2;                          // 转换：100 → 200
                })
                .thenApply(n -> "结果=" + n)              // 继续转换：200 → "结果=200"
                .thenAccept(s -> System.out.println("  thenAccept: 消费最终结果：" + s)) // 消费，链到此结束
                .thenRun(() -> System.out.println("  thenRun: 消费完毕，触发后续逻辑（如发监控）"))
                .get();
    }

    // ----------------------------------------------------------------
    // 4. 串行依赖：thenCompose
    // ----------------------------------------------------------------
    static void part4_ThenCompose() throws Exception {
        System.out.println("  场景：查用户 → 查用户所在部门 → 查部门权限（每步依赖上一步结果）");
        System.out.println();

        long start = System.currentTimeMillis();

        String result = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("  [1] 查用户ID，耗时100ms");
                    sleep(100);
                    return 1001;
                }, POOL)
                .thenCompose(userId ->
                        CompletableFuture.supplyAsync(() -> {
                            System.out.println("  [2] 用userId=" + userId + " 查部门ID，耗时100ms");
                            sleep(100);
                            return "DEPT-" + userId;
                        }, POOL)
                )
                .thenCompose(deptId ->
                        CompletableFuture.supplyAsync(() -> {
                            System.out.println("  [3] 用deptId=" + deptId + " 查权限，耗时100ms");
                            sleep(100);
                            return "PERM:READ,WRITE (dept=" + deptId + ")";
                        }, POOL)
                )
                .thenApply(perm -> "最终权限: " + perm)  // 纯转换，用 thenApply
                .get();

        System.out.println("  " + result);
        System.out.println("  总耗时：" + (System.currentTimeMillis() - start) + "ms（串行，约300ms）");
        System.out.println();
        System.out.println("  ★ 关键：整个链在工作线程上执行，主线程只在最后 get() 时短暂等待");
        System.out.println("    如果用回调替代 get()，主线程完全不阻塞");
    }

    // ----------------------------------------------------------------
    // 5. 并行合并
    // ----------------------------------------------------------------
    static void part5_Parallel() throws Exception {
        System.out.println("  【thenCombine：两个任务并行，都完成后合并结果】\n");

        long start = System.currentTimeMillis();

        // 两个任务同时启动，互不依赖
        CompletableFuture<String> priceF = CompletableFuture.supplyAsync(() -> {
            System.out.println("  [并行] 查价格，耗时200ms");
            sleep(200);
            return "¥99";
        }, POOL);

        CompletableFuture<String> stockF = CompletableFuture.supplyAsync(() -> {
            System.out.println("  [并行] 查库存，耗时300ms");
            sleep(300);
            return "库存500件";
        }, POOL);

        String combined = priceF.thenCombine(stockF, (price, stock) ->
                "商品信息：价格=" + price + "，" + stock
        ).get();

        System.out.println("  合并结果：" + combined);
        System.out.println("  耗时：" + (System.currentTimeMillis() - start) + "ms（并行，约300ms而非500ms）\n");

        System.out.println("  【allOf：等待全部任务完成】\n");

        start = System.currentTimeMillis();
        CompletableFuture<String> t1 = CompletableFuture.supplyAsync(() -> { sleep(100); return "A"; }, POOL);
        CompletableFuture<String> t2 = CompletableFuture.supplyAsync(() -> { sleep(200); return "B"; }, POOL);
        CompletableFuture<String> t3 = CompletableFuture.supplyAsync(() -> { sleep(150); return "C"; }, POOL);

        // allOf 本身返回 Void，需单独 get 每个任务的结果
        CompletableFuture.allOf(t1, t2, t3).get();
        System.out.println("  allOf 全部完成：" + t1.get() + t2.get() + t3.get());
        System.out.println("  耗时：" + (System.currentTimeMillis() - start) + "ms（约200ms）\n");

        System.out.println("  【anyOf：任意一个完成就返回（多活容灾/就近查询场景）】\n");

        start = System.currentTimeMillis();
        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "机房A响应（300ms）"; }, POOL),
                CompletableFuture.supplyAsync(() -> { sleep(80);  return "机房B响应（80ms）★"; }, POOL),
                CompletableFuture.supplyAsync(() -> { sleep(200); return "机房C响应（200ms）"; }, POOL)
        );
        System.out.println("  anyOf 最快结果：" + fastest.get());
        System.out.println("  耗时：" + (System.currentTimeMillis() - start) + "ms（取最快的机房B）");
    }

    // ----------------------------------------------------------------
    // 6. 异常处理
    // ----------------------------------------------------------------
    static void part6_Exception() throws Exception {
        System.out.println("  三种异常处理方式对比：");
        System.out.println("    exceptionally(fn) : 只在异常时触发，类比 catch，可返回默认值");
        System.out.println("    handle(fn)        : 无论正常/异常都触发，类比 finally，有返回值");
        System.out.println("    whenComplete(fn)  : 无论正常/异常都触发，类比 finally，无返回值（仅观测）");
        System.out.println();

        System.out.println("  --- exceptionally（最常用，降级兜底）---");
        String r1 = CompletableFuture
                .<String>supplyAsync(() -> {
                    System.out.println("  查询用户，发生异常...");
                    throw new RuntimeException("DB 连接超时");
                }, POOL)
                .exceptionally(ex -> {
                    // ex 是 CompletionException，getCause() 才是原始异常
                    System.out.println("  exceptionally 捕获：" + ex.getCause().getMessage());
                    return "匿名用户（降级默认值）";
                })
                .get();
        System.out.println("  结果：" + r1);

        System.out.println();
        System.out.println("  --- handle（同时处理正常值和异常）---");
        String r2 = CompletableFuture
                .supplyAsync(() -> "正常查询结果", POOL)
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("  handle 捕获异常：" + ex.getMessage());
                        return "降级值";
                    }
                    System.out.println("  handle 正常处理：result=" + result);
                    return result + "（handle加工）";
                })
                .get();
        System.out.println("  结果：" + r2);

        System.out.println();
        System.out.println("  --- whenComplete（观测，不改变结果）---");
        String r3 = CompletableFuture
                .supplyAsync(() -> "原始结果", POOL)
                .whenComplete((result, ex) -> {
                    // 只能观测，无法改变结果
                    if (ex != null) System.out.println("  whenComplete 观测到异常：" + ex.getMessage());
                    else            System.out.println("  whenComplete 观测到结果：" + result + "（但无法改变它）");
                })
                .get();
        System.out.println("  结果（未被改变）：" + r3);

        System.out.println();
        System.out.println("  ★ 异常在链中的传播规律：");
        System.out.println("    链中任意一步抛异常 → 后续所有 thenApply/thenCompose 被跳过");
        System.out.println("    直到遇到 exceptionally 或 handle 才被捕获处理");
        System.out.println("    类似 try 块里的异常会跳过后续代码直接进入 catch");

        // 演示异常传播跳过中间步骤
        System.out.println();
        System.out.println("  --- 异常传播演示（中间步骤被跳过）---");
        CompletableFuture
                .<String>supplyAsync(() -> {
                    System.out.println("  Step1 执行");
                    throw new RuntimeException("Step1 异常");
                }, POOL)
                .thenApply(s -> {
                    System.out.println("  Step2 执行（有异常会跳过这里）");
                    return s + "_step2";
                })
                .thenApply(s -> {
                    System.out.println("  Step3 执行（有异常会跳过这里）");
                    return s + "_step3";
                })
                .exceptionally(ex -> {
                    System.out.println("  exceptionally 捕获，Step2/Step3 都被跳过了");
                    return "降级";
                })
                .get();
    }

    // ----------------------------------------------------------------
    // 7. 同步与异步变体
    // ----------------------------------------------------------------
    static void part7_AsyncVariants() throws Exception {
        System.out.println("  每个 then* 方法都有三个版本：");
        System.out.println("  ┌──────────────────────────────────────────────────────────────────┐");
        System.out.println("  │  thenApply(fn)       在触发它的线程上同步执行 fn                 │");
        System.out.println("  │  thenApplyAsync(fn)  在 ForkJoinPool.commonPool() 上异步执行    │");
        System.out.println("  │  thenApplyAsync(fn, executor)  在指定线程池上异步执行  ← 推荐   │");
        System.out.println("  └──────────────────────────────────────────────────────────────────┘\n");

        System.out.println("  thenApply（同步）：fn 在上一步任务完成的那个工作线程上直接执行");
        System.out.println("  thenApplyAsync：fn 重新提交到线程池，由线程池分配线程执行\n");

        // 演示：观察执行线程
        CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("  supplyAsync 线程：" + Thread.currentThread().getName());
                    return "hello";
                }, POOL)
                .thenApply(s -> {
                    // 同步：在上一步的线程上执行（或主线程，取决于完成时机）
                    System.out.println("  thenApply 线程：" + Thread.currentThread().getName());
                    return s.toUpperCase();
                })
                .thenApplyAsync(s -> {
                    // 异步：重新提交到 POOL
                    System.out.println("  thenApplyAsync 线程：" + Thread.currentThread().getName());
                    return s + "!";
                }, POOL)
                .thenAccept(s -> System.out.println("  最终结果：" + s))
                .get();

        System.out.println();
        System.out.println("  ★ 什么时候用 Async 变体？");
        System.out.println("    fn 本身是重型 CPU 计算 → 用 Async，避免阻塞工作线程，放到专用线程池");
        System.out.println("    fn 只是轻量转换（字段取值/格式化）→ 用同步版本，减少线程切换开销");
    }

    // ----------------------------------------------------------------
    // 8. 手动控制
    // ----------------------------------------------------------------
    static void part8_ManualControl() throws Exception {
        System.out.println("  complete(value)              : 手动完成，设置正常结果");
        System.out.println("  completeExceptionally(ex)    : 手动以异常方式完成");
        System.out.println("  cancel(mayInterrupt)         : 取消（等价于 completeExceptionally(CancellationException)）");
        System.out.println("  obtrudeValue(value)          : 强制覆盖已有结果（慎用）");
        System.out.println();

        System.out.println("  --- 手动 complete 应用场景：超时降级 ---");
        CompletableFuture<String> cf = new CompletableFuture<>();

        // 模拟慢服务
        POOL.execute(() -> {
            sleep(5000); // 假设超时了
            cf.complete("慢服务结果"); // 如果已经被超时降级，这里 complete 无效
        });

        // 超时降级：200ms 内没完成就用默认值
        POOL.execute(() -> {
            sleep(200);
            boolean succeeded = cf.complete("超时降级默认值");
            System.out.println("  超时降级触发：" + succeeded + "（true=成功设置，false=已经有结果了）");
        });

        System.out.println("  结果：" + cf.get());
        System.out.println();

        System.out.println("  --- completeExceptionally 应用场景：主动报错 ---");
        CompletableFuture<String> cf2 = new CompletableFuture<>();
        cf2.completeExceptionally(new RuntimeException("主动触发失败"));
        try {
            cf2.get();
        } catch (java.util.concurrent.ExecutionException e) {
            System.out.println("  捕获到异常：" + e.getCause().getMessage());
        }
    }

    // ----------------------------------------------------------------
    // 9. 实战案例一：电商下单页面并行数据聚合
    // ----------------------------------------------------------------
    static void part9_EcommerceCase() throws Exception {
        System.out.println("  业务场景：用户打开商品详情页，需要同时展示：");
        System.out.println("    - 商品基本信息（查商品服务，耗时100ms）");
        System.out.println("    - 实时价格       （查价格服务，耗时200ms）");
        System.out.println("    - 库存状态       （查库存服务，耗时150ms）");
        System.out.println("    - 用户优惠券     （查营销服务，耗时300ms，允许失败降级）");
        System.out.println("    - 商品评价数     （查评价服务，耗时120ms）");
        System.out.println("    串行总耗时：100+200+150+300+120 = 870ms");
        System.out.println("    并行总耗时：max(100,200,150,300,120) = 300ms\n");

        long start = System.currentTimeMillis();

        // 五个查询全部并行发出
        CompletableFuture<String> productF = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "iPhone 16 Pro，128G，深空黑";
        }, POOL);

        CompletableFuture<String> priceF = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "¥8999（限时优惠¥500）";
        }, POOL);

        CompletableFuture<String> stockF = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            return "现货，预计明日达";
        }, POOL);

        CompletableFuture<String> couponF = CompletableFuture
                .supplyAsync(() -> {
                    sleep(300);
                    // 模拟营销服务偶发失败
                    if (System.currentTimeMillis() % 2 == 0) throw new RuntimeException("营销服务不可用");
                    return "满10000减500券";
                }, POOL)
                .exceptionally(ex -> {
                    System.out.println("  [降级] 优惠券查询失败：" + ex.getCause().getMessage() + "，返回默认值");
                    return "暂无可用优惠券";  // 降级：不影响主流程
                });

        CompletableFuture<String> reviewF = CompletableFuture.supplyAsync(() -> {
            sleep(120);
            return "好评率98%（12万条评价）";
        }, POOL);

        // 等全部完成
        CompletableFuture.allOf(productF, priceF, stockF, couponF, reviewF).get();
        long elapsed = System.currentTimeMillis() - start;

        // 聚合结果
        System.out.println("  ===== 商品详情页数据 =====");
        System.out.println("  商品：" + productF.get());
        System.out.println("  价格：" + priceF.get());
        System.out.println("  库存：" + stockF.get());
        System.out.println("  优惠：" + couponF.get());
        System.out.println("  评价：" + reviewF.get());
        System.out.println("  总耗时：" + elapsed + "ms（并行，比串行870ms快约" + (870 - elapsed) + "ms）");
    }

    // ----------------------------------------------------------------
    // 10. 实战案例二：超时降级 + 重试
    // ----------------------------------------------------------------
    static void part10_TimeoutFallback() throws Exception {
        System.out.println("  业务场景：调用外部 RPC 服务，要求：");
        System.out.println("    1. 超过 200ms 未返回，用本地缓存降级");
        System.out.println("    2. 调用成功则直接返回结果\n");

        // 方案：两个 CompletableFuture 竞速，anyOf 取最快的
        System.out.println("  --- 超时降级实现 ---");
        for (int i = 0; i < 3; i++) {
            final int round = i + 1;
            final int rpcTime = new int[]{100, 350, 180}[i]; // 三次RPC耗时：100ms, 350ms, 180ms

            // RPC 调用
            CompletableFuture<String> rpcFuture = CompletableFuture.supplyAsync(() -> {
                sleep(rpcTime);
                return "RPC真实结果";
            }, POOL);

            // 超时降级（200ms 后返回缓存值）
            CompletableFuture<String> fallbackFuture = CompletableFuture.supplyAsync(() -> {
                sleep(200);
                return "本地缓存降级值";
            }, POOL);

            // anyOf：谁先完成用谁
            String result = (String) CompletableFuture.anyOf(rpcFuture, fallbackFuture).get();
            System.out.println("  第" + round + "次（RPC耗时" + rpcTime + "ms）→ 结果：" + result);
        }

        System.out.println();
        System.out.println("  --- 更优雅的写法：orTimeout（JDK 9+）和 completeOnTimeout（JDK 9+）---");
        System.out.println("  // JDK 9 起可以直接用：");
        System.out.println("  // future.orTimeout(200, TimeUnit.MILLISECONDS)              // 超时抛异常");
        System.out.println("  // future.completeOnTimeout(\"降级值\", 200, TimeUnit.MILLISECONDS) // 超时返回默认值");
        System.out.println("  // JDK 8 用 anyOf + 定时任务来模拟（如上面的实现）");
    }

    // ----------------------------------------------------------------
    // 11. 常见陷阱与最佳实践
    // ----------------------------------------------------------------
    static void part11_Pitfalls() {
        System.out.println("  【陷阱一：忘记传线程池，用了默认的 ForkJoinPool.commonPool()】");
        System.out.println("  ❌ CompletableFuture.supplyAsync(() -> heavyTask())");
        System.out.println("  ✅ CompletableFuture.supplyAsync(() -> heavyTask(), myPool)");
        System.out.println("  原因：commonPool 是全局共享的，如果你的任务是慢 IO，");
        System.out.println("        会把其他模块的任务也阻塞，造成全局响应变慢");
        System.out.println();

        System.out.println("  【陷阱二：在 thenApply 里调用 get()，造成死锁】");
        System.out.println("  ❌ future.thenApply(v -> anotherFuture.get()) // 可能死锁！");
        System.out.println("  ✅ future.thenCompose(v -> anotherFuture)     // 正确");
        System.out.println("  原因：如果两个 Future 用同一线程池，thenApply 里 get() 会阻塞");
        System.out.println("        等待 anotherFuture，而 anotherFuture 又在等空闲线程，死锁");
        System.out.println();

        System.out.println("  【陷阱三：异常被吞掉，没有打日志】");
        System.out.println("  ❌ CompletableFuture.runAsync(task)  // 异常悄悄消失");
        System.out.println("  ✅ CompletableFuture.runAsync(task)");
        System.out.println("       .exceptionally(ex -> { log.error(\"异常\", ex); return null; })");
        System.out.println("  原因：不调用 get() 的话，异常永远不会被抛出，任务静默失败");
        System.out.println();

        System.out.println("  【陷阱四：误以为 thenApply 和 thenApplyAsync 只是写法不同】");
        System.out.println("  thenApply     在上一步完成的线程上继续执行（可能是工作线程或主线程）");
        System.out.println("  thenApplyAsync 重新提交到线程池，换一个线程执行");
        System.out.println("  fn 是耗时操作 → 必须用 Async 版本，否则阻塞工作线程");
        System.out.println();

        System.out.println("  【最佳实践总结】");
        System.out.println("  1. 永远传自定义线程池，不用 commonPool");
        System.out.println("  2. 有依赖关系用 thenCompose，无依赖用 thenCombine/allOf");
        System.out.println("  3. 所有链必须有 exceptionally 或 handle，不让异常静默消失");
        System.out.println("  4. 耗时操作用 Async 变体，轻量转换用同步版本");
        System.out.println("  5. 最终结果必须 get() 或 join() 或 thenAccept 消费，否则整条链可能被 GC");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

// ====================================================================
// 第八部分：四者对比与选型指南（层层递进，从痛点到演进）
// ====================================================================
class Part8_Evolution {

    static void run() throws Exception {
        sec("【第一层：从零开始——没有任何工具时的原始问题】");
        stage0_NoPrimitive();

        sec("【第二层：Callable 出现——解决「有返回值的任务」问题】");
        stage1_Callable();

        sec("【第三层：Future 出现——解决「怎么拿到异步结果」问题】");
        stage2_Future();

        sec("【第四层：FutureTask 出现——解决「Callable 无法直接给 Thread 运行」问题】");
        stage3_FutureTask();

        sec("【第五层：CompletableFuture 出现——解决 Future 的所有深层痛点】");
        stage4_CompletableFuture();

        sec("【第六层：四者横向对比表】");
        stage5_ComparisonTable();

        sec("【第七层：实际场景选型决策树】");
        stage6_DecisionTree();

        sec("【第八层：反模式——常见的错误选择】");
        stage7_AntiPatterns();
    }

    static void sec(String title) {
        System.out.println("\n  " + title + "\n");
    }

    // ----------------------------------------------------------------
    // 第零层：没有任何工具
    // ----------------------------------------------------------------
    static void stage0_NoPrimitive() throws Exception {
        System.out.println("  最原始的需求：我想在后台做一件耗时的事，同时主线程继续工作，最后拿到结果。");
        System.out.println();
        System.out.println("  用最原始的 Thread + 共享变量 实现：");
        System.out.println();

        // 原始写法：用数组绕过 lambda 的 final 限制
        String[] result = new String[1];
        boolean[] done = new boolean[1];

        Thread t = new Thread(() -> {
            System.out.println("  [子线程] 执行耗时查询...");
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            result[0] = "查询结果";
            done[0] = true;
        });
        t.start();

        System.out.println("  [主线程] 继续做其他事...");

        // 轮询等待（busy-wait），浪费 CPU
        while (!done[0]) {
            Thread.sleep(10);
        }
        System.out.println("  [主线程] 拿到结果：" + result[0]);
        System.out.println();
        System.out.println("  ❌ 原始写法的问题：");
        System.out.println("     1. 需要手动管理共享变量，线程安全全靠自己");
        System.out.println("     2. 没有标准的「等待结果」方式，只能轮询（浪费CPU）或 join()（阻塞）");
        System.out.println("     3. 异常处理没有机制，子线程异常直接丢失");
        System.out.println("     4. 代码冗长，不可复用");
        System.out.println();
        System.out.println("  → 需要一个标准的「有返回值的任务」抽象 → Callable 诞生");
    }

    // ----------------------------------------------------------------
    // 第一层：Callable
    // ----------------------------------------------------------------
    static void stage1_Callable() throws Exception {
        System.out.println("  JDK 1.5（2004年）引入 Callable<V>，解决「任务有返回值」这一个问题。");
        System.out.println();
        System.out.println("  Runnable（旧）：void run()           无返回值，无法抛受检异常");
        System.out.println("  Callable（新）：V    call() throws E 有返回值，可以抛受检异常");
        System.out.println();

        // Callable 本质：只是一个更好的函数接口
        java.util.concurrent.Callable<Integer> task = () -> {
            System.out.println("  [Callable] 执行计算...");
            return 42;
        };

        // 直接 call() 是同步的，和普通方法调用没区别
        Integer val = task.call();
        System.out.println("  同步 call() 结果：" + val);
        System.out.println();
        System.out.println("  ✅ Callable 解决的问题：提供了「有返回值的任务」标准接口");
        System.out.println("  ❌ Callable 遗留的问题：");
        System.out.println("     1. 无法直接交给 Thread 执行（Thread 只认 Runnable）");
        System.out.println("        new Thread(callable).start() ← 编译错误！");
        System.out.println("     2. 没有「异步获取结果」的机制，call() 只能同步调用");
        System.out.println("     3. 有了任务，但没有「持有异步结果的句柄」");
        System.out.println();
        System.out.println("  → 需要一个「异步结果的句柄」 → Future 诞生");
    }

    // ----------------------------------------------------------------
    // 第二层：Future
    // ----------------------------------------------------------------
    static void stage2_Future() throws Exception {
        System.out.println("  JDK 1.5 同期引入 Future<V>，代表「一个将来才会有的结果」。");
        System.out.println("  配合 ExecutorService.submit(Callable) 使用：");
        System.out.println();

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);

        // submit 返回 Future，代表「结果的凭证」
        java.util.concurrent.Future<String> future = pool.submit(() -> {
            Thread.sleep(300);
            return "异步计算结果";
        });

        System.out.println("  [主线程] 任务已提交，future 已拿到，主线程继续...");
        System.out.println("  [主线程] isDone=" + future.isDone() + "（任务还没完成）");
        System.out.println("  [主线程] 调用 get() 等待结果...");

        String result = future.get(); // 阻塞，直到结果返回
        System.out.println("  [主线程] 结果：" + result);

        pool.shutdown();
        System.out.println();
        System.out.println("  ✅ Future 解决的问题：");
        System.out.println("     1. 提供了「异步结果句柄」，可以查状态、等结果、取消任务");
        System.out.println("     2. 配合线程池，Callable 终于可以异步执行并拿到结果");
        System.out.println("     3. 异常通过 ExecutionException 包装，统一处理");
        System.out.println();
        System.out.println("  ❌ Future 的深层痛点（直到用过才知道）：");
        System.out.println();
        System.out.println("  痛点A：get() 是阻塞的，没有回调机制");
        System.out.println("    设想一下：你买了票（Future），但查票的唯一方式是一直站在售票窗口等。");
        System.out.println("    无法「票到了自动通知我」，只能主动傻等。");
        System.out.println("    → 真正的异步应该是：任务完成后「自动」触发下一步");
        System.out.println();
        System.out.println("  痛点B：无法链式编排");

        // 演示 Future 链式需要的冗长写法
        System.out.println("    // 要实现 A→B→C 串行异步，Future 的写法：");
        System.out.println("    Future<Integer> fa = pool.submit(() -> 1);");
        System.out.println("    int a = fa.get();                          // ← 阻塞");
        System.out.println("    Future<String> fb = pool.submit(() -> \"result\" + a);");
        System.out.println("    String b = fb.get();                       // ← 再阻塞");
        System.out.println("    // 每一步都阻塞，和同步写法一样，毫无异步优势");

        System.out.println();
        System.out.println("  痛点C：多个 Future 合并繁琐");
        System.out.println("    // 要等待多个 Future 全部完成，只能逐个 get()：");
        System.out.println("    String r1 = f1.get();  // 即使 f2 先完成，也要先等 f1");
        System.out.println("    String r2 = f2.get();");
        System.out.println("    // 没有真正意义的「等所有人完成」或「取最快的那个」");
        System.out.println();
        System.out.println("  痛点D：异常处理繁琐");
        System.out.println("    // get() 抛 ExecutionException，必须手动 getCause() 解包");
        System.out.println("    // 链中多个步骤的异常，需要每步都套 try-catch");
        System.out.println();
        System.out.println("  → 需要一个「能直接给 Thread 执行又能取结果」的实现 → FutureTask");
        System.out.println("  → 需要一个「彻底解决 Future 痛点」的新工具 → CompletableFuture");
    }

    // ----------------------------------------------------------------
    // 第三层：FutureTask
    // ----------------------------------------------------------------
    static void stage3_FutureTask() throws Exception {
        System.out.println("  FutureTask<V> 实现了 RunnableFuture<V>，即同时实现了 Runnable + Future。");
        System.out.println("  它解决的是一个很具体的问题：如何不用线程池，直接用 Thread 执行 Callable？");
        System.out.println();

        System.out.println("  没有 FutureTask 的困境：");
        System.out.println("    Thread 只认 Runnable，Callable 是另一套接口");
        System.out.println("    new Thread(callable) ← 编译错误！");
        System.out.println();
        System.out.println("  FutureTask 是桥梁：把 Callable 包成 Runnable，同时自身也是 Future");
        System.out.println();

        // FutureTask 用法一：配合裸 Thread（不依赖线程池）
        System.out.println("  用法一：配合裸 Thread（不依赖线程池）");
        java.util.concurrent.FutureTask<String> ft1 = new java.util.concurrent.FutureTask<>(() -> {
            Thread.sleep(100);
            return "FutureTask结果";
        });
        new Thread(ft1, "ft-thread").start(); // Thread 可以接受 FutureTask（因为它是 Runnable）
        System.out.println("  结果：" + ft1.get());

        // FutureTask 用法二：幂等缓存（多线程共享同一 FutureTask，计算只执行一次）
        System.out.println();
        System.out.println("  用法二：幂等缓存场景（最体现 FutureTask 独特价值的场景）");
        System.out.println("  场景：高并发下同一个 key 的数据只查一次数据库，其他请求复用结果");
        System.out.println();

        java.util.concurrent.atomic.AtomicInteger dbCallCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.FutureTask<String> sharedTask = new java.util.concurrent.FutureTask<>(() -> {
            dbCallCount.incrementAndGet();
            System.out.println("  ★ 数据库查询真正执行（只会出现一次）");
            Thread.sleep(200);
            return "db_result";
        });

        // 10个线程同时来，都 get() 同一个 FutureTask
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(10);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            pool.execute(() -> {
                try {
                    latch.await();
                    String r = sharedTask.get();
                    System.out.println("  线程 " + Thread.currentThread().getName() + " 得到：" + r);
                    done.countDown();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        new Thread(sharedTask).start(); // 只启动一次
        latch.countDown(); // 10个线程同时开始 get()
        done.await();
        System.out.println("  DB 实际调用次数：" + dbCallCount.get() + "（10个线程只查了1次）✓");
        pool.shutdown();

        System.out.println();
        System.out.println("  ✅ FutureTask 的独特价值：");
        System.out.println("     1. Callable + Thread 的桥梁，不依赖线程池也能异步执行 Callable");
        System.out.println("     2. 多线程并发 get() 同一个 FutureTask，内部有状态机保证只执行一次");
        System.out.println("        这是天然的「单次执行缓存」，高并发防击穿利器");
        System.out.println();
        System.out.println("  ❌ FutureTask 继承了 Future 的全部痛点（get阻塞、无链式、无组合）");
        System.out.println("     它只解决了「Callable怎么被Thread跑」，没有解决「异步编排」问题");
        System.out.println();
        System.out.println("  → 真正需要解决异步编排的，还是要靠 CompletableFuture");
    }

    // ----------------------------------------------------------------
    // 第四层：CompletableFuture
    // ----------------------------------------------------------------
    static void stage4_CompletableFuture() throws Exception {
        System.out.println("  JDK 8（2014年）引入 CompletableFuture，彻底重构了 Java 的异步编程模型。");
        System.out.println("  它不是对 Future 的小修小补，而是一次范式升级。");
        System.out.println();
        System.out.println("  核心理念转变：");
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │  Future 的思维：「我去拿结果」（主动拉取，pull model）        │");
        System.out.println("  │      result = future.get();  // 我主动去等、去拿              │");
        System.out.println("  │                                                              │");
        System.out.println("  │  CompletableFuture 的思维：「结果来找我」（被动回调，push）  │");
        System.out.println("  │      cf.thenAccept(result -> use(result)); // 结果好了通知我 │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();

        System.out.println("  逐一对照 Future 的四个痛点，CompletableFuture 是如何解决的：\n");

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(4);

        // 解决痛点A：非阻塞回调
        System.out.println("  解决痛点A：非阻塞回调（thenAccept / thenApply）");
        java.util.concurrent.CompletableFuture.supplyAsync(() -> "数据", pool)
                .thenAccept(data -> System.out.println("  → 数据来了自动触发：" + data + "（主线程从未阻塞）"));
        Thread.sleep(100); // 等回调执行（仅演示用，生产中用 join/get）
        System.out.println();

        // 解决痛点B：链式编排（A→B→C，每步不阻塞）
        System.out.println("  解决痛点B：流畅链式编排（thenApply / thenCompose）");
        System.out.println();
        System.out.println("  ★ 先说清楚一个常见误解：");
        System.out.println("    「无中间阻塞」≠ 总耗时更短");
        System.out.println("    A→B→C 是串行依赖，总时间 = A耗时 + B耗时，两种写法一样长。");
        System.out.println("    区别在于：Future 写法主线程全程陪跑被占用，");
        System.out.println("              CF 写法主线程在等待期间可以「同时」做其他事。");
        System.out.println("    → 价值体现在「主线程吞吐量」，不是「单次任务速度」。");
        System.out.println();

        System.out.println("  ── 对比一：Future 写法（主线程全程阻塞，无法干其他事）──");
        {
            // 用 AtomicInteger 模拟主线程本可以做的「其他工作」（每100ms+1）
            java.util.concurrent.atomic.AtomicInteger mainThreadWork = new java.util.concurrent.atomic.AtomicInteger(0);
            long t0 = System.currentTimeMillis();

            java.util.concurrent.Future<Integer> fa = pool.submit(() -> { sleep(500); return 1; });
            System.out.println("    [主线程] 等 A... ★阻塞中，无法干其他事★");
            int a = fa.get(); // 主线程在这里阻塞 500ms，啥也干不了
            System.out.println("    [主线程] A 完成，耗时 " + (System.currentTimeMillis() - t0) + "ms");

            java.util.concurrent.Future<String> fb = pool.submit(() -> { sleep(500); return "R" + a; });
            System.out.println("    [主线程] 等 B... ★再次阻塞中，无法干其他事★");
            String b = fb.get(); // 再阻塞 500ms
            System.out.println("    [主线程] B 完成，耗时 " + (System.currentTimeMillis() - t0) + "ms");
            System.out.println("    Future 总耗时：" + (System.currentTimeMillis() - t0) + "ms"
                    + "，主线程「其他工作」完成量：" + mainThreadWork.get()
                    + "（等于0，全程被占用）");
        }

        System.out.println();
        System.out.println("  ── 对比二：CF 写法（主线程自由，等待期间可以同时处理其他请求）──");
        {
            java.util.concurrent.atomic.AtomicInteger mainThreadWork = new java.util.concurrent.atomic.AtomicInteger(0);
            long t0 = System.currentTimeMillis();

            // 构建链：瞬间返回，不阻塞主线程
            java.util.concurrent.CompletableFuture<String> chain =
                    java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> { sleep(500); return 1; }, pool)
                    .thenApply(a -> { sleep(500); return "R" + a; });

            // ★ 主线程此刻完全自由，可以处理其他业务 ★
            System.out.println("    [主线程] 链已提交（t=" + (System.currentTimeMillis() - t0)
                    + "ms），主线程去处理其他请求...");
            long loopStart = System.currentTimeMillis();
            while (!chain.isDone()) {
                // 模拟主线程在等待期间处理其他轻量工作（每次耗时约50ms）
                Thread.sleep(50);
                mainThreadWork.incrementAndGet();
            }
            System.out.println("    [主线程] 链完成了，get() 立即返回（几乎不等）");
            String result = chain.get();
            System.out.println("    [主线程] 结果：" + result);
            System.out.println("    CF 总耗时：" + (System.currentTimeMillis() - t0) + "ms"
                    + "，主线程「其他工作」完成量：" + mainThreadWork.get()
                    + "（等待期间处理了 " + mainThreadWork.get() + " 个其他任务！）");
        }

        System.out.println();
        System.out.println("  ★ 结论：两者总耗时相同（都是 A+B 的时间），");
        System.out.println("    但 Future 写法主线程被完全占用，CF 写法主线程同期处理了其他工作。");
        System.out.println("    在高并发服务器场景：");
        System.out.println("    Future 写法 → 处理一个请求期间，该线程不能服务其他请求（吞吐量↓）");
        System.out.println("    CF 写法    → 线程在等待 IO 期间可以处理其他请求（吞吐量↑）");
        System.out.println();

        // 解决痛点C：多任务组合
        System.out.println("  解决痛点C：多任务组合（allOf / anyOf / thenCombine）");
        long s = System.currentTimeMillis();
        java.util.concurrent.CompletableFuture<String> c1 =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> { sleep(100); return "A"; }, pool);
        java.util.concurrent.CompletableFuture<String> c2 =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> { sleep(150); return "B"; }, pool);
        java.util.concurrent.CompletableFuture.allOf(c1, c2).get();
        System.out.println("  → allOf 并行完成：" + c1.get() + c2.get()
                + " 耗时=" + (System.currentTimeMillis() - s) + "ms（约150ms，非250ms）\n");

        // 解决痛点D：优雅异常处理
        System.out.println("  解决痛点D：优雅异常处理（exceptionally / handle）");
        String r = java.util.concurrent.CompletableFuture
                .<String>supplyAsync(() -> { throw new RuntimeException("查询失败"); }, pool)
                .exceptionally(ex -> "降级默认值（" + ex.getCause().getMessage() + "）")
                .get();
        System.out.println("  → 异常降级结果：" + r);

        pool.shutdown();

        System.out.println();
        System.out.println("  ✅ CompletableFuture 的核心优势：");
        System.out.println("     1. 回调驱动，主线程真正解放，不用傻等");
        System.out.println("     2. 链式 API，代码像流水线一样清晰");
        System.out.println("     3. 内置组合能力，allOf/anyOf/thenCombine 一行搞定");
        System.out.println("     4. 异常处理内嵌在链中，像 try-catch 一样自然");
        System.out.println("     5. 手动完成（complete/completeExceptionally），灵活控制完成时机");
        System.out.println();
        System.out.println("  ❌ CompletableFuture 的缺点（诚实面对）：");
        System.out.println("     1. 学习曲线陡：thenApply/thenCompose/thenCombine 容易混淆");
        System.out.println("     2. 调试困难：异步链的堆栈信息不直观，出了问题难定位");
        System.out.println("     3. 默认线程池危险：忘传 executor 会污染 ForkJoinPool.commonPool()");
        System.out.println("     4. 异常容易被吞：不 get() 的话异常静默消失");
        System.out.println("     5. JDK 8 不支持超时（需 JDK 9 的 orTimeout/completeOnTimeout）");
    }

    // ----------------------------------------------------------------
    // 第五层：横向对比表
    // ----------------------------------------------------------------
    static void stage5_ComparisonTable() {
        System.out.println("  ┌──────────────┬────────────┬──────────────────────────────────────────────────┐");
        System.out.println("  │ 维度         │ Runnable   │ 说明（作为基准对比）                             │");
        System.out.println("  ├──────────────┼────────────┼──────────────────────────────────────────────────┤");
        System.out.println("  │ 类型         │ 接口       │                                                  │");
        System.out.println("  │ 有返回值     │ ❌         │                                                  │");
        System.out.println("  │ 抛受检异常   │ ❌         │                                                  │");
        System.out.println("  │ 可异步执行   │ ✅         │ 配合 Thread/线程池                               │");
        System.out.println("  │ 链式编排     │ ❌         │                                                  │");
        System.out.println("  │ 异常处理机制 │ ❌         │                                                  │");
        System.out.println("  └──────────────┴────────────┴──────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  ┌──────────────┬────────────┬────────────┬──────────────┬──────────────────────┐");
        System.out.println("  │ 维度         │ Callable   │ Future     │ FutureTask   │ CompletableFuture    │");
        System.out.println("  ├──────────────┼────────────┼────────────┼──────────────┼──────────────────────┤");
        System.out.println("  │ 类型         │ 接口       │ 接口       │ 实现类       │ 实现类               │");
        System.out.println("  │ JDK版本      │ 1.5        │ 1.5        │ 1.5          │ 8                    │");
        System.out.println("  │ 有返回值     │ ✅         │ ✅         │ ✅           │ ✅                   │");
        System.out.println("  │ 抛受检异常   │ ✅         │ -          │ ✅           │ -（需wrap）          │");
        System.out.println("  │ 需要线程池   │ 依赖配合   │ 是         │ 可不用       │ 强烈建议用           │");
        System.out.println("  │ 非阻塞获取   │ ❌         │ ❌         │ ❌           │ ✅（回调）           │");
        System.out.println("  │ 链式编排     │ ❌         │ ❌         │ ❌           │ ✅                   │");
        System.out.println("  │ 并行组合     │ ❌         │ ❌         │ ❌           │ ✅（allOf/anyOf）    │");
        System.out.println("  │ 内置异常处理 │ ❌         │ 弱         │ 弱           │ ✅（exceptionally）  │");
        System.out.println("  │ 手动完成     │ ❌         │ ❌         │ ❌           │ ✅（complete()）     │");
        System.out.println("  │ 超时支持     │ ❌         │ get(timeout)│ get(timeout)│ ✅（JDK9+ 原生）     │");
        System.out.println("  │ 幂等缓存     │ ❌         │ ❌         │ ✅（天然）   │ 需自行实现           │");
        System.out.println("  │ 学习曲线     │ 极低       │ 低         │ 低           │ 较高                 │");
        System.out.println("  └──────────────┴────────────┴────────────┴──────────────┴──────────────────────┘");
        System.out.println();
        System.out.println("  关系说明：");
        System.out.println("    Callable   → 描述「有返回值的任务」（是什么）");
        System.out.println("    Future     → 描述「怎么持有和获取异步结果」（怎么取）");
        System.out.println("    FutureTask → Callable 的可运行包装 + Future 的实现（能跑 + 能取）");
        System.out.println("    CompletableFuture → 独立的异步框架（能跑 + 能取 + 能编排）");
    }

    // ----------------------------------------------------------------
    // 第六层：选型决策树
    // ----------------------------------------------------------------
    static void stage6_DecisionTree() throws Exception {
        System.out.println("  决策树：根据场景选择正确的工具");
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("  │  你的需求是什么？                                               │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  1. 只需异步执行，不关心结果                                   │");
        System.out.println("  │     → Runnable + execute()                                      │");
        System.out.println("  │     → CompletableFuture.runAsync()（需要回调/链式时）           │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  2. 需要异步执行 + 拿到返回值，逻辑简单（无链式）              │");
        System.out.println("  │     → Callable + submit() → Future.get(timeout)                │");
        System.out.println("  │     适合：批量计算，提交N个任务，最后汇总结果                  │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  3. 需要不依赖线程池、直接用 Thread 执行 Callable               │");
        System.out.println("  │     → FutureTask + new Thread(task).start()                    │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  4. 高并发下，同一个 key 的计算只执行一次（防缓存击穿）        │");
        System.out.println("  │     → FutureTask（天然幂等，多线程 get() 只跑一次）            │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  5. 需要链式编排（A完成后自动触发B）                           │");
        System.out.println("  │     → CompletableFuture.thenCompose / thenApply                │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  6. 需要并行聚合（多个查询并行，等全部完成）                   │");
        System.out.println("  │     → CompletableFuture.allOf + thenCombine                    │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  7. 需要超时降级（N毫秒没返回就用默认值）                      │");
        System.out.println("  │     → CompletableFuture.anyOf（RPC vs 降级竞速）               │");
        System.out.println("  │     → JDK9+ 用 completeOnTimeout                               │");
        System.out.println("  │                                                                 │");
        System.out.println("  │  8. 需要「任意一个完成就返回」（多活容灾）                     │");
        System.out.println("  │     → CompletableFuture.anyOf                                  │");
        System.out.println("  └─────────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  实战案例对应：");
        System.out.println();
        System.out.println("  ─── 案例：批量查询用户信息（适合 Future） ───");
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(5);
        java.util.List<Integer> userIds = java.util.Arrays.asList(1, 2, 3, 4, 5);
        java.util.List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();

        for (int id : userIds) {
            final int uid = id;
            futures.add(pool.submit(() -> {
                Thread.sleep(50);
                return "User-" + uid;
            }));
        }
        // 汇总所有结果（简单批量场景，Future 足够）
        StringBuilder sb = new StringBuilder();
        for (java.util.concurrent.Future<String> f : futures) {
            sb.append(f.get()).append(" ");
        }
        System.out.println("  批量查询结果（Future）：" + sb.toString().trim());
        pool.shutdown();

        System.out.println();
        System.out.println("  ─── 案例：微服务下单流程（适合 CompletableFuture） ───");
        System.out.println("  步骤：校验用户 → 并行(锁库存 + 生成订单号) → 扣钱 → 发短信通知");
        System.out.println("  （有串有并，CompletableFuture 天然适合这种混合场景）");

        java.util.concurrent.ExecutorService bizPool = java.util.concurrent.Executors.newFixedThreadPool(4);
        long start = System.currentTimeMillis();

        String orderResult = java.util.concurrent.CompletableFuture
                // 第一步：校验用户（串行）
                .supplyAsync(() -> {
                    sleep(50);
                    return "USER-001";
                }, bizPool)
                // 第二步：并行做两件事（锁库存 + 生成订单号）
                .thenCompose(userId -> {
                    java.util.concurrent.CompletableFuture<String> lockStock =
                            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                                sleep(100); return "STOCK_LOCKED";
                            }, bizPool);
                    java.util.concurrent.CompletableFuture<String> genOrderNo =
                            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                                sleep(80); return "ORDER-" + System.currentTimeMillis();
                            }, bizPool);
                    // 等两个都完成，合并
                    return lockStock.thenCombine(genOrderNo, (stock, orderNo) ->
                            userId + "|" + stock + "|" + orderNo);
                })
                // 第三步：扣钱（串行）
                .thenApply(info -> {
                    sleep(60);
                    return info + "|PAID";
                })
                // 第四步：发通知（异步，不阻塞主流程）
                .whenComplete((info, ex) -> {
                    if (ex == null) System.out.println("  发短信通知：订单成功 - " + info.split("\\|")[2]);
                })
                .get();

        System.out.println("  下单结果：" + orderResult);
        System.out.println("  耗时：" + (System.currentTimeMillis() - start)
                + "ms（并行优化后：50+100+60=210ms，而非50+100+80+60=290ms）");
        bizPool.shutdown();
    }

    // ----------------------------------------------------------------
    // 第七层：反模式
    // ----------------------------------------------------------------
    static void stage7_AntiPatterns() throws Exception {
        System.out.println("  【反模式一：该用 CompletableFuture 却用 Future（浪费并行机会）】");
        System.out.println();
        System.out.println("  场景：查商品详情页（需要商品、价格、库存三个数据）");
        System.out.println();
        System.out.println("  ❌ 错误写法（用 Future + get，实际是串行的！）：");
        System.out.println("    Future<String> f1 = pool.submit(() -> queryProduct());  // 提交");
        System.out.println("    String product = f1.get();  // ← 阻塞等商品查完");
        System.out.println("    Future<String> f2 = pool.submit(() -> queryPrice());    // 再提交");
        System.out.println("    String price = f2.get();    // ← 阻塞等价格查完");
        System.out.println("    // 实际耗时 = 商品时间 + 价格时间 + 库存时间（串行！）");
        System.out.println();
        System.out.println("  ✅ 正确写法（CompletableFuture，真正并行）：");
        System.out.println("    先全部提交，再统一等待：");
        System.out.println("    CF<String> f1 = CF.supplyAsync(product, pool);  // 同时发出");
        System.out.println("    CF<String> f2 = CF.supplyAsync(price, pool);    // 同时发出");
        System.out.println("    CF<String> f3 = CF.supplyAsync(stock, pool);    // 同时发出");
        System.out.println("    CF.allOf(f1,f2,f3).get();  // 一起等，耗时=max(三者)");
        System.out.println();

        System.out.println("  【反模式二：该用 FutureTask 却用 CompletableFuture（防击穿场景）】");
        System.out.println();
        System.out.println("  场景：高并发下，userId=1001 的数据只查一次 DB，其他请求等待复用");
        System.out.println();
        System.out.println("  ❌ 用 CompletableFuture 的问题：");
        System.out.println("    // 需要自己维护 Map<key, CF>，需要加锁，并发控制复杂");
        System.out.println("    // CompletableFuture 没有「只执行一次」的内置保证");
        System.out.println();
        System.out.println("  ✅ 用 FutureTask 的优势：");
        System.out.println("    // FutureTask 内部状态机天然保证：无论多少线程 get()，task 只 run() 一次");
        System.out.println("    // ConcurrentHashMap + FutureTask 是经典的防击穿组合");
        System.out.println("    ConcurrentHashMap<String, FutureTask<T>> cache = new ConcurrentHashMap<>();");
        System.out.println("    FutureTask<T> ft = cache.computeIfAbsent(key, k -> new FutureTask<>(task));");
        System.out.println("    // computeIfAbsent 保证同一 key 只创建一个 FutureTask");
        System.out.println("    // 多个线程 ft.get()，只执行一次 task.call()");
        System.out.println();

        System.out.println("  【反模式三：在 thenApply 里 get() 另一个 Future（可能死锁）】");
        System.out.println();
        System.out.println("  ❌ 危险写法：");
        System.out.println("    CF<String> f2 = CF.supplyAsync(queryUser, pool);");
        System.out.println("    CF<String> f1 = CF.supplyAsync(queryOrder, pool)");
        System.out.println("        .thenApply(order -> f2.get());  // ← 可能死锁！");
        System.out.println("    // 原因：thenApply 占用了 pool 的一个线程，然后 f2.get() 在等 pool");
        System.out.println("    //       的另一个线程，如果 pool 线程全被 thenApply 占满，f2 永远等不到");
        System.out.println();
        System.out.println("  ✅ 正确写法：");
        System.out.println("    f1.thenCompose(order -> f2)  // thenCompose 不占线程等待");

        System.out.println();
        System.out.println("  ══════════════════════════════════════════════════");
        System.out.println("  最终选型口诀：");
        System.out.println("  ══════════════════════════════════════════════════");
        System.out.println("  简单批量、结果拿来就用    → Callable + Future");
        System.out.println("  不用线程池、只有一两个任务 → FutureTask + Thread");
        System.out.println("  高并发防击穿缓存          → FutureTask + ConcurrentHashMap");
        System.out.println("  有编排需求（链/并行/超时） → CompletableFuture（传自定义线程池！）");
        System.out.println("  ══════════════════════════════════════════════════");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

// ====================================================================
// 第九部分：thenApply vs thenCompose 深入剖析
// ====================================================================
class Part9_ThenApplyVsCompose {

    static final java.util.concurrent.ExecutorService POOL =
            java.util.concurrent.Executors.newFixedThreadPool(4);

    static void run() throws Exception {
        sec("1. 类型签名对比——从函数签名读懂本质");
        part1_TypeSignature();

        sec("2. 最直观的例子：普通值变换 vs 异步任务变换");
        part2_BasicDiff();

        sec("3. 错误用法：在 thenApply 里返回 CompletableFuture（嵌套地狱）");
        part3_WrongUsage();

        sec("4. thenCompose 正确展平：避免 CF<CF<T>>");
        part4_Flatten();

        sec("5. 实际案例：用户登录流程（串行多步异步）");
        part5_LoginFlow();

        sec("6. 什么时候只能用 thenApply，不能用 thenCompose");
        part6_WhenOnlyApply();

        sec("7. 混用场景：同一条链上既有 thenApply 又有 thenCompose");
        part7_MixedChain();

        sec("8. 底层原理：为什么 thenCompose 能展平");
        part8_Internals();

        sec("9. 一句话总结 + 选择口诀");
        part9_Summary();

        POOL.shutdown();
    }

    static void sec(String title) {
        System.out.println("\n  ── " + title + " ──\n");
    }

    // ----------------------------------------------------------------
    // 1. 类型签名
    // ----------------------------------------------------------------
    static void part1_TypeSignature() {
        System.out.println("  thenApply  签名：<U> CompletableFuture<U> thenApply(Function<T, U> fn)");
        System.out.println("  thenCompose签名：<U> CompletableFuture<U> thenCompose(Function<T, CompletableFuture<U>> fn)");
        System.out.println();
        System.out.println("  关键差别在 fn 的返回类型：");
        System.out.println();
        System.out.println("  thenApply  的 fn：T  ──→  U                    （普通值，同步）");
        System.out.println("  thenCompose的 fn：T  ──→  CompletableFuture<U>  （另一个异步任务）");
        System.out.println();
        System.out.println("  ┌───────────────────────────────────────────────────────────────┐");
        System.out.println("  │  thenApply  适合：fn 里做同步计算、类型转换、字段提取         │");
        System.out.println("  │  thenCompose适合：fn 里要发起另一个异步操作（查DB/调RPC等）   │");
        System.out.println("  └───────────────────────────────────────────────────────────────┘");
    }

    // ----------------------------------------------------------------
    // 2. 最直观的例子
    // ----------------------------------------------------------------
    static void part2_BasicDiff() throws Exception {
        System.out.println("  场景：拿到一个字符串后：");
        System.out.println("    情况A：把结果转成大写（同步，不涉及异步 IO）→ 用 thenApply");
        System.out.println("    情况B：拿结果去查另一个接口（异步 IO）       → 用 thenCompose");
        System.out.println();

        // 情况A：thenApply —— fn 是同步的，直接返回普通值
        System.out.println("  情况A：thenApply（同步转换）");
        String r1 = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> "hello", POOL)
                .thenApply(String::toUpperCase)     // 同步：String → String，无 IO
                .thenApply(s -> "[" + s + "]")      // 继续同步转换
                .get();
        System.out.println("    结果：" + r1);
        System.out.println("    fn 返回的是普通 String，thenApply 把它包成 CF<String>");
        System.out.println();

        // 情况B：thenCompose —— fn 要发起另一个异步任务，返回 CF
        System.out.println("  情况B：thenCompose（异步接异步）");
        String r2 = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> "user123", POOL)
                .thenCompose(userId ->
                        // fn 返回的是 CompletableFuture<String>，不是 String
                        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                            sleep(50);
                            return "UserDetail{id=" + userId + ", name=Alice}";
                        }, POOL)
                )
                .get();
        System.out.println("    结果：" + r2);
        System.out.println("    fn 返回的是 CF<String>，thenCompose 把它「展平」成 CF<String>（不是 CF<CF<String>>）");
    }

    // ----------------------------------------------------------------
    // 3. 错误用法：thenApply 里返回 CF → 得到 CF<CF<T>>
    // ----------------------------------------------------------------
    static void part3_WrongUsage() throws Exception {
        System.out.println("  ❌ 如果把异步操作错误地写成 thenApply：");
        System.out.println();
        System.out.println("    CompletableFuture<CompletableFuture<String>> WRONG =");
        System.out.println("        CF.supplyAsync(() -> \"user123\", pool)");
        System.out.println("          .thenApply(userId ->                     // ← 用 thenApply");
        System.out.println("              CF.supplyAsync(() -> queryDB(userId)) // fn 返回 CF");
        System.out.println("          );");
        System.out.println("    // 外层 CF 在 fn 执行完就「完成」了（完成 = 拿到了内层 CF）");
        System.out.println("    // 内层 CF 还没跑完，外层就认为自己完成了！");
        System.out.println("    // 要拿到真正结果：WRONG.get().get()  ← 两层 get，很丑");
        System.out.println();

        // 实际演示两层 get
        java.util.concurrent.CompletableFuture<
                java.util.concurrent.CompletableFuture<String>> wrong =
                java.util.concurrent.CompletableFuture
                        .supplyAsync(() -> "user123", POOL)
                        .thenApply(userId ->
                                java.util.concurrent.CompletableFuture.supplyAsync(
                                        () -> { sleep(50); return "Detail(" + userId + ")"; }, POOL)
                        );

        java.util.concurrent.CompletableFuture<String> inner = wrong.get();   // 第一层 get：拿到内层 CF
        String realResult = inner.get();                                        // 第二层 get：才拿到真正结果
        System.out.println("  两层 get 结果：" + realResult + "  ← 可以工作，但：");
        System.out.println();
        System.out.println("  ❌ 问题一：类型变成 CF<CF<String>>，无法继续 .thenApply(s -> ...) 链式");
        System.out.println("             因为 s 的类型是 CF<String>，不是 String！");
        System.out.println("  ❌ 问题二：外层 CF 在「fn 返回内层 CF 的瞬间」就完成了，");
        System.out.println("             此时内层 CF 还没执行完，注册在外层的回调会提前触发");
        System.out.println("  ❌ 问题三：异常处理断掉，内层 CF 的异常不会传播到外层 CF");

        // 演示回调提前触发
        System.out.println();
        System.out.println("  演示「回调提前触发」问题：");
        java.util.concurrent.CompletableFuture
                .supplyAsync(() -> "user123", POOL)
                .thenApply(userId ->
                        java.util.concurrent.CompletableFuture.supplyAsync(
                                () -> { sleep(300); return "Detail(" + userId + ")"; }, POOL)
                )
                .thenAccept(innerCf ->
                        // 这里 innerCf 是 CF<String>，不是 String
                        // 而且此刻内层 CF 可能还没跑完！
                        System.out.println("  thenAccept 触发了，但拿到的是: "
                                + innerCf.getClass().getSimpleName()
                                + "，内层isDone=" + innerCf.isDone() + " ← 还没完成！")
                )
                .get();
        Thread.sleep(400); // 等内层 CF 跑完
        System.out.println("  400ms 后内层才跑完 ← 说明 thenAccept 触发时内层根本没完成");
    }

    // ----------------------------------------------------------------
    // 4. thenCompose 正确展平
    // ----------------------------------------------------------------
    static void part4_Flatten() throws Exception {
        System.out.println("  ✅ 用 thenCompose 代替，自动展平，链式无缝衔接：");
        System.out.println();

        String right = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> "user123", POOL)
                .thenCompose(userId ->
                        java.util.concurrent.CompletableFuture.supplyAsync(
                                () -> { sleep(50); return "Detail(" + userId + ")"; }, POOL)
                )
                // ← 这里 s 的类型是 String，而不是 CF<String>，因为 thenCompose 展平了
                .thenApply(detail -> detail + " [已格式化]")
                .get();
        System.out.println("  结果：" + right + "（类型干净，链式无缝衔接）");
        System.out.println();
        System.out.println("  展平过程示意：");
        System.out.println("    .thenApply(fn)    → CF< CF<\"Detail\"> >   ← 嵌套，不好");
        System.out.println("    .thenCompose(fn)  → CF< \"Detail\" >        ← 展平，干净");
        System.out.println();
        System.out.println("  thenCompose 等内层 CF 真正完成后，外层 CF 才完成");
        System.out.println("  → 后续 .thenApply() 收到的是真实的 String，不是 CF 对象");
    }

    // ----------------------------------------------------------------
    // 5. 实际案例：用户登录流程
    // ----------------------------------------------------------------
    static void part5_LoginFlow() throws Exception {
        System.out.println("  步骤1：校验密码（异步，查 auth-service）           → 拿到 userId");
        System.out.println("  步骤2：用 userId 查用户详情（异步，查 user-DB）   → 拿到 UserInfo");
        System.out.println("  步骤3：用 UserInfo 查权限列表（异步，查 perm-DB） → 拿到 permissions");
        System.out.println("  步骤4：格式化输出（同步，无 IO）                   → 最终字符串");
        System.out.println();
        System.out.println("  步骤1-3：异步 IO → thenCompose");
        System.out.println("  步骤4：同步格式化 → thenApply");
        System.out.println();

        long start = System.currentTimeMillis();

        String loginResult = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("    [步骤1-thenCompose] 校验密码...");
                    sleep(100);
                    return "userId=1001";
                }, POOL)
                .thenCompose(userId -> {
                    System.out.println("    [步骤2-thenCompose] 查用户详情，userId=" + userId);
                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        sleep(150);
                        return userId + ", name=Alice, dept=Engineering";
                    }, POOL);
                })
                .thenCompose(userInfo -> {
                    System.out.println("    [步骤3-thenCompose] 查权限...");
                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        sleep(120);
                        return userInfo + ", perms=[READ, WRITE, DEPLOY]";
                    }, POOL);
                })
                .thenApply(fullInfo -> {
                    // 纯格式化，没有 IO，用 thenApply
                    System.out.println("    [步骤4-thenApply]  格式化（同步）");
                    return "✅ 登录成功 | " + fullInfo;
                })
                .get();

        System.out.println();
        System.out.println("  结果：" + loginResult);
        System.out.println("  耗时：" + (System.currentTimeMillis() - start) + "ms（100+150+120=370ms）");
    }

    // ----------------------------------------------------------------
    // 6. 什么时候只能用 thenApply
    // ----------------------------------------------------------------
    static void part6_WhenOnlyApply() throws Exception {
        System.out.println("  thenApply 正确场景：fn 不涉及任何异步 IO（纯内存操作）");
        System.out.println();

        String result = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> "{\"id\":42,\"name\":\"Alice\",\"score\":95.5}", POOL)
                .thenApply(json -> json.replace("{", "").replace("}", ""))  // 1. 清理（同步）
                .thenApply(s -> s.split(",")[1])                             // 2. 提取字段（同步）
                .thenApply(nameKv -> nameKv.split(":")[1].trim())           // 3. 取值（同步）
                .thenApply(name -> "Hello, " + name + "!")                  // 4. 格式化（同步）
                .get();
        System.out.println("  纯 thenApply 链结果：" + result);
        System.out.println();
        System.out.println("  ★ 以上每步都是内存操作，不需要线程切换。");
        System.out.println();
        System.out.println("  ❌ 如果误用 thenCompose 做同步操作：");
        System.out.println("    .thenCompose(s -> CompletableFuture.completedFuture(s.toUpperCase()))");
        System.out.println("    → 功能正确，但创建了不必要的 CF 对象，且语义上传达了错误信息");
        System.out.println("    → 有同事看到 thenCompose 会以为里面有异步 IO，增加理解成本");
        System.out.println("    → thenApply(s -> s.toUpperCase()) 更简洁，意图更清晰");
    }

    // ----------------------------------------------------------------
    // 7. 混用场景
    // ----------------------------------------------------------------
    static void part7_MixedChain() throws Exception {
        System.out.println("  场景：根据订单 ID 查询订单并生成账单");
        System.out.println("    1. 解析 orderId（同步）          → thenApply");
        System.out.println("    2. 查订单数据（异步 DB）         → thenCompose");
        System.out.println("    3. 提取商品列表（同步）          → thenApply");
        System.out.println("    4. 查每件商品实时价格（异步 RPC）→ thenCompose");
        System.out.println("    5. 计算总价并格式化（同步）      → thenApply");
        System.out.println();

        long start = System.currentTimeMillis();

        String bill = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> "  ORDER-20240101-88888  ", POOL)

                // 步骤1：解析（同步，thenApply）
                .thenApply(raw -> {
                    String orderId = raw.trim();
                    System.out.println("    [1-thenApply]  解析 orderId=" + orderId);
                    return orderId;
                })

                // 步骤2：查订单（异步 DB，thenCompose）
                .thenCompose(orderId -> {
                    System.out.println("    [2-thenCompose] 查订单...");
                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        sleep(100);
                        return orderId + "|items=[iPhone×1,AirPods×2]";
                    }, POOL);
                })

                // 步骤3：提取商品（同步，thenApply）
                .thenApply(orderData -> {
                    String items = orderData.split("\\|items=")[1].replace("[", "").replace("]", "");
                    System.out.println("    [3-thenApply]  提取商品=" + items);
                    return items;
                })

                // 步骤4：查实时价格（异步 RPC，thenCompose）
                .thenCompose(items -> {
                    System.out.println("    [4-thenCompose] 查实时价格...");
                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        sleep(80);
                        return items + "|prices=[iPhone=8999,AirPods=1299]";
                    }, POOL);
                })

                // 步骤5：计算总价（同步，thenApply）
                .thenApply(priceData -> {
                    System.out.println("    [5-thenApply]  计算总价");
                    int total = 8999 + 1299 * 2;
                    return "账单: " + priceData + " | 总计=" + total + "元";
                })

                .get();

        System.out.println();
        System.out.println("  账单：" + bill);
        System.out.println("  耗时：" + (System.currentTimeMillis() - start) + "ms");
        System.out.println();
        System.out.println("  ★ 步骤 1、3、5 → thenApply（纯内存，轻量）");
        System.out.println("    步骤 2、4   → thenCompose（有异步 IO）");
    }

    // ----------------------------------------------------------------
    // 8. 底层原理
    // ----------------------------------------------------------------
    static void part8_Internals() {
        System.out.println("  thenApply 内部：");
        System.out.println("    上一步完成 → 在当前线程同步执行 fn → 用结果 complete(fn(v)) → 新 CF 完成");
        System.out.println();
        System.out.println("  伪代码：");
        System.out.println("    CF<U> next = new CF<>();");
        System.out.println("    prevCF.whenDone(v -> next.complete( fn.apply(v) ));  // 同步");
        System.out.println("    return next;");
        System.out.println();
        System.out.println("  thenCompose 内部：");
        System.out.println("    上一步完成 → 执行 fn 拿到内层 CF（innerCF）");
        System.out.println("    → 不立即完成，而是监听 innerCF");
        System.out.println("    → innerCF 完成后，用 innerCF 的结果 complete 外层 CF");
        System.out.println("    → 外层 CF 才算完成（两层生命周期串联）");
        System.out.println();
        System.out.println("  伪代码：");
        System.out.println("    CF<U> next = new CF<>();");
        System.out.println("    prevCF.whenDone(v -> {");
        System.out.println("        CF<U> inner = fn.apply(v);        // fn 返回 CF");
        System.out.println("        inner.whenDone(u -> next.complete(u));  // 等内层，再完成外层");
        System.out.println("    });");
        System.out.println("    return next;  // 外层 CF 的完成时机 = 内层 CF 完成的时机");
        System.out.println();
        System.out.println("  ★ thenCompose 又叫 flatMap，与 Stream/Optional 中的 flatMap 同理：");
        System.out.println("    Stream<Stream<T>>  .flatMap() → Stream<T>   （展平）");
        System.out.println("    Optional<Optional<T>>.flatMap() → Optional<T>（展平）");
        System.out.println("    CF<CF<T>>  .thenCompose() → CF<T>           （展平）");
        System.out.println("    本质都是：「把包了两层的结构展平成一层」");
    }

    // ----------------------------------------------------------------
    // 9. 总结
    // ----------------------------------------------------------------
    static void part9_Summary() {
        System.out.println("  ┌────────────────┬─────────────────────────┬────────────────────────────────┐");
        System.out.println("  │                │ thenApply               │ thenCompose                    │");
        System.out.println("  ├────────────────┼─────────────────────────┼────────────────────────────────┤");
        System.out.println("  │ fn 返回类型    │ U（普通值）             │ CompletableFuture<U>           │");
        System.out.println("  │ 整体返回类型   │ CF<U>                   │ CF<U>（展平，不是 CF<CF<U>>）  │");
        System.out.println("  │ fn 里做什么    │ 同步操作（转换/计算）   │ 发起新的异步任务               │");
        System.out.println("  │ 流中类比       │ Stream.map              │ Stream.flatMap                 │");
        System.out.println("  │ 典型场景       │ JSON解析/格式化/计算    │ 查DB/调RPC/读文件              │");
        System.out.println("  │ 外层何时完成   │ fn 执行完立即完成       │ 内层 CF 完成后才完成           │");
        System.out.println("  └────────────────┴─────────────────────────┴────────────────────────────────┘");
        System.out.println();
        System.out.println("  选择口诀（一个问题）：");
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │  fn 里面，需不需要发起一个新的异步操作（查DB/RPC/读文件）？  │");
        System.out.println("  │                                                              │");
        System.out.println("  │  需要  → thenCompose，fn 返回 CompletableFuture<U>          │");
        System.out.println("  │  不需要 → thenApply，fn 返回普通值 U                         │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  最常犯的错误：");
        System.out.println("    fn 里调用了返回 CF 的方法，却用了 thenApply");
        System.out.println("    → 类型变成 CF<CF<T>>，链断掉，回调提前触发，异常不传播");
        System.out.println("    → 编译器不会报错，只有运行时才暴露！");
        System.out.println("    → 凡是 fn 返回的是 CF，就用 thenCompose，不用 thenApply");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

