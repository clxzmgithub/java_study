package org.example.basetest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池深度解析
 *
 * 目录：
 *   第一部分：为什么需要线程池（不用线程池的代价）
 *   第二部分：ThreadPoolExecutor 七大核心参数
 *   第三部分：线程池的生命周期与任务执行流程
 *   第四部分：★ 核心问题：线程执行完任务为什么不会死掉
 *   第五部分：线程池的五种状态
 *   第六部分：常见线程池类型对比
 *   第七部分：最佳实践
 */
public class ThreadPoolDemo {

    public static void main(String[] args) throws Exception {
        sep("第一部分：为什么需要线程池（不用线程池的代价）");
        Part1_WhyPool.run();

        sep("第二部分：ThreadPoolExecutor 七大核心参数");
        Part2_Parameters.run();

        sep("第三部分：线程池的生命周期与任务执行流程");
        Part3_Lifecycle.run();

        sep("第四部分：★ 核心问题：线程执行完任务为什么不会死掉");
        Part4_WhyNotDie.run();

        sep("第五部分：线程池的五种状态");
        Part5_States.run();

        sep("第六部分：常见线程池类型对比");
        Part6_Types.run();

        sep("第七部分：最佳实践");
        Part7_BestPractice.run();
    }

    static void sep(String title) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" " + title);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
}

// ====================================================================
// 第一部分：为什么需要线程池
// ====================================================================
class Part1_WhyPool {
    static void run() throws Exception {
        System.out.println("【不用线程池：每个任务都 new Thread()】\n");

        long start = System.currentTimeMillis();
        int taskCount = 100;

        // 坏做法：每个任务 new 一个线程
        Thread[] threads = new Thread[taskCount];
        for (int i = 0; i < taskCount; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                // 模拟轻量任务（仅做一个小计算）
                int sum = 0;
                for (int j = 0; j < 1000; j++) sum += j;
                _ = sum; // 防止编译器优化掉
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        long rawThreadTime = System.currentTimeMillis() - start;
        System.out.println("  " + taskCount + " 个任务，每次 new Thread()：" + rawThreadTime + " ms");

        // 好做法：用线程池
        start = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> {
                int sum = 0;
                for (int j = 0; j < 1000; j++) sum += j;
                _ = sum;
                latch.countDown();
            });
        }
        latch.await();
        pool.shutdown();
        long poolTime = System.currentTimeMillis() - start;
        System.out.println("  " + taskCount + " 个任务，使用线程池（10线程）：" + poolTime + " ms");

        System.out.println();
        System.out.println("【new Thread() 的三大开销】");
        System.out.println();
        System.out.println("  1. 系统调用开销");
        System.out.println("     new Thread() → JVM 调用 OS 的 pthread_create()（Linux）");
        System.out.println("     OS 要分配线程描述符、PCB 等内核数据结构");
        System.out.println("     这是「系统调用」，要从用户态切换到内核态，代价很高");
        System.out.println("     单次 new Thread() 耗时约 0.1ms ~ 1ms（看 OS 负载）");
        System.out.println();
        System.out.println("  2. 内存开销");
        System.out.println("     每个线程默认栈大小：-Xss 参数，JVM 默认 512KB~1MB");
        System.out.println("     1000 个线程 = 500MB~1GB 栈内存！！");
        System.out.println("     线程过多还会导致频繁 GC，因为每个线程对象本身也是堆对象");
        System.out.println();
        System.out.println("  3. 上下文切换开销");
        System.out.println("     CPU 核数有限，线程数 >> CPU 核数时，OS 要频繁切换线程");
        System.out.println("     每次切换需要保存/恢复寄存器、程序计数器等，约 1~10 微秒");
        System.out.println("     线程越多，切换越频繁，真正干活的时间反而越少");
        System.out.println();
        System.out.println("【线程池解决的问题】");
        System.out.println("  ✅ 线程复用：线程创建一次，反复使用，摊薄创建成本");
        System.out.println("  ✅ 线程数量控制：避免无限创建线程耗尽内存");
        System.out.println("  ✅ 任务队列：来不及处理的任务先排队，不丢任务");
        System.out.println("  ✅ 统一管理：关闭、监控、调优都有统一入口");
    }

    // 用于防止编译器优化的变量
    @SuppressWarnings("unused")
    static volatile int _ = 0;
}

// ====================================================================
// 第二部分：七大核心参数
// ====================================================================
class Part2_Parameters {
    static void run() {
        System.out.println("【ThreadPoolExecutor 构造方法】\n");
        System.out.println("  new ThreadPoolExecutor(");
        System.out.println("      int corePoolSize,          // ① 核心线程数");
        System.out.println("      int maximumPoolSize,        // ② 最大线程数");
        System.out.println("      long keepAliveTime,         // ③ 非核心线程空闲存活时间");
        System.out.println("      TimeUnit unit,              // ④ 时间单位");
        System.out.println("      BlockingQueue<Runnable> workQueue,  // ⑤ 任务队列");
        System.out.println("      ThreadFactory threadFactory, // ⑥ 线程工厂");
        System.out.println("      RejectedExecutionHandler handler    // ⑦ 拒绝策略");
        System.out.println("  );\n");

        System.out.println("【每个参数详解】\n");

        System.out.println("  ① corePoolSize（核心线程数）");
        System.out.println("    线程池「长期驻留」的线程数量");
        System.out.println("    即使空闲，核心线程也不会被销毁（除非设置 allowCoreThreadTimeOut=true）");
        System.out.println("    类比：餐厅的「正式员工」，没客人也要上班");
        System.out.println();
        System.out.println("  ② maximumPoolSize（最大线程数）");
        System.out.println("    线程池能创建的线程总上限");
        System.out.println("    当核心线程都忙、队列也满了，才会创建额外的「临时工」线程");
        System.out.println("    总线程数不超过 maximumPoolSize");
        System.out.println("    类比：餐厅「正式员工 + 兼职」的总人数上限");
        System.out.println();
        System.out.println("  ③④ keepAliveTime + unit（非核心线程存活时间）");
        System.out.println("    「临时工」线程空闲超过这个时间就会被销毁");
        System.out.println("    核心线程默认不受此约束（除非 allowCoreThreadTimeOut=true）");
        System.out.println("    类比：兼职员工没活干超过 X 小时就让回家");
        System.out.println();
        System.out.println("  ⑤ workQueue（任务队列）—— 非常重要！");
        System.out.println("    核心线程都忙时，新任务先入队等待");
        System.out.println("    三种常见队列：");
        System.out.println("      LinkedBlockingQueue（无界）：队列无限长，maximumPoolSize 永远不生效");
        System.out.println("      ArrayBlockingQueue（有界）：队列满了才创建临时线程，满了触发拒绝策略");
        System.out.println("      SynchronousQueue（零容量）：不存储任务，直接交给线程，用于 CachedThreadPool");
        System.out.println();
        System.out.println("  ⑥ threadFactory（线程工厂）");
        System.out.println("    负责创建新线程，可以自定义线程名称、优先级、是否为守护线程等");
        System.out.println("    生产环境必须自定义，方便排查问题（线程名 = 排查日志的关键）");
        System.out.println();
        System.out.println("  ⑦ handler（拒绝策略）");
        System.out.println("    线程数达到 maximumPoolSize 且队列满时触发");
        System.out.println("    四种内置策略：");
        System.out.println("      AbortPolicy（默认）：直接抛出 RejectedExecutionException");
        System.out.println("      CallerRunsPolicy：让提交任务的线程自己来执行（降速）");
        System.out.println("      DiscardPolicy：静默丢弃，不抛异常");
        System.out.println("      DiscardOldestPolicy：丢弃队列里最老的任务，然后重试提交");
        System.out.println();

        System.out.println("【任务提交完整流程（核心逻辑）】\n");
        System.out.println("  pool.submit(task) 时：");
        System.out.println();
        System.out.println("  step1: 当前线程数 < corePoolSize?");
        System.out.println("           是 → 创建新核心线程执行任务（即使有空闲核心线程！）");
        System.out.println("           否 → step2");
        System.out.println();
        System.out.println("  step2: 任务队列未满?");
        System.out.println("           是 → 任务入队，等待核心线程来取");
        System.out.println("           否 → step3");
        System.out.println();
        System.out.println("  step3: 当前线程数 < maximumPoolSize?");
        System.out.println("           是 → 创建新「临时工」线程执行任务");
        System.out.println("           否 → step4");
        System.out.println();
        System.out.println("  step4: 触发拒绝策略（handler.rejectedExecution）");
        System.out.println();
        System.out.println("  流程图：");
        System.out.println("  ┌─────────────────────────────────────────────────────────┐");
        System.out.println("  │ submit(task)                                            │");
        System.out.println("  │      ↓                                                  │");
        System.out.println("  │  线程数 < core? ──是──► 新建核心线程立即执行              │");
        System.out.println("  │      ↓否                                                │");
        System.out.println("  │  队列未满? ─────是──► 入队等待                           │");
        System.out.println("  │      ↓否                                                │");
        System.out.println("  │  线程数 < max? ──是──► 新建临时线程立即执行              │");
        System.out.println("  │      ↓否                                                │");
        System.out.println("  │  触发拒绝策略                                            │");
        System.out.println("  └─────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  ⚠️ 一个常见误解：");
        System.out.println("     很多人以为「核心线程满了就创建临时线程」");
        System.out.println("     实际上：核心线程满了 → 先入队 → 队列也满了 → 才创建临时线程");
        System.out.println("     临时线程是「最后手段」，不是「第二选择」");
    }
}

// ====================================================================
// 第三部分：生命周期与任务执行流程
// ====================================================================
class Part3_Lifecycle {
    static void run() throws Exception {
        System.out.println("【用自定义线程工厂，观察线程创建时机】\n");

        AtomicInteger threadCreateCount = new AtomicInteger(0);
        AtomicInteger taskCount = new AtomicInteger(0);

        ThreadFactory namedFactory = r -> {
            int id = threadCreateCount.incrementAndGet();
            Thread t = new Thread(r, "pool-worker-" + id);
            System.out.println("  [ThreadFactory] 创建了新线程：" + t.getName()
                    + "（累计创建第 " + id + " 个线程）");
            return t;
        };

        // core=2, max=4, queue=2 的线程池（方便观察各阶段）
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2,                          // corePoolSize
                4,                          // maximumPoolSize
                5, TimeUnit.SECONDS,        // keepAliveTime
                new ArrayBlockingQueue<>(2), // 有界队列，容量=2
                namedFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );

        System.out.println("  配置：core=2, max=4, queue容量=2\n");
        System.out.println("  阶段1：提交前2个任务（应创建2个核心线程）");

        CountDownLatch blockLatch = new CountDownLatch(1); // 用来阻塞工作线程，方便观察
        CountDownLatch doneLatch = new CountDownLatch(4);

        for (int i = 1; i <= 2; i++) {
            final int id = i;
            pool.submit(() -> {
                System.out.println("    任务" + id + " 由 " + Thread.currentThread().getName() + " 执行");
                try { blockLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                taskCount.incrementAndGet();
                doneLatch.countDown();
            });
        }
        Thread.sleep(100); // 等线程启动
        System.out.println("  → 当前活跃线程数：" + pool.getActiveCount()
                + "，队列大小：" + pool.getQueue().size());

        System.out.println("\n  阶段2：再提交2个任务（核心线程都忙，应入队）");
        for (int i = 3; i <= 4; i++) {
            final int id = i;
            pool.submit(() -> {
                System.out.println("    任务" + id + " 由 " + Thread.currentThread().getName() + " 执行");
                try { blockLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                taskCount.incrementAndGet();
                doneLatch.countDown();
            });
        }
        Thread.sleep(100);
        System.out.println("  → 当前活跃线程数：" + pool.getActiveCount()
                + "，队列大小：" + pool.getQueue().size()
                + "（任务3、4在排队）");

        System.out.println("\n  阶段3：再提交2个任务（队列也满了，应创建临时线程）");
        for (int i = 5; i <= 6; i++) {
            final int id = i;
            pool.submit(() -> {
                System.out.println("    任务" + id + " 由 " + Thread.currentThread().getName() + " 执行");
                try { blockLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                taskCount.incrementAndGet();
                doneLatch.countDown();
            });
        }
        Thread.sleep(100);
        System.out.println("  → 当前总线程数：" + pool.getPoolSize()
                + "，活跃：" + pool.getActiveCount()
                + "，队列：" + pool.getQueue().size());

        System.out.println("\n  阶段4：再提交1个任务（线程数=max=4，队列也满，触发拒绝策略）");
        try {
            pool.submit(() -> System.out.println("    任务7"));
            System.out.println("  → 任务7 提交成功（不应该发生）");
        } catch (RejectedExecutionException e) {
            System.out.println("  → ✅ 触发拒绝策略：RejectedExecutionException（符合预期）");
        }

        System.out.println("\n  放开所有阻塞的任务...");
        blockLatch.countDown();
        doneLatch.await();

        System.out.println("  所有任务完成！已完成任务数：" + taskCount.get());

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
    }
}

// ====================================================================
// 第四部分：★ 核心问题：线程执行完任务为什么不会死掉
// ====================================================================
class Part4_WhyNotDie {

    static void run() throws Exception {
        System.out.println("【这是线程池最核心的设计，必须从源码理解】\n");

        principle();
        simulateRunWorker();
        observeReuse();
    }

    static void principle() {
        System.out.println("【一句话答案】");
        System.out.println("  线程执行完任务后，不退出，而是「阻塞在队列的 take()/poll() 上」");
        System.out.println("  等待下一个任务到来 —— 就像收银员做完一单，静静等待下一个顾客");
        System.out.println();
        System.out.println("【关键：Worker.run() 的核心逻辑（JDK 源码精简版）】\n");
        System.out.println("  // Worker 是 ThreadPoolExecutor 的内部类，实现了 Runnable");
        System.out.println("  class Worker implements Runnable {");
        System.out.println("      final Thread thread;     // 绑定的真实线程");
        System.out.println("      Runnable firstTask;      // 线程创建时的第一个任务");
        System.out.println();
        System.out.println("      public void run() {");
        System.out.println("          runWorker(this);     // 核心在这里");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  final void runWorker(Worker w) {");
        System.out.println("      Runnable task = w.firstTask;");
        System.out.println("      w.firstTask = null;");
        System.out.println();
        System.out.println("      // ★ 这是一个「永不退出」的大循环");
        System.out.println("      while (task != null || (task = getTask()) != null) {");
        System.out.println("          //                  ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑");
        System.out.println("          //         这里！没有任务时会「阻塞等待」");
        System.out.println("          try {");
        System.out.println("              task.run();      // 执行任务");
        System.out.println("          } finally {");
        System.out.println("              task = null;     // 任务清零，回到 while 条件检查");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("      // ← 只有 getTask() 返回 null，才会退出循环，线程才会死亡");
        System.out.println("  }");
        System.out.println();
        System.out.println("  private Runnable getTask() {");
        System.out.println("      boolean timed = (allowCoreThreadTimeOut || wc > corePoolSize);");
        System.out.println();
        System.out.println("      for (;;) {");
        System.out.println("          // 核心线程：workQueue.take()   —— 无限阻塞，永不超时");
        System.out.println("          // 非核心线程：workQueue.poll(keepAliveTime, unit) —— 超时返回 null");
        System.out.println("          Runnable r = timed");
        System.out.println("              ? workQueue.poll(keepAliveTime, unit)  // 超时返回 null → 线程退出");
        System.out.println("              : workQueue.take();                     // 一直阻塞，永不退出");
        System.out.println();
        System.out.println("          if (r != null) return r;  // 拿到任务，返回给 runWorker");
        System.out.println("          // r == null：超时了，返回 null → runWorker 退出循环 → 线程死亡");
        System.out.println("          return null;");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("【关键：BlockingQueue 的阻塞机制】");
        System.out.println();
        System.out.println("  take()：队列空时，线程进入 WAITING 状态（挂起，不占 CPU）");
        System.out.println("          有任务入队时，被唤醒，取走任务，继续执行");
        System.out.println();
        System.out.println("  poll(timeout)：队列空时，等待 timeout 时间");
        System.out.println("                 超时仍无任务 → 返回 null → 触发线程退出");
        System.out.println();
        System.out.println("  底层原理：");
        System.out.println("    LinkedBlockingQueue 内部用 ReentrantLock + Condition 实现");
        System.out.println("    take() → notEmpty.await() → 线程挂起，释放 CPU");
        System.out.println("    put()  → notEmpty.signal() → 唤醒等待的线程");
        System.out.println("    这是「生产者-消费者」模型的经典实现");
        System.out.println();
        System.out.println("【线程状态转换】");
        System.out.println();
        System.out.println("  RUNNABLE                   WAITING / TIMED_WAITING");
        System.out.println("  ──────────────────         ─────────────────────────────");
        System.out.println("  task.run() 执行中           getTask() 阻塞在 queue.take()");
        System.out.println("       ↕ 任务完成                    ↕ 新任务入队");
        System.out.println("  task = null, 回到 while      被唤醒，取到任务，进入 RUNNABLE");
        System.out.println();
        System.out.println("  线程始终在这两个状态之间切换，永不死亡（核心线程）");
        System.out.println("  非核心线程：TIMED_WAITING 超时后返回 null → 退出 → TERMINATED");
    }

    static void simulateRunWorker() throws Exception {
        System.out.println("\n【手写 runWorker + getTask 模拟，看穿「不死」的本质】\n");

        // 模拟线程池的核心机制
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        AtomicInteger tasksDone = new AtomicInteger(0);

        // 手写「工作线程」：模拟 runWorker 逻辑
        Thread worker = new Thread(() -> {
            System.out.println("  [工作线程] 启动，进入任务循环...");
            Runnable task;
            while (true) {
                try {
                    // 模拟 getTask()：从队列取任务
                    // take() = 无限等待，不超时 —— 核心线程不死的秘密就在这里！
                    System.out.println("  [工作线程] 队列为空，阻塞在 take()，进入 WAITING 状态...");
                    task = queue.take();           // ← 这里！阻塞！不占 CPU！
                    System.out.println("  [工作线程] 被唤醒！取到任务，开始执行...");
                } catch (InterruptedException e) {
                    System.out.println("  [工作线程] 收到中断信号，线程退出");
                    break; // 线程池 shutdown 时通过中断信号让线程退出
                }

                // 执行任务（对应 task.run()）
                try {
                    task.run();
                } finally {
                    tasksDone.incrementAndGet();
                }
                // 任务执行完，不退出，回到 while 顶部，再次 take()
                // 这就是「线程复用」的全部秘密
            }
            System.out.println("  [工作线程] 退出，线程死亡");
        }, "my-worker");

        worker.start();

        // 主线程模拟「提交任务」
        Thread.sleep(500); // 等工作线程进入 WAITING
        System.out.println("\n  [主线程] 提交任务1");
        queue.put(() -> {
            System.out.println("  [执行任务1] hello from task-1，线程：" + Thread.currentThread().getName());
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Thread.sleep(400); // 等任务1执行完，工作线程再次进入 WAITING

        System.out.println("\n  [主线程] 提交任务2");
        queue.put(() -> System.out.println(
                "  [执行任务2] hello from task-2，同一个线程：" + Thread.currentThread().getName()));

        Thread.sleep(400);

        System.out.println("\n  [主线程] 提交任务3");
        queue.put(() -> System.out.println(
                "  [执行任务3] hello from task-3，同一个线程：" + Thread.currentThread().getName()));

        Thread.sleep(400);
        System.out.println("\n  [主线程] 发送中断信号，通知工作线程退出");
        worker.interrupt();
        worker.join();

        System.out.println("\n  3个任务都由同一个线程执行，线程名：my-worker");
        System.out.println("  这就是「线程复用」—— 一个线程，执行了多个任务");
        System.out.println("  秘密只有一个：while(true) + queue.take() 阻塞等待");
    }

    static void observeReuse() throws Exception {
        System.out.println("\n【用真实线程池验证线程复用（观察线程名）】\n");

        // 固定1个线程的线程池，提交5个任务，观察是否都是同一个线程在执行
        ExecutorService pool = Executors.newFixedThreadPool(1, r -> new Thread(r, "reuse-worker"));

        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            pool.submit(() -> {
                System.out.println("  任务" + id + " → 执行线程：" + Thread.currentThread().getName());
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                latch.countDown();
            });
        }
        latch.await();
        pool.shutdown();

        System.out.println();
        System.out.println("  ↑ 5个任务，全部由 reuse-worker 执行");
        System.out.println("  线程只创建了一次，但服务了5个任务 —— 这就是「复用」");
    }
}

// ====================================================================
// 第五部分：线程池的五种状态
// ====================================================================
class Part5_States {
    static void run() throws Exception {
        System.out.println("【ThreadPoolExecutor 内部用一个 AtomicInteger ctl 同时记录两件事】");
        System.out.println("  高3位：线程池状态（5种）");
        System.out.println("  低29位：线程数量");
        System.out.println();
        System.out.println("  五种状态：");
        System.out.println();
        System.out.println("  RUNNING  (-1 << 29)  接受新任务，处理队列任务");
        System.out.println("      ↓ shutdown()");
        System.out.println("  SHUTDOWN (0 << 29)   不接受新任务，但继续处理队列中剩余任务");
        System.out.println("      ↓ 队列清空 & 线程数=0");
        System.out.println("  TIDYING  (2 << 29)   所有任务已终止，即将调用 terminated()");
        System.out.println("      ↓ terminated() 执行完");
        System.out.println("  TERMINATED (3<<29)   完全终止");
        System.out.println();
        System.out.println("      另外还有：");
        System.out.println("  RUNNING → shutdownNow() → STOP (1 << 29)");
        System.out.println("    STOP：不接受新任务，不处理队列任务，中断正在执行的任务");
        System.out.println("      ↓ 线程数=0");
        System.out.println("  TIDYING → TERMINATED");
        System.out.println();
        System.out.println("【shutdown() vs shutdownNow() 的区别】");
        System.out.println();
        System.out.println("  shutdown()：");
        System.out.println("    状态变为 SHUTDOWN");
        System.out.println("    不再接受新任务（提交会抛 RejectedExecutionException）");
        System.out.println("    但队列里已有的任务会继续执行完");
        System.out.println("    正在执行的任务也不会被中断");
        System.out.println("    温和关闭，类比「发出最后通牒，等现有工作做完再下班」");
        System.out.println();
        System.out.println("  shutdownNow()：");
        System.out.println("    状态变为 STOP");
        System.out.println("    不再接受新任务");
        System.out.println("    清空并返回队列中未执行的任务");
        System.out.println("    对所有工作线程调用 interrupt()（不保证能停止！看任务是否响应中断）");
        System.out.println("    强硬关闭，类比「立刻拉闸，强制下班」");
        System.out.println();

        // 演示 shutdown() 的行为
        System.out.println("【演示 shutdown() 的行为】\n");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 1; i <= 3; i++) {
            final int id = i;
            pool.submit(() -> {
                System.out.println("  任务" + id + " 开始执行");
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("  任务" + id + " 执行完毕");
                latch.countDown();
            });
        }

        Thread.sleep(50); // 让任务1、2先开始执行
        System.out.println("  调用 shutdown()...");
        pool.shutdown();

        // 尝试再提交一个任务（应该失败）
        try {
            pool.submit(() -> System.out.println("  任务4（shutdown后提交）"));
        } catch (RejectedExecutionException e) {
            System.out.println("  → shutdown 后提交被拒绝：RejectedExecutionException ✅");
        }

        latch.await();
        System.out.println("  → 所有任务执行完毕，shutdown 后已有任务正常执行完 ✅");
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}

// ====================================================================
// 第六部分：常见线程池类型
// ====================================================================
class Part6_Types {
    static void run() {
        System.out.println("【Executors 提供的4种快捷线程池（底层都是 ThreadPoolExecutor）】\n");

        System.out.println("  1. newFixedThreadPool(n)");
        System.out.println("     new ThreadPoolExecutor(n, n, 0L, MILLISECONDS,");
        System.out.println("         new LinkedBlockingQueue<>())");
        System.out.println("     核心数 = 最大数 = n，队列「无界」");
        System.out.println("     ⚠️ 风险：无界队列 → 任务堆积 → OOM");
        System.out.println("     适用：任务量可控，需要固定并发度");
        System.out.println();

        System.out.println("  2. newCachedThreadPool()");
        System.out.println("     new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, SECONDS,");
        System.out.println("         new SynchronousQueue<>())");
        System.out.println("     核心数=0，最大数=无限，队列容量=0（直接交给线程）");
        System.out.println("     任务来了立刻有线程接，60s 空闲就销毁");
        System.out.println("     ⚠️ 风险：线程数无上限 → 任务大爆发 → 创建海量线程 → OOM");
        System.out.println("     适用：任务量少、任务执行很短的场景");
        System.out.println();

        System.out.println("  3. newSingleThreadExecutor()");
        System.out.println("     new ThreadPoolExecutor(1, 1, 0L, MILLISECONDS,");
        System.out.println("         new LinkedBlockingQueue<>())");
        System.out.println("     只有1个线程，所有任务串行执行，保证顺序");
        System.out.println("     ⚠️ 风险：同 FixedThreadPool，无界队列可能 OOM");
        System.out.println("     适用：需要顺序执行的场景（如日志顺序写入）");
        System.out.println();

        System.out.println("  4. newScheduledThreadPool(n)");
        System.out.println("     底层是 ScheduledThreadPoolExecutor");
        System.out.println("     支持延时执行、周期执行");
        System.out.println("     适用：定时任务");
        System.out.println();

        System.out.println("  ⚠️ 阿里巴巴 Java 开发手册 规定：");
        System.out.println("     「禁止使用 Executors 创建线程池，必须通过 ThreadPoolExecutor 手动创建」");
        System.out.println("     原因：Executors 的默认配置隐藏了无界队列/无限线程数的风险");
        System.out.println("     生产环境一定要明确指定：有界队列 + 合理的线程数上限");
        System.out.println();

        System.out.println("【生产环境推荐写法】\n");
        System.out.println("  ThreadPoolExecutor pool = new ThreadPoolExecutor(");
        System.out.println("      Runtime.getRuntime().availableProcessors(),   // corePoolSize");
        System.out.println("      Runtime.getRuntime().availableProcessors() * 2, // maxPoolSize");
        System.out.println("      60, TimeUnit.SECONDS,                          // keepAliveTime");
        System.out.println("      new ArrayBlockingQueue<>(1000),                // 有界队列！");
        System.out.println("      new ThreadFactory() {                          // 自定义线程名");
        System.out.println("          AtomicInteger count = new AtomicInteger(0);");
        System.out.println("          public Thread newThread(Runnable r) {");
        System.out.println("              return new Thread(r, \"biz-worker-\" + count.incrementAndGet());");
        System.out.println("          }");
        System.out.println("      },");
        System.out.println("      new ThreadPoolExecutor.CallerRunsPolicy()      // 调用者执行，不丢任务");
        System.out.println("  );");
    }
}

// ====================================================================
// 第七部分：最佳实践
// ====================================================================
class Part7_BestPractice {
    static void run() throws Exception {
        System.out.println("【一、线程数怎么定】\n");
        System.out.println("  经典公式（Little's Law 变体）：");
        System.out.println("    CPU 密集型任务：线程数 = CPU 核数 + 1");
        System.out.println("      线程一直在算，不会等待 IO，线程多了反而增加切换成本");
        System.out.println("      +1 是为了防止某个线程偶发的 page fault 等情况");
        System.out.println();
        System.out.println("    IO 密集型任务：线程数 = CPU 核数 × (1 + 等待时间/计算时间)");
        System.out.println("      线程大部分时间在等待 IO（网络请求、数据库查询），CPU 空闲");
        System.out.println("      多线程可以让 CPU 在等待 IO 时去处理其他任务");
        System.out.println("      典型 Web 服务：IO 时间 >> 计算时间，可设 CPU核数 × 2 ~ × 4");
        System.out.println();
        System.out.println("    实际建议：压测决定，公式只是起点，不是终点");
        System.out.println("    监控指标：CPU 利用率、队列积压、任务平均等待时间、P99 响应时间");
        System.out.println();
        System.out.println("【二、必须处理的异常问题】\n");
        System.out.println("  submit() 提交任务：异常被「吞掉」存入 Future，需调用 future.get() 才会抛出");
        System.out.println("  execute() 提交任务：异常直接抛给线程，触发 UncaughtExceptionHandler");
        System.out.println();
        System.out.println("  最佳实践：业务任务一律 try-catch，打日志，不要让异常静默消失");
        System.out.println("  监控线程的 UncaughtExceptionHandler：");
        System.out.println("    factory = r -> {");
        System.out.println("        Thread t = new Thread(r, name);");
        System.out.println("        t.setUncaughtExceptionHandler((thread, e) -> log.error(\"线程异常\", e));");
        System.out.println("        return t;");
        System.out.println("    };");
        System.out.println();
        System.out.println("【三、优雅关闭】\n");
        System.out.println("  // 标准的关闭模式：");
        System.out.println("  pool.shutdown();");
        System.out.println("  try {");
        System.out.println("      if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {");
        System.out.println("          pool.shutdownNow();  // 超时强制关闭");
        System.out.println("          if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {");
        System.out.println("              log.error(\"线程池无法正常关闭\");");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  } catch (InterruptedException e) {");
        System.out.println("      pool.shutdownNow();");
        System.out.println("      Thread.currentThread().interrupt();");
        System.out.println("  }");
        System.out.println();

        System.out.println("【四、总结：线程池的完整工作图】\n");
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │                      ThreadPoolExecutor                      │");
        System.out.println("  │                                                              │");
        System.out.println("  │  submit(task)                                                │");
        System.out.println("  │      ↓                                                       │");
        System.out.println("  │  ┌─────────────────────────────────────────────────────┐    │");
        System.out.println("  │  │             任务分配逻辑                             │    │");
        System.out.println("  │  │  线程数<core → 新建核心线程                          │    │");
        System.out.println("  │  │  队列未满   → 入队等待                               │    │");
        System.out.println("  │  │  线程数<max → 新建临时线程                           │    │");
        System.out.println("  │  │  否则       → 拒绝策略                               │    │");
        System.out.println("  │  └─────────────────────────────────────────────────────┘    │");
        System.out.println("  │                     ↓                                        │");
        System.out.println("  │  ┌────────────┐  ┌────────────────────┐                     │");
        System.out.println("  │  │ 任务队列   │  │  工作线程（Worker） │                     │");
        System.out.println("  │  │ BlockingQ  │  │                    │                     │");
        System.out.println("  │  │ [task1]   │→ │ while(true) {      │                     │");
        System.out.println("  │  │ [task2]   │  │   task=queue.take()│ ← 没任务时 WAITING  │");
        System.out.println("  │  │ [task3]   │  │   task.run()       │ ← 有任务时 RUNNING  │");
        System.out.println("  │  │  ...      │  │ }                  │                     │");
        System.out.println("  │  └────────────┘  └────────────────────┘                     │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  核心线程：while(true) + queue.take() 永不退出");
        System.out.println("  非核心线程：while(true) + queue.poll(timeout) 超时退出");
        System.out.println("  线程「不死」的秘密 = BlockingQueue 的阻塞等待机制");

        int cpuCores = Runtime.getRuntime().availableProcessors();
        System.out.println("\n  当前机器 CPU 核数：" + cpuCores);
        System.out.println("  推荐 IO 密集型线程数：" + (cpuCores * 2) + " ~ " + (cpuCores * 4));
        System.out.println("  推荐 CPU 密集型线程数：" + (cpuCores + 1));
    }
}

