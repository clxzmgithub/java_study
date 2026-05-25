package org.example.java_base_test.io.bio.showcase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO 演示 - 服务端【创建子线程】
 *
 * ── 现象说明 ─────────────────────────────────────────────────────────────
 * accept() 接到客户端后，主线程立刻 new Thread() 把该连接的 IO 处理甩到子线程，
 * 主线程不阻塞，马上回到 accept() 等待下一个连接。
 * 多个客户端可以同时连接，每个都分配一个独立的 Worker 线程，互不影响。
 *
 * ── 演示步骤 ─────────────────────────────────────────────────────────────
 * 1. 先运行本类 main()，启动服务端
 * 2. 同时开多个终端运行 BIOClient.main()
 * 3. 观察：每个客户端都立刻收到 Echo 响应，互不干扰
 * ─────────────────────────────────────────────────────────────────────────
 */
public class BIOServer_WithThread {

    static final int PORT = 9090;

    public static void main(String[] args) throws IOException {
        System.out.println("=== BIOServer_WithThread 启动，监听端口 " + PORT + " ===");
        System.out.println("【模式】每个连接 new Thread() 处理，主线程持续 accept()");
        System.out.println("【预期】多个客户端同时连接，每个都能立刻收到响应");
        System.out.println();

        ServerSocket serverSocket = new ServerSocket(PORT);
        int connId = 0;

        while (true) {
            System.out.println("[Server] ★ 调用 accept()，等待客户端连接...");

            // ① 阻塞，直到有客户端发起连接
            Socket clientSocket = serverSocket.accept();
            connId++;
            int id = connId;
            System.out.println("[Server] 接到连接 #" + id + "，客户端端口：" + clientSocket.getPort());

            // ② 立刻 new Thread() 处理这个连接，主线程不做任何 IO
            new Thread(() -> handleClient(clientSocket, id), "Worker-" + id).start();

            System.out.println("[Server] ★ 已为连接 #" + id + " 新建线程，主线程立刻回到 accept()");
            System.out.println();
            // ③ 主线程马上进入下一次循环，可以立刻接受第二个、第三个连接
        }
    }

    /**
     * 子线程处理单个客户端的收发，read() 阻塞只影响本线程，不影响主线程的 accept()
     */
    static void handleClient(Socket socket, int id) {
        System.out.println("    [Worker-" + id + "] 开始处理连接 #" + id
                + "（线程：" + Thread.currentThread().getName() + "）");
        try {
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("    [Worker-" + id + "] 收到：" + line);
                out.println("Echo[Worker-" + id + "]: " + line);
            }

            System.out.println("    [Worker-" + id + "] 客户端断开，线程退出");
        } catch (IOException e) {
            System.out.println("    [Worker-" + id + "] 异常：" + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}

