package org.example.java_base_test.io.nio.show_multi_agent;

class Part10_BufferFullAndCongestion {

    static void explain() {
        System.out.println("【第十部分：内核缓冲区满 & 网络拥塞】");
        System.out.println();
        System.out.println("═══ 🚗 生活场景：高速公路收费站 理解缓冲区满和网络拥塞 ═══");
        System.out.println();
        System.out.println("  把数据传输想象成高速公路运输：");
        System.out.println("  你的程序 = 货主，数据 = 货物，TCP连接 = 高速公路");
        System.out.println();
        System.out.println("  【发送缓冲区 = 高速公路入口收费站的停车区】");
        System.out.println("  你（程序）把货（数据）送到收费站停车区（发送缓冲区 128KB）");
        System.out.println("  货车（TCP）陆续从停车区出发，按限速（接收窗口）行驶");
        System.out.println("  停车区满了 → 你的货车进不来 → transferTo 传不完！");
        System.out.println();
        System.out.println("  【接收窗口 rwnd = 对端的接收停车区容量】");
        System.out.println("  对端也有个停车区（接收缓冲区），它满了就通知你：");
        System.out.println("  「我这边停满了，你先别发了」（TCP ACK 携带 rwnd=0）");
        System.out.println("  就像目的地仓库满了，入口立了个牌子「暂停接收」");
        System.out.println();
        System.out.println("  【拥塞窗口 cwnd = 高速公路限速标志】");
        System.out.println("  即使你和对端都有空位，高速公路本身也可能堵了");
        System.out.println("  TCP 自动降速（慢启动/拥塞避免），就像雾天限速60km/h");
        System.out.println("  有事故（丢包） → 限速更严格（cwnd 砍半）");
        System.out.println();
        System.out.println("  【为什么一定要写 while 循环？】");
        System.out.println("  你要运1000吨货，但每次最多只能过去30吨（缓冲区/窗口限制）");
        System.out.println("  不写循环 = 只运了第一批30吨，剩下970吨直接消失，还不报错！");
        System.out.println("  写了循环 = 分批运，直到1000吨全部到对端");
        System.out.println();
        System.out.println("═══ 以下是技术细节 ═══");
        System.out.println();

        System.out.println("一、内核 Socket 发送缓冲区（SO_SNDBUF）");
        System.out.println();
        System.out.println("  结构：");
        System.out.println("    你的进程              内核                    网络");
        System.out.println("    ──────────            ──────────────────      ──────");
        System.out.println("    transferTo() ──写──► Socket 发送缓冲区 ──发──► 对端");
        System.out.println("                          (默认 ~128KB)");
        System.out.println();
        System.out.println("  缓冲区为什么会满？");
        System.out.println("    t=0  transferTo 尝试发送 1GB");
        System.out.println("    t=1  内核把前 128KB 放入发送缓冲区      → 缓冲区满");
        System.out.println("    t=2  TCP 开始向对端发包");
        System.out.println("    t=3  对端接收窗口（rwnd）只有 64KB     → 对端也快满");
        System.out.println("    t=4  TCP 拥塞控制算法限制发送速率       → 缓冲区积压");
        System.out.println("    t=5  transferTo 返回 64KB              → 只传了一部分！");
        System.out.println("    t=6  while 循环继续下一次 transferTo");
        System.out.println();

        System.out.println("二、TCP 流控两层机制（都会导致 transferTo 传不完）");
        System.out.println();
        System.out.println("  ① 接收窗口（rwnd）—— 接收方说：我只能收这么多");
        System.out.println("    发送方               接收方");
        System.out.println("    [发送缓冲区 128KB]   [接收缓冲区快满了，rwnd=20KB]");
        System.out.println("    ←── ACK 报文携带 rwnd=20KB ────────────");
        System.out.println("    → 只能再发 20KB！（哪怕发送缓冲区有 128KB 空间）");
        System.out.println("    极端情况：rwnd=0，发送方必须停下等零窗口探测");
        System.out.println();
        System.out.println("  ② 拥塞窗口（cwnd）—— 内核说：网络可能堵了，我自己限速");
        System.out.println("    慢启动：cwnd 从 1 MSS 指数增长 → 达到 ssthresh");
        System.out.println("    拥塞避免：cwnd 线性增长");
        System.out.println("    检测到丢包（超时/3个重复ACK）→ cwnd 砍半，重新慢启动");
        System.out.println();
        System.out.println("  本次能发出多少 = min(rwnd, cwnd, 发送缓冲区剩余空间)");
        System.out.println();

        System.out.println("三、阻塞 vs 非阻塞模式下 transferTo 的行为差异");
        System.out.println();
        System.out.println("  阻塞模式（默认）：");
        System.out.println("    发送缓冲区满 → transferTo 阻塞等待");
        System.out.println("    缓冲区有空间 → 继续传");
        System.out.println("    通常一次调用就能传完（但可能等很久）");
        System.out.println();
        System.out.println("  非阻塞模式（NIO Selector 模式）：");
        System.out.println("    发送缓冲区满 → transferTo 立刻返回（可能 0）");
        System.out.println("    必须注册 OP_WRITE，等内核通知「缓冲区有空间了」再继续");
        System.out.println();

        System.out.println("四、文件到文件 vs 文件到 Socket");
        System.out.println();
        System.out.println("  文件到文件（你的 Demo）：");
        System.out.println("    目标是本地磁盘，没有 TCP 协议栈，没有接收窗口限制");
        System.out.println("    底层 copy_file_range 通常一次就能传完");
        System.out.println("    while 循环几乎只跑一次");
        System.out.println();
        System.out.println("  文件到 Socket（生产场景：Kafka/Nginx）：");
        System.out.println("    经过完整 TCP 协议栈，有缓冲区、接收窗口、拥塞控制");
        System.out.println("    while 循环可能跑很多次");
        System.out.println("    不写 while → 大概率数据不完整（静默丢失！）");
        System.out.println();

        System.out.println("  while 循环模板：");
        System.out.println("    long transferred = 0;");
        System.out.println("    while (transferred < fileSize) {");
        System.out.println("        long n = srcChannel.transferTo(");
        System.out.println("            transferred,            // 从哪里开始");
        System.out.println("            fileSize - transferred, // 还剩多少");
        System.out.println("            dstChannel              // 发到哪里");
        System.out.println("        );");
        System.out.println("        // n = 本次实际传了多少（可能远小于 fileSize）");
        System.out.println("        transferred += n;");
        System.out.println("    }");
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
    }
}

