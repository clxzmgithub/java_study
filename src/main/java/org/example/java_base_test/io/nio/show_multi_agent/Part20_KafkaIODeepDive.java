package org.example.java_base_test.io.nio.show_multi_agent;

class Part20_KafkaIODeepDive {

    static void explain() {
        System.out.println("【第二十部分：Kafka 完整 IO 体系深度解析】");
        System.out.println();
        System.out.println("═══ 📰 生活场景类比：报社印刷厂，理解 Kafka 完整 IO 体系 ═══");
        System.out.println();
        System.out.println("  把 Kafka 想象成一家「日报印刷厂」：");
        System.out.println();
        System.out.println("  ① 存储结构（.log + .index 文件）= 印刷厂的「流水号报纸仓库」");
        System.out.println("    仓库按「期号」分区存放，每摞报纸（Segment）最多堆1米高（1GB）");
        System.out.println("    每摞第一张报纸的期号就是文件名（方便快速定位：二分查找）");
        System.out.println("    .index 文件 = 每摞旁边贴的「抽查清单」：第100张在哪页？");
        System.out.println();
        System.out.println("  ② 顺序写磁盘 = 印刷机「连续出纸」，不跳格不倒退");
        System.out.println("    连续出纸（顺序写）比「随机跳页印刷」快 100 倍");
        System.out.println("    磁盘顺序写速度可媲美内存随机写，这是 Kafka 高吞吐的第一个秘密");
        System.out.println();
        System.out.println("  ③ Page Cache = 印刷厂门口的「展示架」");
        System.out.println("    最新出炉的报纸直接摆展示架（OS Page Cache），客户直接拿走");
        System.out.println("    根本不用进仓库找（跳过磁盘 IO），Kafka 消费者几乎消费 Page Cache 里的数据");
        System.out.println();
        System.out.println("  ④ sendfile 零拷贝消费 = 印刷厂直接用「传送带」把报纸送上卡车");
        System.out.println("    不经过「搬运工手中转」（用户态），直接从展示架（Page Cache）传送带到卡车（网卡）");
        System.out.println("    Consumer 消费时，数据：Page Cache → 网卡（2次DMA，0次CPU拷贝）");
        System.out.println();
        System.out.println("  ⑤ Producer 写入链路 = 记者投稿 → 编辑暂存 → 批量印刷");
        System.out.println("    记者（Producer）发稿 → 编辑桌（OS Page Cache）暂存");
        System.out.println("    积攒一批后一次性送印刷机（fsync刷盘），而不是每篇稿子都印一次");
        System.out.println();
        System.out.println("═══ 以上是 Kafka IO 的「印刷厂全流程」，技术细节见下 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：Kafka 存储结构（IO 优化的基础）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. Kafka 存储结构（为 IO 优化而设计）━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Kafka 数据目录结构：");
        System.out.println("    /kafka/data/");
        System.out.println("      ├── my-topic-0/          ← Partition 0");
        System.out.println("      │     ├── 00000000000000000000.log     ← 消息数据文件");
        System.out.println("      │     ├── 00000000000000000000.index   ← 稀疏偏移量索引");
        System.out.println("      │     ├── 00000000000000000000.timeindex ← 时间索引");
        System.out.println("      │     ├── 00000000000001000000.log     ← 下一个 Segment");
        System.out.println("      │     └── 00000000000001000000.index");
        System.out.println("      └── my-topic-1/          ← Partition 1");
        System.out.println();
        System.out.println("  文件名 = 该 Segment 第一条消息的 offset（方便二分查找定位）");
        System.out.println("  一个 Partition 由多个 Segment（默认 1GB 或 7天）组成");
        System.out.println();
        System.out.println("  .log 文件格式（每条消息）：");
        System.out.println("    ┌──────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("    │offset(8B)│size(4B)  │CRC(4B)   │attributes│key/value │");
        System.out.println("    └──────────┴──────────┴──────────┴──────────┴──────────┘");
        System.out.println("    消息追加写，永远是顺序写！");
        System.out.println();
        System.out.println("  .index 文件（稀疏索引，mmap 加载）：");
        System.out.println("    ┌──────────────────┬──────────────────┐");
        System.out.println("    │ relativeOffset(4B)│ position(4B)    │");
        System.out.println("    └──────────────────┴──────────────────┘");
        System.out.println("    每隔 N 条消息记一个索引（稀疏），不是每条都有索引");
        System.out.println("    用 mmap 加载到内存，二分查找速度极快");
        System.out.println("    定位消息：二分查找 index → 找到 position → 顺序扫描 log 文件");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：Producer 写入流程（完整 IO 链路）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. Producer 写入流程（完整 IO 链路）━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Producer → Broker 的完整数据流：");
        System.out.println();
        System.out.println("  ① Producer 批量发送（batch.size + linger.ms）");
        System.out.println("     消息不是一条条发的！Producer 有 RecordAccumulator 累积器");
        System.out.println("     batch.size=16KB：积满 16KB 才发一批");
        System.out.println("     linger.ms=5：或者等 5ms 后不管有没满都发");
        System.out.println("     类比：快递公司不是收到一个包裹就发车，而是装满一车再发");
        System.out.println();
        System.out.println("  ② Producer 压缩（可选，减少网络 IO）");
        System.out.println("     compression.type = snappy/lz4/zstd");
        System.out.println("     压缩在 Producer 端进行，Broker 存压缩后的数据");
        System.out.println("     Consumer 解压，Broker 不解压（节省 CPU）");
        System.out.println("     lz4 压缩率约 50%，吞吐量提升约 50%（网络省一半）");
        System.out.println();
        System.out.println("  ③ Broker 接收数据（网络 IO）");
        System.out.println("     Kafka 网络层：1个 Acceptor + N个 Processor + M个 Handler");
        System.out.println("     Acceptor（1线程）：accept 新连接，分发给 Processor");
        System.out.println("     Processor（默认3线程）：Selector 管理连接，读取请求数据");
        System.out.println("     Handler（默认8线程）：真正处理请求（写文件等）");
        System.out.println("     这是 Reactor 多线程模式，和 Netty 的 Boss+Worker 思路一样");
        System.out.println();
        System.out.println("  ④ Broker 写入 Page Cache（关键！不是直接写磁盘）");
        System.out.println("     Log.append() → 写入 .log 文件（FileChannel.write）");
        System.out.println("     实际上写到 OS Page Cache，OS 异步刷盘（dirty page 回写）");
        System.out.println("     acks=1：写到 Leader Page Cache 就返回成功");
        System.out.println("     acks=all：Leader + ISR 副本都写 Page Cache 才返回");
        System.out.println();
        System.out.println("  ⑤ Broker 更新 .index（稀疏索引，每 N 字节写一条）");
        System.out.println("     .index 文件用 mmap 映射，写 = 写内存（极快）");
        System.out.println();
        System.out.println("  ⑥ 刷盘策略（可配置）");
        System.out.println("     log.flush.interval.messages=100000（每10万条强制刷盘）");
        System.out.println("     log.flush.interval.ms=1000（每1秒强制刷盘）");
        System.out.println("     默认：不主动刷盘，完全依赖 OS Page Cache（性能最好，但有风险）");
        System.out.println("     Kafka 高可用靠多副本（Replication），而不是依赖本地刷盘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：Consumer 消费流程（完整 IO 链路）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. Consumer 消费流程（sendfile 零拷贝的实战）━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Consumer 发送 FETCH 请求 → Broker 响应数据");
        System.out.println();
        System.out.println("  定位消息的过程：");
        System.out.println("    Consumer 告诉 Broker：我要 topic-0，从 offset=12345 开始");
        System.out.println("    Step1：在 /data/topic-0/ 目录下，二分查找 Segment 文件名");
        System.out.println("           找到 00000000000010000.log（第10000条消息开始的文件）");
        System.out.println("    Step2：mmap 加载 .index 文件，二分查找 offset=12345 的位置");
        System.out.println("           找到最近的索引：offset=12300 → file_position=5678");
        System.out.println("    Step3：从 position=5678 开始顺序扫描 .log 文件");
        System.out.println("           找到 offset=12345 的具体字节位置");
        System.out.println("    整个定位过程：约 3次磁盘IO（2次二分查找 + 1次顺序扫描）");
        System.out.println();
        System.out.println("  发送数据（零拷贝）：");
        System.out.println("    FileRecords.writeTo() 调用 FileChannel.transferTo()");
        System.out.println("    → sendfile()：Page Cache → 网卡（0次 CPU 拷贝）");
        System.out.println();
        System.out.println("  Page Cache 的巨大作用：");
        System.out.println("    消费者通常消费最新的消息（Consumer lag 不大）");
        System.out.println("    刚写入的消息还在 Page Cache 里（还没刷到磁盘！）");
        System.out.println("    transferTo 直接从 Page Cache 发给网卡，根本没有磁盘 IO！");
        System.out.println("    类比：外卖刚到手（Page Cache），直接给来取的客人（Consumer）");
        System.out.println("          根本没有放进冰箱（磁盘）再取出来的步骤");
        System.out.println();
        System.out.println("  为什么 Kafka 官方建议给 Broker 留大内存？");
        System.out.println("    不是给 JVM，而是给 OS Page Cache！");
        System.out.println("    JVM 堆建议只给 6~8GB（Kafka 本身不缓存数据在堆上）");
        System.out.println("    剩余内存全给 OS 当 Page Cache");
        System.out.println("    如果 Producer 生产速度 < Cache 命中率，消费者永远不读磁盘！");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：Kafka 性能数字全景
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. Kafka 性能数字全景（各项技术叠加的效果）━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  单机 Kafka（8核 64GB 1G网卡）典型吞吐量：");
        System.out.println("    写入：~600MB/s（接近网卡上限）");
        System.out.println("    读取：~600MB/s（接近网卡上限）");
        System.out.println("    写+读同时：~900MB/s（接近网卡满载）");
        System.out.println();
        System.out.println("  各项优化贡献：");
        System.out.println("  ┌──────────────────────────────┬──────────────────────────────┐");
        System.out.println("  │ 优化技术                     │ 贡献                         │");
        System.out.println("  ├──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ 顺序写（vs 随机写）          │ 吞吐提升 6000 倍（HDD）      │");
        System.out.println("  │ Page Cache（不直接写磁盘）   │ 写延迟降低 10~100x           │");
        System.out.println("  │ 零拷贝（sendfile）           │ CPU 使用率降低 80%           │");
        System.out.println("  │ 批量发送（batch.size）       │ 网络 IO 次数降低 100x        │");
        System.out.println("  │ 压缩（lz4/snappy）           │ 网络带宽节省 50~70%          │");
        System.out.println("  │ Partition 并行               │ 吞吐线性扩展（分区越多越快）  │");
        System.out.println("  └──────────────────────────────┴──────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：Kafka 网络层源码导读
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. Kafka 网络层源码导读（Java NIO 的实际应用）━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Kafka 网络层是手写的 Java NIO，没有用 Netty（故意的）");
        System.out.println("  原因：Kafka 要精确控制每个 IO 细节，不想让框架层引入不确定性");
        System.out.println();
        System.out.println("  核心类：");
        System.out.println("    SocketServer.java   → 管理所有网络连接（Acceptor + Processors）");
        System.out.println("    Selector.java       → 封装 java.nio.Selector（Kafka 自己的！）");
        System.out.println("    KafkaChannel.java   → 单个 TCP 连接的抽象");
        System.out.println("    NetworkReceive.java → 负责接收完整消息（处理粘包/拆包）");
        System.out.println();
        System.out.println("  NetworkReceive（处理粘包/拆包）核心逻辑：");
        System.out.println("    // Kafka 用「4字节长度头 + body」的方式解决粘包");
        System.out.println("    if (size == null) {");
        System.out.println("        // 先读 4 字节长度");
        System.out.println("        if (sizeBuffer.hasRemaining()) {");
        System.out.println("            channel.read(sizeBuffer); // NIO 非阻塞读");
        System.out.println("        }");
        System.out.println("        if (!sizeBuffer.hasRemaining()) {");
        System.out.println("            sizeBuffer.flip();");
        System.out.println("            size = sizeBuffer.getInt(); // 长度");
        System.out.println("            buffer = ByteBuffer.allocate(size); // 按长度分配");
        System.out.println("        }");
        System.out.println("    } else {");
        System.out.println("        // 读 body（可能多次才读完）");
        System.out.println("        if (buffer.hasRemaining()) {");
        System.out.println("            channel.read(buffer);");
        System.out.println("        }");
        System.out.println("        if (!buffer.hasRemaining()) {");
        System.out.println("            // 一条完整消息收齐了！");
        System.out.println("            buffer.flip();");
        System.out.println("            complete = true;");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("    // 这就是手动实现了 LengthFieldBasedFrameDecoder 的功能！");
        System.out.println();
        NIODemo.printSeparator();
    }
}

