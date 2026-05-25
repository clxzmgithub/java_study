package org.example.java_base_test.io.nio.showcase.jdknio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * 订单查询服务 - 完整可运行版本（整合所有步骤）
 * ═══════════════════════════════════════════════════════════════════════
 *
 * 这是 Step0 ~ Step3 的整合，真正可以处理 HTTP 请求的 NIO 服务器。
 *
 * 启动后用浏览器或 curl 访问：
 *   curl "http://localhost:8080/order?id=123"
 *   curl "http://localhost:8080/health"
 *
 * ───────────────────────────────────────────────────────────────────────
 * 完整的线程视角 + epoll 对应关系总览
 * ───────────────────────────────────────────────────────────────────────
 *
 *  Java NIO 代码                  底层系统调用                  线程状态
 *  ─────────────────────────────────────────────────────────────────────
 *  ServerSocketChannel.open()  →  socket()                    RUNNABLE
 *  .bind(8080)                 →  bind() + listen()           RUNNABLE
 *  .configureBlocking(false)   →  fcntl(O_NONBLOCK)           RUNNABLE
 *  Selector.open()             →  epoll_create()              RUNNABLE
 *  .register(OP_ACCEPT)        →  epoll_ctl(ADD, fd=3)        RUNNABLE
 *  ─── 事件循环 ───────────────────────────────────────────────────────
 *  selector.select()           →  epoll_wait(...)             WAITING ★
 *                                 （挂起，0%CPU，等就绪）
 *  ─── 被内核唤醒后 ──────────────────────────────────────────────────
 *  selector.selectedKeys()     ←  epoll_wait返回events[]      RUNNABLE
 *  serverChannel.accept()      →  accept(fd=3)               RUNNABLE（不阻塞）
 *  .register(OP_READ)          →  epoll_ctl(ADD, fd=6)        RUNNABLE
 *  selector.select()           →  epoll_wait(...)             WAITING ★
 *                                 （再次挂起）
 *  ─── 客户端发数据，被再次唤醒 ──────────────────────────────────────
 *  channel.read(buffer)        →  read(fd=6, buf)            RUNNABLE（不阻塞）
 *  channel.write(buffer)       →  write(fd=6, buf)           RUNNABLE（不阻塞）
 *  key.cancel() + close()      →  epoll_ctl(DEL) + close()   RUNNABLE
 *  selector.select()           →  epoll_wait(...)             WAITING ★
 *
 * ───────────────────────────────────────────────────────────────────────
 * 为什么 accept()/read()/write() 不阻塞？
 * ───────────────────────────────────────────────────────────────────────
 *
 *   selector.select() 返回，意味着 epoll_wait 已经确认：
 *   - OP_ACCEPT 就绪 → fd=3 的 Accept 队列里有连接，accept() 直接取，不等
 *   - OP_READ   就绪 → fd 的内核接收缓冲区有数据，read() 直接拷贝，不等
 *   - OP_WRITE  就绪 → fd 的内核发送缓冲区有空间，write() 直接写，不等
 *
 *   这就是"epoll 预先确认就绪，后续操作不阻塞"的核心原理。
 *
 * ───────────────────────────────────────────────────────────────────────
 * 内核 epoll 数据结构演进（以连接两个客户端为例）
 * ───────────────────────────────────────────────────────────────────────
 *
 *   启动后：
 *     红黑树：[fd=3(监听)]
 *     就绪链表：[]
 *     等待队列：[main线程]  ← selector.select() 时加入
 *
 *   客户端A 连接后（accept + register）：
 *     红黑树：[fd=3(监听)] [fd=6(客户端A)]
 *     就绪链表：[]
 *     等待队列：[main线程]  ← 再次 select()
 *
 *   客户端B 也连接后：
 *     红黑树：[fd=3(监听)] [fd=6(客户端A)] [fd=7(客户端B)]
 *     就绪链表：[]
 *
 *   客户端A 和 客户端B 同时发来数据：
 *     红黑树：[fd=3] [fd=6] [fd=7]
 *     就绪链表：[fd=6的epitem] → [fd=7的epitem]  ← 两个都就绪！
 *     epoll_wait 返回 2，main线程一次性处理两个请求
 *
 *   处理完后（close + cancel）：
 *     红黑树：[fd=3(监听)]  ← 只剩监听fd
 */
public class OrderQueryServer {

    static final int    PORT     = 8080;
    static final String CRLF     = "\r\n";
    static final int    BUF_SIZE = 8192;

    // ─────────────────────────────────────────────────────────────────────
    // main：入口，启动服务器
    // ─────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {

        log("============================================================");
        log("  订单查询服务（JDK NIO + epoll 演示）");
        log("  监听端口：" + PORT);
        log("  测试命令：curl \"http://localhost:" + PORT + "/order?id=123\"");
        log("============================================================");
        log();

        // ══════════════════════════════════════════════════════════════════
        // 第一阶段：服务器启动（对应 Step1_ServerSetup）
        // 执行者：main 线程
        // 全部操作：立即返回，无阻塞
        // ══════════════════════════════════════════════════════════════════

        // ① 创建 TCP 监听 socket
        // 底层：socket(AF_INET, SOCK_STREAM, 0) → fd=3（具体值由内核决定）
        // 内核：分配 socket 结构体，状态 CLOSED
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        // ② 绑定端口 + 开始监听
        // 底层：bind(fd=3, {0.0.0.0, 8080})
        //       listen(fd=3, 128)   ← 128 = backlog：Accept 队列最大长度
        // 内核：fd=3 状态变为 LISTEN，建立 SYN 队列 + Accept 队列
        serverChannel.bind(new InetSocketAddress(PORT));

        // ③ 设为非阻塞
        // 底层：fcntl(fd=3, F_SETFL, O_NONBLOCK)
        // 效果：accept(fd=3) 在 Accept 队列为空时立即返回 EAGAIN，不挂起线程
        serverChannel.configureBlocking(false);

        // ④ 创建 Selector（= epoll 实例）
        // 底层：epoll_create(1)  → 返回 epfd（假设=5）
        // 内核：
        //   struct eventpoll {
        //       rb_root   rbr;      → 红黑树（初始为空）
        //       list_head rdllist;  → 就绪链表（初始为空）
        //       waitqueue wq;       → 等待队列（初始为空）
        //   }
        Selector selector = Selector.open();

        // ⑤ 把监听 fd 注册进 epoll
        // 底层：epoll_ctl(epfd=5, EPOLL_CTL_ADD, fd=3, {EPOLLIN, fd=3})
        // 内核：
        //   a. 创建 epitem（fd=3 的档案）
        //   b. 把 epitem 插入红黑树（O(log n)）
        //   c. 在 fd=3 的 socket 等待队列挂 ep_poll_callback 回调函数
        //      ↑ 这个回调就是「有新连接时把 fd=3 加入就绪链表并唤醒线程」的执行者
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        log("[main线程] 服务器启动完毕");
        log("  epoll 红黑树当前内容：[fd=3(监听socket)]");
        log("  就绪链表：（空）");
        log("  等待队列：（空，即将进入 selector.select() 挂起）");
        log();

        // ══════════════════════════════════════════════════════════════════
        // 第二阶段：事件循环（对应 Step2_EventLoop）
        // 执行者：main 线程
        // 核心：大部分时间在 epoll_wait 挂起，只有事件来了才处理
        // ══════════════════════════════════════════════════════════════════
        eventLoop(selector, serverChannel);
    }

    // ─────────────────────────────────────────────────────────────────────
    // eventLoop：事件循环主体
    //
    // 线程状态变化：
    //   RUNNABLE（刚进来）
    //     ↓ selector.select()
    //   WAITING（挂在 epoll_wait，0% CPU）
    //     ↓ 内核 ep_poll_callback 唤醒
    //   RUNNABLE（处理事件）
    //     ↓ 处理完毕，再次 select()
    //   WAITING（再次挂起）
    //     ...（循环往复）
    // ─────────────────────────────────────────────────────────────────────
    static void eventLoop(Selector selector,
                          ServerSocketChannel serverChannel) throws IOException {

        log("[main线程] 进入事件循环");
        log();

        while (true) {
            // ──────────────────────────────────────────────────────────────
            // selector.select()：main 线程在这里真正挂起
            //
            // 底层：epoll_wait(epfd=5, events[], 1024, -1)
            //
            // 内核执行流程：
            //   1. 检查就绪链表是否为空
            //      └─ 非空 → 直接返回（不挂起），把就绪fd填入events[]
            //      └─ 为空 → 执行步骤2
            //   2. 创建等待条目 {当前线程=main, 唤醒函数}
            //      加入 epoll 等待队列（wq）
            //   3. 调 schedule()，main 线程让出 CPU
            //      OS 把 main 线程状态改为 WAITING，从调度队列移走
            //      CPU 空出来给其他进程/线程用
            //   4. ...（某时刻）某个 fd 有事件：
            //      硬件中断 → 中断处理程序 → ep_poll_callback
            //      → fd 的 epitem 加入就绪链表
            //      → 从 wq 找到 main 线程，加回调度队列
            //   5. main 线程被 OS 重新调度，继续执行
            //      把就绪链表内容填入 events[]
            //      清空就绪链表（LT模式下下次继续通知）
            //      返回就绪数量 n
            //
            // 注：Linux 的 LT（Level Triggered，默认）vs ET（Edge Triggered）
            //   LT（电平触发）：只要缓冲区有数据，每次 epoll_wait 都通知你
            //                   更安全，不会漏读，适合大多数场景
            //   ET（边缘触发）：只在「状态变化」时通知一次（无→有）
            //                   需要循环读到 EAGAIN，性能稍好，但容易漏读
            //   JDK Selector 默认使用 LT 模式
            // ──────────────────────────────────────────────────────────────
            int readyCount = selector.select();  // ★ main 线程在这里挂起

            if (readyCount == 0) continue;

            // ── 获取就绪事件 ───────────────────────────────────────────────
            // JDK 把 epoll_wait 返回的 events[] 数组转成 Set<SelectionKey>
            // 注意：JDK 只往这个 Set 里「加」，不会「删」
            //       必须手动 iterator.remove() 否则下轮会重复处理旧事件
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();  // ★ 必须！移除已处理的 key

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    // ── OP_ACCEPT：处理新连接 ─────────────────────────────
                    // 此时 epoll 已确认 fd=3 的 Accept 队列非空
                    onAccept(selector, serverChannel);

                } else if (key.isReadable()) {
                    // ── OP_READ：处理可读数据 ─────────────────────────────
                    // 此时 epoll 已确认该 fd 的接收缓冲区有数据
                    onRead(key);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // onAccept：OP_ACCEPT 事件处理器
    //
    // 调用时机：selector.select() 返回，且某个 key 的 isAcceptable() = true
    // 此时保证：fd=3 的 Accept 队列里至少有一个已完成三次握手的连接
    // ─────────────────────────────────────────────────────────────────────
    static void onAccept(Selector selector,
                         ServerSocketChannel serverChannel) throws IOException {

        // 底层：accept(fd=3, &addr, &addrlen)
        // 内核：
        //   1. 从 Accept 队列取出第一个已完成握手的连接
        //   2. 分配新 fd（假设 fd=6），创建新 socket 结构体
        //   3. 填入客户端 IP:Port 信息
        //   4. 返回 fd=6
        //
        // ★ 为什么不阻塞：epoll 已确认 Accept 队列非空，accept() 直接取，不等
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) return;  // 极端情况（并发竞争）

        // 底层：fcntl(fd=6, F_SETFL, O_NONBLOCK)
        // ★ 必须！非阻塞模式才能保证 read() 在没数据时不挂起 main 线程
        clientChannel.configureBlocking(false);

        // 底层：epoll_ctl(epfd=5, EPOLL_CTL_ADD, fd=6, EPOLLIN)
        // 内核：
        //   a. 创建 fd=6 的 epitem
        //   b. 插入红黑树（O(log n)）
        //   c. fd=6 的 socket 等待队列挂 ep_poll_callback
        //      → 将来客户端A发数据时，内核自动把 fd=6 加就绪链表
        clientChannel.register(selector, SelectionKey.OP_READ);

        log("[main线程][ACCEPT] 新客户端连接");
        log("  来源：" + clientChannel.getRemoteAddress());
        log("  底层：accept(fd=3) → 新fd，epoll_ctl(ADD, 新fd)");
        log("  epoll红黑树：[fd=3] + [新fd]");
    }

    // ─────────────────────────────────────────────────────────────────────
    // onRead：OP_READ 事件处理器
    //
    // 调用时机：selector.select() 返回，且某个 key 的 isReadable() = true
    // 此时保证：该 fd 的内核接收缓冲区里有数据（DMA 已写好）
    // ─────────────────────────────────────────────────────────────────────
    static void onRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();

        // 用户空间 ByteBuffer（JVM 堆内）
        // 注：allocateDirect 是堆外内存，read() 时少一次内核→堆外→堆内的拷贝
        //     allocate     是堆内内存，read() 时内核→堆外临时缓冲→堆内 ByteBuffer
        //     生产环境 Netty 用 allocateDirect 减少拷贝次数
        ByteBuffer readBuf = ByteBuffer.allocate(BUF_SIZE);

        // 底层：read(fd, buf, BUF_SIZE)
        //
        // 内核执行路径（阶段②：数据搬运）：
        //   ① 用户态 → 内核态（系统调用陷入，CPU切内核模式，保存寄存器）
        //   ② 找到 fd 对应 socket 的接收缓冲区（sk_rcvbuf，在内核内存）
        //   ③ sk_rcvbuf → readBuf（memcpy，CPU 执行这次内存拷贝）
        //   ④ 内核态 → 用户态（恢复寄存器，返回读取字节数）
        //
        // ★ 为什么不阻塞：epoll_wait 已确认接收缓冲区有数据，read() 直接拷贝
        int bytesRead = clientChannel.read(readBuf);

        if (bytesRead == -1) {
            // 对端发了 FIN（正常关闭），read 返回 -1
            // 底层：epoll_ctl(DEL, fd) + close(fd)
            key.cancel();
            clientChannel.close();
            log("[main线程][CLOSE] 客户端断开，fd 从 epoll 红黑树删除");
            return;
        }

        if (bytesRead == 0) return;

        // ── 解析 HTTP 请求 ─────────────────────────────────────────────
        // buffer 现在处于写模式（position=bytesRead, limit=BUF_SIZE）
        // flip() 切换到读模式（position=0, limit=bytesRead）
        readBuf.flip();
        byte[] rawBytes = new byte[readBuf.limit()];
        readBuf.get(rawBytes);
        String rawRequest = new String(rawBytes);

        // 取第一行（请求行）：GET /order?id=123 HTTP/1.1
        String requestLine = rawRequest.split("\r\n")[0];
        log("[main线程][READ ] 收到请求：" + requestLine);
        log("  底层：read(fd) → 内核接收缓冲区 → readBuf（" + bytesRead + " 字节）");

        // ── 业务处理 ──────────────────────────────────────────────────
        String responseBody = handleBusiness(rawRequest);

        // ── 构造 HTTP 响应 ─────────────────────────────────────────────
        String httpResponse = "HTTP/1.1 200 OK" + CRLF
                + "Content-Type: application/json;charset=UTF-8" + CRLF
                + "Content-Length: " + responseBody.getBytes("UTF-8").length + CRLF
                + "Connection: close" + CRLF
                + CRLF
                + responseBody;

        // ── 写响应 ────────────────────────────────────────────────────
        // 底层：write(fd, buf, len)
        // 内核执行路径：
        //   ① 用户态 → 内核态
        //   ② buf → fd 的内核发送缓冲区（sk_sndbuf）（内存拷贝）
        //   ③ TCP 协议栈把发送缓冲区数据打包成 TCP 段
        //   ④ 网卡 DMA 从发送缓冲区取数据，写到网线
        //   ⑤ 内核态 → 用户态，返回（不等对方收到！）
        //
        // 注：如果发送缓冲区满了（网络拥塞），write() 在非阻塞模式下返回 EAGAIN
        //     此时应注册 OP_WRITE，等缓冲区空出来再写剩余数据
        //     本示例响应体小，一次写完，不需要注册 OP_WRITE
        ByteBuffer writeBuf = ByteBuffer.wrap(httpResponse.getBytes("UTF-8"));
        while (writeBuf.hasRemaining()) {
            clientChannel.write(writeBuf);
        }

        log("[main线程][WRITE] 响应已写入内核发送缓冲区");
        log("  响应体：" + responseBody);
        log("  底层：write(fd) → sk_sndbuf → TCP分段 → 网卡DMA → 网线");

        // ── 关闭连接（HTTP/1.0 短连接）────────────────────────────────
        // key.cancel() 底层：epoll_ctl(DEL, fd) → fd 从红黑树删除，O(log n)
        // clientChannel.close() 底层：close(fd) → 释放 fd 和 socket 结构
        key.cancel();
        clientChannel.close();
        log("[main线程][CLOSE] 连接关闭，fd 从 epoll 红黑树删除");
        log();
    }

    // ─────────────────────────────────────────────────────────────────────
    // handleBusiness：业务层，解析请求，返回响应 JSON
    //
    // 注意：这个方法是纯 CPU 计算（解析字符串），没有 IO。
    // 如果这里有耗时操作（查数据库），应扔给业务线程池，
    // 否则 main-IO 线程被卡住，其他连接的事件全部积压！
    // ─────────────────────────────────────────────────────────────────────
    static String handleBusiness(String rawRequest) {
        // 健康检查
        if (rawRequest.startsWith("GET /health")) {
            return "{\"status\":\"UP\",\"service\":\"order-query\"}";
        }

        // 订单查询
        if (rawRequest.contains("/order")) {
            String orderId = extractParam(rawRequest, "id");
            if (orderId == null) {
                return "{\"code\":400,\"msg\":\"缺少参数 id\"}";
            }
            // 模拟查询结果（实际场景这里会查数据库）
            return "{\"code\":0,\"orderId\":\"" + orderId + "\","
                    + "\"status\":\"已发货\","
                    + "\"amount\":299.0,"
                    + "\"createTime\":\"2026-05-25 10:00:00\"}";
        }

        return "{\"code\":404,\"msg\":\"接口不存在\"}";
    }

    // ─────────────────────────────────────────────────────────────────────
    // 工具方法：从 HTTP 请求里提取 query 参数值
    // ─────────────────────────────────────────────────────────────────────
    static String extractParam(String rawRequest, String paramName) {
        String search = paramName + "=";
        int idx = rawRequest.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        // 参数结束于空格（URL结束）、& (多参数)、\r（请求行结束）
        int end = rawRequest.length();
        for (int i = start; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            if (c == ' ' || c == '&' || c == '\r' || c == '\n') {
                end = i;
                break;
            }
        }
        return rawRequest.substring(start, end);
    }

    static void log(String msg) {
        System.out.println(msg);
    }

    static void log() {
        System.out.println();
    }
}

