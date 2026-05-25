package org.example.java_base_test.io.nio.showcase.jdknio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * 第1步：服务器启动流程
 * ═══════════════════════════════════════════════════════════════════════
 *
 * 这一步做三件事，对应五个底层系统调用：
 *
 *   Java 代码                      底层系统调用                  内核数据结构变化
 *   ─────────────────────────────────────────────────────────────────────────
 *   ServerSocketChannel.open()  →  socket(AF_INET,SOCK_STREAM)  → 分配 fd=3，建 socket 结构
 *   .bind(8080)                 →  bind(fd=3, 0.0.0.0:8080)     → 绑定端口
 *   .bind(8080) 内部也调了       →  listen(fd=3, backlog=128)    → 建 SYN 队列+Accept 队列
 *   .configureBlocking(false)   →  fcntl(fd=3, O_NONBLOCK)      → fd=3 标记为非阻塞
 *   Selector.open()             →  epoll_create()               → 建 epoll 实例(epfd=5)
 *   .register(selector,         →  epoll_ctl(epfd=5, ADD,       → fd=3 插入红黑树
 *       OP_ACCEPT)                          fd=3, EPOLLIN)        + fd=3 socket上挂回调
 *
 * 线程视角：
 *   以上全部由 main 线程执行，全部立即返回，没有任何阻塞。
 *   这些只是"注册/配置"操作，不涉及等待数据。
 *
 * 内核状态（启动后）：
 *
 *   epoll 实例（epfd=5）
 *   ├── 红黑树：[fd=3（监听socket）]
 *   ├── 就绪链表：（空）
 *   └── 等待队列：（空，main线程还没调 epoll_wait）
 *
 *   fd=3 对应的 socket
 *   ├── 状态：LISTEN
 *   ├── SYN 队列：（空，等待三次握手中的连接）
 *   ├── Accept 队列：（空，等待被 accept() 取走的已完成连接）
 *   └── 等待队列钩子：[ep_poll_callback]  ← epoll_ctl(ADD) 挂上的
 */
public class Step1_ServerSetup {

    // ★ 把这两个字段设为 public static，供后续 Step 复用
    public static Selector selector;
    public static ServerSocketChannel serverChannel;

    public static void setup() throws IOException {
        log("══════════════════════════════════════════════");
        log("  Step1：服务器启动");
        log("══════════════════════════════════════════════");
        log();

        // ── 第1行：创建 TCP 监听 socket ──────────────────────────────────────
        // 底层：socket(AF_INET, SOCK_STREAM, 0)
        // 内核：分配 fd=3（具体数字由内核决定），创建 socket 数据结构
        // 返回：立即，不阻塞
        serverChannel = ServerSocketChannel.open();
        log("[main线程] ServerSocketChannel.open()");
        log("  底层：socket(AF_INET, SOCK_STREAM)");
        log("  内核：分配 fd（假设fd=3），建监听socket结构体");
        log("  状态：立即返回，不阻塞");
        log();

        // ── 第2行：绑定端口 + 开始监听 ──────────────────────────────────────
        // 底层：bind(fd=3, {0.0.0.0, 8080}) + listen(fd=3, 128)
        // listen() 的 backlog=128 表示 Accept 队列最多放 128 个已完成握手的连接
        // 超出的连接会被内核直接丢弃（或发 RST，取决于内核配置）
        serverChannel.bind(new InetSocketAddress(8080));
        log("[main线程] serverChannel.bind(8080)");
        log("  底层：bind(fd=3, 0.0.0.0:8080)");
        log("        listen(fd=3, backlog=128)");
        log("  内核：fd=3 绑定到8080端口，建 SYN队列 + Accept队列");
        log("  状态：立即返回，不阻塞");
        log();

        // ── 第3行：设为非阻塞 ─────────────────────────────────────────────
        // 底层：fcntl(fd=3, F_SETFL, O_NONBLOCK)
        // 意义：之后对 fd=3 调 accept()，如果 Accept 队列为空，立即返回 EAGAIN，
        //       不会挂起线程。（没有 epoll 确认时不能随便调 accept，否则白等）
        serverChannel.configureBlocking(false);
        log("[main线程] serverChannel.configureBlocking(false)");
        log("  底层：fcntl(fd=3, F_SETFL, O_NONBLOCK)");
        log("  意义：accept() 在没有新连接时立即返回，不阻塞线程");
        log("  状态：立即返回，不阻塞");
        log();

        // ── 第4行：创建 Selector（= epoll 实例）─────────────────────────────
        // 底层：epoll_create(1)（参数1在新内核被忽略，历史遗留）
        // 内核：分配 eventpoll 结构体，包含：
        //   - rb_root rbr         → 红黑树根（初始为空，用来存"我关心的fd"）
        //   - list_head rdllist   → 就绪链表（初始为空，数据就绪时fd进这里）
        //   - wait_queue_head_t wq → 等待队列（初始为空，epoll_wait 挂起的线程进这里）
        // 返回：epfd=5（一个普通 fd，代表这个 epoll 实例）
        selector = Selector.open();
        log("[main线程] Selector.open()");
        log("  底层：epoll_create()");
        log("  内核：分配 epoll 实例（设 epfd=5）");
        log("        内含：红黑树（空）+ 就绪链表（空）+ 等待队列（空）");
        log("  状态：立即返回，不阻塞");
        log();

        // ── 第5行：把监听 fd 注册进 epoll ───────────────────────────────────
        // 底层：epoll_ctl(epfd=5, EPOLL_CTL_ADD, fd=3, {EPOLLIN, fd=3})
        // 内核做三件事：
        //   1. 创建 epitem 结构体（fd=3 的档案），记录 fd、关心的事件、所属epoll
        //   2. 把 epitem 插入红黑树（按 fd 数值大小排序，O(log n)）
        //   3. 在 fd=3 的 socket 等待队列上挂 ep_poll_callback 回调函数
        //      → 这个回调的作用：将来 fd=3 有新连接时（Accept队列非空），
        //        内核自动把 fd=3 的 epitem 放进就绪链表，并唤醒 epoll_wait 挂起的线程
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        log("[main线程] serverChannel.register(selector, OP_ACCEPT)");
        log("  底层：epoll_ctl(epfd=5, ADD, fd=3, EPOLLIN)");
        log("  内核：");
        log("    ① 创建 fd=3 的 epitem（档案）");
        log("    ② fd=3 的 epitem 插入红黑树");
        log("    ③ fd=3 的 socket 等待队列 挂上 ep_poll_callback");
        log("       └─ 回调作用：有新连接时 → fd=3 加就绪链表 → 唤醒 epoll_wait 线程");
        log("  状态：立即返回，不阻塞");
        log();

        log("【启动完成后内核状态】");
        log("  epoll实例(epfd=5)");
        log("  ├── 红黑树：[fd=3(监听)]");
        log("  ├── 就绪链表：（空）");
        log("  └── 等待队列：（空，main线程还没进来）");
        log();
        log("  fd=3 的 socket");
        log("  ├── 状态：LISTEN，绑定 0.0.0.0:8080");
        log("  ├── SYN队列：（空）");
        log("  ├── Accept队列：（空）");
        log("  └── 等待队列上已挂 ep_poll_callback ← 关键！");
        log();
        log("下一步：请看 Step2_EventLoop.java，main线程进入事件循环");
    }

    public static void main(String[] args) throws IOException {
        setup();
    }

    private static void log() {
        System.out.println();
    }

    private static void log(String msg) {
        System.out.println(msg);
    }
}

