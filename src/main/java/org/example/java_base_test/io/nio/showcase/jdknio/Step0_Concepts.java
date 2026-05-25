package org.example.java_base_test.io.nio.showcase.jdknio;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * 第0步：读这里的注释，建立三个基础概念，后面所有代码都建立在这之上
 * ═══════════════════════════════════════════════════════════════════════
 *
 * ───────────────────────────────────────────────
 * 概念1：内核空间 vs 用户空间
 * ───────────────────────────────────────────────
 *
 *   物理内存被 OS 分成两个区域：
 *
 *   ┌─────────────────────────────────────────────┐
 *   │  内核空间（Kernel Space）                    │  ← 只有 OS 能碰
 *   │  - 网卡驱动代码                              │
 *   │  - TCP/IP 协议栈                             │
 *   │  - socket 接收/发送缓冲区（sk_rcvbuf）        │
 *   │  - epoll 数据结构（红黑树、就绪链表）          │
 *   │  - 进程调度器                                │
 *   ├─────────────────────────────────────────────┤
 *   │  用户空间（User Space）                      │  ← 你写的 Java 程序在这里
 *   │  - ByteBuffer buf = ByteBuffer.allocate(4096)│
 *   │  - 你的对象、堆、线程栈                       │
 *   └─────────────────────────────────────────────┘
 *
 *   你的程序不能直接读写内核空间。
 *   要让内核做事，必须发起「系统调用」：
 *     CPU 从用户模式 → 内核模式 → 内核帮你做 → 回用户模式
 *   每次切换有开销（约 100~300ns），所以要尽量减少系统调用次数。
 *
 * ───────────────────────────────────────────────
 * 概念2：fd（文件描述符）是什么
 * ───────────────────────────────────────────────
 *
 *   Linux 里「一切皆文件」。
 *   每个打开的文件/socket/管道，内核都分配一个整数 id，叫 fd（file descriptor）。
 *
 *   Java 代码里：
 *     ServerSocketChannel serverChannel = ServerSocketChannel.open();
 *     // 底层：内核分配 fd=3，创建监听 socket 的数据结构
 *
 *     Socket client = server.accept();
 *     // 底层：内核分配 fd=6，表示与客户端A的 TCP 连接
 *
 *   fd 就是一个"门牌号"，你对连接的所有操作（read/write/close）
 *   都通过这个整数来告诉内核：「对 fd=6 做 xxx」。
 *
 * ───────────────────────────────────────────────
 * 概念3：非阻塞 IO 的含义
 * ───────────────────────────────────────────────
 *
 *   阻塞 read（BIO 默认）：
 *     read(fd=6, buf) → 如果接收缓冲区没数据 → 线程挂起，等到有数据为止
 *
 *   非阻塞 read（NIO 设置 O_NONBLOCK）：
 *     read(fd=6, buf) → 如果接收缓冲区没数据 → 立即返回 EAGAIN（表示「现在没有，你再试」）
 *
 *   Java NIO 的关键：
 *     channel.configureBlocking(false);  // 底层：fcntl(fd, F_SETFL, O_NONBLOCK)
 *
 *   为什么 NIO + epoll 下 read() 不阻塞？
 *     因为 epoll_wait 已经确认"fd=6 的缓冲区有数据"才唤醒你，
 *     你再调 read() 就直接拷贝，不需要等，所以不阻塞。
 *     ↑ 这是理解 NIO 的最核心一句话
 *
 * ───────────────────────────────────────────────
 * 概念4：数据从网线到你手里，经历了什么
 * ───────────────────────────────────────────────
 *
 *   网线/网卡 ─DMA─► 内核接收缓冲区 ─read()─► 你的 ByteBuffer
 *               ↑                       ↑
 *         硬件自动，不需要CPU        系统调用，CPU参与拷贝
 *
 *   DMA（Direct Memory Access）：
 *     网卡收到数据包后，硬件直接把数据写入内核内存，不占用 CPU。
 *     写完后触发「硬件中断」通知 CPU：「数据我已经写好了」。
 *     CPU 的中断处理程序运行，触发 epoll 回调，唤醒等待的线程。
 *
 * 整个链路：
 *   客户端发数据
 *     → 网线传输
 *     → 网卡接收
 *     → DMA 写入内核接收缓冲区
 *     → 硬件中断
 *     → 内核中断处理 → epoll 回调 → 就绪链表 → 唤醒业务线程
 *     → 业务线程 read() 从内核缓冲区拷贝到 ByteBuffer
 *     → 业务线程处理数据
 */
public class Step0_Concepts {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  Step0：前置概念说明");
        System.out.println("============================================================");
        System.out.println();

        System.out.println("【概念1：内核空间 vs 用户空间】");
        System.out.println("  你的 ByteBuffer 在用户空间");
        System.out.println("  socket 接收缓冲区在内核空间");
        System.out.println("  read() 系统调用 = 内核缓冲区 → 你的 ByteBuffer");
        System.out.println();

        System.out.println("【概念2：fd（文件描述符）】");
        System.out.println("  每个 socket/连接 都是一个整数：fd=3, fd=6, fd=7...");
        System.out.println("  你对连接的所有操作都通过 fd 告诉内核");
        System.out.println();

        System.out.println("【概念3：非阻塞 IO】");
        System.out.println("  channel.configureBlocking(false)");
        System.out.println("  = fcntl(fd, O_NONBLOCK)");
        System.out.println("  = 缓冲区没数据时 read() 立即返回，不挂起线程");
        System.out.println();

        System.out.println("【概念4：数据到手的链路】");
        System.out.println("  网线 → 网卡 → DMA写内核缓冲区 → 硬件中断");
        System.out.println("       → epoll回调 → 唤醒线程 → read()拷贝到ByteBuffer");
        System.out.println();

        System.out.println("概念建立完毕，请继续看 Step1_ServerSetup.java");
    }
}

