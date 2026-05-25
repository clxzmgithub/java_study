package org.example.java_base_test.io.nio.showcase.jdknio;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * 第3步：线程视角完整演示
 * ═══════════════════════════════════════════════════════════════════════
 *
 * 本文件可以直接运行。
 *
 * 运行后会看到：
 *   - [main-IO线程] 在哪里挂起，挂起多久，被谁唤醒
 *   - [客户端线程A] 什么时候发请求
 *   - [客户端线程B] 什么时候发请求
 *   - main-IO线程 如何用单线程处理两个客户端的请求
 *
 * ───────────────────────────────────────────────────────────────────────
 * 线程角色说明
 * ───────────────────────────────────────────────────────────────────────
 *
 *   本演示涉及三个线程：
 *
 *   线程1：main-IO线程（就是 main 线程）
 *     → 职责：调 selector.select() 等待事件，处理 accept/read/write
 *     → 特点：大部分时间在 WAITING 状态（挂在 epoll_wait 上），不占CPU
 *             只有处理事件时才占CPU，处理完立刻回去等待
 *
 *   线程2：client-A线程
 *     → 模拟客户端A，延迟500ms后发出第一个HTTP请求
 *
 *   线程3：client-B线程
 *     → 模拟客户端B，延迟1200ms后发出第二个HTTP请求
 *
 *   注意：内核、网卡DMA、中断处理程序不是线程，是内核代码/硬件，
 *         它们的执行借用任意CPU核，不属于任何用户线程。
 *
 * ───────────────────────────────────────────────────────────────────────
 * 预期输出（时间轴）
 * ───────────────────────────────────────────────────────────────────────
 *
 *   T=0ms   [main-IO] 服务器启动完毕
 *   T=0ms   [main-IO] select() 挂起 ← 0% CPU，让出调度权
 *   T=500ms [client-A] 发起连接 + 发送请求
 *           [内核] 三次握手完成 → ep_poll_callback → 唤醒main
 *   T=500ms [main-IO] select() 返回（OP_ACCEPT）
 *   T=500ms [main-IO] accept() fd=6，注册 OP_READ
 *   T=500ms [main-IO] select() 再次挂起 ← 0% CPU
 *           [内核] 客户端A发来数据 → DMA → 中断 → epoll → 唤醒main
 *   T=500ms [main-IO] select() 返回（OP_READ，fd=6）
 *   T=500ms [main-IO] read(fd=6)，处理订单请求，write响应
 *   T=500ms [main-IO] select() 再次挂起 ← 0% CPU
 *   T=1200ms [client-B] 发起连接 + 发送请求
 *   T=1200ms [main-IO] 同上流程处理客户端B
 *   T=1200ms [main-IO] 全部请求处理完毕，等待下一个事件...
 */
public class Step3_ThreadPerspective {

    static final int PORT = 8082;
    static final String CRLF = "\r\n";
    static final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

    public static void main(String[] args) throws Exception {
        tlog("main-IO", "═══ Step3：线程视角完整演示 ═══");
        tlog("main-IO", "本次将模拟 2 个客户端依次发送订单查询请求");
        tlog("main-IO", "观察 main-IO 线程何时挂起、何时运行");
        tlog("main-IO", "");

        // ── 服务器启动 ────────────────────────────────────────────────────
        tlog("main-IO", "【服务器启动阶段】");

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        // 底层：socket(AF_INET, SOCK_STREAM) → fd=3
        tlog("main-IO", "  ServerSocketChannel.open() → socket(AF_INET,SOCK_STREAM) → fd=3");

        serverChannel.bind(new java.net.InetSocketAddress(PORT));
        // 底层：bind(fd=3, 0.0.0.0:8082) + listen(fd=3, 128)
        tlog("main-IO", "  bind(" + PORT + ") + listen() → SYN队列 + Accept队列 建好");

        serverChannel.configureBlocking(false);
        // 底层：fcntl(fd=3, O_NONBLOCK)
        tlog("main-IO", "  configureBlocking(false) → fcntl(O_NONBLOCK) → 非阻塞模式");

        Selector selector = Selector.open();
        // 底层：epoll_create() → epfd=5
        tlog("main-IO", "  Selector.open() → epoll_create() → 建 epoll 实例(epfd=5)");
        tlog("main-IO", "    内核：红黑树(空) + 就绪链表(空) + 等待队列(空)");

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 底层：epoll_ctl(ADD, fd=3, EPOLLIN)
        tlog("main-IO", "  register(OP_ACCEPT) → epoll_ctl(ADD,fd=3) → fd=3 进红黑树");
        tlog("main-IO", "    fd=3 socket 等待队列挂上 ep_poll_callback ← 关键！");
        tlog("main-IO", "");
        tlog("main-IO", "  ★ 当前 epoll 红黑树：[fd=3]  就绪链表：[]");
        tlog("main-IO", "");

        // ── 启动两个模拟客户端线程 ─────────────────────────────────────────
        // CountDownLatch 让两个客户端线程等服务器完全就绪后才开始
        CountDownLatch serverReady = new CountDownLatch(1);

        // 客户端A：500ms 后发请求（查询订单 id=101）
        Thread clientA = new Thread(() -> {
            try {
                serverReady.await();
                Thread.sleep(500);
                tlog("client-A", "发起连接到 localhost:" + PORT);
                tlog("client-A", "  底层：connect() → TCP三次握手（内核自动完成）");
                sendHttpRequest(PORT, "GET /order?id=101 HTTP/1.0");
                tlog("client-A", "请求完毕");
            } catch (Exception e) {
                tlog("client-A", "出错：" + e.getMessage());
            }
        }, "client-A");

        // 客户端B：1200ms 后发请求（查询订单 id=202）
        Thread clientB = new Thread(() -> {
            try {
                serverReady.await();
                Thread.sleep(1200);
                tlog("client-B", "发起连接到 localhost:" + PORT);
                tlog("client-B", "  底层：connect() → TCP三次握手（内核自动完成）");
                sendHttpRequest(PORT, "GET /order?id=202 HTTP/1.0");
                tlog("client-B", "请求完毕");
            } catch (Exception e) {
                tlog("client-B", "出错：" + e.getMessage());
            }
        }, "client-B");

        clientA.setDaemon(true);
        clientB.setDaemon(true);
        clientA.start();
        clientB.start();

        // ── 事件循环（main-IO 线程执行）─────────────────────────────────────
        serverReady.countDown();  // 通知客户端线程：服务器就绪

        tlog("main-IO", "【事件循环开始】");
        tlog("main-IO", "");

        // 处理完 2 个请求后退出演示
        int requestsHandled = 0;
        final int MAX_REQUESTS = 2;

        while (requestsHandled < MAX_REQUESTS) {

            // ── selector.select()：main线程在这里挂起 ─────────────────────
            tlog("main-IO", "--------------------------------------------------");
            tlog("main-IO", "selector.select() 调用");
            tlog("main-IO", "  底层：epoll_wait(epfd=5, events[], 1024, 3000ms)");
            tlog("main-IO", "  main-IO 线程状态：RUNNABLE → WAITING（让出CPU）");
            tlog("main-IO", "  此刻 main-IO 线程 0% CPU，OS 把CPU给其他线程/进程");
            tlog("main-IO", "  等待 epoll 就绪链表出现内容...");

            long waitStart = System.currentTimeMillis();
            // 用 3000ms 超时防止演示卡死
            int readyCount = selector.select(3000L);
            long waitMs = System.currentTimeMillis() - waitStart;

            tlog("main-IO", "  main-IO 线程状态：WAITING → RUNNABLE（被内核唤醒）");
            tlog("main-IO", "  挂起了 " + waitMs + "ms，就绪事件数 = " + readyCount);
            tlog("main-IO", "  底层：epoll_wait 返回，就绪链表有 " + readyCount + " 个 epitem");

            if (readyCount == 0) {
                tlog("main-IO", "  超时，无事件，继续等待...");
                continue;
            }

            // ── 遍历就绪事件 ──────────────────────────────────────────────
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();  // ★ 必须手动移除，否则下轮还会重复处理

                if (key.isAcceptable()) {
                    // ── OP_ACCEPT：新连接 ─────────────────────────────────
                    tlog("main-IO", "");
                    tlog("main-IO", "  【OP_ACCEPT 事件】");
                    tlog("main-IO", "  epoll 告诉我：fd=3 的 Accept 队列有新连接");

                    SocketChannel clientCh = serverChannel.accept();
                    // 底层：accept(fd=3) → 返回新 fd（内核从Accept队列取出连接）
                    // 不阻塞原因：epoll 已确认 Accept 队列非空，直接取，不等
                    tlog("main-IO", "  accept(fd=3) → 新连接 fd（不阻塞，队列里已有连接）");
                    tlog("main-IO", "  客户端：" + clientCh.getRemoteAddress());

                    clientCh.configureBlocking(false);
                    // 底层：fcntl(新fd, O_NONBLOCK)

                    clientCh.register(selector, SelectionKey.OP_READ);
                    // 底层：epoll_ctl(ADD, 新fd, EPOLLIN)
                    tlog("main-IO", "  epoll_ctl(ADD, 新fd, EPOLLIN) → 新连接进红黑树");
                    tlog("main-IO", "  ★ epoll 红黑树：[fd=3] + [新fd]");
                    tlog("main-IO", "  立即再次 select()，等新连接发来数据...");

                } else if (key.isReadable()) {
                    // ── OP_READ：数据可读 ─────────────────────────────────
                    tlog("main-IO", "");
                    tlog("main-IO", "  【OP_READ 事件】");
                    tlog("main-IO", "  epoll 告诉我：某个 fd 的内核接收缓冲区有数据");
                    tlog("main-IO", "  （数据已由 DMA 写好，硬件中断触发 ep_poll_callback 通知我）");

                    SocketChannel clientCh = (SocketChannel) key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(4096);

                    int n = clientCh.read(buf);
                    // 底层：read(fd, buf, 4096)
                    // 内核：接收缓冲区 → buf（内存拷贝）
                    // 不阻塞原因：epoll 已确认有数据，直接拷贝，不等

                    tlog("main-IO", "  read(fd) → 内核接收缓冲区 → buf（内存拷贝，不阻塞）");
                    tlog("main-IO", "  读到 " + n + " 字节，数据已在用户空间 buf 里");

                    if (n <= 0) {
                        key.cancel();
                        clientCh.close();
                        tlog("main-IO", "  对端关闭连接，fd 从红黑树删除");
                        continue;
                    }

                    buf.flip();
                    byte[] bytes = new byte[buf.limit()];
                    buf.get(bytes);
                    String request = new String(bytes);
                    String firstLine = request.split("\r\n")[0];
                    tlog("main-IO", "  请求首行：" + firstLine);

                    // 业务处理
                    String orderId = "unknown";
                    int idIdx = request.indexOf("id=");
                    if (idIdx != -1) {
                        int end = request.indexOf(' ', idIdx);
                        if (end == -1) end = request.indexOf('\r', idIdx);
                        if (end != -1) orderId = request.substring(idIdx + 3, end);
                    }
                    tlog("main-IO", "  业务处理：查询订单 id=" + orderId);
                    String body = "{\"code\":0,\"orderId\":\"" + orderId
                            + "\",\"status\":\"已发货\",\"amount\":299.0}";

                    String response = "HTTP/1.1 200 OK" + CRLF
                            + "Content-Type: application/json" + CRLF
                            + "Content-Length: " + body.getBytes().length + CRLF
                            + "Connection: close" + CRLF
                            + CRLF + body;

                    ByteBuffer respBuf = ByteBuffer.wrap(response.getBytes());
                    while (respBuf.hasRemaining()) {
                        clientCh.write(respBuf);
                    }
                    // 底层：write(fd, respBuf) → 数据写入内核发送缓冲区
                    // TCP协议栈 → 网卡DMA → 网线 → 客户端（异步，不等对方收到）
                    tlog("main-IO", "  write(fd) → 响应写入内核发送缓冲区（不等对方收到）");
                    tlog("main-IO", "  响应内容：" + body);

                    key.cancel();
                    clientCh.close();
                    // 底层：epoll_ctl(DEL, fd) + close(fd)
                    tlog("main-IO", "  epoll_ctl(DEL, fd) + close(fd) → 连接关闭，从红黑树删除");

                    requestsHandled++;
                    tlog("main-IO", "  ★ 已处理 " + requestsHandled + "/" + MAX_REQUESTS + " 个请求");
                }
            }
        }

        tlog("main-IO", "");
        tlog("main-IO", "==================================================");
        tlog("main-IO", "演示完毕！总结：");
        tlog("main-IO", "  main-IO 线程 全程只有 1 个线程");
        tlog("main-IO", "  处理了 2 个客户端的请求（多路复用）");
        tlog("main-IO", "  等待期间 0% CPU（挂在 epoll_wait 上）");
        tlog("main-IO", "  read()/accept()/write() 全部不阻塞（epoll预先确认就绪）");
        tlog("main-IO", "==================================================");

        selector.close();
        serverChannel.close();
    }

    /**
     * 模拟客户端发 HTTP 请求，返回响应体
     */
    static void sendHttpRequest(int port, String requestLine) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            String httpReq = requestLine + " HTTP/1.0\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n\r\n";
            // 处理 requestLine 可能已含 HTTP/1.0
            if (requestLine.contains("HTTP")) {
                httpReq = requestLine + "\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            }
            out.write(httpReq.getBytes());
            out.flush();

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int len;
            while ((len = socket.getInputStream().read(tmp)) != -1) {
                baos.write(tmp, 0, len);
            }
            byte[] resp = baos.toByteArray();
            String responseStr = new String(resp);
            // 只打印响应体（最后一行JSON）
            String[] parts = responseStr.split("\r\n\r\n");
            if (parts.length > 1) {
                tlog(Thread.currentThread().getName(), "  收到响应：" + parts[parts.length - 1]);
            }
        }
    }

    /** 带时间戳和线程名的日志 */
    static void tlog(String role, String msg) {
        long elapsed = System.currentTimeMillis() - startTime.get();
        System.out.printf("  [T+%4dms][%-10s] %s%n", elapsed, role, msg);
    }
}

