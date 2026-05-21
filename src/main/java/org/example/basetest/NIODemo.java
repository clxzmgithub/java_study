package org.example.basetest;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

/**
 * ============================================================
 * Java IO / NIO 全体系深度演示
 * ============================================================
 *
 * 本文件覆盖本次对话全部核心知识点：
 *
 * Part1  - 五种 IO 模型对比（理论）
 * Part2  - BIO 的根本问题：一连接一线程
 * Part3  - BufferedInputStream 为什么能提速
 * Part4  - Buffer 三指针状态机（flip/clear/compact）
 * Part5  - Channel 类型体系与 FileChannel 不能非阻塞的原因
 * Part6  - Selector + epoll 工作原理
 * Part7  - SelectionKey 四种事件详解（含 OP_WRITE 陷阱）
 * Part8  - 完整 NIO Server（Reactor 单线程模式）
 * Part9  - transferTo 从 Java 到内核的完整调用链
 * Part10 - 内核缓冲区满 & 网络拥塞导致 transferTo 传不完
 * Part11 - 小文件零拷贝反而慢的原因分析
 * Part12 - Kafka / Nginx 案例：零拷贝的实际应用
 * Part13 - 常见误区 & 选型指南
 *
 * 运行方式：执行 main() 方法，按提示查看各部分输出
 * ============================================================
 */
public class NIODemo {

    public static void main(String[] args) throws Exception {
        System.out.println("============================================================");
        System.out.println("  Java IO / NIO 全体系深度演示");
        System.out.println("============================================================");
        System.out.println();

        Part1_FiveIOModels.explain();
        Part2_BIOProblem.explain();
        Part3_BufferedInputStreamSpeedup.explain();
        Part4_BufferStateMachine.demonstrate();
        Part5_ChannelTypes.explain();
        Part6_SelectorEpoll.explain();
        Part7_SelectionKeyEvents.explain();
        Part8_NIOServerExplained.explain();
        Part9_TransferToCallChain.explain();
        Part10_BufferFullAndCongestion.explain();
        Part11_SmallFileSlower.explain();
        Part12_KafkaNginxCases.explain();
        Part13_MistakesAndSelection.explain();

        System.out.println("============================================================");
        System.out.println("  全部演示完毕");
        System.out.println("============================================================");
    }

    static void printSeparator() {
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
    }
}

// ====================================================================
// 第一部分：五种 IO 模型（操作系统基础，后面所有内容的地基）
// ====================================================================
class Part1_FiveIOModels {

    static void explain() {
        System.out.println("【第一部分：五种 IO 模型】");
        System.out.println();
        System.out.println("理解 IO 模型是理解 BIO/NIO/AIO 的前提。");
        System.out.println("所有 IO 操作都要经历两个阶段：");
        System.out.println("  阶段1：等待数据就绪（数据从硬件到内核缓冲区）");
        System.out.println("  阶段2：数据拷贝（内核缓冲区 → 用户空间）");
        System.out.println();

        System.out.println("模型① 阻塞 IO（BIO）← Java 传统 IO / BIO 服务器");
        System.out.println();
        System.out.println("  用户进程             内核");
        System.out.println("  ─────────            ────────────────────────");
        System.out.println("  read()  ──syscall──► 等待数据（阻塞中...）");
        System.out.println("  （线程挂起）             数据就绪，拷贝到用户空间");
        System.out.println("  read() 返回 ◄──────  返回");
        System.out.println("  特点：两个阶段都阻塞，线程全程等待");
        System.out.println("  类比：站在窗口一直等，饭好了才离开");
        System.out.println();

        System.out.println("模型② 非阻塞 IO（NIO 轮询，注意不是 Java NIO！）");
        System.out.println();
        System.out.println("  用户进程             内核");
        System.out.println("  read()  ──────────► 没好，返回 EAGAIN");
        System.out.println("  read()  ──────────► 没好，返回 EAGAIN");
        System.out.println("  read()  ──────────► 就绪！拷贝数据，返回");
        System.out.println("  特点：阶段1不阻塞，但 CPU 一直轮询（忙等，浪费）");
        System.out.println("  类比：每隔一秒去问「好了吗」，没好就走，一直问");
        System.out.println();

        System.out.println("模型③ IO 多路复用（Java NIO 的 Selector 底层）← 重点！");
        System.out.println();
        System.out.println("  用户进程             内核");
        System.out.println("  epoll_wait ────────► 同时监控 N 个 fd（线程休眠）");
        System.out.println("  （CPU 去干别的）        某个 fd 就绪！唤醒线程");
        System.out.println("  select() 返回 ◄────  返回就绪 fd 列表");
        System.out.println("  read(fd_57) ───────► 拷贝数据（阶段2阻塞）");
        System.out.println("  核心优势：1个线程同时等 N 个 fd，有就绪就处理");
        System.out.println("  底层：Linux epoll / macOS kqueue / Windows IOCP");
        System.out.println("  类比：服务员拿单子，谁的菜好了叫谁，不用站窗口");
        System.out.println();

        System.out.println("模型④ 信号驱动 IO（了解即可，Java 基本不用）");
        System.out.println("  注册 SIGIO 信号处理函数，数据就绪时内核发信号通知");
        System.out.println("  阶段1不阻塞，阶段2（数据拷贝）仍是同步阻塞");
        System.out.println();

        System.out.println("模型⑤ 异步 IO（AIO）← Java AsynchronousFileChannel");
        System.out.println();
        System.out.println("  用户进程             内核");
        System.out.println("  aio_read(buf,cb) ──► 注册：读完放buf，然后回调cb");
        System.out.println("  （立刻返回，干别的）    等待就绪，自动拷贝到buf");
        System.out.println("  cb() 被调用 ◄──────  通知完成");
        System.out.println("  特点：两个阶段都不阻塞，内核全程负责");
        System.out.println("  注意：Linux aio 实现有缺陷，生产用得少");
        System.out.println();

        System.out.println("  对比表：");
        System.out.println("  ┌────────────────┬──────────────┬──────────────┐");
        System.out.println("  │  IO 模型       │ 阶段1（等待） │ 阶段2（拷贝） │");
        System.out.println("  ├────────────────┼──────────────┼──────────────┤");
        System.out.println("  │ 阻塞 IO        │ 阻塞         │ 阻塞         │");
        System.out.println("  │ 非阻塞 IO      │ 轮询（忙等） │ 阻塞         │");
        System.out.println("  │ IO 多路复用    │ 阻塞（休眠） │ 阻塞         │");
        System.out.println("  │ 信号驱动 IO    │ 非阻塞       │ 阻塞         │");
        System.out.println("  │ 异步 IO        │ 非阻塞       │ 非阻塞       │");
        System.out.println("  └────────────────┴──────────────┴──────────────┘");
        System.out.println();
        System.out.println("  ★ IO 多路复用 vs BIO 阻塞的本质区别：");
        System.out.println("    BIO：1个线程等1个fd（其余fd无人照料）");
        System.out.println("    多路复用：1个线程等N个fd（谁就绪处理谁）");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第二部分：BIO 的根本问题——一个连接一个线程
// ====================================================================
class Part2_BIOProblem {

    static void explain() {
        System.out.println("【第二部分：BIO 的根本问题——一连接一线程】");
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

// ====================================================================
// 第三部分：BufferedInputStream 为什么能大幅提速
// ====================================================================
class Part3_BufferedInputStreamSpeedup {

    static void explain() throws Exception {
        System.out.println("【第三部分：BufferedInputStream 为什么能大幅提速】");
        System.out.println();
        System.out.println("核心原理：减少系统调用次数");
        System.out.println();
        System.out.println("  没有 BufferedInputStream，用 byte[1] 读 1MB：");
        System.out.println("    每次 fis.read() → 1次 read() 系统调用");
        System.out.println("    1MB = 1,048,576 字节 → 1,048,576 次 syscall");
        System.out.println("    每次 syscall 约 100~200ns → 浪费约 100~200ms");
        System.out.println();
        System.out.println("  加了 BufferedInputStream（默认 8KB 缓冲）：");
        System.out.println("    每次内部批量读 8192 字节 → 1次 read() 系统调用");
        System.out.println("    1MB / 8KB = 128 次 syscall → 系统调用减少 8192 倍！");
        System.out.println();
        System.out.println("装饰器模式（Decorator Pattern）：");
        System.out.println("  // 每包一层，加一个能力");
        System.out.println("  InputStream raw      = new FileInputStream(\"data.bin\");");
        System.out.println("  InputStream buffered = new BufferedInputStream(raw);   // +缓冲");
        System.out.println("  DataInputStream data = new DataInputStream(buffered);  // +读基本类型");
        System.out.println();
        System.out.println("  实际写法（一行）：");
        System.out.println("  DataInputStream dis = new DataInputStream(");
        System.out.println("      new BufferedInputStream(new FileInputStream(\"data.bin\")));");
        System.out.println();
        System.out.println("  类比：");
        System.out.println("    FileInputStream   = 快递小哥，每次只能送1件");
        System.out.println("    BufferedInputStream = 货车，攒够100件再一起送");
        System.out.println();

        // 实际演示
        System.out.println("实际性能演示（4MB 文件，8KB buffer）：");
        Path tmpFile = Files.createTempFile("bio_test_", ".dat");
        byte[] data = new byte[4 * 1024 * 1024];
        for (int i = 0; i < data.length; i++) data[i] = (byte)(i % 256);
        Files.write(tmpFile, data);

        long start = System.currentTimeMillis();
        try (BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(tmpFile.toFile()))) {
            byte[] buf = new byte[8192];
            while (bis.read(buf) != -1) {}
        }
        long withBuf = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        try (FileInputStream fis = new FileInputStream(tmpFile.toFile())) {
            byte[] buf = new byte[8192];
            while (fis.read(buf) != -1) {}
        }
        long withoutBuf = System.currentTimeMillis() - start;

        System.out.println("  BufferedInputStream：" + withBuf + "ms");
        System.out.println("  FileInputStream 直接读（同 8KB 数组）：" + withoutBuf + "ms");
        System.out.println("  （差距在字节级 read() 时更明显；大数组读时 JVM 内部也有缓冲优化）");
        System.out.println("  ★ 结论：永远给 FileInputStream 套 BufferedInputStream");
        System.out.println("    除非你自己传入大的 byte[] 数组");
        Files.deleteIfExists(tmpFile);
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第四部分：Buffer 三指针状态机（flip / clear / compact）
// ====================================================================
class Part4_BufferStateMachine {

    static void demonstrate() {
        System.out.println("【第四部分：Buffer 三指针状态机】");
        System.out.println();
        System.out.println("ByteBuffer 四个关键字段：");
        System.out.println("  capacity  = 总容量，创建后不变");
        System.out.println("  limit     = 有效上界（写模式=capacity，读模式=已写入量）");
        System.out.println("  position  = 当前读/写游标");
        System.out.println("  mark      = 标记位（reset() 跳回这里）");
        System.out.println();

        // ─── 演示 1：写 → flip → 读 → clear ───
        System.out.println("演示 1：写 → flip → 读 → clear");
        System.out.println("─────────────────────────────────");
        ByteBuffer buf = ByteBuffer.allocate(10);
        printState("刚分配 allocate(10)", buf);

        buf.put((byte)'H');
        buf.put((byte)'i');
        buf.put((byte)'!');
        printState("put 3字节('H','i','!')后", buf);

        System.out.println("  → flip()：limit = position(3)，position = 0（切换为读模式）");
        buf.flip();
        printState("flip() 后", buf);

        byte b1 = buf.get();
        byte b2 = buf.get();
        System.out.println("  → get() 两次，读到：'" + (char)b1 + "','" + (char)b2 + "'");
        printState("get() 两次后（'!' 还没读）", buf);

        System.out.println("  → clear()：position=0，limit=capacity（清空，但数据还在数组里！）");
        System.out.println("    ⚠️ clear() 不是真正清空数据，只是重置指针，未读的 '!' 被丢弃");
        buf.clear();
        printState("clear() 后", buf);
        System.out.println();

        // ─── 演示 2：compact ───
        System.out.println("演示 2：compact() —— 保留未读数据（处理粘包必备）");
        System.out.println("─────────────────────────────────────────────────");
        System.out.println("  场景：Buffer 里有「消息A完整数据 + 消息B的半包」");
        System.out.println("        处理完消息A后，消息B半包不能丢，不能用 clear()");
        ByteBuffer buf2 = ByteBuffer.allocate(10);
        buf2.put(new byte[]{'A', 'B', 'C', 'D', 'E'});
        buf2.flip();
        printState("写入5字节后 flip", buf2);

        buf2.get(); buf2.get(); buf2.get(); // 读消息A（3字节）
        printState("读了3字节（消息A处理完，D/E是消息B半包）", buf2);

        System.out.println("  → compact()：把[position,limit)的未读数据移到头部");
        System.out.println("    执行后：D,E 在头部，position=2，limit=capacity=10");
        System.out.println("    可以继续往后面写新收到的数据，下次处理完整的消息B");
        buf2.compact();
        printState("compact() 后", buf2);
        System.out.println();

        // ─── 演示 3：三者对比 ───
        System.out.println("演示 3：flip / clear / compact 对比");
        System.out.println("─────────────────────────────────────");
        System.out.println("  flip()    → 写模式 → 读模式");
        System.out.println("              limit=position, position=0");
        System.out.println("              使用：写完数据，准备发给 channel");
        System.out.println();
        System.out.println("  clear()   → 任意 → 写模式（丢弃未读数据！）");
        System.out.println("              position=0, limit=capacity");
        System.out.println("              使用：读完所有数据，buffer 准备复用");
        System.out.println();
        System.out.println("  compact() → 读模式 → 写模式（保留未读数据）");
        System.out.println("              未读数据移到头部，position=剩余量，limit=capacity");
        System.out.println("              使用：处理粘包，半包数据不能丢");
        System.out.println();

        // ─── 演示 4：Heap vs Direct ───
        System.out.println("演示 4：Heap Buffer vs Direct Buffer");
        System.out.println("─────────────────────────────────────");
        System.out.println("  ByteBuffer.allocate(1024)       → 堆内存（JVM 管理）");
        System.out.println("    IO 时 JVM 先把数据复制到直接内存再发起 syscall（多一次拷贝）");
        System.out.println("    原因：GC 会移动堆对象，DMA 不能用会被移动的地址");
        System.out.println();
        System.out.println("  ByteBuffer.allocateDirect(1024) → 堆外内存（OS malloc）");
        System.out.println("    内核直接 DMA 到这块内存，省一次拷贝");
        System.out.println("    缺点：分配慢，需要等 GC 触发 Cleaner 释放（或手动 unsafe）");
        System.out.println();
        System.out.println("  Netty ByteBuf：直接内存 + 内存池（PooledByteBufAllocator）");
        System.out.println("    → 避免频繁 malloc/free，减少 GC 压力，性能更好");
        System.out.println();

        // ─── 演示 5：最常见 Bug ───
        System.out.println("演示 5：最常见的 Bug —— 写完忘了 flip()");
        System.out.println("────────────────────────────────────────");
        ByteBuffer bugBuf = ByteBuffer.allocate(10);
        bugBuf.put("hello".getBytes());
        // 模拟忘了 flip
        System.out.println("  写入 'hello'，忘了 flip()，直接 get()：");
        System.out.println("    position=" + bugBuf.position()
                + ", limit=" + bugBuf.limit()
                + ", remaining=" + bugBuf.remaining());
        System.out.println("    remaining=5 表示还能写5个，但此时你误以为可以读5个！");
        System.out.println("    get() 读到的全是 0（position 到 limit 之间是空的）");
        System.out.println();
        System.out.println("  正确写法：");
        System.out.println("    bugBuf.flip();                        // ← 别忘了！");
        System.out.println("    byte[] data = new byte[bugBuf.remaining()];");
        System.out.println("    bugBuf.get(data);");
        System.out.println("    System.out.println(new String(data)); // 输出 hello");
        System.out.println();
        NIODemo.printSeparator();
    }

    static void printState(String label, ByteBuffer buf) {
        System.out.println("  [" + label + "]");
        System.out.println("    capacity=" + buf.capacity()
                + "  limit=" + buf.limit()
                + "  position=" + buf.position()
                + "  remaining=" + buf.remaining());
    }
}

// ====================================================================
// 第五部分：Channel 类型体系
// ====================================================================
class Part5_ChannelTypes {

    static void explain() {
        System.out.println("【第五部分：Channel 类型体系】");
        System.out.println();
        System.out.println("Channel vs Stream 根本区别：");
        System.out.println("  ┌──────────────┬──────────────────┬─────────────────────┐");
        System.out.println("  │              │ Stream（传统IO）  │ Channel（NIO）       │");
        System.out.println("  ├──────────────┼──────────────────┼─────────────────────┤");
        System.out.println("  │ 方向         │ 单向             │ 双向（可读可写）     │");
        System.out.println("  │ 数据单位     │ 字节为单位       │ 必须配合 Buffer      │");
        System.out.println("  │ 是否可非阻塞  │ 不支持           │ 支持（部分）         │");
        System.out.println("  │ 是否可多路复用│ 不支持           │ 支持（Selectable）  │");
        System.out.println("  └──────────────┴──────────────────┴─────────────────────┘");
        System.out.println();
        System.out.println("Channel 类型体系：");
        System.out.println("  Channel（接口）");
        System.out.println("    ├── FileChannel                 文件读写");
        System.out.println("    │     ├── read(ByteBuffer)");
        System.out.println("    │     ├── write(ByteBuffer)");
        System.out.println("    │     ├── transferTo(pos,n,ch)  ← 零拷贝！");
        System.out.println("    │     └── map(mode,pos,size)    ← 内存映射");
        System.out.println("    │     ⚠️ 不支持非阻塞！不能注册 Selector");
        System.out.println("    │");
        System.out.println("    ├── ServerSocketChannel         监听端口（接受连接）");
        System.out.println("    │     ├── bind(address)");
        System.out.println("    │     ├── accept() → SocketChannel");
        System.out.println("    │     └── register(sel, OP_ACCEPT)");
        System.out.println("    │");
        System.out.println("    ├── SocketChannel               TCP 连接");
        System.out.println("    │     ├── connect(address)");
        System.out.println("    │     ├── read(ByteBuffer)");
        System.out.println("    │     ├── write(ByteBuffer)");
        System.out.println("    │     └── register(sel, OP_READ | OP_WRITE)");
        System.out.println("    │");
        System.out.println("    └── DatagramChannel             UDP");
        System.out.println();
        System.out.println("★ FileChannel 为什么不能非阻塞？");
        System.out.println("  Linux 内核设计：普通文件 fd 对 epoll「永远是就绪的」");
        System.out.println("  因为磁盘数据最终都能读到，epoll 不支持监控普通文件 fd");
        System.out.println("  所以 FileChannel 不能 configureBlocking(false)");
        System.out.println("         不能 register(selector, ...)");
        System.out.println("  想要异步文件 IO → AsynchronousFileChannel（AIO）");
        System.out.println("                  或把 FileChannel 操作扔到独立线程池");
        System.out.println();
        System.out.println("FileChannel 创建方式：");
        System.out.println("  // 推荐：NIO Files");
        System.out.println("  FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);");
        System.out.println("  // 传统 IO 流获取");
        System.out.println("  FileChannel fc = new FileInputStream(file).getChannel();");
        System.out.println("  // 注意：关闭 Channel 不会自动关闭 Stream，反之亦然");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第六部分：Selector + epoll 工作原理
// ====================================================================
class Part6_SelectorEpoll {

    static void explain() {
        System.out.println("【第六部分：Selector + epoll 工作原理】");
        System.out.println();
        System.out.println("核心问题：selector.select() 为什么会阻塞？");
        System.out.println();
        System.out.println("  你调用 selector.select()");
        System.out.println("      ↓");
        System.out.println("  JVM 调用 Linux epoll_wait(epfd, events, maxevents, timeout)");
        System.out.println("      ↓");
        System.out.println("  内核检查所有注册 fd，没有就绪 → 线程挂起（进等待队列）");
        System.out.println("      ↓");
        System.out.println("  CPU 去干别的事（这就是单线程能管N个连接的关键！）");
        System.out.println("      ↓");
        System.out.println("  某个 fd 就绪（网卡收到数据 → 硬件中断触发）");
        System.out.println("      ↓");
        System.out.println("  内核唤醒线程，返回就绪 fd 列表");
        System.out.println("      ↓");
        System.out.println("  selector.select() 返回，值 = 就绪 fd 数量");
        System.out.println();
        System.out.println("  ★ 这里的「阻塞」= 线程真正休眠，CPU 利用率接近 0%");
        System.out.println("    不是「傻等循环忙等」（那是非阻塞 IO 的轮询模式）");
        System.out.println("    和 Thread.sleep() 本质相同，唤醒条件不同而已");
        System.out.println();
        System.out.println("epoll vs select/poll（为什么 epoll 支持百万并发）：");
        System.out.println();
        System.out.println("  select/poll（O(n)）：");
        System.out.println("    每次调用，把所有 fd 列表从用户空间拷贝到内核");
        System.out.println("    内核逐个遍历，问每个 fd「你就绪了吗」");
        System.out.println("    100万连接 → 每次遍历 100万 fd → 不可接受");
        System.out.println();
        System.out.println("  epoll（O(1)）：");
        System.out.println("    epoll_create → 内核建事件表（红黑树 + 就绪链表）");
        System.out.println("    epoll_ctl(ADD) → 把 fd 加入红黑树，注册回调函数");
        System.out.println("    fd 就绪 → 硬件中断 → 回调自动把 fd 加入就绪链表");
        System.out.println("    epoll_wait → 只看就绪链表，不遍历所有 fd！");
        System.out.println();
        System.out.println("    100万连接中只有100个活跃：");
        System.out.println("      select：每次处理 100万 fd");
        System.out.println("      epoll：每次只处理 100 个 fd（精准！）");
        System.out.println();
        System.out.println("Java Selector 底层对应：");
        System.out.println("  Selector.open()              → epoll_create1(0)");
        System.out.println("  channel.register(sel, ops)   → epoll_ctl(ADD, fd, event)");
        System.out.println("  selector.select()            → epoll_wait(...)");
        System.out.println("  selector.wakeup()            → 向内部 pipe 写1字节让 epoll 返回");
        System.out.println("                                 macOS: kqueue, Windows: IOCP");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第七部分：SelectionKey 四种事件详解（含 OP_WRITE 陷阱）
// ====================================================================
class Part7_SelectionKeyEvents {

    static void explain() {
        System.out.println("【第七部分：SelectionKey 四种事件详解】");
        System.out.println();
        System.out.println("  OP_ACCEPT  = 16  ServerSocketChannel 专用");
        System.out.println("    触发时机：TCP 三次握手完成，有新连接到来");
        System.out.println("    响应：serverChannel.accept() 得到 SocketChannel");
        System.out.println();
        System.out.println("  OP_CONNECT = 8   客户端 SocketChannel 专用");
        System.out.println("    触发时机：非阻塞 connect() 的握手完成");
        System.out.println("    响应：必须调用 channel.finishConnect()");
        System.out.println("    不调用后果：channel 不可用，后续 read/write 报错");
        System.out.println();
        System.out.println("  OP_READ    = 1   最常用");
        System.out.println("    触发时机：Socket 接收缓冲区有数据 / 对端关闭连接");
        System.out.println("    响应：channel.read(buffer)");
        System.out.println("    ⚠️ 返回 -1 = 对端关闭，必须 key.cancel() + channel.close()");
        System.out.println("    ⚠️ 返回 0  = 非阻塞模式下没数据，稍后再来");
        System.out.println();
        System.out.println("  OP_WRITE   = 4   最容易误用！");
        System.out.println("    含义：Socket 发送缓冲区有空间，可以写数据");
        System.out.println();
        System.out.println("    ⚠️ 陷阱：发送缓冲区「几乎一直有空间」");
        System.out.println("       一直注册 OP_WRITE → select() 一直立刻返回 → CPU 100%！");
        System.out.println();
        System.out.println("    ✅ 正确用法：");
        System.out.println("       Step1：正常直接写 channel.write(buf)，不注册 OP_WRITE");
        System.out.println("       Step2：如果 buf.hasRemaining()（没写完，发送缓冲区满了）");
        System.out.println("              → 注册 OP_WRITE，等缓冲区有空间的通知");
        System.out.println("       Step3：OP_WRITE 触发，继续写剩余数据");
        System.out.println("       Step4：写完，立刻取消 OP_WRITE！");
        System.out.println("       代码：");
        System.out.println("         // 增加 OP_WRITE");
        System.out.println("         key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);");
        System.out.println("         // 取消 OP_WRITE（写完后）");
        System.out.println("         key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);");
        System.out.println();
        System.out.println("SelectionKey 重要 API：");
        System.out.println("  key.channel()      → 取回关联的 Channel");
        System.out.println("  key.attachment()   → 取出附加对象（如 Session、ByteBuffer）");
        System.out.println("  key.attach(obj)    → 绑定业务对象（NIO 编程传递状态的标准方式）");
        System.out.println("  key.cancel()       → 取消注册，不再监控");
        System.out.println("  key.interestOps()  → 当前注册的感兴趣事件");
        System.out.println("  key.readyOps()     → 当前已就绪的事件");
        System.out.println();
        System.out.println("  ⚠️ selectedKeys() 必须手动 remove()：");
        System.out.println("     Selector 不会自动清理，不 remove → 下次还会触发 → 重复处理");
        System.out.println("     标准写法：");
        System.out.println("       Iterator<SelectionKey> iter = selector.selectedKeys().iterator();");
        System.out.println("       while (iter.hasNext()) {");
        System.out.println("           SelectionKey key = iter.next();");
        System.out.println("           iter.remove(); // ← 必须！");
        System.out.println("           // 处理 key ...");
        System.out.println("       }");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第八部分：完整 NIO Server 代码讲解（Reactor 单线程模式）
// ====================================================================
class Part8_NIOServerExplained {

    static void explain() {
        System.out.println("【第八部分：完整 NIO Server（Reactor 单线程模式）】");
        System.out.println();
        System.out.println("完整代码（逐行注释版）：");
        System.out.println();
        System.out.println("  // ① 创建 Selector（底层 epoll_create1）");
        System.out.println("  Selector selector = Selector.open();");
        System.out.println();
        System.out.println("  // ② 创建 ServerSocketChannel，绑定端口");
        System.out.println("  ServerSocketChannel server = ServerSocketChannel.open();");
        System.out.println("  server.bind(new InetSocketAddress(8080));");
        System.out.println("  server.configureBlocking(false); // ★ 必须设非阻塞");
        System.out.println();
        System.out.println("  // ③ 注册 OP_ACCEPT（底层 epoll_ctl ADD）");
        System.out.println("  server.register(selector, SelectionKey.OP_ACCEPT);");
        System.out.println();
        System.out.println("  // ④ 事件循环（Reactor 核心）");
        System.out.println("  while (true) {");
        System.out.println("      selector.select(); // 底层 epoll_wait，休眠等待事件");
        System.out.println("      Iterator<SelectionKey> iter = selector.selectedKeys().iterator();");
        System.out.println("      while (iter.hasNext()) {");
        System.out.println("          SelectionKey key = iter.next();");
        System.out.println("          iter.remove(); // ★ 必须手动移除！");
        System.out.println();
        System.out.println("          if (key.isAcceptable()) {");
        System.out.println("              // 新连接");
        System.out.println("              SocketChannel client = server.accept();");
        System.out.println("              client.configureBlocking(false); // ★ 也要非阻塞");
        System.out.println("              // 附加 ByteBuffer 作为这个连接的读缓冲");
        System.out.println("              client.register(selector, SelectionKey.OP_READ,");
        System.out.println("                  ByteBuffer.allocate(1024));");
        System.out.println();
        System.out.println("          } else if (key.isReadable()) {");
        System.out.println("              SocketChannel client = (SocketChannel) key.channel();");
        System.out.println("              ByteBuffer buf = (ByteBuffer) key.attachment();");
        System.out.println("              int n = client.read(buf);");
        System.out.println("              if (n == -1) {");
        System.out.println("                  key.cancel(); client.close(); // 对端关闭");
        System.out.println("              } else if (n > 0) {");
        System.out.println("                  buf.flip();");
        System.out.println("                  ByteBuffer response = ByteBuffer.wrap(处理(buf));");
        System.out.println("                  int written = client.write(response);");
        System.out.println("                  if (response.hasRemaining()) {");
        System.out.println("                      // 没写完，注册 OP_WRITE 等待通知");
        System.out.println("                      key.attach(response);");
        System.out.println("                      key.interestOps(OP_READ | OP_WRITE);");
        System.out.println("                  }");
        System.out.println("                  buf.clear();");
        System.out.println("              }");
        System.out.println();
        System.out.println("          } else if (key.isWritable()) {");
        System.out.println("              SocketChannel client = (SocketChannel) key.channel();");
        System.out.println("              ByteBuffer buf = (ByteBuffer) key.attachment();");
        System.out.println("              client.write(buf);");
        System.out.println("              if (!buf.hasRemaining()) {");
        System.out.println("                  // 写完，取消 OP_WRITE！否则 CPU 100%");
        System.out.println("                  key.interestOps(SelectionKey.OP_READ);");
        System.out.println("                  key.attach(ByteBuffer.allocate(1024));");
        System.out.println("              }");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("这就是 Reactor 单线程模式：1个线程 + 1个 Selector 管理所有连接");
        System.out.println();
        System.out.println("Netty 是对这个模式的工业级封装：");
        System.out.println("  Boss EventLoopGroup（1线程）  → 只负责 accept");
        System.out.println("  Worker EventLoopGroup（CPU×2）→ 负责 read/write/业务");
        System.out.println("  每个 Channel 绑定一个 EventLoop，整个生命周期不换");
        System.out.println("  → 无锁化：同一 Channel 的事件总在同一线程处理");
        System.out.println();
        System.out.println("Netty 解决的原生 NIO 七宗罪：");
        System.out.println("  ① Buffer flip/clear 容易搞错  → ByteBuf 双指针，无需 flip");
        System.out.println("  ② 粘包/拆包需自己处理         → 内置多种 FrameDecoder");
        System.out.println("  ③ JDK Selector epoll 空轮询 Bug→ 检测重建 Selector");
        System.out.println("  ④ 异常处理繁琐                → Pipeline 统一处理");
        System.out.println("  ⑤ 无连接池                    → Channel 对象池");
        System.out.println("  ⑥ 无编解码                    → 内置 HTTP/WebSocket/自定义");
        System.out.println("  ⑦ 无心跳机制                  → IdleStateHandler");
        System.out.println();
        NIODemo.printSeparator();
    }

    /** 可选：后台启动一个真实的 NIO Echo Server */
    static Thread startAsync(int port) {
        Thread t = new Thread(() -> {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel server = ServerSocketChannel.open();
                server.bind(new InetSocketAddress(port));
                server.configureBlocking(false);
                server.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("  [NIO EchoServer] 启动在端口 " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    if (selector.select(500) == 0) continue;
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        try {
                            if (key.isAcceptable()) {
                                SocketChannel client = server.accept();
                                if (client != null) {
                                    client.configureBlocking(false);
                                    client.register(selector, SelectionKey.OP_READ,
                                            ByteBuffer.allocate(1024));
                                }
                            } else if (key.isReadable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                ByteBuffer buf = (ByteBuffer) key.attachment();
                                int n = client.read(buf);
                                if (n == -1) { key.cancel(); client.close(); }
                                else if (n > 0) { buf.flip(); client.write(buf); buf.clear(); }
                            }
                        } catch (IOException e) { key.cancel(); }
                    }
                }
                server.close(); selector.close();
            } catch (IOException e) {
                System.err.println("  [NIO EchoServer] 启动失败：" + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}

// ====================================================================
// 第九部分：transferTo 从 Java 到内核的完整调用链
// ====================================================================
class Part9_TransferToCallChain {

    static void explain() {
        System.out.println("【第九部分：transferTo() 完整调用链 Java→JVM→OS→内核】");
        System.out.println();

        System.out.println("第一层：Java 应用层（你写的代码）");
        System.out.println("  srcChannel.transferTo(0, fileSize, dstChannel);");
        System.out.println("  FileChannel 是抽象类，实现在 sun.nio.ch.FileChannelImpl");
        System.out.println();

        System.out.println("第二层：JDK Java 层（FileChannelImpl.java）");
        System.out.println("  transferTo() {");
        System.out.println("      // 优先尝试零拷贝路径");
        System.out.println("      long n = transferToDirectly(position, count, target);");
        System.out.println("      if (n >= 0) return n;           // 成功，直接返回");
        System.out.println("      // 失败退化：ByteBuffer 中转（传统路径）");
        System.out.println("      return transferToTrustedChannel(...);");
        System.out.println("  }");
        System.out.println();
        System.out.println("  transferToDirectly() {");
        System.out.println("      // 只有目标是 SocketChannel 或 FileChannel 才走零拷贝");
        System.out.println("      return transferTo0(this.fd, position, count, targetFD);");
        System.out.println("      // transferTo0 是 native 方法（声明边界）");
        System.out.println("  }");
        System.out.println();

        System.out.println("第三层：JVM Native 层（FileChannelImpl.c，C 代码）");
        System.out.println("  Java_sun_nio_ch_FileChannelImpl_transferTo0() {");
        System.out.println("      jint srcFD = fdval(env, srcFDO); // Java FileDescriptor → int fd");
        System.out.println("      jint dstFD = fdval(env, dstFDO);");
        System.out.println();
        System.out.println("  #if defined(__linux__)              // Linux 分支");
        System.out.println("      jlong n = sendfile64(dstFD, srcFD, &offset, count);");
        System.out.println();
        System.out.println("  #elif defined(__APPLE__)            // macOS 分支");
        System.out.println("      sendfile(srcFD, dstFD, position, &numBytes, NULL, 0);");
        System.out.println("  }");
        System.out.println();

        System.out.println("第四层：glibc 系统调用包装层");
        System.out.println("  sendfile64() {");
        System.out.println("      return SYSCALL_CANCEL(sendfile, out_fd, in_fd, offset, count);");
        System.out.println("  }");
        System.out.println("  // x86-64 汇编展开：");
        System.out.println("  //   mov rax, 40    ; SYS_sendfile 系统调用号");
        System.out.println("  //   mov rdi, out_fd");
        System.out.println("  //   mov rsi, in_fd");
        System.out.println("  //   syscall        ; ← Ring3 → Ring0 特权级切换");
        System.out.println();

        System.out.println("第五层：Linux 内核（fs/sendfile.c → fs/splice.c）");
        System.out.println("  sys_sendfile64() → do_sendfile() → do_splice_direct()");
        System.out.println("  → splice_direct_to_actor()");
        System.out.println("  // 核心：在 pipe buffer 里传递 Page Cache 页指针");
        System.out.println("  // 数据的物理内存页不移动，只传递指针！");
        System.out.println("  → DMA 控制器：Page Cache → 网卡缓冲区（CPU 不参与）");
        System.out.println();

        System.out.println("完整调用链一图流：");
        System.out.println("  srcChannel.transferTo()        [Java 应用层]");
        System.out.println("      ↓");
        System.out.println("  FileChannelImpl.transferToDirectly()   [JDK]");
        System.out.println("      ↓");
        System.out.println("  transferTo0() native 方法              [JNI 边界]");
        System.out.println("      ↓  ← Java → C");
        System.out.println("  sendfile64(dstFD, srcFD, offset, count)[glibc]");
        System.out.println("      ↓");
        System.out.println("  syscall 指令                           [CPU Ring3→Ring0]");
        System.out.println("      ↓  ← 进入内核");
        System.out.println("  sys_sendfile64() → do_splice_direct()  [内核 fs/splice.c]");
        System.out.println("      ↓");
        System.out.println("  DMA：Page Cache → 网卡                 [硬件层]");
        System.out.println();

        System.out.println("文件到文件 vs 文件到 Socket：");
        System.out.println("  目标 SocketChannel → sendfile()       （经典零拷贝）");
        System.out.println("  目标 FileChannel（Linux 4.5+）→ copy_file_range()（更高效）");
        System.out.println("  目标 FileChannel（旧内核）→ sendfile() 降级模拟");
        System.out.println();

        System.out.println("★ while 循环为什么不可缺少：");
        System.out.println("  每次 transferTo 能发多少取决于：");
        System.out.println("  min(发送缓冲区剩余, 接收窗口 rwnd, 拥塞窗口 cwnd)");
        System.out.println("  三者最小值决定本次传输量，可能远小于 fileSize");
        System.out.println("  去掉 while，大文件/网络场景会「静默丢失数据而不报错」");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十部分：内核缓冲区满 & 网络拥塞导致 transferTo 传不完
// ====================================================================
class Part10_BufferFullAndCongestion {

    static void explain() {
        System.out.println("【第十部分：内核缓冲区满 & 网络拥塞】");
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
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十一部分：小文件零拷贝反而慢的原因分析
// ====================================================================
class Part11_SmallFileSlower {

    static void explain() {
        System.out.println("【第十一部分：小文件零拷贝为什么反而可能更慢】");
        System.out.println();

        System.out.println("核心答案：零拷贝的收益是「省 CPU 拷贝」，");
        System.out.println("         但它自身有固定成本，文件小时固定成本占比太高。");
        System.out.println();

        System.out.println("原因一：系统调用的固定开销（最主要）");
        System.out.println();
        System.out.println("  文件大小 = 4KB（一个内存页）");
        System.out.println();
        System.out.println("  传统拷贝（8KB buffer，比文件大）：");
        System.out.println("    read()  1次 ~200ns  +  write() 1次 ~200ns  = ~400ns");
        System.out.println("    CPU 拷贝：4KB × 1次 → 微秒级");
        System.out.println();
        System.out.println("  零拷贝 transferTo：");
        System.out.println("    sendfile() 1次 ~500~1000ns（更重的系统调用）");
        System.out.println("    内核还要：建立 pipe buffer 描述符、协调 scatter/gather DMA");
        System.out.println("    总开销：~1μs");
        System.out.println();
        System.out.println("  结论：4KB 文件，零拷贝反而慢！");
        System.out.println();

        System.out.println("原因二：Page Cache 的存在让 CPU 拷贝代价极低");
        System.out.println();
        System.out.println("  CPU 拷贝的实际路径：Page Cache（内存）→ JVM 堆（内存）");
        System.out.println("  不是：磁盘 → JVM 堆（那是 DMA 拷贝）");
        System.out.println();
        System.out.println("  现代 CPU 内存带宽：DDR4-3200 约 50 GB/s");
        System.out.println("  L3 Cache 带宽：约 200~500 GB/s");
        System.out.println("  拷贝 4KB 数据（从内存）：4KB ÷ 50GB/s ≈ 80ns");
        System.out.println("  而一次 syscall 上下文切换：100~300ns");
        System.out.println();
        System.out.println("  小文件数据可能全在 CPU L1/L2 缓存里，拷贝接近零代价！");
        System.out.println("  而 syscall 的固定开销反而更显著");
        System.out.println();

        System.out.println("原因三：JIT + CPU 预取让传统拷贝循环极快");
        System.out.println();
        System.out.println("  while ((len = fis.read(buf)) != -1) { fos.write(buf, 0, len); }");
        System.out.println("  这段代码 JIT 编译后生成极紧凑的机器码");
        System.out.println("  CPU 预取机制（Prefetcher）提前把数据载入 L1/L2 缓存");
        System.out.println("  小文件场景整个 buffer 可能完全命中 L1 缓存");
        System.out.println();
        System.out.println("  而 transferTo → JNI → C → syscall 每次有固定框架开销");
        System.out.println();

        System.out.println("文件大小 vs 哪种方式更快（近似规律）：");
        System.out.println("  < 4KB        传统拷贝 ≈ 或 > 零拷贝   syscall 固定开销主导");
        System.out.println("  4KB ~ 64KB   差距不明显，互有胜负");
        System.out.println("  64KB ~ 1MB   零拷贝开始有优势");
        System.out.println("  > 1MB        零拷贝明显更快");
        System.out.println("  网络传输场景  零拷贝碾压（还少了用户态往返）");
        System.out.println();

        System.out.println("零拷贝的真正价值：高吞吐量场景下释放 CPU");
        System.out.println();
        System.out.println("  场景：Nginx 向 10000 个客户端各发 1KB 的 favicon.ico");
        System.out.println();
        System.out.println("  传统拷贝（10000次）：");
        System.out.println("    每次：2次 syscall，2次 CPU 拷贝");
        System.out.println("    合计：20000次 syscall，20000次 CPU 拷贝");
        System.out.println("    结果：CPU 被大量小拷贝占满，响应延迟上升");
        System.out.println();
        System.out.println("  零拷贝（10000次）：");
        System.out.println("    每次：1次 syscall，0次 CPU 拷贝");
        System.out.println("    合计：10000次 syscall，0次 CPU 拷贝");
        System.out.println("    结果：CPU 从拷贝中解放，处理更多连接");
        System.out.println();
        System.out.println("  → 零拷贝的核心价值不是「单次更快」");
        System.out.println("    而是「释放 CPU，提升系统整体吞吐量」");
        System.out.println("    和 CompletableFuture 的道理一样：");
        System.out.println("    单条链路不一定更快，但系统整体吞吐量提升了");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十二部分：Kafka / Nginx 案例——零拷贝的实际应用
// ====================================================================
class Part12_KafkaNginxCases {

    static void explain() {
        System.out.println("【第十二部分：Kafka & Nginx 案例——零拷贝的实际应用】");
        System.out.println();

        System.out.println("一、Kafka 高吞吐的秘密");
        System.out.println();
        System.out.println("  Kafka 写入（Producer → Broker）：");
        System.out.println("    1. 数据进入 Socket 接收缓冲区（内核）");
        System.out.println("    2. Broker 从 Socket 读数据（进用户空间）");
        System.out.println("    3. 写入 Page Cache（内核）← 不直接写磁盘！");
        System.out.println("    4. OS 异步顺序刷盘");
        System.out.println();
        System.out.println("  为什么顺序写这么快？");
        System.out.println("    机械磁盘顺序写：~600 MB/s");
        System.out.println("    机械磁盘随机写：~100 KB/s （差了 6000 倍！）");
        System.out.println("    Kafka Log 文件只追加写，永远是顺序写");
        System.out.println();
        System.out.println("  Kafka 发送（Broker → Consumer）：");
        System.out.println("    传统做法：磁盘→PageCache(DMA)→用户空间(CPU拷贝)→Socket(CPU拷贝)→网卡(DMA)");
        System.out.println("             2次 CPU 拷贝 + 4次上下文切换");
        System.out.println();
        System.out.println("    Kafka 做法（零拷贝）：");
        System.out.println("    磁盘 → Page Cache（DMA 拷贝）");
        System.out.println("    Page Cache → 网卡（sendfile，0次 CPU 拷贝）");
        System.out.println("    合计：0次 CPU 拷贝 + 2次上下文切换");
        System.out.println();
        System.out.println("  Kafka 源码（FileRecords.java）核心就一行：");
        System.out.println("    public long writeTo(GatheringByteChannel destChannel,");
        System.out.println("                        long offset, int length) {");
        System.out.println("        return channel.transferTo(offset,");
        System.out.println("                   Math.min(length, size()), destChannel);");
        System.out.println("    }");
        System.out.println();
        System.out.println("  Kafka 官方数据：同硬件，零拷贝吞吐量是传统的 6 倍！");
        System.out.println();

        System.out.println("二、Nginx 的事件驱动模型");
        System.out.println();
        System.out.println("  Nginx vs Apache 根本区别：");
        System.out.println("    Apache（类 BIO）：1请求 = 1进程/线程");
        System.out.println("      1000并发 → 1000进程 → 1000×8MB = 8GB（直接崩）");
        System.out.println();
        System.out.println("    Nginx（事件驱动，类 NIO）：");
        System.out.println("      Worker 进程数 = CPU 核数（通常4~8个）");
        System.out.println("      每个 Worker 一个事件循环（类似 Selector）");
        System.out.println("      1000并发 → 4个进程处理 → 内存：4 × 几MB");
        System.out.println();
        System.out.println("  Nginx 处理一个 HTTP 静态文件请求：");
        System.out.println("    ① accept() 新连接，注册到 epoll（非阻塞）");
        System.out.println("    ② epoll_wait 返回：「这个连接有数据」");
        System.out.println("    ③ read() 读取 HTTP 请求头");
        System.out.println("    ④ 解析 URL，找到静态文件路径");
        System.out.println("    ⑤ open() 文件，得到 fd");
        System.out.println("    ⑥ sendfile(socket_fd, file_fd, ...)  ← 零拷贝！");
        System.out.println("    ⑦ 发送完毕，关闭连接");
        System.out.println("    ⑧ 回到 ② 处理下一个事件");
        System.out.println("    全程单线程，无阻塞");
        System.out.println();
        System.out.println("  为什么 Nginx 发静态资源性能远超 Java Spring MVC？");
        System.out.println();
        System.out.println("    Java Spring MVC 发静态文件：");
        System.out.println("      磁盘→PageCache(DMA)→JVM堆(CPU拷贝)→Socket缓冲区(CPU拷贝)→网卡(DMA)");
        System.out.println("      2次 CPU 拷贝 + 4次上下文切换");
        System.out.println();
        System.out.println("    Nginx 发静态文件：");
        System.out.println("      磁盘→PageCache(DMA)→网卡(sendfile，0次CPU拷贝)");
        System.out.println("      0次 CPU 拷贝 + 2次上下文切换");
        System.out.println();
        System.out.println("    → 结论：静态资源要放 CDN 或 Nginx，不要让 Java 服务直接发！");
        System.out.println();

        System.out.println("三、RocketMQ 的 MappedByteBuffer（mmap）");
        System.out.println();
        System.out.println("  CommitLog 写入：mmap 将文件映射到内存");
        System.out.println("    写数据 = 直接写内存地址 → OS 自动同步到磁盘");
        System.out.println("    省去了传统的 write() 系统调用 + 内核缓冲区拷贝");
        System.out.println();
        System.out.println("  vs Kafka 的区别：");
        System.out.println("    Kafka：读路径用 transferTo（sendfile）");
        System.out.println("    RocketMQ：写路径用 mmap（更快，适合高频写）");
        System.out.println("    读路径也用 mmap（零 CPU 拷贝地将文件数据暴露给应用）");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十三部分：常见误区 & 选型指南
// ====================================================================
class Part13_MistakesAndSelection {

    static void explain() {
        System.out.println("【第十三部分：常见误区 & 选型指南】");
        System.out.println();

        System.out.println("一、NIO/Selector 编程常见误区");
        System.out.println();
        System.out.println("  误区1：以为 NIO 一定比 BIO 快");
        System.out.println("    真相：连接数少（<100）时，BIO 代码更简单，性能差不多");
        System.out.println("         连接数多（>1000）时，NIO 才有明显优势");
        System.out.println("         本质：BIO 瓶颈是线程数，NIO 瓶颈是 CPU 处理能力");
        System.out.println();
        System.out.println("  误区2：以为 selector.select() 是忙等（CPU spin）");
        System.out.println("    真相：是真正的线程休眠（epoll_wait），CPU 利用率接近 0%");
        System.out.println();
        System.out.println("  误区3：OP_WRITE 一直注册");
        System.out.println("    真相：发送缓冲区几乎一直有空间，OP_WRITE 几乎一直就绪");
        System.out.println("         select() 一直立刻返回 → CPU 100%！");
        System.out.println("         正确：只在写不完时注册，写完立刻取消");
        System.out.println();
        System.out.println("  误区4：selectedKeys() 不 remove()");
        System.out.println("    真相：已处理的 key 不自动移除，下次 select() 还会返回");
        System.out.println("         → 同一事件重复处理");
        System.out.println();
        System.out.println("  误区5：FileChannel 以为能注册 Selector");
        System.out.println("    真相：FileChannel 不支持非阻塞模式，不能注册 Selector");
        System.out.println();
        System.out.println("  误区6：transferTo 不写 while 循环");
        System.out.println("    真相：网络场景一次可能传不完，不写 while 会静默丢数据");
        System.out.println();
        System.out.println("  误区7：Netty handler 里做阻塞操作（数据库查询）");
        System.out.println("    真相：会把 EventLoop 线程卡死，整个服务停响");
        System.out.println("         解决：ctx.executor().execute(() → { 阻塞操作 → 回写 })");
        System.out.println("         或用 eventLoop.submit() 提交到业务线程池");
        System.out.println();

        System.out.println("二、CPU 拷贝 vs DMA 拷贝（容易混淆）");
        System.out.println();
        System.out.println("  DMA 拷贝：");
        System.out.println("    执行者 = DMA 控制器（硬件芯片，独立于 CPU）");
        System.out.println("    触发条件 = 必须有硬件设备参与（磁盘、网卡...）");
        System.out.println("    路径 = 硬件设备 ↔ 内存");
        System.out.println();
        System.out.println("  CPU 拷贝：");
        System.out.println("    执行者 = CPU（用 memcpy 搬字节）");
        System.out.println("    触发条件 = 纯内存之间复制，不涉及硬件");
        System.out.println("    路径 = 内存某区域 → 内存另一区域（哪怕都在内核！）");
        System.out.println();
        System.out.println("  ★ 都在内核空间 ≠ 就是 DMA 拷贝！");
        System.out.println("    Linux 2.4 之前，Page Cache → Socket 缓冲区");
        System.out.println("    两者都在内核，但没有硬件参与，只能 CPU memcpy");
        System.out.println("    Linux 2.4+ 用 Scatter/Gather DMA：");
        System.out.println("    CPU 只写一个「去 Page Cache 哪个地址取数据」的描述符");
        System.out.println("    网卡 DMA 控制器自己去 Page Cache 取，CPU 不搬数据");
        System.out.println("    → 真正的「零 CPU 拷贝」");
        System.out.println();

        System.out.println("三、技术选型指南");
        System.out.println();
        System.out.println("  ┌─────────────────────────┬────────────────────────────────────┐");
        System.out.println("  │ 场景                    │ 推荐方案                           │");
        System.out.println("  ├─────────────────────────┼────────────────────────────────────┤");
        System.out.println("  │ 读写本地小文件           │ BufferedInputStream/OutputStream   │");
        System.out.println("  │                         │ 或 Files.readAllBytes（< 64MB）    │");
        System.out.println("  │ 大文件随机读取（GB级）   │ MappedByteBuffer（内存映射）       │");
        System.out.println("  │ 本地文件传输到网络       │ FileChannel.transferTo（零拷贝）   │");
        System.out.println("  │ 自己写 HTTP/TCP 服务     │ Netty（不要用原生NIO，太复杂）     │");
        System.out.println("  │ 微服务 RPC               │ Dubbo/gRPC（底层都是 Netty）       │");
        System.out.println("  │ 消息队列                 │ Kafka（零拷贝+顺序写）             │");
        System.out.println("  │ 高并发 Web 静态资源      │ Nginx（sendfile）                  │");
        System.out.println("  │ 高并发 Web 动态接口      │ Spring Boot（底层 Netty/Undertow）  │");
        System.out.println("  └─────────────────────────┴────────────────────────────────────┘");
        System.out.println();

        System.out.println("四、整体知识体系一张图");
        System.out.println();
        System.out.println("  操作系统内核");
        System.out.println("    │");
        System.out.println("    ├── 阻塞 IO ────────► Java BIO（InputStream/OutputStream）");
        System.out.println("    │                         └─► 传统 Tomcat、早期 Spring");
        System.out.println("    │");
        System.out.println("    ├── IO 多路复用（epoll）→ Java NIO（Selector+Channel+Buffer）");
        System.out.println("    │                         └─► Netty（工业级封装）");
        System.out.println("    │                               └─► Dubbo / gRPC / Kafka Client");
        System.out.println("    │                               └─► Undertow（新版 Tomcat）");
        System.out.println("    │");
        System.out.println("    ├── sendfile 零拷贝 ──► FileChannel.transferTo()");
        System.out.println("    │                         └─► Kafka Broker（消息转发）");
        System.out.println("    │                         └─► Nginx（静态文件）");
        System.out.println("    │");
        System.out.println("    └── mmap 内存映射 ───► MappedByteBuffer");
        System.out.println("                              └─► RocketMQ（CommitLog）");
        System.out.println("                              └─► 大文件随机读取");
        System.out.println();

        System.out.println("五、推荐学习顺序");
        System.out.println();
        System.out.println("  第1周：打好基础");
        System.out.println("    理解用户态/内核态、五种 IO 模型");
        System.out.println("    掌握 InputStream/OutputStream + BufferedInputStream");
        System.out.println("    写一个 BIO 版本的聊天室（感受它的问题）");
        System.out.println();
        System.out.println("  第2周：NIO 核心");
        System.out.println("    Buffer 三指针（flip/clear/compact）");
        System.out.println("    FileChannel 基本操作");
        System.out.println("    Selector + ServerSocketChannel + SocketChannel");
        System.out.println("    用 NIO 重写聊天室（感受差距）");
        System.out.println();
        System.out.println("  第3周：深入 NIO");
        System.out.println("    transferTo 零拷贝（调用链 + 原理）");
        System.out.println("    MappedByteBuffer 内存映射");
        System.out.println("    epoll vs select/poll 区别");
        System.out.println();
        System.out.println("  第4周：Netty 入门");
        System.out.println("    EventLoopGroup / EventLoop");
        System.out.println("    Pipeline + ChannelHandler");
        System.out.println("    ByteBuf vs ByteBuffer");
        System.out.println("    粘包/拆包：LengthFieldBasedFrameDecoder");
        System.out.println("    用 Netty 写一个 echo server");
        System.out.println();
        System.out.println("  第5周：实战");
        System.out.println("    用 Netty 写简单 HTTP 服务器");
        System.out.println("    读 Kafka 源码（FileRecords.writeTo 那几行）");
        System.out.println("    对比 Nginx sendfile on/off 的性能差距");
        System.out.println();
        NIODemo.printSeparator();
    }
}

