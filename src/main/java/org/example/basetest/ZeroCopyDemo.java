package org.example.basetest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 零拷贝（Zero-Copy）深度演示
 * <p>
 * 运行方式：直接运行 main 方法，会依次演示：
 * 1. 准备工作：理解用户态/内核态、DMA/CPU 拷贝
 * 2. 传统拷贝（BIO）的完整过程
 * 3. 零拷贝（NIO FileChannel.transferTo）
 * 4. 内存映射（MappedByteBuffer）
 * 5. 性能对比（两种方式传输同一个文件的耗时对比）
 * 6. 零拷贝的局限性
 */
public class ZeroCopyDemo {

    public static void main(String[] args) throws Exception {
        sep("第一部分：核心概念——用户态 vs 内核态，DMA vs CPU 拷贝");
        Part1_Concepts.explain();

        sep("第二部分：传统拷贝（BIO）——数据搬了几次家");
        Part2_TraditionalCopy.explain();

        sep("第三部分：零拷贝（sendfile）——绕过用户空间");
        Part3_ZeroCopy.explain();

        sep("第四部分：内存映射（mmap）——另一种零拷贝");
        Part4_MemoryMap.explain();

        sep("第五部分：性能对比——用真实传输看差距");
        Part5_Benchmark.run();

        sep("第六部分：零拷贝的局限性");
        Part6_Limitations.explain();
    }

    static void sep(String title) {
        System.out.println("\n============================================================");
        System.out.println("  " + title);
        System.out.println("============================================================\n");
    }
}

// ====================================================================
// 第一部分：核心概念讲解
// ====================================================================
class Part1_Concepts {

    static void explain() {
        System.out.println("在理解零拷贝之前，必须先搞清楚两个基础概念。");
        System.out.println("否则后面的流程图会看不懂。");
        System.out.println();

        System.out.println("【概念一：用户态 vs 内核态】");
        System.out.println();
        System.out.println("  计算机内存分两个区域：");
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │           用户空间（User Space）         │");
        System.out.println("  │                                         │");
        System.out.println("  │  • 你的 Java 程序就跑在这里             │");
        System.out.println("  │  • byte[] buffer 数组在这里             │");
        System.out.println("  │  • JVM 堆、栈都在这里                   │");
        System.out.println("  │  • 不能直接操作硬件                     │");
        System.out.println("  │                                         │");
        System.out.println("  └──────────────┬──────────────────────────┘");
        System.out.println("                 │ 每次穿越都有开销（上下文切换）");
        System.out.println("                 │ 穿越 = 系统调用（read/write/sendfile）");
        System.out.println("  ┌──────────────┴──────────────────────────┐");
        System.out.println("  │           内核空间（Kernel Space）       │");
        System.out.println("  │                                         │");
        System.out.println("  │  • 操作系统内核在这里                   │");
        System.out.println("  │  • Page Cache（磁盘数据的内存缓存）在这里│");
        System.out.println("  │  • Socket 发送缓冲区在这里              │");
        System.out.println("  │  • 可以直接操控硬件（磁盘、网卡）       │");
        System.out.println("  │                                         │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println("                 │");
        System.out.println("  ┌──────────────┴──────────────────────────┐");
        System.out.println("  │           硬件层                         │");
        System.out.println("  │  磁盘（存数据）  网卡（发/收数据）       │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  关键：每次从用户空间调用 read()/write() 等系统调用，");
        System.out.println("        都会切换到内核态，完成后再切回用户态。");
        System.out.println("        这个切换本身有时间开销（保存/恢复 CPU 寄存器等）。");
        System.out.println();

        System.out.println("【概念二：DMA 拷贝 vs CPU 拷贝】");
        System.out.println();
        System.out.println("  DMA 拷贝（Direct Memory Access，直接内存访问）：");
        System.out.println("    → 硬件控制器（磁盘控制器/网卡）自己完成数据搬运");
        System.out.println("    → CPU 不参与，不占用 CPU 时间");
        System.out.println("    → CPU 可以同时处理其他任务");
        System.out.println("    → 发生场景：磁盘→内核缓冲区，内核Socket缓冲区→网卡");
        System.out.println();
        System.out.println("  CPU 拷贝：");
        System.out.println("    → CPU 亲自把数据从地址A搬到地址B");
        System.out.println("    → CPU 在这段时间内什么别的事都干不了");
        System.out.println("    → 发生场景：内核缓冲区→用户缓冲区，用户缓冲区→内核Socket缓冲区");
        System.out.println();
        System.out.println("  ★ 零拷贝的目标：消灭 CPU 拷贝，只留 DMA 拷贝（硬件免费做）");
        System.out.println();

        System.out.println("【核心矛盾】");
        System.out.println("  传统传输中，数据从磁盘读出来，进入用户空间（你的 byte[]），");
        System.out.println("  然后立刻又发回内核 Socket 缓冲区准备发送。");
        System.out.println("  在用户空间里，数据什么都没做，就是「路过」了一下。");
        System.out.println("  但为了这次「路过」，我们付出了 2 次 CPU 拷贝 + 2 次上下文切换的代价。");
        System.out.println("  这就是零拷贝要解决的问题。");
    }
}

// ====================================================================
// 第二部分：传统拷贝详解（含逐步代码注释）
// ====================================================================
class Part2_TraditionalCopy {

    static void explain() throws Exception {
        System.out.println("【传统拷贝的完整流程】");
        System.out.println();
        System.out.println("场景：服务器收到请求，要把磁盘上的 video.mp4 发给客户端");
        System.out.println();
        System.out.println("对应 Java 代码（传统 BIO 写法）：");
        System.out.println();
        System.out.println("  FileInputStream fis = new FileInputStream(\"/data/video.mp4\");");
        System.out.println("  OutputStream out = socket.getOutputStream();");
        System.out.println("  byte[] buffer = new byte[8192];  // 用户空间的缓冲区");
        System.out.println("  int len;");
        System.out.println("  while ((len = fis.read(buffer)) != -1) {  // 读文件");
        System.out.println("      out.write(buffer, 0, len);             // 写 Socket");
        System.out.println("  }");
        System.out.println();
        System.out.println("这段代码背后发生了什么（逐步分析）：");
        System.out.println();

        System.out.println("  步骤① fis.read(buffer) 被调用");
        System.out.println("  ─────────────────────────────");
        System.out.println("  • read() 是一个系统调用");
        System.out.println("  • 调用 read() 的瞬间，程序从「用户态」切换到「内核态」");
        System.out.println("  • 这就是第 1 次上下文切换");
        System.out.println("  • 此时 CPU 要保存当前用户程序的所有状态（寄存器、程序计数器等）");
        System.out.println("    切换进内核去处理这个请求");
        System.out.println();

        System.out.println("  步骤② 内核执行 DMA 拷贝（磁盘 → 内核 Page Cache）");
        System.out.println("  ─────────────────────────────────────────────────");
        System.out.println("  • 内核收到 read 请求后，通知磁盘控制器去读数据");
        System.out.println("  • 磁盘控制器启动 DMA 传输：");
        System.out.println("    磁盘上的 video.mp4 数据 → 内核 Page Cache（内核内存中的缓冲区）");
        System.out.println("  • 这个过程 CPU 不参与，由 DMA 控制器负责");
        System.out.println("  • CPU 可以去干别的事（但实际上通常在等待，因为下一步需要 CPU）");
        System.out.println("  • 这是第 1 次拷贝（DMA 拷贝，免费）");
        System.out.println();

        System.out.println("  步骤③ CPU 拷贝（内核 Page Cache → 用户缓冲区 byte[]）");
        System.out.println("  ──────────────────────────────────────────────────────");
        System.out.println("  • DMA 把数据搬到内核 Page Cache 之后");
        System.out.println("  • CPU 亲自把数据从 Page Cache 复制到你的 byte[] buffer");
        System.out.println("  • 也就是说，video.mp4 的数据现在到了 JVM 堆里的 byte[] 里");
        System.out.println("  • 这是第 2 次拷贝（CPU 拷贝，有代价！CPU 时间被占用！）");
        System.out.println("  • 拷贝完成后，read() 返回，程序从「内核态」切回「用户态」");
        System.out.println("  • 这是第 2 次上下文切换");
        System.out.println();

        System.out.println("  步骤④ out.write(buffer, 0, len) 被调用");
        System.out.println("  ────────────────────────────────────────");
        System.out.println("  • write() 也是一个系统调用");
        System.out.println("  • 调用 write() 的瞬间，程序再次从「用户态」切换到「内核态」");
        System.out.println("  • 这是第 3 次上下文切换");
        System.out.println();

        System.out.println("  步骤⑤ CPU 拷贝（用户缓冲区 byte[] → 内核 Socket 缓冲区）");
        System.out.println("  ─────────────────────────────────────────────────────────");
        System.out.println("  • CPU 把 byte[] buffer 中的数据复制到内核的 Socket 发送缓冲区");
        System.out.println("  • Socket 缓冲区是网卡发送数据的「发射台」");
        System.out.println("  • 这是第 3 次拷贝（CPU 拷贝，有代价！）");
        System.out.println();

        System.out.println("  步骤⑥ DMA 拷贝（内核 Socket 缓冲区 → 网卡）");
        System.out.println("  ───────────────────────────────────────────");
        System.out.println("  • 网卡控制器启动 DMA，把 Socket 缓冲区里的数据发到物理网卡");
        System.out.println("  • CPU 不参与，DMA 完成后，数据真正发出去了");
        System.out.println("  • 这是第 4 次拷贝（DMA 拷贝，免费）");
        System.out.println("  • write() 返回，程序从「内核态」切回「用户态」");
        System.out.println("  • 这是第 4 次上下文切换");
        System.out.println();

        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │                   传统拷贝账单                               │");
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");
        System.out.println("  │  拷贝次数：4 次                                              │");
        System.out.println("  │    DMA 拷贝 2 次：免费（磁盘→Page Cache，Socket缓冲区→网卡）│");
        System.out.println("  │    CPU 拷贝 2 次：有代价（Page Cache→byte[]，byte[]→Socket）│");
        System.out.println("  │  上下文切换：4 次（用户态↔内核态各两次）                    │");
        System.out.println("  │  数据路径：磁盘→Page Cache→byte[]→Socket缓冲区→网卡         │");
        System.out.println("  │                          ↑                                  │");
        System.out.println("  │                  在这里什么都没做，纯路过！                  │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        System.out.println();

        // 实际演示一次传统拷贝
        demonstrateTraditionalCopy();
    }

    static void demonstrateTraditionalCopy() throws Exception {
        System.out.println("【实际演示传统拷贝的每一步（用本地文件读写模拟）】");
        System.out.println();

        // 使用 128MB 测试文件，足够体现差距，又不会太慢
        int FILE_SIZE = 1024 * 1024 * 1024; // 128MB

        Path srcFile = Files.createTempFile("traditional_src_", ".dat");
        Path dstFile = Files.createTempFile("traditional_dst_", ".dat");

        // ★ 注意：这里只用于准备文件，testData 不参与计时
        // 准备数据阶段不计入拷贝耗时
        byte[] testData = new byte[FILE_SIZE];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        Files.write(srcFile, testData);
        testData = null; // ★ 立即释放，不让它占着内存影响后续测试
        System.out.println("  准备工作：创建了 128MB 的测试文件 → " + srcFile.getFileName());
        System.out.println("  文件完整路径：" + srcFile.toAbsolutePath());
        System.out.println();

        // ★ 预热：先读一遍让文件进入 Page Cache，排除磁盘冷读的影响
        // （零拷贝测试也会受同样影响，所以两者都预热才公平）
        System.out.println("  预热：先读一遍，让文件进入 OS Page Cache...");
        try (FileInputStream warmup = new FileInputStream(srcFile.toFile())) {
            byte[] wb = new byte[65536];
            while (warmup.read(wb) != -1) {
            }
        }
        System.out.println("  预热完成，正式开始计时...");
        System.out.println();

        System.out.println("  开始传统拷贝（FileInputStream → FileOutputStream）：");
        System.out.println();

        long start = System.currentTimeMillis();

        // ★ 以下每一步都对应上面流程的某个步骤 ★

        // new FileInputStream(srcFile.toFile())
        // 作用：打开文件，获得文件描述符（fd），但不读取任何数据
        // 只是告诉 OS："我要操作这个文件"
        FileInputStream fis = new FileInputStream(srcFile.toFile());

        // new FileOutputStream(dstFile.toFile())
        // 作用：打开/创建目标文件，获得另一个文件描述符
        FileOutputStream fos = new FileOutputStream(dstFile.toFile());

        // new byte[8192]
        // 作用：在 JVM 堆（用户空间）分配 8KB 的缓冲区
        // 这就是数据的"中转站"——数据会从内核拷贝到这里，然后再拷贝回内核
        // ★ 这个 buffer 是整个传统拷贝低效的根源 ★
        byte[] buffer = new byte[8192]; // 8KB 缓冲区（用户空间）

        int len;
        int loopCount = 0;

        while ((len = fis.read(buffer)) != -1) {
            // fis.read(buffer) 做了这些事：
            //   1. 触发 read() 系统调用 → 进入内核态（上下文切换 ①）
            //   2. 内核检查 Page Cache 有没有这段数据，没有就通知磁盘控制器读
            //      磁盘控制器 DMA 传输到 Page Cache（DMA 拷贝 ①）
            //   3. CPU 把 Page Cache 的数据拷贝到 buffer（CPU 拷贝 ①，有代价！）
            //   4. 返回用户态（上下文切换 ②）
            //   返回值 len：本次实际读了多少字节（最多 buffer.length）

            fos.write(buffer, 0, len);
            // fos.write(buffer, 0, len) 做了这些事：
            //   1. 触发 write() 系统调用 → 进入内核态（上下文切换 ③）
            //   2. CPU 把 buffer 中的数据拷贝到内核的文件缓冲区（CPU 拷贝 ②，有代价！）
            //      （如果是 Socket，就是拷贝到 Socket 发送缓冲区）
            //   3. 内核安排 DMA 把缓冲区数据写到磁盘/发到网卡（DMA 拷贝 ②）
            //   4. 返回用户态（上下文切换 ④）

            loopCount++;
        }

        fis.close();
        // fis.close()：关闭文件描述符，释放资源
        // 注意：应该用 try-with-resources，这里为了演示清晰写成了显式 close

        fos.close();
        // fos.close()：刷新缓冲区（把还没写到磁盘的数据强制写入），然后关闭 fd

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("  传统拷贝完成！");
        System.out.println("    文件大小：128MB");
        System.out.println("    缓冲区大小：8192 字节（8KB）");
        System.out.println("    循环次数：" + loopCount + " 次");
        System.out.println("      → 意味着发生了约 " + loopCount + " 次 read 系统调用");
        System.out.println("      → 意味着发生了约 " + loopCount + " 次 write 系统调用");
        System.out.println("      → 意味着发生了约 " + (loopCount * 4) + " 次上下文切换（每次循环4次）");
        System.out.println("      → 意味着发生了约 " + (loopCount * 2) + " 次 CPU 拷贝");
        System.out.println("    耗时：" + elapsed + "ms");
        System.out.println();

        // ★ 不在计时范围内做数据验证（验证本身也是 IO，不能计入拷贝耗时）
        System.out.println("  （数据验证跳过，避免污染计时结果）");

        // 清理临时文件
        Files.deleteIfExists(srcFile);
        Files.deleteIfExists(dstFile);
    }
}

// ====================================================================
// 第三部分：零拷贝（FileChannel.transferTo）详解
// ====================================================================
class Part3_ZeroCopy {

    static void explain() throws Exception {
        System.out.println("【零拷贝（sendfile）的完整流程】");
        System.out.println();
        System.out.println("核心思想：数据不进入用户空间，直接在内核里从磁盘流向网卡。");
        System.out.println();
        System.out.println("对应 Java 代码（NIO 零拷贝写法）：");
        System.out.println();
        System.out.println("  FileChannel fileChannel = FileChannel.open(");
        System.out.println("      Path.of(\"/data/video.mp4\"), StandardOpenOption.READ);");
        System.out.println("  SocketChannel socketChannel = ...;  // 客户端连接");
        System.out.println("  // 一行代码，底层是 sendfile 系统调用");
        System.out.println("  fileChannel.transferTo(0, fileChannel.size(), socketChannel);");
        System.out.println();
        System.out.println("这段代码背后发生了什么（逐步分析）：");
        System.out.println();

        System.out.println("  步骤① FileChannel.open() 被调用");
        System.out.println("  ─────────────────────────────────");
        System.out.println("  • 打开文件，得到一个 FileChannel 对象");
        System.out.println("  • FileChannel 代表一个可以双向读写的文件通道");
        System.out.println("  • 与 FileInputStream 不同：FileChannel 支持随机访问、内存映射等高级特性");
        System.out.println("  • 此时没有读取任何数据，只是建立了文件描述符");
        System.out.println();

        System.out.println("  步骤② transferTo(0, size, socketChannel) 被调用");
        System.out.println("  ───────────────────────────────────────────────");
        System.out.println("  参数含义：");
        System.out.println("    position = 0        → 从文件的第 0 个字节开始");
        System.out.println("    count = size        → 传输多少字节（这里是整个文件）");
        System.out.println("    target = socketChannel → 发送到哪个 Channel（这里是 Socket）");
        System.out.println();
        System.out.println("  底层调用：");
        System.out.println("    Java 的 transferTo() 最终调用 Linux 的 sendfile() 系统调用");
        System.out.println("    程序从「用户态」切换到「内核态」（第 1 次，也是唯一一次进入内核）");
        System.out.println();

        System.out.println("  步骤③ DMA 拷贝（磁盘 → 内核 Page Cache）");
        System.out.println("  ──────────────────────────────────────────");
        System.out.println("  • 磁盘控制器通过 DMA 把文件数据读到内核 Page Cache");
        System.out.println("  • CPU 不参与，这是免费的拷贝");
        System.out.println("  • Page Cache 是内核用来缓存磁盘数据的内存区域");
        System.out.println("  • 这是第 1 次拷贝（DMA，免费）");
        System.out.println();

        System.out.println("  步骤④ 「文件描述符」传递给 Socket（现代 Linux 的特殊优化）");
        System.out.println("  ──────────────────────────────────────────────────────────");
        System.out.println("  • Linux 2.4+ 支持 scatter-gather DMA");
        System.out.println("  • 内核不需要把数据从 Page Cache 再拷贝到 Socket 缓冲区");
        System.out.println("  • 只需要把「数据在哪里、偏移量、长度」等元信息告诉网卡控制器");
        System.out.println("  • 网卡控制器直接从 Page Cache 读取数据发送");
        System.out.println("  • 连这一步 CPU 拷贝都省了！");
        System.out.println();
        System.out.println("  （Linux 2.4 以前还有一次 CPU 拷贝：Page Cache → Socket 缓冲区）");
        System.out.println("  （Linux 2.4+ 这次也省了，所以叫「真零拷贝」）");
        System.out.println();

        System.out.println("  步骤⑤ DMA 拷贝（Page Cache → 网卡）");
        System.out.println("  ─────────────────────────────────────");
        System.out.println("  • 网卡控制器通过 DMA 直接从 Page Cache 读取数据发出去");
        System.out.println("  • CPU 不参与");
        System.out.println("  • 这是第 2 次拷贝（DMA，免费）");
        System.out.println();

        System.out.println("  步骤⑥ transferTo() 返回");
        System.out.println("  ──────────────────────────");
        System.out.println("  • 数据发送完毕，从「内核态」切回「用户态」（第 2 次，最后一次）");
        System.out.println("  • 返回值：实际传输的字节数（可能小于 count，需要循环直到传完）");
        System.out.println();

        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │                   零拷贝账单                                 │");
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");
        System.out.println("  │  拷贝次数：2 次（全是 DMA，CPU 完全不参与！）                │");
        System.out.println("  │    DMA 拷贝 ①：磁盘 → 内核 Page Cache                      │");
        System.out.println("  │    DMA 拷贝 ②：Page Cache → 网卡                           │");
        System.out.println("  │  上下文切换：2 次（进入内核 1 次，返回 1 次）               │");
        System.out.println("  │  数据路径：磁盘 → Page Cache → 网卡                        │");
        System.out.println("  │  数据从未进入用户空间！                                     │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        System.out.println();

        System.out.println("  与传统拷贝对比：");
        System.out.println("  ┌────────────┬──────────────┬──────────────┐");
        System.out.println("  │            │  传统拷贝     │   零拷贝     │");
        System.out.println("  ├────────────┼──────────────┼──────────────┤");
        System.out.println("  │ CPU 拷贝   │  2 次（有代价）│  0 次（消灭）│");
        System.out.println("  │ DMA 拷贝   │  2 次（免费） │  2 次（免费）│");
        System.out.println("  │ 上下文切换 │  4 次        │  2 次        │");
        System.out.println("  │ 进入用户空间│  是          │  否          │");
        System.out.println("  └────────────┴──────────────┴──────────────┘");
        System.out.println();

        // 实际演示零拷贝
        demonstrateZeroCopy();
    }

    static void demonstrateZeroCopy() throws Exception {
        System.out.println("【实际演示零拷贝的每一步（用文件到文件模拟）】");
        System.out.println();

        // ★ 与传统拷贝完全相同的文件大小，保证对比公平
        int FILE_SIZE = 1024 * 1024 * 1024; // 128MB

        Path srcFile = Files.createTempFile("zerocopy_src_", ".dat");
        Path dstFile = Files.createTempFile("zerocopy_dst_", ".dat");

        byte[] testData = new byte[FILE_SIZE];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        Files.write(srcFile, testData);
        testData = null; // ★ 立即释放，与传统拷贝测试条件一致
        System.out.println("  准备工作：创建了 128MB 的测试文件 → " + srcFile.getFileName());

        // ★ 同样预热，保证两个测试的 Page Cache 状态一致
        System.out.println("  预热：先读一遍，让文件进入 OS Page Cache...");
        try (FileChannel warmupCh = FileChannel.open(srcFile, StandardOpenOption.READ)) {
            ByteBuffer wb = ByteBuffer.allocate(65536);
            while (warmupCh.read(wb) != -1) {
                wb.clear();
            }
        }
        System.out.println("  预热完成，正式开始计时...");
        System.out.println();

        System.out.println("  开始零拷贝（FileChannel.transferTo）：");
        System.out.println();

        long start = System.currentTimeMillis();

        // FileChannel.open() 以只读方式打开源文件
        // StandardOpenOption.READ：表示只需要读权限
        // 返回的 FileChannel 支持 transferTo 等高级 NIO 操作
        FileChannel srcChannel = FileChannel.open(srcFile, StandardOpenOption.READ);

        // FileChannel.open() 以写入方式打开目标文件
        // StandardOpenOption.WRITE：写权限
        // StandardOpenOption.CREATE：文件不存在则创建
        // StandardOpenOption.TRUNCATE_EXISTING：文件存在则清空
        FileChannel dstChannel = FileChannel.open(dstFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        // srcChannel.size()
        // 作用：获取文件的总字节数，用来知道要传多少数据
        long fileSize = srcChannel.size();
        System.out.println("  文件大小：" + fileSize + " 字节（" + fileSize / 1024 + " KB）");

        long transferred = 0;  // 已经传输了多少字节

        // 为什么用 while 循环而不是直接一次 transferTo？
        // 因为 transferTo 可能一次没传完（比如网络拥塞、内核缓冲区满等情况）
        // 返回值是「本次实际传输的字节数」，可能小于 count
        // 所以需要循环直到全部传完
        while (transferred < fileSize) {
            long n = srcChannel.transferTo(
                    transferred,           // position：从文件的哪个位置开始
                    fileSize - transferred, // count：还剩多少字节没传
                    dstChannel             // target：传到哪里
            );
            // transferTo 底层调用 OS 的 sendfile/copy_file_range 等零拷贝系统调用
            // 数据从不经过 JVM 堆
            // 注意：文件到文件用的是 copy_file_range，网络传输才是 sendfile
            transferred += n;
        }

        // srcChannel.close() / dstChannel.close()
        // 关闭 Channel，释放文件描述符
        // ★ Channel 用完必须关，否则文件描述符泄漏
        // 生产代码应该用 try-with-resources，这里显式写出来方便理解
        srcChannel.close();
        dstChannel.close();

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("  零拷贝完成！");
        System.out.println("    文件大小：1MB（1,048,576 字节）");
        System.out.println("    循环次数：1 次（通常一次就传完了）");
        System.out.println("    上下文切换：约 2 次（进入内核 + 返回）");
        System.out.println("    CPU 拷贝：0 次");
        System.out.println("    耗时：" + elapsed + "ms");
        System.out.println();

        System.out.println("  （数据验证跳过，避免污染计时结果）");

        Files.deleteIfExists(srcFile);
        Files.deleteIfExists(dstFile);
    }
}

// ====================================================================
// 第四部分：内存映射（MappedByteBuffer）详解
// ====================================================================
class Part4_MemoryMap {

    static void explain() throws Exception {
        System.out.println("【内存映射（mmap）解决的是另一个问题】");
        System.out.println();
        System.out.println("transferTo 解决的是「读文件 → 发网络」这条路径的零拷贝。");
        System.out.println("MappedByteBuffer 解决的是「在 Java 代码里高效处理大文件」的问题。");
        System.out.println();

        System.out.println("【传统读大文件的问题】");
        System.out.println();
        System.out.println("  // 传统读文件的写法");
        System.out.println("  byte[] all = Files.readAllBytes(Path.of(\"10gb_file.dat\"));");
        System.out.println("  // ← 10GB 数据全部拷贝到 JVM 堆 → OutOfMemoryError！");
        System.out.println();
        System.out.println("  // 或者分块读，但随机访问很麻烦：");
        System.out.println("  // 要读第 5GB 处的数据，必须从头 skip 5GB");
        System.out.println();

        System.out.println("【内存映射的工作方式】");
        System.out.println();
        System.out.println("  MappedByteBuffer 背后是 mmap() 系统调用。");
        System.out.println("  它把文件的某一段「映射」到进程的虚拟地址空间。");
        System.out.println("  注意：映射不等于加载！这只是建立了一个「地址对应关系」。");
        System.out.println("  当你真正访问某个地址时，OS 才把对应的数据从磁盘加载进来（按需分页）。");
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │                内存映射原理图                                │");
        System.out.println("  │                                                             │");
        System.out.println("  │  进程虚拟地址空间：                                         │");
        System.out.println("  │  ┌─────────────────────────────────┐                       │");
        System.out.println("  │  │  代码段 │ 栈 │ 堆 │[mmap区域]  │ ← 虚拟地址            │");
        System.out.println("  │  └─────────────────────────────────┘                       │");
        System.out.println("  │                           ↕  映射关系（不是拷贝）           │");
        System.out.println("  │  内核 Page Cache：                                          │");
        System.out.println("  │  ┌─────────────────────────────────┐                       │");
        System.out.println("  │  │       文件内容缓存               │ ← 实际物理内存        │");
        System.out.println("  │  └─────────────────────────────────┘                       │");
        System.out.println("  │                    ↕  按需加载                              │");
        System.out.println("  │  磁盘文件：                                                 │");
        System.out.println("  │  ┌─────────────────────────────────┐                       │");
        System.out.println("  │  │       10gb_file.dat              │                       │");
        System.out.println("  │  └─────────────────────────────────┘                       │");
        System.out.println("  │                                                             │");
        System.out.println("  │  你的代码访问 buffer.get(1000000) 时：                     │");
        System.out.println("  │    1. OS 检查对应页是否在内存（Page Cache）                │");
        System.out.println("  │    2. 没有 → 触发「缺页中断」→ 从磁盘加载这一页           │");
        System.out.println("  │    3. 有 → 直接读，0 拷贝！                               │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        System.out.println();

        System.out.println("【代码逐行解析】");
        System.out.println();
        System.out.println("  try (FileChannel fc = FileChannel.open(");
        System.out.println("          Path.of(\"bigfile.dat\"), StandardOpenOption.READ)) {");
        System.out.println();
        System.out.println("      MappedByteBuffer buffer = fc.map(");
        System.out.println("          FileChannel.MapMode.READ_ONLY,  // 映射模式");
        System.out.println("          0,                               // 映射起始位置（文件偏移）");
        System.out.println("          fc.size()                        // 映射多少字节");
        System.out.println("      );");
        System.out.println("      // ★ 此时没有任何数据被读入内存！");
        System.out.println("      // fc.map() 只是系统调用 mmap()，建立虚拟地址映射");
        System.out.println("      // 即使文件是 10GB，这行代码也是瞬间完成的");
        System.out.println();
        System.out.println("      byte b = buffer.get(0);");
        System.out.println("      // ★ 此时才真正读取数据！");
        System.out.println("      // OS 发现地址 0 对应的文件页不在内存 → 缺页中断 → 从磁盘加载");
        System.out.println("      // 加载的是这一页（通常 4KB），不是整个文件");
        System.out.println();
        System.out.println("      byte b2 = buffer.get(5_000_000_000L);  // 直接跳到第 5GB 处");
        System.out.println("      // ★ 不需要 skip 5GB！直接按地址访问，OS 自动加载对应页");
        System.out.println("      // 这就是内存映射随机访问的优势");
        System.out.println("  }");
        System.out.println();

        // 实际演示内存映射
        demonstrateMemoryMap();
    }

    static void demonstrateMemoryMap() throws Exception {
        System.out.println("【实际演示内存映射的每一步】");
        System.out.println();

        Path testFile = Files.createTempFile("mmap_test_", ".dat");

        // 写入 4MB 测试数据
        int fileSize = 4 * 1024 * 1024; // 4MB
        byte[] testData = new byte[fileSize];
        for (int i = 0; i < fileSize; i++) {
            testData[i] = (byte) ((i * 7 + 13) % 256); // 固定规律，方便验证
        }
        Files.write(testFile, testData);
        System.out.println("  准备工作：创建了 4MB 的测试文件");
        System.out.println();

        // try-with-resources：自动关闭 FileChannel
        // 等价于 finally { fc.close(); }，但更简洁，不会忘记关
        try (FileChannel fc = FileChannel.open(testFile, StandardOpenOption.READ)) {

            System.out.println("  fc.size() = " + fc.size() + " 字节（" + fc.size() / 1024 / 1024 + " MB）");
            System.out.println("  调用 fc.map() 建立内存映射（瞬间完成，不加载数据）...");
            long mapStart = System.nanoTime();

            MappedByteBuffer buffer = fc.map(
                    // MapMode.READ_ONLY：只读映射
                    //   → 修改 buffer 会抛 ReadOnlyBufferException
                    //   → 对应 mmap 的 PROT_READ 保护位
                    // MapMode.READ_WRITE：读写映射
                    //   → 修改 buffer 会直接修改文件（不需要 flush！OS 会自动同步）
                    // MapMode.PRIVATE：私有映射
                    //   → 修改不影响文件，只在进程内可见（写时复制）
                    FileChannel.MapMode.READ_ONLY,

                    0L,         // position：从文件第 0 字节开始映射
                    fc.size()   // size：映射整个文件
                    // 注意：映射整个 10GB 文件也没问题，因为这只是虚拟地址，不是物理内存
            );
            long mapElapsed = System.nanoTime() - mapStart;

            System.out.println("  fc.map() 完成，耗时：" + mapElapsed / 1000 + " 微秒（很快，因为没有真正读数据）");
            System.out.println("  buffer.capacity() = " + buffer.capacity() + " 字节");
            System.out.println();

            // 演示随机访问（不需要 skip，直接按索引访问）
            System.out.println("  演示随机访问（内存映射的优势）：");

            // buffer.get(position)：读取指定位置的字节
            // 第一次访问某页时会触发缺页中断，OS 自动从磁盘加载
            byte b1 = buffer.get(0);                         // 读第 1 字节
            byte b2 = buffer.get(fileSize / 2);              // 直接跳到中间
            byte b3 = buffer.get(fileSize - 1);              // 直接跳到末尾

            System.out.println("    buffer.get(0) = " + (b1 & 0xFF)
                    + "，预期=" + ((0 * 7 + 13) % 256)
                    + (b1 == (byte) ((0 * 7 + 13) % 256) ? " ✓" : " ✗"));
            System.out.println("    buffer.get(中间) = " + (b2 & 0xFF)
                    + "，预期=" + (((fileSize / 2) * 7 + 13) % 256)
                    + (b2 == (byte) (((fileSize / 2) * 7 + 13) % 256) ? " ✓" : " ✗"));
            System.out.println("    buffer.get(末尾) = " + (b3 & 0xFF)
                    + "，预期=" + (((fileSize - 1) * 7 + 13) % 256)
                    + (b3 == (byte) (((fileSize - 1) * 7 + 13) % 256) ? " ✓" : " ✗"));
            System.out.println();
            System.out.println("  ★ 关键点：跳到「中间」和「末尾」不需要先 skip 中间的数据");
            System.out.println("    传统 InputStream 做不到这一点（必须从头读到那里）");
            System.out.println();

            // buffer.load()：提前把整个映射区域加载到内存（预热）
            // 用于知道接下来要大量读取，提前触发缺页，避免零散的缺页中断
            // 注意：这会真正占用物理内存！4MB 文件就占 4MB 内存
            // buffer.load(); // 这里注释掉，4MB 没必要，大文件才有意义

            // buffer.isLoaded()：检查映射区域是否已经全部在内存中
            System.out.println("  buffer.isLoaded() = " + buffer.isLoaded()
                    + "（只访问了部分页，不一定全在内存）");
        }
        // try-with-resources 块结束，FileChannel 自动关闭
        // 但注意：MappedByteBuffer 对应的内存映射不会立即释放（GC 时才释放）
        // 这是 MappedByteBuffer 的一个已知问题（JDK bug），需要注意

        Files.deleteIfExists(testFile);
        System.out.println();
        System.out.println("  内存映射 vs 传统读取：");
        System.out.println("  ┌──────────────┬────────────────────────┬────────────────────────┐");
        System.out.println("  │              │ 传统 InputStream.read()│ MappedByteBuffer       │");
        System.out.println("  ├──────────────┼────────────────────────┼────────────────────────┤");
        System.out.println("  │ 读 10GB 文件 │ OOM（全进堆）          │ 没问题（按需分页）     │");
        System.out.println("  │ 随机访问     │ 必须 skip 到那个位置   │ 直接 get(position)     │");
        System.out.println("  │ CPU 拷贝     │ 内核→用户，有代价      │ 减少一次（省一次拷贝） │");
        System.out.println("  │ 多进程共享   │ 不支持                 │ 支持（同一物理内存页） │");
        System.out.println("  └──────────────┴────────────────────────┴────────────────────────┘");
    }
}

// ====================================================================
// 第五部分：性能对比（用相同数据量做实测）
// ====================================================================
class Part5_Benchmark {

    static void run() throws Exception {
        System.out.println("【性能对比：用 32MB 文件，对比两种方式的传输耗时】");
        System.out.println();
        System.out.println("注意：本地文件到文件的 transferTo 底层用 copy_file_range，");
        System.out.println("      而非 sendfile（sendfile 是文件到 Socket）。");
        System.out.println("      两者都是零拷贝，原理相同，只是系统调用名不同。");
        System.out.println();

        // 准备 32MB 测试文件
        int size = 32 * 1024 * 1024; // 32MB
        Path srcFile = Files.createTempFile("bench_src_", ".dat");

        System.out.println("  准备 32MB 测试文件...");
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) data[i] = (byte) (i % 256);
        Files.write(srcFile, data);
        System.out.println("  准备完成：" + srcFile.getFileName());
        System.out.println();

        // 预热（第一次可能因为磁盘缓存冷而偏慢）
        warmup(srcFile, size);

        // 正式测试，各跑 3 次取平均
        System.out.println("  正式测试（各跑 3 次）：");
        System.out.println();

        long[] trad = new long[3];
        long[] zero = new long[3];

        for (int i = 0; i < 3; i++) {
            Path dst1 = Files.createTempFile("bench_trad_", ".dat");
            Path dst2 = Files.createTempFile("bench_zero_", ".dat");

            trad[i] = traditionalCopy(srcFile, dst1);
            zero[i] = zeroCopy(srcFile, dst2);

            System.out.printf("  第%d轮 → 传统拷贝: %4dms  |  零拷贝: %4dms%n",
                    i + 1, trad[i], zero[i]);

            Files.deleteIfExists(dst1);
            Files.deleteIfExists(dst2);
        }

        long avgTrad = (trad[0] + trad[1] + trad[2]) / 3;
        long avgZero = (zero[0] + zero[1] + zero[2]) / 3;

        System.out.println();
        System.out.println("  平均耗时：");
        System.out.println("    传统拷贝：" + avgTrad + "ms");
        System.out.println("    零拷贝：  " + avgZero + "ms");
        if (avgZero > 0) {
            System.out.printf("    性能提升：约 %.1f 倍%n", (double) avgTrad / avgZero);
        }
        System.out.println();
        System.out.println("  说明：");
        System.out.println("    本地测试文件较小（32MB），数据在 Page Cache 中缓存，差距不太明显。");
        System.out.println("    在网络传输、大文件（GB级）场景下，差距会显著放大。");
        System.out.println("    Kafka 官方测试：同硬件下零拷贝吞吐量是传统的 6 倍。");

        Files.deleteIfExists(srcFile);
    }

    static void warmup(Path src, int size) throws IOException {
        // 预热：让文件进入 Page Cache，排除磁盘冷读影响
        Path tmp = Files.createTempFile("bench_warmup_", ".dat");
        traditionalCopy(src, tmp);
        Files.deleteIfExists(tmp);
    }

    static long traditionalCopy(Path src, Path dst) throws IOException {
        long start = System.currentTimeMillis();
        // 8KB 缓冲区（传统写法，数据必须经过 JVM 堆）
        byte[] buf = new byte[8192];
        try (FileInputStream fis = new FileInputStream(src.toFile());
             FileOutputStream fos = new FileOutputStream(dst.toFile())) {
            int len;
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        }
        return System.currentTimeMillis() - start;
    }

    static long zeroCopy(Path src, Path dst) throws IOException {
        long start = System.currentTimeMillis();
        // 零拷贝：数据不经过 JVM 堆
        try (FileChannel srcCh = FileChannel.open(src, StandardOpenOption.READ);
             FileChannel dstCh = FileChannel.open(dst,
                     StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
            long size = srcCh.size();
            long transferred = 0;
            while (transferred < size) {
                transferred += srcCh.transferTo(transferred, size - transferred, dstCh);
            }
        }
        return System.currentTimeMillis() - start;
    }
}

// ====================================================================
// 第六部分：零拷贝的局限性
// ====================================================================
class Part6_Limitations {

    static void explain() {
        System.out.println("零拷贝不是万能的，有一个根本限制：");
        System.out.println();
        System.out.println("  ★ 核心限制：数据不能被修改");
        System.out.println();
        System.out.println("  零拷贝的本质是「让数据绕过用户空间」。");
        System.out.println("  但如果你需要在传输前对数据做任何处理，");
        System.out.println("  数据就必须进入用户空间，零拷贝就用不了。");
        System.out.println();

        System.out.println("  ┌──────────────────────┬────────────┬────────────────────────┐");
        System.out.println("  │ 场景                 │ 能否零拷贝  │ 原因                   │");
        System.out.println("  ├──────────────────────┼────────────┼────────────────────────┤");
        System.out.println("  │ 静态文件直接发网络   │  ✅ 能     │ 数据不需要修改         │");
        System.out.println("  │ Kafka 消息消费       │  ✅ 能     │ Broker 只转发不修改    │");
        System.out.println("  │ Nginx 发静态资源     │  ✅ 能     │ 文件原样发出           │");
        System.out.println("  ├──────────────────────┼────────────┼────────────────────────┤");
        System.out.println("  │ 传输前需要加密       │  ❌ 不能   │ 加密需要 CPU 处理数据  │");
        System.out.println("  │ 传输前需要压缩       │  ❌ 不能   │ 压缩需要 CPU 处理数据  │");
        System.out.println("  │ 数据库查询结果发网络 │  ❌ 不能   │ 数据来自内存，不是文件  │");
        System.out.println("  │ 动态生成内容         │  ❌ 不能   │ 内容是计算出来的        │");
        System.out.println("  └──────────────────────┴────────────┴────────────────────────┘");
        System.out.println();

        System.out.println("【举例说明为什么 HTTPS 不能完全零拷贝】");
        System.out.println();
        System.out.println("  HTTP（明文）：");
        System.out.println("    磁盘文件 → Page Cache → 网卡（零拷贝，数据不变）");
        System.out.println();
        System.out.println("  HTTPS（加密传输）：");
        System.out.println("    磁盘文件 → Page Cache → 用户空间（TLS 加密）→ Socket 缓冲区 → 网卡");
        System.out.println("                                ↑");
        System.out.println("                      加密必须在用户空间完成（调用 OpenSSL 等库）");
        System.out.println("                      所以这段路必须经过用户空间，零拷贝失效");
        System.out.println();
        System.out.println("  这就是为什么 Nginx 开启 SSL 后，sendfile 指令有时会自动降级处理。");
        System.out.println();

        System.out.println("【面试答法总结】");
        System.out.println();
        System.out.println("  问：什么是零拷贝？");
        System.out.println("  答：传统传输中，数据从磁盘读到内核缓冲区，");
        System.out.println("      再 CPU 拷贝到用户空间，再 CPU 拷贝回内核 Socket 缓冲区，");
        System.out.println("      最后发到网卡，共 4 次拷贝（2次DMA+2次CPU）、4次上下文切换。");
        System.out.println("      零拷贝（sendfile）让数据绕过用户空间，");
        System.out.println("      直接从内核缓冲区到网卡，只有 2 次 DMA 拷贝、2 次上下文切换，");
        System.out.println("      CPU 完全不参与数据搬运。");
        System.out.println("      Java 里用 FileChannel.transferTo() 实现，");
        System.out.println("      Kafka/Netty/Nginx 利用了这个特性实现高吞吐量。");
        System.out.println("      局限：数据不能在传输前被修改，否则必须过用户空间，零拷贝失效。");
    }
}

