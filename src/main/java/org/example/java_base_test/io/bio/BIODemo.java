package org.example.java_base_test.io.bio;

/**
 * BIO 演示入口说明
 *
 * 本包包含 3 个独立可运行的类，分别演示 BIO 创建子线程 vs 不创建子线程的区别：
 *
 *   BIOServer_NoThread  - 服务端【不创建子线程】
 *   BIOServer_WithThread - 服务端【创建子线程】
 *   BIOClient           - 客户端（两个服务端都可以用这个客户端测试）
 *
 * ── 演示一：不创建子线程 ────────────────────────────────────────────────
 *   1. 运行 BIOServer_NoThread.main()
 *   2. 开第一个终端运行 BIOClient.main()  → 正常收到 Echo 响应
 *   3. 再开第二个终端运行 BIOClient.main() → 卡住，没有任何输出
 *   4. 等第一个客户端断开，第二个才会被处理
 *
 * ── 演示二：创建子线程 ──────────────────────────────────────────────────
 *   1. 运行 BIOServer_WithThread.main()
 *   2. 同时开多个终端运行 BIOClient.main() → 每个都立刻收到响应，互不影响
 * ────────────────────────────────────────────────────────────────────────
 */
public class BIODemo {
    public static void main(String[] args) {
        System.out.println("请直接运行以下类的 main() 方法进行演示：");
        System.out.println("  BIOServer_NoThread  - 不创建子线程（端口 9090）");
        System.out.println("  BIOServer_WithThread - 创建子线程（端口 9090）");
        System.out.println("  BIOClient           - 客户端");
    }
}

