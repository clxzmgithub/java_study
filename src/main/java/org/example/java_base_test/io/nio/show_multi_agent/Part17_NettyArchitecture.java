package org.example.java_base_test.io.nio.show_multi_agent;

class Part17_NettyArchitecture {

    static void explain() {
        System.out.println("【第十七部分：Netty 核心架构深度解析】");
        System.out.println();
        System.out.println("═══ 🚚 生活场景：自建快递 vs 找顺丰 理解为什么用Netty ═══");
        System.out.println();
        System.out.println("  原生 NIO 就像「自己组建快递公司」：");
        System.out.println("  买车（Selector）、招司机（Thread）、建仓库（Buffer）...");
        System.out.println("  理论上能做，但你要搞定的事情超多：");
        System.out.println("  → 包裹破了怎么办？（粘包拆包）");
        System.out.println("  → 车坏了怎么办？（epoll 空轮询 Bug）");
        System.out.println("  → 客户一直不取件怎么处理？（心跳/空闲检测）");
        System.out.println("  → 包裹格式不对怎么办？（编解码）");
        System.out.println("  这些全要你自己搞，一不小心就出 Bug");
        System.out.println();
        System.out.println("  Netty 就像「直接用顺丰」：");
        System.out.println("  顺丰帮你处理好了所有烦恼，你只管告诉顺丰「寄什么、发哪里」");
        System.out.println("  专注业务逻辑，底层细节 Netty 全包");
        System.out.println();
        System.out.println("  【Netty 核心组件类比快递公司】");
        System.out.println("  EventLoopGroup（Boss）= 快递接单中心（只负责接新订单）");
        System.out.println("  EventLoopGroup（Worker）= 配送员团队（处理实际收发件）");
        System.out.println("  Channel = 每一个快递包裹（每个网络连接）");
        System.out.println("  Pipeline = 包裹处理流水线（分拣→称重→打包→发货）");
        System.out.println("  ChannelHandler = 流水线上的每个工序（你写的业务逻辑）");
        System.out.println("  ByteBuf = 改良版货箱（比ByteBuffer好用，不需要flip）");
        System.out.println();
        System.out.println("  【为什么 Netty 比原生 NIO 好？】");
        System.out.println("  就像「为什么用顺丰比自建快递好」：");
        System.out.println("  - 成熟可靠，Bug 已经被数百万用户验证过了");
        System.out.println("  - 功能完整，HTTP/WebSocket/自定义协议都支持");
        System.out.println("  - 性能极佳，内存池/零拷贝都已优化好");
        System.out.println("  Netty 是 Dubbo/gRPC/Kafka Client 的底层，经受了工业级考验");
        System.out.println();
        System.out.println("═══ 以下是 Netty 核心架构技术细节 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：原生 NIO 七宗罪 vs Netty 解决方案
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 原生 NIO 七宗罪 vs Netty 解决方案 ━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌────┬─────────────────────────────┬──────────────────────────────┐");
        System.out.println("  │ # │ 原生 NIO 痛点               │ Netty 解决方案               │");
        System.out.println("  ├────┼─────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ ① │ ByteBuffer flip/clear 易错  │ ByteBuf 双指针，无需 flip    │");
        System.out.println("  │ ② │ 粘包/拆包需自己处理         │ 内置 FrameDecoder 全家桶     │");
        System.out.println("  │ ③│ JDK epoll 空轮询 Bug        │ 检测计数，自动重建 Selector  │");
        System.out.println("  │ ④ │ 异常处理分散繁琐            │ Pipeline 统一 exceptionCaught│");
        System.out.println("  │ ⑤ │ 无连接池                   │ Channel 对象池               │");
        System.out.println("  │ ⑥ │ 无编解码                   │ 内置 HTTP/WebSocket/自定义   │");
        System.out.println("  │ ⑦ │ 无心跳机制                 │ IdleStateHandler             │");
        System.out.println("  └────┴─────────────────────────────┴──────────────────────────────┘");
        System.out.println();
        System.out.println("  类比：原生 NIO 是「毛坯房 + 一堆砖头」，Netty 是「精装修公寓」");
        System.out.println("        你进去就可以住（写业务），不用自己砌墙（处理底层细节）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：Netty 线程模型（Reactor 主从模式）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. Netty 线程模型：Reactor 主从模式 ━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  原生 NIO 单线程 Reactor（Part8 讲过）的问题：");
        System.out.println("    1个线程既 accept 又 read/write 又处理业务");
        System.out.println("    某个 Handler 执行慢 → 整个 Selector 被卡住 → 其他连接无法响应");
        System.out.println();
        System.out.println("  Netty 的 Reactor 主从模式（默认）：");
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │                    Netty Reactor 主从模式                    │");
        System.out.println("  │                                                              │");
        System.out.println("  │  BossGroup（通常1个线程）                                   │");
        System.out.println("  │  ┌─────────────────────────────────────┐                    │");
        System.out.println("  │  │ EventLoop-0                         │                    │");
        System.out.println("  │  │  Selector（只监听 OP_ACCEPT）       │                    │");
        System.out.println("  │  │  有新连接 → 把 SocketChannel 交给  │                    │");
        System.out.println("  │  │            WorkerGroup 中某个线程  │                    │");
        System.out.println("  │  └─────────────────────────────────────┘                    │");
        System.out.println("  │           ↓ （Round-Robin 分配）                            │");
        System.out.println("  │  WorkerGroup（CPU×2 个线程，默认）                         │");
        System.out.println("  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │");
        System.out.println("  │  │ EventLoop-0 │ │ EventLoop-1 │ │ EventLoop-2 │  ...       │");
        System.out.println("  │  │ Selector    │ │ Selector    │ │ Selector    │           │");
        System.out.println("  │  │ Channel A   │ │ Channel C   │ │ Channel E   │           │");
        System.out.println("  │  │ Channel B   │ │ Channel D   │ │ Channel F   │           │");
        System.out.println("  │  └─────────────┘ └─────────────┘ └─────────────┘           │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  关键设计：每个 Channel 绑定一个 EventLoop，整个生命周期不换");
        System.out.println("    优点：同一 Channel 的所有事件（read/write/连接/断开）");
        System.out.println("          都在同一线程处理 → 天然无锁，不需要 synchronized！");
        System.out.println("    类比：一个快递员（EventLoop）负责固定几栋楼（Channel）");
        System.out.println("          他认识每家住户，不需要和别人交接，效率高且不出错");
        System.out.println();
        System.out.println("  代码结构：");
        System.out.println("    EventLoopGroup bossGroup   = new NioEventLoopGroup(1);");
        System.out.println("    EventLoopGroup workerGroup = new NioEventLoopGroup(); // 默认 CPU×2");
        System.out.println("    ServerBootstrap bootstrap  = new ServerBootstrap();");
        System.out.println("    bootstrap");
        System.out.println("        .group(bossGroup, workerGroup)");
        System.out.println("        .channel(NioServerSocketChannel.class)  // 用 NIO");
        System.out.println("        .childHandler(new ChannelInitializer<SocketChannel>() {");
        System.out.println("            protected void initChannel(SocketChannel ch) {");
        System.out.println("                ch.pipeline()");
        System.out.println("                  .addLast(new LengthFieldDecoder(...)) // 拆包");
        System.out.println("                  .addLast(new MyBusinessHandler());    // 业务");
        System.out.println("            }");
        System.out.println("        });");
        System.out.println("    bootstrap.bind(8080).sync(); // 绑定端口，开始监听");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：Pipeline & ChannelHandler（责任链模式）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. Pipeline & ChannelHandler（责任链模式）━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  每个 Channel 都有一个 ChannelPipeline（固定的双向链表）");
        System.out.println("  数据进来（Inbound）从链表头往尾走");
        System.out.println("  数据出去（Outbound）从链表尾往头走");
        System.out.println();
        System.out.println("  [网络] ←→ [Head] → [H1] → [H2] → [H3] → [Tail]");
        System.out.println("            ←─────────────────────────────────────");
        System.out.println("  Inbound流 →  Head  →  H1  →  H2  →  H3  → Tail（数据读进来）");
        System.out.println("  Outbound流←  Head  ←  H1  ←  H2  ←  H3  ← Tail（数据写出去）");
        System.out.println();
        System.out.println("  生活类比：流水线工厂");
        System.out.println("    原材料进来：工位1（解码）→ 工位2（验签）→ 工位3（业务处理）");
        System.out.println("    成品出去：  工位3（生成响应）→ 工位2（加签）→ 工位1（编码）");
        System.out.println();
        System.out.println("  三种 Handler：");
        System.out.println("    ChannelInboundHandlerAdapter  - 只处理读进来的数据");
        System.out.println("    ChannelOutboundHandlerAdapter - 只处理写出去的数据");
        System.out.println("    ChannelDuplexHandler          - 双向都处理");
        System.out.println();
        System.out.println("  典型 Pipeline 配置（一个 RPC 服务器）：");
        System.out.println("    pipeline.addLast(new IdleStateHandler(60, 0, 0)); // 心跳检测");
        System.out.println("    pipeline.addLast(new LengthFieldBasedFrameDecoder(...)); // 拆包");
        System.out.println("    pipeline.addLast(new ProtobufDecoder(MyProto.getDefaultInstance())); // 解码");
        System.out.println("    pipeline.addLast(new LengthFieldPrepender(4));    // 加长度头");
        System.out.println("    pipeline.addLast(new ProtobufEncoder());          // 编码");
        System.out.println("    pipeline.addLast(new BusinessHandler());          // 业务逻辑");
        System.out.println();
        System.out.println("  ⚠ 黄金法则：Handler 里不能做阻塞操作（数据库/HTTP调用）！");
        System.out.println("    原因：Handler 在 EventLoop 线程里执行，阻塞 → 整个线程卡死");
        System.out.println("          EventLoop 线程卡死 → 所有绑定的 Channel 都无法响应！");
        System.out.println("    正确做法：把阻塞操作提交到独立的业务线程池");
        System.out.println("      ctx.channel().eventLoop().execute(() -> {");
        System.out.println("          // 阻塞操作（在 EventLoop 线程里，不推荐）");
        System.out.println("      });");
        System.out.println("      // 正确：提交到业务线程池");
        System.out.println("      businessExecutor.submit(() -> {");
        System.out.println("          String result = db.query(...); // 阻塞无所谓，独立线程");
        System.out.println("          ctx.writeAndFlush(result);     // 写回时回到 EventLoop");
        System.out.println("      });");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：ByteBuf vs ByteBuffer —— 双指针 vs 单指针
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. ByteBuf vs ByteBuffer —— 双指针彻底消灭 flip() ━━━━━━━━");
        System.out.println();
        System.out.println("  ByteBuffer（JDK 原生）的痛苦：");
        System.out.println("    只有 1 个 position 指针，读写共用");
        System.out.println("    写完 → flip() → 读，读完 → clear() → 再写");
        System.out.println("    忘了 flip() = 读到垃圾数据，是 NIO 新手最常见 Bug");
        System.out.println();
        System.out.println("  ByteBuf（Netty）的优雅：");
        System.out.println("    readerIndex ─── 下一次读从这里开始");
        System.out.println("    writerIndex ─── 下一次写从这里开始");
        System.out.println("    两个指针独立，读写互不干扰，永远不需要 flip()！");
        System.out.println();
        System.out.println("  ByteBuf 内存结构：");
        System.out.println("    ┌──────────────┬─────────────────┬──────────────────────────┐");
        System.out.println("    │ 已读（废弃区）│  可读数据区     │      可写空间            │");
        System.out.println("    └──────────────┴─────────────────┴──────────────────────────┘");
        System.out.println("    0          readerIndex       writerIndex                capacity");
        System.out.println();
        System.out.println("    readableBytes()  = writerIndex - readerIndex  （有多少可读）");
        System.out.println("    writableBytes()  = capacity - writerIndex      （还能写多少）");
        System.out.println();
        System.out.println("  ByteBuf 读写示例（对比 ByteBuffer）：");
        System.out.println();
        System.out.println("  // ByteBuffer（痛苦版）");
        System.out.println("  ByteBuffer buf = ByteBuffer.allocate(64);");
        System.out.println("  buf.put(\"hello\".getBytes());  // 写");
        System.out.println("  buf.flip();                    // ← 必须！否则读到 0");
        System.out.println("  byte[] data = new byte[buf.remaining()];");
        System.out.println("  buf.get(data);                 // 读");
        System.out.println("  buf.clear();                   // ← 准备下次写");
        System.out.println();
        System.out.println("  // ByteBuf（优雅版）");
        System.out.println("  ByteBuf buf = Unpooled.buffer(64);");
        System.out.println("  buf.writeBytes(\"hello\".getBytes()); // 写，writerIndex 自动增");
        System.out.println("  byte[] data = new byte[buf.readableBytes()];");
        System.out.println("  buf.readBytes(data);                 // 读，readerIndex 自动增");
        System.out.println("  // 不需要 flip()！不需要 clear()！");
        System.out.println();
        System.out.println("  ByteBuf 三种内存类型：");
        System.out.println("    堆内 ByteBuf：  Unpooled.heapBuffer()   在 JVM 堆，GC 管理");
        System.out.println("    堆外 ByteBuf：  Unpooled.directBuffer() 在 OS 内存，需手动释放");
        System.out.println("    复合 ByteBuf：  Unpooled.wrappedBuffer() 多个 buf 的逻辑合并");
        System.out.println("                   不发生内存拷贝！（类似指针数组）");
        System.out.println();
        System.out.println("  池化 vs 非池化：");
        System.out.println("    Unpooled.xxx()              非池化，用完就丢，GC 回收");
        System.out.println("    PooledByteBufAllocator.DEFAULT.buffer()  池化，借出用完还回去");
        System.out.println("    Netty 默认：堆外 + 池化（高并发下大幅减少 GC 压力）");
        System.out.println();
        System.out.println("  ⚠ 内存泄漏陷阱：ByteBuf 是引用计数的（refCnt）");
        System.out.println("    创建时 refCnt = 1，调用 release() → refCnt - 1 → 归零则回收");
        System.out.println("    如果忘了 release()，堆外内存泄漏，最终 OutOfMemoryError");
        System.out.println("    Netty 原则：谁最后用，谁 release()");
        System.out.println("    SimpleChannelInboundHandler 会自动 release inbound 消息");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：Netty 处理一个请求的完整流程
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. 从「客户端发数据」到「Handler 收到」的完整流程 ━━━━━━━");
        System.out.println();
        System.out.println("  Step1：客户端发送数据包");
        System.out.println("    网卡收到数据 → DMA 写入 Socket 接收缓冲区");
        System.out.println("    硬件中断 → 内核唤醒 epoll_wait → Selector.select() 返回");
        System.out.println();
        System.out.println("  Step2：EventLoop 线程处理 OP_READ 事件");
        System.out.println("    channel.read(ByteBuf)  ← 把数据从 Socket 缓冲区搬到 ByteBuf");
        System.out.println("    触发 pipeline.fireChannelRead(ByteBuf)");
        System.out.println();
        System.out.println("  Step3：ByteBuf 流过 Pipeline（Inbound 方向）");
        System.out.println("    [Head] → [LengthFieldDecoder] → [ProtobufDecoder] → [BusinessHandler]");
        System.out.println("    每个 Handler 处理完调用 ctx.fireChannelRead(msg) 传给下一个");
        System.out.println();
        System.out.println("  Step4：BusinessHandler 处理业务，写响应");
        System.out.println("    ctx.writeAndFlush(response)  ← 触发 Outbound 方向");
        System.out.println();
        System.out.println("  Step5：响应流过 Pipeline（Outbound 方向）");
        System.out.println("    [BusinessHandler] → [ProtobufEncoder] → [LengthPrepender] → [Head]");
        System.out.println("    Head 将数据写入 Socket 发送缓冲区，网卡 DMA 发出");
        System.out.println();
        System.out.println("  整条链：");
        System.out.println("    网卡 → Socket缓冲区 → ByteBuf → Pipeline(decode) → Handler");
        System.out.println("    Handler → Pipeline(encode) → Socket缓冲区 → 网卡 → 客户端");
        System.out.println();
        NIODemo.printSeparator();
    }
}

