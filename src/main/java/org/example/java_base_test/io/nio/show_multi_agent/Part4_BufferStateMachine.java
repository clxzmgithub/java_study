package org.example.java_base_test.io.nio.show_multi_agent;

import java.nio.ByteBuffer;

// ====================================================================
// 第四部分：Buffer 三指针状态机（flip / clear / compact）
//
// ── 这部分讲什么？干什么用？ ──────────────────────────────────────────
//
// NIO 与传统 BIO 最核心的区别之一就是：
//   BIO：直接操作 InputStream/OutputStream，数据像"水流"一样流动
//   NIO：所有数据的读写都必须经过 Buffer，数据先装进桶，再统一处理
//
// ByteBuffer 就是那个"桶"。
// 但这个桶不是普通的桶，它内部有 3 个指针（游标）来精确控制：
//   - 现在写到哪了？
//   - 现在读到哪了？
//   - 总共能装多少？
//
// flip() / clear() / compact() 就是用来切换这 3 个指针状态的操作。
// 搞不懂这个，NIO 的所有代码都看不懂，这是 NIO 的绝对地基。
//
// ── ByteBuffer 的内存结构（源码中的字段） ─────────────────────────────
//
// java.nio.Buffer 源码（JDK 8）：
//
//   private int mark = -1;   // 标记位，调用 mark() 时记录当前 position
//   private int position = 0; // 当前游标：下一次 get/put 从这里开始
//   private int limit;        // 有效边界：不能读/写超过这里
//   private int capacity;     // 总容量：创建后不变
//
// 四者关系必须满足：0 <= mark <= position <= limit <= capacity
//
// 可以这样理解——把 ByteBuffer 想象成一个停车场：
//
//   capacity = 停车场总车位数（10个）
//   limit    = 今天开放的车位上限（可能只开放了前8个）
//   position = 下一辆车停的位置（当前游标）
//   mark     = 某个"书签"位置（可以随时回来）
//
// ── 两种"模式"的概念 ──────────────────────────────────────────────────
//
// ByteBuffer 没有显式的"读模式/写模式"标志位，
// 但通过约定，三个指针的值不同代表不同用途：
//
// 【写模式】刚 allocate 或 clear() 后：
//   position = 0（从头写）
//   limit    = capacity（全部空间都能写）
//   图示：
//   [0][1][2][3][4][5][6][7][8][9]
//    P                             L=C
//   (position=0, limit=10, capacity=10)
//
// 往里写 3 个字节 put('H'), put('i'), put('!') 后：
//   [H][i][!][3][4][5][6][7][8][9]
//               P                 L=C
//   (position=3, limit=10)
//   position 自动向右移动到 3
//
// 【读模式】flip() 后：
//   limit    = position（之前写到哪，读就只能读到哪）
//   position = 0（从头读）
//   图示：
//   [H][i][!][3][4][5][6][7][8][9]
//    P        L
//   (position=0, limit=3)
//   limit 收紧到 3，防止读到没写过的垃圾数据
//
// ────────────────────────────────────────────────────────────────────
// ====================================================================
class Part4_BufferStateMachine {

    // ── 前置知识：GC 与内存管理范围 ───────────────────────────────────────
    //
    // GC（Garbage Collection，垃圾回收）只管 JVM 堆内的内存：
    //
    //   物理内存整体布局：
    //   ┌────────────────────────────────┐
    //   │     整个物理内存 (RAM)          │
    //   ├────────────────────────────────┤
    //   │  ┌──────────────────────────┐  │
    //   │  │  JVM 进程空间             │  │ ← GC 只管理这里
    //   │  │  ┌────────────────────┐  │  │
    //   │  │  │  JVM 堆 (Heap)     │  │  │ ← GC 主要工作区
    //   │  │  │  - 对象实例         │  │  │   （会移动对象地址）
    //   │  │  │  - 数组             │  │  │
    //   │  │  └────────────────────┘  │  │
    //   │  └──────────────────────────┘  │
    //   │                                │
    //   │  ┌──────────────────────────┐  │
    //   │  │  堆外内存                 │  │ ← GC 不管这里
    //   │  │  - Direct Buffer        │  │   （地址固定不变）
    //   │  │  - mmap 映射文件         │  │
    //   │  │  - JNI native 内存      │  │
    //   │  └──────────────────────────┘  │
    //   └────────────────────────────────┘
    //
    // 关键洞察：
    //   • Heap Buffer 在 JVM 堆内 → GC 会移动它的地址 → DMA 不能用
    //   • Direct Buffer 在堆外 → GC 碰不到 → 地址固定 → DMA 可以直接用
    //   • 所以 Heap Buffer IO 需要额外拷贝：堆内 → 堆外临时区 → 内核
    //   • Direct Buffer IO 少一次拷贝：堆外Direct → 内核
    //
    // ────────────────────────────────────────────────────────────────────

    static void demonstrate() {
        System.out.println("【第四部分：Buffer 三指针状态机（flip / clear / compact）】");
        System.out.println();
        System.out.println("═══ 🎙️ 生活场景：把 ByteBuffer 想象成一盒录音磁带 ═══");
        System.out.println();
        System.out.println("  想象一盒旧式录音磁带，你要用它来录音、再播放：");
        System.out.println();
        System.out.println("  【磁带的三个指针】");
        System.out.println("  capacity  = 磁带总长度（买来是多少就是多少，不变）");
        System.out.println("  position  = 录音/播放头现在在哪个位置（当前游标）");
        System.out.println("  limit     = 这次最多能录/能播到哪里（有效边界）");
        System.out.println();
        System.out.println("  【录音阶段 = 写模式】刚买来的磁带：");
        System.out.println("    position = 0（从头录）");
        System.out.println("    limit = capacity（整盘磁带都能录）");
        System.out.println("    你开始录音「Hello」，录音头自动往后走5格");
        System.out.println("    position 变成 5，剩余的磁带还空着");
        System.out.println();
        System.out.println("  【flip() = 录完了，切换成播放模式】");
        System.out.println("    把磁带倒回到开头（position = 0）");
        System.out.println("    同时告诉播放器「只播到第5格就停」（limit = 5）");
        System.out.println("    → 防止播放到后面空白区域（对应未写入数据）");
        System.out.println();
        System.out.println("  【播放中 = 读模式】");
        System.out.println("    播放头从 0 开始往后走，播到 limit=5 停止");
        System.out.println("    播放头走到哪就是 position");
        System.out.println();
        System.out.println("  【clear() = 播完了，重新录制（磁带不擦，只归零）】");
        System.out.println("    position = 0，limit = capacity");
        System.out.println("    ⚠️ 磁带上旧内容还在，只是录音头回到开头，下次录会覆盖");
        System.out.println("    就像磁带「清空」其实没有消磁，只是重新从头录而已");
        System.out.println();
        System.out.println("  【compact() = 播了一半，中途要接着录】");
        System.out.println("    场景：磁带上有 ABC 三首歌，播完 AB 后想接着录新歌 D");
        System.out.println("         但 C 还没播完，不能直接清空！");
        System.out.println("    compact() 做的事：把 C 移到最前面，然后录音头从 C 后面开始");
        System.out.println("    这样下次可以先听 C 再听 D（粘包处理的核心！）");
        System.out.println();
        System.out.println("  💡 关键理解：为什么 AB 被覆盖没关系？");
        System.out.println("    因为 AB 已经播放完了（已读取并处理），不需要再保留");
        System.out.println("    Buffer 的设计哲学：「读完即弃」—— position 之前的数据都是已处理的垃圾");
        System.out.println("    compact() 只关心保留 [position, limit) 区间的未读数据");
        System.out.println("    已读数据被覆盖是正确且必要的行为，为后续写入腾出空间");
        System.out.println();
        System.out.println("═══ 以下是技术演示（对照上面的比喻来看）═══");
        System.out.println();
        System.out.println("━━━ ByteBuffer 的内存结构 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ByteBuffer 底层就是一个 byte[]，");
        System.out.println("  配合 4 个指针精确控制读写位置：");
        System.out.println();
        System.out.println("  capacity  = 总容量，创建后永远不变");
        System.out.println("  limit     = 有效边界（写模式=capacity；读模式=已写入量）");
        System.out.println("  position  = 当前游标（下一次 get/put 的位置）");
        System.out.println("  mark      = 书签（调用 mark() 记录，reset() 跳回）");
        System.out.println();
        System.out.println("  约束：0 <= mark <= position <= limit <= capacity");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 1：完整生命周期：allocate → put → flip → get → clear
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示1：完整生命周期 allocate→put→flip→get→clear ━━━━━━━━━━━");
        System.out.println();

        // Step1: allocate
        ByteBuffer buf = ByteBuffer.allocate(10);
        System.out.println("  Step1: ByteBuffer.allocate(10)");
        System.out.println("  底层创建了 byte[10]，指针初始化：");
        printBufferDiagram("allocate后【写模式】", buf, "← P（下一次put从这里写）");
        // 图示：capacity=10, limit=10, position=0
        // [_][_][_][_][_][_][_][_][_][_]
        //  P                             L=C

        // Step2: put
        buf.put((byte) 'H');
        buf.put((byte) 'i');
        buf.put((byte) '!');
        System.out.println("  Step2: put('H'), put('i'), put('!') 各一次");
        System.out.println("  每次 put 都把 position+1，游标右移：");
        printBufferDiagram("put 3字节后【写模式】", buf, "← P（下一次put从这里写）");
        // [H][i][!][_][_][_][_][_][_][_]
        //           P                    L=C

        // Step3: flip
        System.out.println("  Step3: buf.flip()");
        System.out.println("  flip() 源码等价于：");
        System.out.println("    limit = position;  // 把 limit 收紧到已写位置");
        System.out.println("    position = 0;      // 游标归零，从头读");
        System.out.println("    mark = -1;         // 清除书签");
        System.out.println("  作用：切换为【读模式】，limit 就是防止读到未写区域的护栏");
        buf.flip();
        printBufferDiagram("flip()后【读模式】", buf, "← P（下一次get从这里读）");
        // [H][i][!][_][_][_][_][_][_][_]
        //  P        L

        // Step4: get
        byte b1 = buf.get(); // 读 'H'
        byte b2 = buf.get(); // 读 'i'
        System.out.println("  Step4: get() 两次，读到：'" + (char)b1 + "' 和 '" + (char)b2 + "'");
        System.out.println("  每次 get 把 position+1，游标右移：");
        printBufferDiagram("get两次后【读模式，'!'未读】", buf, "← P（下一次get从这里读）");
        // [H][i][!][_][_][_][_][_][_][_]
        //       P  L

        // Step5: clear
        System.out.println("  Step5: buf.clear()");
        System.out.println("  clear() 源码等价于：");
        System.out.println("    position = 0;       // 游标归零");
        System.out.println("    limit = capacity;   // limit 恢复到最大");
        System.out.println("    mark = -1;          // 清除书签");
        System.out.println("  ⚠ 重要：clear() 并不清空 byte[] 里的数据！");
        System.out.println("    '!' 还安静地躺在 index=2 里，只是下次 put 会覆盖它");
        System.out.println("    所以叫 clear 有点误导，准确说是「重置指针，假装清空」");
        buf.clear();
        printBufferDiagram("clear()后【写模式，数据未清】", buf, "← P（重新从头写）");
        // [H][i][!][_][_][_][_][_][_][_]  ← 数据还在！只是指针归零了
        //  P                              L=C
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 2：compact —— NIO 粘包处理的核心操作
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示2：compact() —— 粘包场景下的救命操作 ━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  真实场景：TCP 是流式协议，没有消息边界");
        System.out.println("  一次 channel.read(buf) 可能读到：");
        System.out.println("    「消息A 完整数据(3字节)」+「消息B 的半包(2字节)」");
        System.out.println("  处理完消息A后，消息B的半包不能丢，不能用 clear()！");
        System.out.println("  这时就要用 compact()");
        System.out.println();

        ByteBuffer buf2 = ByteBuffer.allocate(10);
        // 模拟 channel.read(buf) 一次读进来 5 字节
        buf2.put(new byte[]{'A', 'A', 'A', 'B', 'B'}); // AAA=消息A，BB=消息B半包
        buf2.flip();
        printBufferDiagram("channel.read后 flip【读模式】", buf2, "← P（准备读取）");
        // [A][A][A][B][B][_][_][_][_][_]
        //  P              L

        // 处理消息A：读3字节
        buf2.get(); buf2.get(); buf2.get();
        printBufferDiagram("读完消息A(3字节)，BB是消息B半包", buf2, "← P（'B','B'还没读）");
        // [A][A][A][B][B][_][_][_][_][_]
        //           P   L

        System.out.println("  此时如果用 clear()，'B','B' 就丢了！");
        System.out.println("  应该用 compact()：");
        System.out.println("  compact() 做的事：");
        System.out.println("    1. 把 [position, limit) 之间的未读数据复制到数组头部");
        System.out.println("    2. position = 未读数据量（2）");
        System.out.println("    3. limit = capacity（开放全部空间，准备继续写入）");
        buf2.compact();
        printBufferDiagram("compact()后【写模式，BB保留在头部】", buf2, "← P（从这里继续写新数据）");
        // [B][B][A][B][B][_][_][_][_][_]  ← 前2位是BB，后面是旧数据残留（会被覆盖）
        //       P                          L=C
        System.out.println("  下次 channel.read(buf) 会把新数据追加到 position=2 之后");
        System.out.println("  再次 flip() 后，可以读到完整的 消息B（BB + 新到的数据）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 3：三个操作的对比表
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示3：flip / clear / compact 对比速查表 ━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌───────────┬──────────────────────────────┬──────────────────────────────┐");
        System.out.println("  │  操作     │  指针变化                    │  使用场景                    │");
        System.out.println("  ├───────────┼──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ flip()    │ limit=position, position=0   │ 写完数据，准备读取/发送      │");
        System.out.println("  │           │ 写模式 → 读模式              │ channel.write(buf)之前必调   │");
        System.out.println("  ├───────────┼──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ clear()   │ position=0, limit=capacity   │ 重置指针，从头开始写入    │");
        System.out.println("  │           │ 任意 → 写模式（不清数据）    │ ⚠ 未读数据会被覆盖！     │");
        System.out.println("  ├───────────┼──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ compact() │ 未读数据移到头部             │ 处理粘包/半包，保留未读数据  │");
        System.out.println("  │           │ position=剩余量,limit=cap    │ 读模式 → 写模式（保留数据）  │");
        System.out.println("  └───────────┴──────────────────────────────┴──────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 4：Heap Buffer vs Direct Buffer
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示4：Heap Buffer vs Direct Buffer ━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ByteBuffer.allocate(1024)       ← Heap Buffer（堆内存）");
        System.out.println("  ByteBuffer.allocateDirect(1024) ← Direct Buffer（堆外内存）");
        System.out.println();
        System.out.println("  【Heap Buffer】");
        System.out.println("  数据存在 JVM 堆里，GC 会管理、移动它的内存地址。");
        System.out.println("  做 IO 时（如 channel.write(buf)），JVM 需要：");
        System.out.println("    ① 先把堆内数据拷贝一份到堆外（固定地址）");
        System.out.println("    ② 再让内核从堆外地址做 DMA");
        System.out.println("  多了一次内存拷贝的原因：GC 随时可能移动堆对象，");
        System.out.println("  DMA 是异步操作，它不认识会移动的地址");
        System.out.println();
        System.out.println("  【Direct Buffer】");
        System.out.println("  数据存在堆外（直接调用 OS 的 malloc），地址固定不动。");
        System.out.println("  内核 DMA 可以直接操作这块内存，省去堆→堆外的拷贝。");
        System.out.println("  代价：分配/释放慢（走 OS），释放依赖 GC 触发 Cleaner，");
        System.out.println("        不适合频繁创建小 Buffer");
        System.out.println();
        System.out.println("  【内存路径对比】");
        System.out.println("  Heap Buffer IO：  磁盘/网卡 → 内核缓冲区 → 堆外临时区 → JVM堆  (3次拷贝)");
        System.out.println("  Direct Buffer IO：磁盘/网卡 → 内核缓冲区 → 堆外Direct区         (2次拷贝)");
        System.out.println();
        System.out.println("  【Netty 的解法：内存池 PooledByteBufAllocator】");
        System.out.println("  Direct Buffer 创建慢的问题，Netty 用内存池解决：");
        System.out.println("  预先 allocateDirect 一大块，然后切分复用，避免频繁 malloc/free");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 5：最经典的 Bug：写完忘了 flip()
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示5：最经典 Bug —— 写完忘了 flip() ━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        ByteBuffer bugBuf = ByteBuffer.allocate(10);
        bugBuf.put("hello".getBytes());
        // ↑ 正常应该调用 flip()，这里故意不调用，模拟 Bug

        System.out.println("  写入 'hello'（5字节），忘了调用 flip()，直接发送给 channel：");
        System.out.println("    position=" + bugBuf.position()
                + ", limit=" + bugBuf.limit()
                + ", remaining=" + bugBuf.remaining());
        System.out.println();
        System.out.println("  问题：channel.write(bugBuf) 或 bugBuf.get() 从 position=5 开始读");
        System.out.println("        但 limit=10，remaining=5，看起来像还有5字节可读");
        System.out.println("        实际读到的是 index[5]~[9]，全是初始值 0x00！");
        System.out.println("        对端收到的是 5 个 \\0 字节，'hello' 一个字没发出去！");
        System.out.println();
        System.out.println("  ✅ 正确写法：");
        System.out.println("    ByteBuffer buf = ByteBuffer.allocate(10);");
        System.out.println("    buf.put(\"hello\".getBytes());");
        System.out.println("    buf.flip();                          // ← 关键！切换为读模式");
        System.out.println("    channel.write(buf);                  // 现在才会读 [0,5) 的数据");
        System.out.println();

        // 演示正确写法
        ByteBuffer correctBuf = ByteBuffer.allocate(10);
        correctBuf.put("hello".getBytes());
        correctBuf.flip(); // 正确！
        byte[] readData = new byte[correctBuf.remaining()];
        correctBuf.get(readData);
        System.out.println("  验证正确写法，flip后 get 到的内容：\"" + new String(readData) + "\"");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 6：mark() & reset() 书签功能
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示6：mark() & reset() 书签功能 ━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  场景：先读几个字节判断消息类型，再重新从头解析完整消息");
        ByteBuffer markBuf = ByteBuffer.allocate(10);
        markBuf.put(new byte[]{'M', 'S', 'G', ':', 'H', 'i'});
        markBuf.flip();

        System.out.println("  数据：[M][S][G][:][H][i]，flip后准备读取");
        printBufferDiagram("flip后", markBuf, "← P");

        // 读3字节判断消息头
        byte t1 = markBuf.get();
        byte t2 = markBuf.get();
        byte t3 = markBuf.get();
        System.out.println("  读前3字节判断类型：'" + (char)t1 + (char)t2 + (char)t3 + "'");

        // 在读完头部之后，mark 当前位置（第4字节 ':'）
        markBuf.mark();
        System.out.println("  mark() 在 position=" + markBuf.position() + " 打上书签");

        // 继续读
        markBuf.get(); // ':'
        markBuf.get(); // 'H'
        System.out.println("  继续读了2字节，现在 position=" + markBuf.position());

        // 发现需要重新从':' 开始解析，reset回去
        markBuf.reset();
        System.out.println("  reset() 跳回书签位置，position=" + markBuf.position());
        byte back = markBuf.get();
        System.out.println("  再次 get() 读到：'" + (char)back + "'（正是 ':' ）");
        System.out.println();

        NIODemo.printSeparator();
    }

    // 打印 Buffer 的可视化状态图
    static void printBufferDiagram(String label, ByteBuffer buf, String ignored) {
        int cap  = buf.capacity();
        int lim  = buf.limit();
        int pos  = buf.position();

        System.out.println("  [" + label + "]");

        // 第一行：数组内容（直接访问 array()，不受 limit 限制）
        byte[] array = buf.array();
        StringBuilder cells = new StringBuilder("    |");
        for (int i = 0; i < cap; i++) {
            byte b = array[i];
            String cell = b == 0 ? " _ " : " " + (char) b + " ";
            cells.append(cell).append("|");
        }
        System.out.println(cells);

        // 第二行：指针标记
        StringBuilder pointers = new StringBuilder("     ");
        for (int i = 0; i < cap; i++) {
            String marker = "   ";
            if (i == pos && i == lim) marker = "P=L";
            else if (i == pos)        marker = " P ";
            else if (i == lim)        marker = " L ";
            else                      marker = "   ";
            pointers.append(marker).append(" ");
        }
        // 如果 pos == cap，P 打在末尾
        if (pos == cap) pointers.append(" P");
        if (lim == cap) pointers.append("(L=C)");
        System.out.println(pointers);

        System.out.println("    capacity=" + cap + "  limit=" + lim
                + "  position=" + pos + "  remaining=" + buf.remaining());
        System.out.println();
    }
}

