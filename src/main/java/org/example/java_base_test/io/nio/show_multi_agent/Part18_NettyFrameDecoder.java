package org.example.java_base_test.io.nio.show_multi_agent;

class Part18_NettyFrameDecoder {

    static void explain() {
        System.out.println("【第十八部分：Netty 粘包/拆包处理】");
        System.out.println();
        System.out.println("═══ 🚰 生活场景：水管里的水 理解粘包/拆包 ═══");
        System.out.println();
        System.out.println("  想象你往水管里灌水（发送数据），对面用碗接水（接收数据）：");
        System.out.println();
        System.out.println("  你往水管里依次灌了 3 桶水（3 条消息）：");
        System.out.println("    第1桶：红色水（消息A）");
        System.out.println("    第2桶：绿色水（消息B）");
        System.out.println("    第3桶：蓝色水（消息C）");
        System.out.println();
        System.out.println("  对面用碗接水，可能出现这些情况：");
        System.out.println("    正常情况：三碗分别接到红/绿/蓝（理想，实际几乎不存在）");
        System.out.println("    粘包：第一碗同时接到了红色+绿色水混在一起（两条消息连在一起）");
        System.out.println("    拆包：第一碗只接到半碗红色，第二碗接到另半碗红+全部绿（一条消息被拆开）");
        System.out.println("    混合：最混乱的情况，各种颜色都可能混在一起");
        System.out.println();
        System.out.println("  问题根源：TCP 是「字节流」，不是「消息流」");
        System.out.println("    TCP 只保证数据按顺序、不丢失地到达，但「消息边界」它不管！");
        System.out.println("    就像水管里的水混在一起，你根本分不清哪段是「第1桶」哪段是「第2桶」");
        System.out.println();
        System.out.println("  解决方案（如何区分消息边界）：");
        System.out.println("    方法1：固定长度——每桶水规定必须是1升（FixedLengthFrameDecoder）");
        System.out.println("    方法2：特殊分隔符——每桶水里加一块石头作为「消息结束标志」(\\n)");
        System.out.println("    方法3：长度+内容——先告诉对方「这桶水有3升」，再灌水（最常用！）");
        System.out.println("    方法4：自定义协议——先加「桶的颜色（类型）」+「桶的容量」+水（RPC协议）");
        System.out.println();
        System.out.println("  Netty 内置了这 4 种方案的 FrameDecoder，你只需配置，不用自己写逻辑！");
        System.out.println();
        System.out.println("═══ 以下是粘包/拆包技术细节 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：粘包/拆包原理
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 粘包/拆包原理（彻底搞清楚）━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  发送方发了 3 条消息（A=5字节，B=3字节，C=4字节）：");
        System.out.println("  ┌─────┬───┬────┐");
        System.out.println("  │AAAAA│BBB│CCCC│");
        System.out.println("  └─────┴───┴────┘");
        System.out.println();
        System.out.println("  接收方可能的 read() 结果（4种情况）：");
        System.out.println();
        System.out.println("  ① 理想情况（不粘不拆）：");
        System.out.println("    read1: AAAAA   read2: BBB   read3: CCCC");
        System.out.println();
        System.out.println("  ② 粘包（两条消息粘在一起收到）：");
        System.out.println("    read1: AAAAABBB   read2: CCCC");
        System.out.println("    原因：发送端 Nagle 算法把小包合并，或接收端 read() 太慢");
        System.out.println();
        System.out.println("  ③ 拆包（一条消息被拆成两次收到）：");
        System.out.println("    read1: AAA   read2: AABBBCCCC");
        System.out.println("    原因：网络 MTU 限制，或发送缓冲区不够，TCP 把消息拆开发");
        System.out.println();
        System.out.println("  ④ 粘包+拆包混合（最头疼）：");
        System.out.println("    read1: AAABBB   read2: AACCCC");
        System.out.println("    前面 A 被拆了，同时 B 和下一个包头粘在一起");
        System.out.println();
        System.out.println("  关键结论：不能假设一次 read() = 一条完整消息！");
        System.out.println("           必须在应用层定义「消息边界」来分割字节流");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：四种解决方案对比
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. 四种消息边界方案对比 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  方案1：固定长度（FixedLengthFrameDecoder）");
        System.out.println("    每条消息固定 N 字节，不够 N 字节就等，凑满再交给上层");
        System.out.println("    ┌────────────────┐┌────────────────┐");
        System.out.println("    │   消息A (16B)  ││   消息B (16B)  │");
        System.out.println("    └────────────────┘└────────────────┘");
        System.out.println("    优点：简单   缺点：消息长度必须固定（浪费空间或截断）");
        System.out.println("    Netty：new FixedLengthFrameDecoder(16)");
        System.out.println();
        System.out.println("  方案2：特殊分隔符（DelimiterBasedFrameDecoder）");
        System.out.println("    用 \\n 或自定义字节序列作为消息结束标志");
        System.out.println("    ┌────────────┬──┐┌────────────┬──┐");
        System.out.println("    │  消息内容  │\\n││  消息内容  │\\n│");
        System.out.println("    └────────────┴──┘└────────────┴──┘");
        System.out.println("    优点：变长消息   缺点：消息内容不能含分隔符（或需转义）");
        System.out.println("    Netty：new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter())");
        System.out.println("    HTTP 的请求头用的就是这种方式（\\r\\n\\r\\n 分隔 header 和 body）");
        System.out.println();
        System.out.println("  方案3：长度字段（LengthFieldBasedFrameDecoder）← 工业最常用");
        System.out.println("    消息 = 长度头（N字节）+ 消息体");
        System.out.println("    ┌───────┬──────────────────────┐");
        System.out.println("    │ 长度  │      消息内容        │");
        System.out.println("    │ (4B)  │   (长度字段指定)     │");
        System.out.println("    └───────┴──────────────────────┘");
        System.out.println("    优点：变长，内容可含任意字节，性能最好");
        System.out.println("    Netty：LengthFieldBasedFrameDecoder（6个参数，下面详讲）");
        System.out.println("    使用者：Dubbo、gRPC、Thrift、自定义 RPC 协议都用这种");
        System.out.println();
        System.out.println("  方案4：自定义协议头（用 ReplayingDecoder）");
        System.out.println("    消息 = 魔数 + 版本 + 长度 + 类型 + 序列号 + 消息体");
        System.out.println("    最灵活，适合完整 RPC 协议设计");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：LengthFieldBasedFrameDecoder 六参数详解
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. LengthFieldBasedFrameDecoder 六参数详解 ━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  构造参数：");
        System.out.println("  new LengthFieldBasedFrameDecoder(");
        System.out.println("      maxFrameLength,    // ① 最大帧长度（防 OOM 攻击）");
        System.out.println("      lengthFieldOffset, // ② 长度字段从第几字节开始");
        System.out.println("      lengthFieldLength, // ③ 长度字段本身占几个字节（通常2或4）");
        System.out.println("      lengthAdjustment,  // ④ 长度字段的值需要加多少才是消息体长度");
        System.out.println("      initialBytesToStrip // ⑤ 解码后去掉前几个字节（去掉头部）");
        System.out.println("  )");
        System.out.println();
        System.out.println("  场景1：最简单，长度 = 消息体长度，4字节长度字段");
        System.out.println("    报文: ┌─── 4B ───┬─── N 字节 ───┐");
        System.out.println("          │  length  │    body      │");
        System.out.println("          └──────────┴──────────────┘");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4)");
        System.out.println("    → 读 4 字节得到 length，再读 length 字节，Strip 掉 4 字节头");
        System.out.println("    → Handler 收到的是纯 body ByteBuf");
        System.out.println();
        System.out.println("  场景2：长度包含了头部（Dubbo 协议风格）");
        System.out.println("    报文: ┌─ 2B ─┬──── 2B ────┬─── N 字节 ───┐");
        System.out.println("          │魔数  │ total_len  │    body      │");
        System.out.println("          └──────┴────────────┴──────────────┘");
        System.out.println("          其中 total_len = 4 (头) + N (body)");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 2, 2, -4, 0)");
        System.out.println("    lengthFieldOffset=2 : 跳过2字节魔数才是长度字段");
        System.out.println("    lengthAdjustment=-4 : 长度值包含了头部4字节，减掉才是body长");
        System.out.println("    initialBytesToStrip=0: 不剥离头部，Handler 收完整帧");
        System.out.println();
        System.out.println("  场景3：自定义RPC协议（最常见设计）");
        System.out.println("    报文: ┌─ 4B ─┬─ 1B ─┬─ 1B ─┬─ 4B ─┬─── N 字节 ───┐");
        System.out.println("          │魔数  │版本  │类型  │body长│    body      │");
        System.out.println("          └──────┴──────┴──────┴──────┴──────────────┘");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 6, 4, 0, 10)");
        System.out.println("    lengthFieldOffset=6  : 前6字节(魔数+版本+类型)跳过");
        System.out.println("    lengthFieldLength=4  : 长度字段4字节");
        System.out.println("    lengthAdjustment=0   : 长度字段的值就是body长度");
        System.out.println("    initialBytesToStrip=10: 剥离掉前10字节头部，Handler收纯body");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：自定义协议编解码完整示例
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. 自定义 RPC 协议完整设计示例 ━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  协议格式（共 12 字节头 + 变长 body）：");
        System.out.println("  ┌──────────┬──────┬──────┬──────────┬──────────┬─────────────┐");
        System.out.println("  │ 魔数(4B) │版本  │类型  │序列号(4B)│body长(4B)│   body(NB)  │");
        System.out.println("  │ 0xCAFEBABE│(1B) │(1B)  │          │          │             │");
        System.out.println("  └──────────┴──────┴──────┴──────────┴──────────┴─────────────┘");
        System.out.println("  消息类型：0x01=请求 0x02=响应 0x03=心跳 0x04=心跳响应");
        System.out.println();
        System.out.println("  编码器（MessageEncoder extends MessageToByteEncoder<Message>）：");
        System.out.println("    protected void encode(ctx, msg, out) {");
        System.out.println("        out.writeInt(0xCAFEBABE);        // 魔数");
        System.out.println("        out.writeByte(1);                // 版本");
        System.out.println("        out.writeByte(msg.getType());    // 类型");
        System.out.println("        out.writeInt(msg.getSeqId());    // 序列号");
        System.out.println("        byte[] body = serialize(msg);");
        System.out.println("        out.writeInt(body.length);       // body 长度");
        System.out.println("        out.writeBytes(body);            // body");
        System.out.println("    }");
        System.out.println();
        System.out.println("  解码器（LengthFieldBasedFrameDecoder 参数）：");
        System.out.println("    // lengthFieldOffset=10(跳过前10字节到body长字段)");
        System.out.println("    // lengthFieldLength=4, lengthAdjustment=0");
        System.out.println("    // initialBytesToStrip=0(保留完整帧，再自己解析头)");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 10, 4, 0, 0)");
        System.out.println();
        System.out.println("  消息解码器（MessageDecoder extends ByteToMessageDecoder）：");
        System.out.println("    protected void decode(ctx, in, out) {");
        System.out.println("        int magic  = in.readInt();   // 0xCAFEBABE");
        System.out.println("        byte ver   = in.readByte();");
        System.out.println("        byte type  = in.readByte();");
        System.out.println("        int seqId  = in.readInt();");
        System.out.println("        int len    = in.readInt();");
        System.out.println("        byte[] body = new byte[len];");
        System.out.println("        in.readBytes(body);");
        System.out.println("        out.add(deserialize(body, type)); // 传给下一个 Handler");
        System.out.println("    }");
        System.out.println();
        System.out.println("  心跳机制（IdleStateHandler）：");
        System.out.println("    pipeline.addLast(new IdleStateHandler(0, 0, 30)); // 30s无读写触发");
        System.out.println("    // Handler 里：");
        System.out.println("    public void userEventTriggered(ctx, evt) {");
        System.out.println("        if (evt instanceof IdleStateEvent) {");
        System.out.println("            ctx.writeAndFlush(HEARTBEAT_FRAME); // 发心跳");
        System.out.println("            // 如果对端也没响应，就关闭连接");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println();
        NIODemo.printSeparator();
    }
}

