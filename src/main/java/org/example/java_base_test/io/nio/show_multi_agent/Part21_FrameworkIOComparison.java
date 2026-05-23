package org.example.java_base_test.io.nio.show_multi_agent;

class Part21_FrameworkIOComparison {

    static void explain() {
        System.out.println("【第二十一部分：四大框架 IO 横向对比与选型总结】");
        System.out.println();
        System.out.println("═══ 🚗 生活场景类比：四种运输公司，理解四大框架 IO 选型 ═══");
        System.out.println();
        System.out.println("  把四个框架想象成四家「专业运输公司」，各自有不同的核心竞争力：");
        System.out.println();
        System.out.println("  🚕 Netty = 「豪华专车平台」（司机接单 + 行程管理 + 服务标准化）");
        System.out.println("    专注「乘客运输」（网络通信），不管货运（文件IO）");
        System.out.println("    特色：统一服务标准（Pipeline）、私家车（线程池）随叫随到");
        System.out.println("    适合：需要实时通信的场景——IM、游戏、RPC 框架");
        System.out.println();
        System.out.println("  📰 Kafka = 「报纸批量印发公司」（大批量、高频次、讲究顺序）");
        System.out.println("    专注「批量派送」（高吞吐消息），能同时服务百万订阅用户");
        System.out.println("    特色：顺序写磁盘+sendfile零拷贝，100万 QPS 不是梦");
        System.out.println("    适合：日志收集、流式数据管道、事件溯源");
        System.out.println();
        System.out.println("  📦 RocketMQ = 「精准速递公司」（既要快，还要保证每单必达）");
        System.out.println("    用 mmap 写消息（超快落盘）+ sendfile 消费（零拷贝）");
        System.out.println("    特色：事务消息、延迟消息、消费位点精确管理");
        System.out.println("    适合：电商订单、支付、需要强一致性的金融业务");
        System.out.println();
        System.out.println("  🏎️ Nginx = 「高速公路收费站」（同时服务海量过路车，自己不运货）");
        System.out.println("    用 epoll + sendfile，C 语言实现，几乎没有框架开销");
        System.out.println("    特色：1个 Worker 进程扛 1万+ 并发，静态文件发送极致快");
        System.out.println("    适合：反向代理、负载均衡、静态资源 CDN 源站");
        System.out.println();
        System.out.println("  选型口诀：");
        System.out.println("    需要「实时双向通信」  → Netty（你的 RPC/IM/游戏首选）");
        System.out.println("    需要「超高吞吐消息」  → Kafka（日志/流式管道）");
        System.out.println("    需要「业务消息可靠性」→ RocketMQ（电商/支付）");
        System.out.println("    需要「接入层/静态服务」→ Nginx（反向代理/CDN）");
        System.out.println();
        System.out.println("═══ 以上是四大框架的「运输公司」类比，技术对比见下表 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：四大框架 IO 技术栈横向对比
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 四大框架 IO 技术栈横向对比 ━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌───────────┬──────────┬───────────────┬─────────────┬────────────┐");
        System.out.println("  │ 框架      │ 网络IO   │ 文件IO        │ 零拷贝      │ 特殊技术   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ Netty     │ NIO+epoll│ N/A（专注网络）│ ByteBuf零拷 │ PooledBuf  │");
        System.out.println("  │           │ Reactor  │               │ CompositeBuf│ Pipeline   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ Kafka     │ 手写NIO  │ FileChannel   │ sendfile    │ PageCache  │");
        System.out.println("  │           │ Reactor  │ 顺序写        │ transferTo  │ 稀疏索引   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ RocketMQ  │ Netty    │ mmap          │ mmap写      │ CommitLog  │");
        System.out.println("  │           │          │ FileChannel   │ sendfile读  │ ConsumeQ   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ Nginx     │ epoll    │ sendfile      │ sendfile    │ 事件驱动   │");
        System.out.println("  │           │ (C语言)  │               │             │ 进程模型   │");
        System.out.println("  └───────────┴──────────┴───────────────┴─────────────┴────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：为什么各框架选择不同的 IO 方案（深度分析）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. 为什么各框架选择不同的 IO 方案 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        System.out.println("  【Netty：为什么用 Reactor 主从模式 + ByteBuf 池化】");
        System.out.println("  核心诉求：通用高性能网络框架，支持任意协议");
        System.out.println("  为什么 Reactor 主从？");
        System.out.println("    Boss 线程只 accept，不做任何耗时操作");
        System.out.println("    accept 是非常快的操作（O(1)），1个线程足够");
        System.out.println("    Worker 线程专注 IO+业务，数量可配置");
        System.out.println("  为什么 ByteBuf 池化？");
        System.out.println("    高并发场景：每秒可能处理 10万+ 请求");
        System.out.println("    每个请求需要分配/释放 ByteBuf（DirectBuffer）");
        System.out.println("    DirectBuffer 分配涉及系统调用，极其昂贵（~μs 级）");
        System.out.println("    池化后：从内存池取用，不涉及系统调用（~ns 级）");
        System.out.println("    池化 DirectBuffer = 减少 GC + 减少系统调用，双重收益");
        System.out.println();

        System.out.println("  【Kafka：为什么顺序写 + PageCache + sendfile 的组合】");
        System.out.println("  核心诉求：最高消息吞吐量，消息持久化");
        System.out.println("  为什么顺序写？");
        System.out.println("    持久化意味着要写磁盘，随机写 HDD：0.1 MB/s，顺序写：600 MB/s");
        System.out.println("    Kafka 把所有消息追加写到文件末尾，永远是顺序写");
        System.out.println("    即使是 SSD，顺序写也比随机写快 3~10 倍（闪存特性）");
        System.out.println("  为什么 PageCache 而不是 Java 内存？");
        System.out.println("    PageCache 是 OS 管理的，进程重启不会丢（OS 还在）");
        System.out.println("    Java 堆对象重启就没了，还要 GC");
        System.out.println("    PageCache 可以和 sendfile 配合做零拷贝，Java 堆不行");
        System.out.println("  为什么不用 mmap（RocketMQ 用了，Kafka 没用）？");
        System.out.println("    Kafka：写入是顺序追加，FileChannel 已经够快");
        System.out.println("           mmap 的优势是随机访问，顺序写用不到");
        System.out.println("    RocketMQ：CommitLog 写入后还要构建 ConsumeQueue（需随机访问）");
        System.out.println("              用 mmap 做随机写更高效");
        System.out.println();

        System.out.println("  【RocketMQ：为什么 mmap 写 + sendfile 读的混合方案】");
        System.out.println("  核心诉求：低延迟写入 + 事务消息 + 顺序消息");
        System.out.println("  为什么写用 mmap？");
        System.out.println("    CommitLog 写入：put() 到 mmap 内存 = 没有 write() 系统调用");
        System.out.println("    比 Kafka 的 FileChannel.write() 还要快（省一次系统调用）");
        System.out.println("    延迟更低：适合对延迟敏感的场景（金融、实时通知）");
        System.out.println("  为什么读用 sendfile？");
        System.out.println("    Consumer 消费：文件 → 网卡，经典零拷贝场景");
        System.out.println("    mmap 读也可以，但 sendfile 对大文件顺序读更有优势");
        System.out.println();

        System.out.println("  【Nginx：为什么用 C 而不是 Java + 多进程而不是多线程】");
        System.out.println("  为什么 C？");
        System.out.println("    Nginx 是 Web 服务器，对每字节的内存占用极敏感");
        System.out.println("    10万并发：Java JVM 光 GC overhead 就不能接受");
        System.out.println("    C 没有 GC，内存精确控制，单 Worker 进程只占几 MB");
        System.out.println("  为什么多进程而不是多线程（Java 是多线程）？");
        System.out.println("    C 的多线程编程极容易出 bug（race condition）");
        System.out.println("    多进程：进程间独立，一个 Worker 崩溃不影响其他 Worker");
        System.out.println("    Master 进程负责管理 Worker，Worker 崩溃自动重启");
        System.out.println("    Java 框架（Netty）用多线程是因为 JVM 线程更轻（没有进程级开销）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：IO 知识体系全景图（把所有 Part 串起来）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. IO 知识体系全景图（21 个 Part 串联）━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │                    Java IO 完整知识体系                      │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 基础层（操作系统原理）                                       │");
        System.out.println("  │   Part1  五种IO模型  Part2  BIO问题  Part3  BufferedStream    │");
        System.out.println("  │   用户态/内核态  Page Cache  DMA  系统调用                   │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ NIO 核心层                                                   │");
        System.out.println("  │   Part4  Buffer三指针  Part5  Channel体系                    │");
        System.out.println("  │   Part6  Selector+epoll  Part7  SelectionKey事件             │");
        System.out.println("  │   Part8  Reactor单线程NIO Server                            │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 本地文件 IO 层                                               │");
        System.out.println("  │   Part14 FileChannel  Part15 MappedByteBuffer(mmap)          │");
        System.out.println("  │   Part16 NIO.2 Path/Files/WatchService                      │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 零拷贝层                                                     │");
        System.out.println("  │   Part9  transferTo调用链  Part10 缓冲区满/拥塞              │");
        System.out.println("  │   Part11 小文件零拷贝反而慢  Part12 Kafka/Nginx案例          │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ Netty 层（工业级封装）                                       │");
        System.out.println("  │   Part17 架构(EventLoop/Pipeline/ByteBuf)                   │");
        System.out.println("  │   Part18 粘包拆包(FrameDecoder/自定义协议)                  │");
        System.out.println("  │   Part19 实战(Echo/HTTP/Future/连接池)                      │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 框架应用层                                                   │");
        System.out.println("  │   Part20 Kafka完整IO体系（顺序写/PageCache/sendfile）       │");
        System.out.println("  │   Part21 四大框架IO横向对比（选型指南）                     │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：面试高频问题 + 标准答案
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. 面试高频问题 & 标准答案 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Q1：BIO 和 NIO 的区别？");
        System.out.println("  A：BIO 是阻塞 IO，每个连接需要一个线程；");
        System.out.println("     NIO 是非阻塞 IO，一个线程（Selector）可以管理多个连接。");
        System.out.println("     BIO 瓶颈：线程数（内存、上下文切换）；");
        System.out.println("     NIO 瓶颈：CPU 处理能力（无线程浪费，但业务逻辑要快）。");
        System.out.println();
        System.out.println("  Q2：Kafka 为什么快？");
        System.out.println("  A：四个核心原因：");
        System.out.println("     ①顺序写（磁盘寻道时间为0）；");
        System.out.println("     ②写 Page Cache（不直接写磁盘，内存速度）；");
        System.out.println("     ③sendfile 零拷贝（读时 0次CPU拷贝）；");
        System.out.println("     ④批量+压缩（减少网络 IO 次数和带宽）。");
        System.out.println();
        System.out.println("  Q3：Netty 的 ByteBuf 和 JDK ByteBuffer 有什么区别？");
        System.out.println("  A：ByteBuffer 是单指针（position），读写需要 flip/clear 切换；");
        System.out.println("     ByteBuf 是双指针（readerIndex/writerIndex），读写独立，无需 flip；");
        System.out.println("     ByteBuf 支持池化（PooledByteBufAllocator），减少 GC；");
        System.out.println("     ByteBuf 支持复合（CompositeByteBuf），零拷贝合并 buf；");
        System.out.println("     ByteBuf 有引用计数，用完必须 release()，否则内存泄漏。");
        System.out.println();
        System.out.println("  Q4：什么是零拷贝？transferTo() 怎么实现零拷贝？");
        System.out.println("  A：零拷贝 = 减少 CPU 参与数据搬运（不是真的0次拷贝）。");
        System.out.println("     传统读文件到 Socket：4次拷贝（DMA+CPU拷贝各2次）。");
        System.out.println("     transferTo 底层调用 sendfile()：只有2次DMA拷贝，0次CPU拷贝。");
        System.out.println("     文件数据路径：磁盘→PageCache（DMA）→网卡（DMA，Linux2.4+）。");
        System.out.println("     Kafka 的 FileRecords.writeTo() 就是用 transferTo() 实现的。");
        System.out.println();
        System.out.println("  Q5：Netty 的 EventLoop 为什么不能做阻塞操作？");
        System.out.println("  A：一个 EventLoop 绑定多个 Channel，一个线程管理多个连接。");
        System.out.println("     如果 EventLoop 线程阻塞（等数据库/HTTP），");
        System.out.println("     所有绑定的 Channel 都无法处理读写事件。");
        System.out.println("     解决：把阻塞操作提交到独立的业务线程池执行。");
        System.out.println();
        System.out.println("  Q6：TCP 粘包是什么？Netty 怎么解决？");
        System.out.println("  A：TCP 是字节流协议，多条消息可能在一次 read() 里全收到（粘包）");
        System.out.println("     或一条消息需要多次 read() 才能收完（拆包）。");
        System.out.println("     Netty 解决方案：");
        System.out.println("     ①固定长度：FixedLengthFrameDecoder");
        System.out.println("     ②分隔符：DelimiterBasedFrameDecoder");
        System.out.println("     ③长度字段（最常用）：LengthFieldBasedFrameDecoder");
        System.out.println("     ④自定义协议头：ReplayingDecoder");
        System.out.println();
        System.out.println("  Q7：mmap 内存映射适合什么场景？不适合什么场景？");
        System.out.println("  A：适合：大文件随机读（如 ES/Lucene 索引）、高频写（RocketMQ）");
        System.out.println("     不适合：小文件（mmap() 系统调用开销 > 节省的拷贝开销）");
        System.out.println("             需要精确管理内存的场景（mmap 无法主动 GC）");
        System.out.println("             Windows 上需要删除/重命名映射文件（句柄被占用）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：推荐学习路线（更新版）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. 完整学习路线（结合本文件 21 个 Part）━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  第1阶段：基础（Part1-3）");
        System.out.println("    目标：理解 IO 的本质，知道为什么需要 NIO");
        System.out.println("    重点：五种IO模型 + BIO的线程开销 + syscall减少原理");
        System.out.println("    实践：写一个 BIO 聊天室，亲身感受它的问题");
        System.out.println();
        System.out.println("  第2阶段：NIO 核心（Part4-8, Part14-16）");
        System.out.println("    目标：能写出正确的 NIO 代码");
        System.out.println("    重点：Buffer三指针 + Selector + FileChannel + NIO.2 Files");
        System.out.println("    实践：把 BIO 聊天室改成 NIO 版本，感受差距");
        System.out.println();
        System.out.println("  第3阶段：零拷贝（Part9-12, Part15）");
        System.out.println("    目标：理解文件传输的 IO 链路");
        System.out.println("    重点：sendfile vs mmap + Page Cache + 什么时候用零拷贝");
        System.out.println("    实践：写一个文件服务器，对比 FileChannel vs transferTo 性能");
        System.out.println();
        System.out.println("  第4阶段：Netty（Part17-19）");
        System.out.println("    目标：能用 Netty 写生产级代码");
        System.out.println("    重点：EventLoop + Pipeline + ByteBuf + 粘包拆包");
        System.out.println("    实践：写一个 Netty Echo Server，然后加 LengthField 拆包");
        System.out.println("          再实现一个简单的 HTTP Server");
        System.out.println();
        System.out.println("  第5阶段：框架源码（Part20-21）");
        System.out.println("    目标：看懂开源框架的 IO 设计思路");
        System.out.println("    重点：Kafka IO 链路 + RocketMQ mmap + 四框架横向对比");
        System.out.println("    实践：读 Kafka FileRecords.writeTo() 源码");
        System.out.println("          读 RocketMQ MappedFile.appendMessage() 源码");
        System.out.println("          读 Netty NioEventLoop.processSelectedKeys() 源码");
        System.out.println();
        System.out.println("  通关标志：");
        System.out.println("    能清晰回答：「Kafka 为什么快」（要讲到 Page Cache 和 sendfile 级别）");
        System.out.println("    能清晰回答：「Netty 如何处理10万并发」（要讲到 EventLoop 无锁化）");
        System.out.println("    能手写出：正确的 NIO Echo Server（Buffer 不出错，事件不漏处理）");
        System.out.println();
        NIODemo.printSeparator();
    }
}

