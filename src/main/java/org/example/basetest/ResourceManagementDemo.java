package org.example.basetest;

/**
 * Java 三种资源（数据库连接、文件流、网络连接）详解
 *
 * 核心问题：
 * 1. 它们分别申请了什么资源？
 * 2. 为什么一定要释放？
 * 3. 不释放会发生什么？
 */
public class ResourceManagementDemo {

    public static void main(String[] args) {
        System.out.println("========== Java 三种资源管理详解 ==========\n");

        System.out.println("【1. 操作系统资源是有限的】");
        demoOsResourcesAreLimited();

        System.out.println("\n【2. 数据库连接：申请了什么资源？】");
        demoDatabaseConnection();

        System.out.println("\n【3. 文件流：申请了什么资源？】");
        demoFileStream();

        System.out.println("\n【4. 网络连接：申请了什么资源？】");
        demoNetworkConnection();

        System.out.println("\n【5. 不释放会发生什么？具体后果】");
        demoWhatHappensIfNotReleased();

        System.out.println("\n【6. 为什么 GC 不能替我们关闭这些资源？】");
        demoWhyGcCantHelp();
    }

    // ================================================================
    // 【1. 操作系统资源是有限的】
    // ================================================================
    private static void demoOsResourcesAreLimited() {
        System.out.println("  Java 程序运行在操作系统之上");
        System.out.println("  所有资源最终都由操作系统管理和分配");
        System.out.println();
        System.out.println("  操作系统有严格的资源上限（以 Linux 为例）：");
        System.out.println("  ┌────────────────────────────────────────────────┐");
        System.out.println("  │ 资源类型        │ 典型上限              │      │");
        System.out.println("  ├────────────────────────────────────────────────┤");
        System.out.println("  │ 文件描述符      │ 每进程默认 1024 个    │      │");
        System.out.println("  │ 数据库连接池    │ 通常 10~200 个        │      │");
        System.out.println("  │ TCP 端口        │ 0~65535（共 65536 个）│      │");
        System.out.println("  │ 内存            │ 物理内存 + 虚拟内存   │      │");
        System.out.println("  └────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  关键点：这些资源不是 Java 堆内存，GC 管不了它们！");
        System.out.println("  必须由程序员显式调用 close() 归还给操作系统。");
    }

    // ================================================================
    // 【2. 数据库连接：申请了什么资源？】
    // ================================================================
    private static void demoDatabaseConnection() {
        System.out.println("  代码示例（JDBC）：");
        System.out.println("  Connection conn = DriverManager.getConnection(url, user, pwd);");
        System.out.println();
        System.out.println("  执行这行代码后，底层申请了：");
        System.out.println();
        System.out.println("  1. 【TCP 网络连接】");
        System.out.println("     Java 程序 ←────TCP Socket────→ MySQL 服务器");
        System.out.println("     • 占用本机的一个 TCP 端口（如 50234）");
        System.out.println("     • 占用 MySQL 服务器的一个连接槽（MySQL 默认最多 151 个）");
        System.out.println("     • 双方各自维护 Socket 缓冲区（占用内存）");
        System.out.println();
        System.out.println("  2. 【MySQL 服务器端的线程】");
        System.out.println("     MySQL 每接受一个连接，就创建一个专属线程处理该连接");
        System.out.println("     • 这个线程占用 MySQL 服务器的 CPU 和内存");
        System.out.println("     • 连接不关，这个线程就一直存在，一直消耗服务器资源");
        System.out.println();
        System.out.println("  3. 【连接池中的一个 '名额'】");
        System.out.println("     生产中通常使用连接池（HikariCP、Druid）");
        System.out.println("     • 连接池大小通常是 10~20 个");
        System.out.println("     • 你借走一个连接不还，其他线程就等待");
        System.out.println("     • 连接池耗尽 → 所有请求阻塞 → 服务崩溃");
        System.out.println();
        System.out.println("  关闭连接（conn.close()）的效果：");
        System.out.println("     • 断开 TCP 连接，释放端口");
        System.out.println("     • MySQL 服务器销毁对应的处理线程");
        System.out.println("     • 连接归还到连接池，其他线程可以使用");
    }

    // ================================================================
    // 【3. 文件流：申请了什么资源？】
    // ================================================================
    private static void demoFileStream() {
        System.out.println("  代码示例：");
        System.out.println("  FileInputStream fis = new FileInputStream(\"/data/order.txt\");");
        System.out.println();
        System.out.println("  执行这行代码后，操作系统做了：");
        System.out.println();
        System.out.println("  1. 【文件描述符（File Descriptor，fd）】");
        System.out.println("     这是最核心的资源！");
        System.out.println("     操作系统维护一张'文件描述符表'（每个进程独立）：");
        System.out.println();
        System.out.println("     fd 0 → 标准输入（键盘）");
        System.out.println("     fd 1 → 标准输出（屏幕）");
        System.out.println("     fd 2 → 标准错误");
        System.out.println("     fd 3 → /data/order.txt   ← 你打开的文件");
        System.out.println("     fd 4 → ...（下一个打开的文件）");
        System.out.println();
        System.out.println("     Linux 默认每个进程最多 1024 个 fd！");
        System.out.println("     每打开一个文件/Socket，就消耗一个 fd");
        System.out.println();
        System.out.println("  2. 【内核缓冲区】");
        System.out.println("     操作系统为文件 I/O 分配内核态的缓冲区（Page Cache）");
        System.out.println("     用于加速读写（减少真正的磁盘 I/O）");
        System.out.println();
        System.out.println("  3. 【文件锁（某些场景）】");
        System.out.println("     某些操作系统下，打开文件会加锁");
        System.out.println("     不关闭 → 其他进程/线程无法访问该文件");
        System.out.println();
        System.out.println("  关闭文件流（fis.close()）的效果：");
        System.out.println("     • 释放文件描述符（fd 3 变为空闲）");
        System.out.println("     • 刷新并释放内核缓冲区");
        System.out.println("     • 解除文件锁（如果有）");
        System.out.println();
        System.out.println("  ⚠️ 写文件时不关闭的特别风险：");
        System.out.println("     BufferedOutputStream 有一个 Java 层的缓冲区");
        System.out.println("     如果不 close()，缓冲区里的数据永远不会写入磁盘！");
        System.out.println("     → 文件内容丢失！");
    }

    // ================================================================
    // 【4. 网络连接：申请了什么资源？】
    // ================================================================
    private static void demoNetworkConnection() {
        System.out.println("  代码示例：");
        System.out.println("  Socket socket = new Socket(\"api.example.com\", 443);");
        System.out.println();
        System.out.println("  执行这行代码后，申请了：");
        System.out.println();
        System.out.println("  1. 【本机 TCP 端口（本地端口）】");
        System.out.println("     操作系统为这个 Socket 分配一个临时端口（如 52341）");
        System.out.println("     • 端口范围：0~65535，其中 0~1023 是系统保留的");
        System.out.println("     • 临时端口（Ephemeral Port）范围：通常 32768~60999");
        System.out.println("     • 所有 Socket（包括文件！）都消耗文件描述符（fd）");
        System.out.println();
        System.out.println("  2. 【TCP 连接状态（四元组）】");
        System.out.println("     操作系统内核维护一条 TCP 连接记录：");
        System.out.println("     (本机IP:本机端口) ←→ (服务器IP:服务器端口)");
        System.out.println("     例：(192.168.1.1:52341) ←→ (1.2.3.4:443)");
        System.out.println();
        System.out.println("  3. 【发送/接收缓冲区】");
        System.out.println("     内核为每个 Socket 分配：");
        System.out.println("     • 发送缓冲区（Send Buffer）：通常 128KB");
        System.out.println("     • 接收缓冲区（Receive Buffer）：通常 128KB");
        System.out.println("     1000 个未关闭的 Socket → 256MB 内核内存被占用");
        System.out.println();
        System.out.println("  4. 【服务器端的资源】");
        System.out.println("     对方服务器也要维护这条连接，消耗对方的端口和内存");
        System.out.println("     你不关连接，对方的资源也一直被占用");
        System.out.println();
        System.out.println("  关闭连接（socket.close()）的效果：");
        System.out.println("     • 发送 TCP FIN 包，正式断开连接");
        System.out.println("     • 操作系统释放文件描述符");
        System.out.println("     • 释放发送/接收缓冲区内存");
        System.out.println("     • 本机临时端口重新变为可用");
    }

    // ================================================================
    // 【5. 不释放会发生什么？具体后果】
    // ================================================================
    private static void demoWhatHappensIfNotReleased() {
        System.out.println("  场景：一个 Web 服务器，每次请求都打开文件/数据库连接但不关闭\n");

        System.out.println("  ---- 文件描述符泄漏 ----");
        System.out.println("  请求 1：fd 3 → order_001.txt  （未关闭）");
        System.out.println("  请求 2：fd 4 → order_002.txt  （未关闭）");
        System.out.println("  请求 3：fd 5 → order_003.txt  （未关闭）");
        System.out.println("  ......");
        System.out.println("  请求 1021：fd 1023 → order_1021.txt （未关闭）");
        System.out.println("  请求 1022：❌ 报错！Too many open files");
        System.out.println("            系统崩溃，无法再打开任何文件/Socket");
        System.out.println();

        System.out.println("  ---- 数据库连接池耗尽 ----");
        System.out.println("  连接池大小：10");
        System.out.println("  10 个并发请求各拿走 1 个连接，都不归还");
        System.out.println("  第 11 个请求来了 → 等待连接池释放连接");
        System.out.println("  等待超时（如 30秒）→ 抛出 ConnectionPoolTimeoutException");
        System.out.println("  → 用户看到：500 Internal Server Error");
        System.out.println();

        System.out.println("  ---- 写文件缓冲区未刷新 ----");
        System.out.println("  写入订单记录到文件...");
        System.out.println("  数据在 Java 的 BufferedOutputStream 缓冲区中");
        System.out.println("  没有 close() / flush() → 程序崩溃");
        System.out.println("  → 缓冲区中的数据永远丢失，文件是空的或不完整");
        System.out.println("  → 订单数据丢失！");
        System.out.println();

        System.out.println("  ---- 内存泄漏（慢性死亡）----");
        System.out.println("  每个 Socket 的内核缓冲区约 256KB");
        System.out.println("  1000 个未关闭的 Socket → 256MB 内核内存");
        System.out.println("  长时间运行 → 内存耗尽 → OOM（Out Of Memory）→ 进程崩溃");
    }

    // ================================================================
    // 【6. 为什么 GC 不能替我们关闭这些资源？】
    // ================================================================
    private static void demoWhyGcCantHelp() {
        System.out.println("  很多人的误区：'Java 有 GC，会自动清理的'");
        System.out.println();
        System.out.println("  GC 能做什么：");
        System.out.println("  ✅ 回收 Java 堆（Heap）上不再使用的对象（String、ArrayList...）");
        System.out.println("  ✅ 回收 Java 对象占用的内存");
        System.out.println();
        System.out.println("  GC 不能做什么：");
        System.out.println("  ❌ GC 回收的是 Java 对象，不是操作系统资源");
        System.out.println("  ❌ 文件描述符（fd）是操作系统内核的资源，GC 不知道");
        System.out.println("  ❌ 数据库连接是 MySQL 服务器上的资源，GC 够不到");
        System.out.println("  ❌ TCP Socket 缓冲区在内核空间，GC 管不了");
        System.out.println();
        System.out.println("  内存结构图：");
        System.out.println("  ┌─────────────────────────────────────────────────┐");
        System.out.println("  │               操作系统                          │");
        System.out.println("  │  ┌──────────────────────────────────────────┐   │");
        System.out.println("  │  │  JVM 进程                                │   │");
        System.out.println("  │  │  ┌──────────────┐  ← GC 管这里          │   │");
        System.out.println("  │  │  │  Java Heap   │                        │   │");
        System.out.println("  │  │  │  Connection  │──┐                     │   │");
        System.out.println("  │  │  │  对象（Java）│  │（Java对象持有fd编号）│   │");
        System.out.println("  │  │  └──────────────┘  │                     │   │");
        System.out.println("  │  └───────────────────────────────────────────   │");
        System.out.println("  │                        │                        │");
        System.out.println("  │  内核空间（GC 看不到）  ↓                        │");
        System.out.println("  │  ┌─────────────────────────────────────────┐    │");
        System.out.println("  │  │ 文件描述符表  fd3→order.txt             │    │");
        System.out.println("  │  │ TCP 连接记录  (192.168.1.1:52341)←→... │    │");
        System.out.println("  │  │ Socket 缓冲区 Send/Recv Buffer          │    │");
        System.out.println("  │  └─────────────────────────────────────────┘    │");
        System.out.println("  └─────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  当 Java 的 Connection 对象被 GC 回收时：");
        System.out.println("  • Java 堆内存释放了 ✅");
        System.out.println("  • 但内核里的 fd、Socket 缓冲区、TCP 连接 ❌ 没有释放！");
        System.out.println("  • 必须调用 close() 才能通知操作系统归还这些资源");
        System.out.println();
        System.out.println("  注：Java 的 finalize() 方法理论上可以在GC时关闭资源");
        System.out.println("      但 finalize() 执行时机不确定，可能很晚才调用");
        System.out.println("      完全不可靠，已在 Java 9 中标注为 @Deprecated");
        System.out.println("      结论：必须显式 close()，不能依赖 GC 或 finalize()");
    }
}

