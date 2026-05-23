package org.example.java_base_test.io.nio.show_multi_agent;

class Part2_BIOProblem {

    static void explain() {
        System.out.println("【第二部分：BIO 的根本问题——一连接一线程】");
        System.out.println();
        System.out.println("═══ 🏦 生活场景：想象一家银行网点 ═══");
        System.out.println();
        System.out.println("  BIO 的工作方式 = 老式银行的「一对一服务」：");
        System.out.println();
        System.out.println("  【BIO 模式】");
        System.out.println("  进来1个客户 → 柜台叫一个服务员专门陪着他");
        System.out.println("  进来10个客户 → 派10个服务员，每人盯着一个");
        System.out.println("  进来1000个客户 → 需要1000个服务员！！！");
        System.out.println();
        System.out.println("  问题1【内存不够】：");
        System.out.println("    每个服务员（线程）要占一间休息室（内存 ~1MB 栈）");
        System.out.println("    1000个服务员 = 需要1000间休息室（1GB内存）→ 撑不住");
        System.out.println();
        System.out.println("  问题2【大部分时间在摸鱼】：");
        System.out.println("    99%的时间，服务员坐在那等客户开口（阻塞在 read()）");
        System.out.println("    真正在处理业务的时间 < 1%");
        System.out.println("    1000个服务员，990个在睡觉，10个在干活");
        System.out.println("    还要频繁切换谁来工作（上下文切换开销巨大）");
        System.out.println();
        System.out.println("  【NIO 模式（对比）】");
        System.out.println("  一个「大堂经理」(Selector 线程) 管理所有客户取号");
        System.out.println("  谁的号到了（数据就绪）就叫谁，后台只需少量处理人员");
        System.out.println("  10个人轻松处理1000个客户！");
        System.out.println();
        System.out.println("═══ 以下是技术代码分析 ═══");
        System.out.println();
        System.out.println("BIO 服务器代码模式（伪代码）：");
        System.out.println();
        System.out.println("  ServerSocket server = new ServerSocket(8080);");
        System.out.println("  while (true) {");
        System.out.println("      Socket socket = server.accept(); // ← 阻塞1：等新连接");
        System.out.println("      // 必须新建线程！否则 accept() 永远不会被再次调用");
        System.out.println("      new Thread(() -> {");
        System.out.println("          InputStream in = socket.getInputStream();");
        System.out.println("          byte[] buf = new byte[1024];");
        System.out.println("          int len;");
        System.out.println("          while ((len = in.read(buf)) != -1) { // ← 阻塞2：等数据");
        System.out.println("              process(buf, len);");
        System.out.println("          }");
        System.out.println("      }).start();");
        System.out.println("  }");
        System.out.println();
        System.out.println("问题分析：");
        System.out.println("  1000个并发连接 → 1000个线程");
        System.out.println("  每个线程默认栈：512KB ~ 1MB");
        System.out.println("  1000个线程内存：500MB ~ 1GB（光内存就撑不住）");
        System.out.println();
        System.out.println("  更大的问题（CPU 利用率）：");
        System.out.println("    每个线程 99% 时间在 in.read() 处阻塞");
        System.out.println("    CPU 真正干活时间 < 1%");
        System.out.println("    1000个线程上下文切换开销 比 干活 还大");
        System.out.println();
        System.out.println("  实际案例：早期 Tomcat（BIO 模式）");
        System.out.println("    默认 maxThreads = 200");
        System.out.println("    超过 200 个并发请求 → 排队等待 → 高并发直接崩");
        System.out.println();
        System.out.println("  解决方案：IO 多路复用（NIO Selector）");
        System.out.println("    NIO：1个 Selector 线程 + 少量 Worker 线程");
        System.out.println("    → 轻松处理 10 万并发连接");
        System.out.println("    → 这是 Netty / 现代 Tomcat（NIO 模式）的基础");
        System.out.println();
        NIODemo.printSeparator();
    }
}

