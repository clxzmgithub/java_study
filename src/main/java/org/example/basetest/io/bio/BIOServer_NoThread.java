package org.example.basetest.io.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO 演示 - 服务端【不创建子线程】
 *
 * ── TCP 连接建立分两个阶段，理解这点是关键 ──────────────────────────────
 *
 * 【阶段一】OS 内核层 —— 与 Java 代码完全无关
 *   客户端 connect() 触发 TCP 三次握手：SYN → SYN-ACK → ACK
 *   这个过程由操作系统内核完成，Java 进程甚至可以不运行，握手也能完成。
 *   握手完成后，连接被放入 ServerSocket 的 backlog 队列等待 accept() 取出。
 *   → 所以客户端2的 connect() 几乎总是立刻成功，这是正常的！
 *
 * 【阶段二】Java 应用层 —— accept() 负责从队列里取连接
 *   只有调用 accept()，才能从 backlog 队列取出连接并拿到 Socket 对象，
 *   才能进行 read/write 数据交换。
 *   → 如果 accept() 没有被调用，连接就一直排在队列里没人处理！
 *
 * ── 为什么 backlog=1 也无法让客户端2 connect() 失败 ─────────────────────
 *   理论上 backlog 表示队列长度，队列满了新的 SYN 会被丢弃，connect() 超时。
 *   但 macOS (BSD 内核) 会自动将 backlog 放大（通常 ×1.5，最小 8），
 *   设成 1 实际生效的可能是 8，所以几个客户端的 connect() 都能成功。
 *   → 在演示中无需关注 backlog，connect() 成功是预期行为。
 *
 * ── 真正的 BIO 问题：连进来了，但没人处理 ───────────────────────────────
 *
 *   服务端主线程：accept() → 拿到客户端1 → readLine() 阻塞 ← 卡死在这里
 *                                                    ↑
 *                                          accept() 不再被调用！
 *
 *   客户端2：connect() 成功（OS层握手完成，排进 backlog 队列）
 *            发送消息 → 数据到达服务端内核缓冲区 ✅
 *            readLine() 等待 Echo → 永远阻塞 ❌
 *                ↑ 因为服务端没有线程在处理客户端2，不会有 Echo 写回来
 *
 * ── 演示步骤 ─────────────────────────────────────────────────────────────
 * 1. 运行本类 main()
 * 2. 终端1 运行 BIOClient → 输入消息，收到 Echo，保持不退出
 * 3. 终端2 运行 BIOClient → connect() 成功，输入消息后卡住，收不到 Echo
 * 4. 终端1 输入 quit 断开 → 服务端回到 accept()，终端2 立刻收到 Echo
 * ─────────────────────────────────────────────────────────────────────────
 */
public class BIOServer_NoThread {

    static final int PORT = 9090;

    public static void main(String[] args) throws IOException {
        System.out.println("=== BIOServer_NoThread 启动，监听端口 " + PORT + " ===");
        System.out.println("【模式】主线程直接处理 IO，不创建子线程");
        System.out.println("【预期】客户端2 connect() 成功，但发消息后永远收不到响应");
        System.out.println("        因为服务端主线程卡在客户端1的 readLine()，没人处理客户端2");
        System.out.println();

        ServerSocket serverSocket = new ServerSocket(PORT);

        while (true) {
            System.out.println("[Server] ★ 调用 accept()，等待客户端连接...");

            // ① 阻塞，直到有客户端发起连接
            Socket clientSocket = serverSocket.accept();
            System.out.println("[Server] 接到连接，客户端端口：" + clientSocket.getPort());
            System.out.println("[Server] ★★ 主线程进入 readLine() 阻塞");
            System.out.println("[Server]    accept() 不再被调用！");
            System.out.println("[Server]    客户端2 虽然能 connect()，但发消息后永远收不到 Echo！");
            System.out.println();

            BufferedReader in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter    out = new PrintWriter(clientSocket.getOutputStream(), true);

            // ② 主线程阻塞在 readLine()
            //    只要当前客户端不断开，就一直卡在这里
            //    此期间任何新来的客户端：connect() 成功，但 accept() 不会被调用
            //    新客户端发消息后，自己的 readLine() 永远阻塞（没有 Echo）
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Server] 收到消息：" + line);
                out.println("Echo: " + line);
            }

            // ③ 只有当前客户端断开（readLine 返回 null），才回到 accept()
            System.out.println("[Server] 客户端断开，回到 accept()，下一个客户端现在才能被处理");
            System.out.println();
            clientSocket.close();
        }
    }
}

