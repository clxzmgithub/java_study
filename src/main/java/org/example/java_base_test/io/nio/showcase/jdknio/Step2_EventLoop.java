package org.example.java_base_test.io.nio.showcase.jdknio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * 第2步：事件循环（Event Loop）
 * ═══════════════════════════════════════════════════════════════════════
 *
 * 事件循环的本质：
 *   while (true) {
 *       等待事件（线程挂起，0% CPU）
 *       处理事件（线程运行，处理完再挂起）
 *   }
 *
 * ───────────────────────────────────────────────────────────────────────
 * selector.select() 的完整内核流程
 * ───────────────────────────────────────────────────────────────────────
 *
 *   底层：epoll_wait(epfd=5, events[], 1024, -1)
 *
 *   内核执行步骤：
 *   ① 检查就绪链表是否为空
 *      └─ 不为空 → 直接把就绪 fd 填入 events[]，返回，main线程不挂起
 *      └─ 为空   → 执行②
 *   ② 创建「等待条目」= { 线程=main, 唤醒函数 }
 *      把等待条目放入 epoll 的等待队列（wq）
 *   ③ 调用 schedule()，main线程让出 CPU
 *      OS 把 main线程从调度队列移走
 *      main线程进入 WAITING 状态（不占CPU，不占时间片）
 *   ④ ...（某个时刻）某个 fd 有事件发生
 *      硬件中断 → ep_poll_callback
 *      → 把该 fd 的 epitem 加入就绪链表
 *      → 从 wq 等待队列找到 main线程，加回调度队列
 *   ⑤ main线程被 OS 重新调度，继续执行 epoll_wait 后续逻辑：
 *      把就绪链表内容填入 events[] 数组
 *      清空（或保留）就绪链表
 *      返回就绪数量 n
 *
 * ───────────────────────────────────────────────────────────────────────
 * 三种事件类型及其底层调用
 * ───────────────────────────────────────────────────────────────────────
 *
 *   OP_ACCEPT（fd=3 可接受新连接）：
 *     触发条件：三次握手完成，连接进入 Accept 队列
 *     你要做：accept(fd=3) → 得到新连接 fd=6
 *             epoll_ctl(ADD, fd=6)  → 注册新连接
 *
 *   OP_READ（fd=6 可读）：
 *     触发条件：fd=6 的内核接收缓冲区有数据（网卡DMA写完并中断通知）
 *     你要做：read(fd=6, buf) → 数据从内核缓冲区拷贝到 buf
 *
 *   OP_WRITE（fd=6 可写，通常不用注册）：
 *     触发条件：fd=6 的内核发送缓冲区有空间（不拥塞）
 *     你要做：write(fd=6, buf) → buf 写入内核发送缓冲区
 *     注意：大多数时候发送缓冲区都有空间，不需要监听 OP_WRITE，
 *           直接 channel.write() 即可。只有写不完时才注册 OP_WRITE。
 *
 * ───────────────────────────────────────────────────────────────────────
 * 关键问题：为什么 accept() 和 read() 在这里不阻塞？
 * ───────────────────────────────────────────────────────────────────────
 *
 *   selector.select() 返回，意味着 epoll_wait 确认：
 *     - fd=3 可 accept  → Accept 队列里有连接在等你取，取了肯定有，不需要等
 *     - fd=6 可 read    → 内核接收缓冲区有数据在等你读，读了肯定有，不需要等
 *
 *   因此 accept() 和 read() 进内核就直接拿数据，不需要等待，瞬间返回。
 *
 * ───────────────────────────────────────────────────────────────────────
 * iterator.remove() 为什么必须手动调？
 * ───────────────────────────────────────────────────────────────────────
 *
 *   selectedKeys() 返回的是 Selector 内部的一个 HashSet。
 *   epoll_wait 返回就绪事件时，JDK 只会往这个 Set 里「加」，不会「删」。
 *
 *   如果不 remove：
 *     第1轮循环：select() 返回 [key6]，处理完 key6
 *     第2轮循环：select() 返回 [key7]，JDK 往 Set 里加 key7
 *     selectedKeys = {key6（旧的！）, key7}
 *     → 你以为 key6 又有事件了，对 fd=6 再次 read() → 没数据 → 返回0/-1 → 逻辑错误！
 *
 *   iterator.remove() 的本质：把刚处理完的 key 从 selectedKeys Set 里移除，
 *   保证下一轮只处理真正新发生的事件。
 */
public class Step2_EventLoop {

    // 协议常量
    static final String CRLF = "\r\n";

    /**
     * 事件循环核心方法，注释里逐行解释每个调用对应的底层行为
     */
    public static void runEventLoop(Selector selector,
                                    ServerSocketChannel serverChannel) throws IOException {
        log("══════════════════════════════════════════════");
        log("  Step2：事件循环开始");
        log("══════════════════════════════════════════════");
        log();

        while (true) {

            // ── epoll_wait：main线程在这里挂起 ────────────────────────────
            //
            // 底层：epoll_wait(epfd=5, events[], 1024, -1)
            //       -1 = 永远等，直到有就绪事件
            //
            // 线程视角：
            //   调用前：main线程 处于 RUNNABLE 状态（在 CPU 上运行）
            //   进入后：main线程 让出 CPU，进入 WAITING 状态
            //           → 0% CPU 占用
            //           → OS 把 CPU 分给其他线程/进程
            //   唤醒后：内核把 main线程 加回调度队列，等待 CPU 时间片
            //           → main线程回到 RUNNABLE 状态
            //   返回后：readyCount = 就绪事件数量
            //
            // 注意：selector.select() 和 selector.select(timeout) 的区别：
            //   select()         → epoll_wait(..., -1)      永远等
            //   select(1000L)    → epoll_wait(..., 1000ms)  最多等1秒
            //   selectNow()      → epoll_wait(..., 0)       立即返回
            log("[main线程] 准备调用 selector.select()，即将挂起...");
            int readyCount = selector.select();
            // ↑ main线程在这里真正挂起，让出CPU
            // ↑ 这行代码"卡住了"，直到有就绪事件
            log("[main线程] selector.select() 返回，就绪事件数 = " + readyCount);
            log("  底层：epoll_wait 返回，就绪链表有 " + readyCount + " 个fd");
            log();

            if (readyCount == 0) {
                // 极少发生（比如 select 被信号打断），直接跳过
                log("[main线程] readyCount=0，跳过本轮");
                continue;
            }

            // ── 获取就绪事件集合 ──────────────────────────────────────────
            // selectedKeys() 返回 JDK 内部维护的 Set<SelectionKey>
            // 这个 Set 的内容是 JDK 把 epoll_wait 返回的 events[] 数组转换来的
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            log("[main线程] 获取就绪 key 集合，共 " + selectedKeys.size() + " 个");
            log();

            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                // ★ 必须手动 remove！
                // 原因见类头注释「iterator.remove() 为什么必须手动调？」
                iterator.remove();

                if (!key.isValid()) {
                    // key 已被取消（比如对端关闭了连接）
                    log("[main线程] key 已失效，跳过");
                    continue;
                }

                if (key.isAcceptable()) {
                    // ── 处理新连接 ───────────────────────────────────────
                    // 此时 epoll 已确认：fd=3 的 Accept 队列有连接，accept() 不会阻塞
                    log("[main线程] 检测到 OP_ACCEPT 事件，处理新连接...");
                    handleAccept(selector, serverChannel);

                } else if (key.isReadable()) {
                    // ── 处理可读数据 ─────────────────────────────────────
                    // 此时 epoll 已确认：fd=xxx 的接收缓冲区有数据，read() 不会阻塞
                    log("[main线程] 检测到 OP_READ 事件，读取数据...");
                    handleRead(key);
                }
            }

            log("[main线程] 本轮事件处理完毕，回到循环顶部准备再次 select()...");
            log();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // handleAccept：处理新连接（对应 epoll 就绪的 OP_ACCEPT 事件）
    // ─────────────────────────────────────────────────────────────────────
    static void handleAccept(Selector selector,
                              ServerSocketChannel serverChannel) throws IOException {

        // 底层：accept(fd=3, &addr, &addrlen)
        // 内核：从 Accept 队列取出已完成三次握手的连接，分配新 fd（假设fd=6）
        //       创建新的 socket 结构体，填入客户端地址信息
        // 返回：立即（epoll 已确认 Accept 队列非空，不阻塞）
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) {
            // 极端情况：epoll 通知了但 accept 返回 null（并发竞争，实际极少）
            return;
        }

        log("  [main线程] accept() 完成");
        log("    底层：accept(fd=3) → 新连接 fd（假设fd=6）");
        log("    内核：从 Accept 队列取出连接，分配新 fd");
        log("    客户端地址：" + clientChannel.getRemoteAddress());

        // 底层：fcntl(fd=6, F_SETFL, O_NONBLOCK)
        // 意义：后续 read(fd=6) 在没有数据时立即返回，不会阻塞 main 线程
        // 必须设！否则如果 TCP 分包导致 read() 没读完，main 线程会阻塞等待
        clientChannel.configureBlocking(false);

        // 底层：epoll_ctl(epfd=5, EPOLL_CTL_ADD, fd=6, EPOLLIN)
        // 内核：
        //   ① 创建 fd=6 的 epitem（档案）
        //   ② 把 epitem 插入红黑树（O(log n)）
        //   ③ 在 fd=6 的 socket 等待队列挂 ep_poll_callback
        //      回调作用：fd=6 有数据到达时 → 加就绪链表 → 唤醒 main 线程
        clientChannel.register(selector, SelectionKey.OP_READ);

        log("  [main线程] 新连接注册进 epoll");
        log("    底层：epoll_ctl(ADD, fd=6, EPOLLIN)");
        log("    内核：fd=6 插入红黑树，挂 ep_poll_callback");
        log("    现在 epoll 红黑树里多了一个节点：fd=6");
        log();
    }

    // ─────────────────────────────────────────────────────────────────────
    // handleRead：处理可读数据（对应 epoll 就绪的 OP_READ 事件）
    // ─────────────────────────────────────────────────────────────────────
    static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();

        // 用户空间的 ByteBuffer（在堆外内存 or 堆内存，取决于 allocate/allocateDirect）
        // allocate(4096) → 在 JVM 堆内分配，read() 时内核会先写临时缓冲区再拷贝
        // allocateDirect(4096) → 在堆外（native memory）分配，read() 直接写入，少一次拷贝
        ByteBuffer buffer = ByteBuffer.allocate(4096);

        // ── 底层：read(fd=xxx, buf, 4096) ───────────────────────────────
        // 内核步骤：
        //   ① 用户态 → 内核态（系统调用陷入，CPU 从用户模式切内核模式）
        //   ② 找到 fd=xxx 的 socket 接收缓冲区（sk_rcvbuf）
        //   ③ 接收缓冲区 → buffer（memcpy，CPU 执行）
        //   ④ 内核态 → 用户态，返回读取字节数
        //
        // 为什么不阻塞：
        //   epoll_wait 已确认该 fd 接收缓冲区有数据，直接拷贝，无需等待
        //
        // 返回值含义：
        //   > 0  → 读到 n 字节
        //   = 0  → 对端正常关闭（发了 FIN）
        //   = -1 → 错误（fd 非阻塞时没数据会返回 -1 且 errno=EAGAIN，但 epoll 确认后不会出现此情况）
        int bytesRead = clientChannel.read(buffer);

        log("  [main线程] read() 完成");
        log("    底层：read(fd) → 内核接收缓冲区 → buffer（内存拷贝）");

        if (bytesRead == -1) {
            // 对端关闭连接（read 返回 -1 对应 Java NIO）
            log("    对端关闭连接，bytesRead=-1");
            log("    执行：epoll_ctl(DEL, fd) + close(fd)");

            // 底层：epoll_ctl(epfd, DEL, fd, NULL) → 从红黑树删除该节点，O(log n)
            key.cancel();

            // 底层：close(fd) → 释放 fd 和 socket 数据结构
            clientChannel.close();
            log("    fd 已从 epoll 红黑树删除并关闭");
            log();
            return;
        }

        if (bytesRead == 0) {
            log("    bytesRead=0，没有数据，跳过");
            log();
            return;
        }

        log("    读取了 " + bytesRead + " 字节，数据已在 buffer 里（用户空间）");

        // ── buffer 操作：flip() 切换到读模式 ────────────────────────────
        // Buffer 内部有三个指针：position、limit、capacity
        //   write 模式后：position=bytesRead, limit=capacity
        //   flip() 后：   position=0, limit=bytesRead  ← 准备从头读
        buffer.flip();

        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String request = new String(bytes);
        log("    请求内容（前50字节）：" +
            (request.length() > 50 ? request.substring(0, 50) + "..." : request).trim());

        // ── 业务处理 ─────────────────────────────────────────────────────
        String responseBody = processRequest(request);
        String httpResponse = "HTTP/1.1 200 OK" + CRLF
                + "Content-Type: application/json;charset=UTF-8" + CRLF
                + "Content-Length: " + responseBody.getBytes().length + CRLF
                + "Connection: close" + CRLF
                + CRLF
                + responseBody;

        // ── 底层：write(fd, buf, len) ─────────────────────────────────
        // 内核步骤：
        //   ① 用户态 → 内核态
        //   ② 把 httpResponse 的内容拷贝到 fd 的内核发送缓冲区（sk_sndbuf）
        //   ③ TCP 协议栈从发送缓冲区取数据，分 TCP 段发出
        //   ④ 网卡 DMA 从发送缓冲区取数据，写到网线
        //   ⑤ 返回（write 不等对方收到，只要放进发送缓冲区就算完成）
        //
        // 注意：如果发送缓冲区满了（网络拥塞），write() 会阻塞（阻塞模式）
        //        或返回 EAGAIN（非阻塞模式）→ 这时需要注册 OP_WRITE 等缓冲区空出来
        ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes());
        while (responseBuffer.hasRemaining()) {
            // 循环写，防止一次 write 没写完（内核发送缓冲区满时）
            clientChannel.write(responseBuffer);
        }

        log("  [main线程] write() 完成");
        log("    底层：write(fd) → buffer 写入内核发送缓冲区");
        log("    内核发送缓冲区 → TCP分段 → 网卡DMA → 网线（异步，不等对方收到）");
        log();

        // 响应完毕，关闭连接（HTTP/1.0 短连接模式）
        key.cancel();
        clientChannel.close();
        log("  [main线程] 连接关闭");
        log("    底层：epoll_ctl(DEL, fd) + close(fd)");
        log();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 业务处理：解析请求，返回响应 JSON
    // ─────────────────────────────────────────────────────────────────────
    static String processRequest(String request) {
        // 简单解析 "GET /order?id=xxx"
        if (request.contains("/order")) {
            String orderId = "123";
            // 从请求里提取 id 参数
            int idIdx = request.indexOf("id=");
            if (idIdx != -1) {
                int end = request.indexOf(' ', idIdx);
                if (end == -1) end = request.indexOf('\r', idIdx);
                if (end != -1) {
                    orderId = request.substring(idIdx + 3, end);
                }
            }
            return "{\"code\":0,\"orderId\":\"" + orderId + "\","
                    + "\"status\":\"已发货\",\"amount\":299.0}";
        }
        return "{\"code\":404,\"msg\":\"not found\"}";
    }

    public static void main(String[] args) throws IOException {
        // Step2 单独运行需要先 setup
        Step1_ServerSetup.setup();
        log("Step2 单独运行模式，进入事件循环（请用 OrderQueryServer 运行完整版）");
        runEventLoop(Step1_ServerSetup.selector, Step1_ServerSetup.serverChannel);
    }

    private static void log() {
        System.out.println();
    }

    private static void log(String msg) {
        System.out.println(msg);
    }
}

