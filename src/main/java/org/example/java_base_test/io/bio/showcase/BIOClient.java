package org.example.java_base_test.io.bio.showcase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * BIO 演示 - 客户端
 *
 * 连接服务端后，循环等待用户从键盘输入消息并发送，打印服务端的 Echo 回复。
 * 输入 quit 或直接回车（空行）断开连接退出。
 *
 * ── 演示步骤 ─────────────────────────────────────────────────────────────
 * 演示一（不创建子线程 BIOServer_NoThread）：
 *   1. 运行 BIOServer_NoThread.main()
 *   2. 开第一个终端运行本类 → 输入消息，正常收到 Echo，保持不退出
 *   3. 开第二个终端运行本类 → connect() 成功，但输入消息后卡住，
 *                              readLine() 永远等不到 Echo（服务端在处理客户端1）
 *   4. 回到第一个终端输入 quit → 服务端主线程回到 accept()，
 *                                  第二个客户端立刻收到 Echo
 *
 * 演示二（创建子线程 BIOServer_WithThread）：
 *   1. 运行 BIOServer_WithThread.main()
 *   2. 同时开多个终端运行本类 → 每个都立刻收到 Echo，互不影响
 * ─────────────────────────────────────────────────────────────────────────
 */
public class BIOClient {

    static final String HOST = "localhost";
    static final int    PORT = 9090;

    public static void main(String[] args) throws Exception {
        System.out.println("=== BIOClient 启动，连接 " + HOST + ":" + PORT + " ===");

        // ① 建立连接
        //    TCP 三次握手由 OS 内核完成，connect() 几乎总是立刻成功
        //    即使服务端没有调用 accept()，connect() 也能成功（OS 帮忙排队）
        //    BIO 的问题不在"连不进来"，而在"连进来了但没人处理"
        Socket socket = new Socket(HOST, PORT);
        System.out.println("[Client] connect() 成功，本地端口：" + socket.getLocalPort());
        System.out.println("[Client] 输入消息后按回车发送，输入 quit 或空行退出");
        System.out.println();

        PrintWriter    out      = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in       = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

        // ② 循环等待用户键盘输入，输入什么就发什么
        //    ★ BIOServer_NoThread 演示时：
        //      服务端主线程若正在处理另一个客户端，accept() 不会被调用
        //      本客户端 connect() 成功，但发消息后 readLine() 永远阻塞
        //      因为服务端根本没在处理本连接，不会写任何 Echo 回来
        while (true) {
            System.out.print("[Client] 请输入消息（quit 退出）: ");
            String input = keyboard.readLine();

            // 空行或 quit → 断开
            if (input == null || input.trim().isEmpty() || input.trim().equalsIgnoreCase("quit")) {
                break;
            }

            // 发送给服务端
            out.println(input);

            // 等服务端 Echo 回来
            // ★ BIOServer_NoThread + 客户端2：卡在这里，永远收不到回复
            System.out.println("[Client] 等待服务端 Echo...");
            String reply = in.readLine();
            System.out.println("[Client] 收到：" + reply);
            System.out.println();
        }

        System.out.println("[Client] 关闭连接");
        socket.close();
    }
}

