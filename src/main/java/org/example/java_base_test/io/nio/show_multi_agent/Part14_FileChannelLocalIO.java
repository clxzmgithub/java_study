package org.example.java_base_test.io.nio.show_multi_agent;

import java.nio.ByteBuffer;

class Part14_FileChannelLocalIO {

    static void demonstrate() throws Exception {
        System.out.println("【第十四部分：FileChannel 本地文件 IO 完整操作】");
        System.out.println();
        System.out.println("═══ 🗄️ 生活场景：档案室 理解 FileChannel ═══");
        System.out.println();
        System.out.println("  传统 IO（InputStream）就像老式档案室的工作方式：");
        System.out.println("  「给你一根吸管，只能从头吸到尾，不能倒回去，不能跳到中间」");
        System.out.println("  想看第500页？对不起，必须从第1页开始一页一页过");
        System.out.println();
        System.out.println("  FileChannel 就像现代数字档案室的工作方式：");
        System.out.println("  「带坐标的文件夹，可以直接跳到任何页码」");
        System.out.println();
        System.out.println("  【FileChannel 对比 InputStream 的优势】");
        System.out.println("  position() 跳转 = 直接输入「第500页」跳过去（随机访问）");
        System.out.println("              不用从第1页翻到500页（O(1) vs O(n)）");
        System.out.println("  双向读写   = 同一个通道可以读也可以写（一个档案员全能）");
        System.out.println("  transferTo = 「把这份文件直接寄给对方」，不用你抄一遍（零拷贝）");
        System.out.println("  force()    = 「现在立刻存档，不是下班后」（防断电丢数据）");
        System.out.println("  lock()     = 「这份文件我在用，其他人进不来」（文件锁）");
        System.out.println();
        System.out.println("  【force() 为什么重要？— 想象关机不保存文档】");
        System.out.println("  channel.write() 只是把文件写到「暂存区」（Page Cache内存）");
        System.out.println("  就像 Word 里改了文档但没点保存，这时断电 = 修改丢失！");
        System.out.println("  force(true) = 强制点击「保存」，确保写到磁盘");
        System.out.println("  数据库的「事务提交」就是在做 force()，保证数据不丢！");
        System.out.println();
        System.out.println("  【FileLock — 多人编辑同一文件的冲突保护】");
        System.out.println("  就像会议室预约系统，你预约了就锁定，别人进不来");
        System.out.println("  但这个锁是「君子协议」：都遵守才有效，暴力进入挡不住");
        System.out.println();
        System.out.println("═══ 以下是代码演示 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示1：创建 FileChannel 的三种方式
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 创建 FileChannel 的三种方式 ━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  // 方式一：推荐，NIO.2 Files API（JDK7+）");
        System.out.println("  FileChannel fc = FileChannel.open(");
        System.out.println("      Paths.get(\"/data/test.txt\"),");
        System.out.println("      StandardOpenOption.READ,");
        System.out.println("      StandardOpenOption.WRITE,");
        System.out.println("      StandardOpenOption.CREATE   // 不存在则创建");
        System.out.println("  );");
        System.out.println();
        System.out.println("  // 方式二：从传统 IO Stream 获取（与老代码桥接用）");
        System.out.println("  FileChannel readCh  = new FileInputStream(file).getChannel();");
        System.out.println("  FileChannel writeCh = new FileOutputStream(file).getChannel();");
        System.out.println("  FileChannel rwCh    = new RandomAccessFile(file, \"rw\").getChannel();");
        System.out.println();
        System.out.println("  ⚠ 注意：关闭 Channel 会自动关闭关联的 Stream，反之亦然");
        System.out.println("          Stream 和 Channel 不要各自关，关一个就够了");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示2：基本读写（write + read）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. 基本读写（write → read）━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("fc_demo_", ".txt");
        try {
            // ── 写入 ──
            try (java.nio.channels.FileChannel writeChannel =
                     java.nio.channels.FileChannel.open(tmpFile,
                             java.nio.file.StandardOpenOption.WRITE)) {

                ByteBuffer writeBuf = ByteBuffer.allocate(64);
                writeBuf.put("Hello, FileChannel!".getBytes());
                writeBuf.flip();                         // 写完必须 flip 再传给 Channel
                int written = writeChannel.write(writeBuf);
                System.out.println("  写入字节数：" + written);
                System.out.println("  写完后 position（文件游标）：" + writeChannel.position());
                // ⚠ 这里不需要调用 force()，演示只是写到 Page Cache
            }

            // ── 读取 ──
            try (java.nio.channels.FileChannel readChannel =
                     java.nio.channels.FileChannel.open(tmpFile,
                             java.nio.file.StandardOpenOption.READ)) {

                ByteBuffer readBuf = ByteBuffer.allocate(64);
                int n = readChannel.read(readBuf);       // Channel → Buffer（写模式）
                System.out.println("  读取字节数：" + n);
                readBuf.flip();                          // 切换为读模式
                byte[] data = new byte[readBuf.remaining()];
                readBuf.get(data);
                System.out.println("  读取内容：\"" + new String(data) + "\"");
            }
        } finally {
            java.nio.file.Files.deleteIfExists(tmpFile);
        }
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示3：position 随机访问（FileChannel 最大特色之一）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. position 随机访问（任意跳转读写位置）━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  传统 InputStream 只能顺序读，不能往回跳");
        System.out.println("  FileChannel.position(long) 可以跳到文件任意字节位置");
        System.out.println();

        java.nio.file.Path tmpFile2 = java.nio.file.Files.createTempFile("fc_pos_", ".txt");
        try {
            // 先写 "ABCDEFGHIJ"（10字节）
            try (java.nio.channels.FileChannel fc =
                     java.nio.channels.FileChannel.open(tmpFile2,
                             java.nio.file.StandardOpenOption.WRITE,
                             java.nio.file.StandardOpenOption.READ)) {

                fc.write(ByteBuffer.wrap("ABCDEFGHIJ".getBytes()));
                System.out.println("  写入：\"ABCDEFGHIJ\"，文件 position = " + fc.position());

                // 跳到第 5 个字节
                fc.position(5);
                System.out.println("  position(5) 后，position = " + fc.position());

                // 覆盖写 "XYZ"（覆盖 FGHIJ 中的 FGH）
                fc.write(ByteBuffer.wrap("XYZ".getBytes()));
                System.out.println("  覆盖写 \"XYZ\" 后，position = " + fc.position());
            }

            // 读出来看结果
            byte[] result = java.nio.file.Files.readAllBytes(tmpFile2);
            System.out.println("  最终文件内容：\"" + new String(result) + "\"");
            System.out.println("  （ABCDE 不变，FGH 被 XYZ 覆盖，IJ 不变）");

        } finally {
            java.nio.file.Files.deleteIfExists(tmpFile2);
        }
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示4：force() —— 强制刷盘（数据安全关键）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. force() —— 强制刷盘 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  写文件的数据路径：");
        System.out.println("    channel.write(buf)");
        System.out.println("         ↓");
        System.out.println("    OS Page Cache（内核缓冲区，纯内存，速度快）");
        System.out.println("         ↓  （OS 定期异步刷盘，默认 30 秒左右）");
        System.out.println("    磁盘（永久存储）");
        System.out.println();
        System.out.println("  问题：如果写完后立刻断电，Page Cache 里的数据丢了！");
        System.out.println("  解决：channel.force(true) 强制把 Page Cache 刷到磁盘");
        System.out.println();
        System.out.println("  // 参数 true = 同时刷文件元数据（修改时间、大小等）");
        System.out.println("  // 参数 false = 只刷文件内容（性能稍好）");
        System.out.println("  channel.force(true);");
        System.out.println();
        System.out.println("  对应 Linux 系统调用：");
        System.out.println("    force(true)  → fsync(fd)   同步数据 + 元数据");
        System.out.println("    force(false) → fdatasync(fd) 只同步数据");
        System.out.println();
        System.out.println("  使用场景：");
        System.out.println("    数据库（每次提交事务都要 fsync，保证 WAL 日志落盘）");
        System.out.println("    Kafka 消息（可配置 flush.messages 每N条 force 一次）");
        System.out.println("    普通文件复制（不需要，OS 自己会刷）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示5：size() 和 truncate()
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. size() 和 truncate() ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        java.nio.file.Path tmpFile3 = java.nio.file.Files.createTempFile("fc_trunc_", ".txt");
        try (java.nio.channels.FileChannel fc =
                 java.nio.channels.FileChannel.open(tmpFile3,
                         java.nio.file.StandardOpenOption.WRITE,
                         java.nio.file.StandardOpenOption.READ)) {

            fc.write(ByteBuffer.wrap("0123456789".getBytes()));
            System.out.println("  写入 \"0123456789\"，size() = " + fc.size());

            // truncate 截断到 6 字节
            fc.truncate(6);
            System.out.println("  truncate(6) 后，size() = " + fc.size());
            System.out.println("  truncate(6) 后，position = " + fc.position());
            System.out.println("  ★ truncate 不影响 position（如果 position > size，position 会被修正为 size）");

        } finally {
            byte[] r = java.nio.file.Files.readAllBytes(tmpFile3);
            System.out.println("  最终文件内容：\"" + new String(r) + "\"");
            java.nio.file.Files.deleteIfExists(tmpFile3);
        }
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示6：FileLock 文件锁（多进程竞争文件时防止数据损坏）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 6. FileLock —— 文件锁 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  场景：两个 JVM 进程同时写同一个文件 → 数据损坏");
        System.out.println("  FileLock 是操作系统级别的锁，不同进程都能感知");
        System.out.println("  注意：同一个 JVM 内多线程用 FileLock 无效，要用 synchronized");
        System.out.println();
        System.out.println("  两种锁：");
        System.out.println("    独占锁（写锁）：fc.lock()        → 其他进程的 lock() 会阻塞");
        System.out.println("    共享锁（读锁）：fc.lock(0, Long.MAX_VALUE, true)");
        System.out.println("                   → 多个进程可同时持有共享锁，但独占锁会阻塞");
        System.out.println();
        System.out.println("  try-with-resources 写法（推荐）：");
        System.out.println("    try (FileLock lock = channel.lock()) {");
        System.out.println("        // 拿到锁，安全操作文件");
        System.out.println("        channel.write(buf);");
        System.out.println("    } // lock 自动释放");
        System.out.println();
        System.out.println("  tryLock()（非阻塞）：");
        System.out.println("    FileLock lock = channel.tryLock();");
        System.out.println("    if (lock == null) {");
        System.out.println("        // 锁被其他进程持有，本次跳过");
        System.out.println("    }");
        System.out.println();
        System.out.println("  ⚠ 跨平台注意：");
        System.out.println("    Linux：Advisory Lock（建议性锁，其他进程可以无视）");
        System.out.println("    Windows：Mandatory Lock（强制锁，其他进程被 OS 直接阻止）");
        System.out.println("    → 文件锁只对「主动调用 lock()」的进程有效，无法阻止暴力写");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示7：FileChannel vs InputStream 选型总结
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 7. FileChannel vs InputStream 选型 ━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌─────────────────────────┬──────────────┬──────────────────┐");
        System.out.println("  │ 特性                    │ InputStream  │ FileChannel      │");
        System.out.println("  ├─────────────────────────┼──────────────┼──────────────────┤");
        System.out.println("  │ 读写方向                │ 单向         │ 双向             │");
        System.out.println("  │ 随机访问                │ ❌（只能顺序）│ ✅ position()   │");
        System.out.println("  │ 零拷贝 transferTo       │ ❌           │ ✅              │");
        System.out.println("  │ 内存映射 map()          │ ❌           │ ✅              │");
        System.out.println("  │ 文件锁 lock()           │ ❌           │ ✅              │");
        System.out.println("  │ 代码简单程度            │ 简单         │ 稍复杂（需Buffer）│");
        System.out.println("  │ 适合场景                │ 顺序读小文件  │ 大文件/随机/传输 │");
        System.out.println("  └─────────────────────────┴──────────────┴──────────────────┘");
        System.out.println();
        System.out.println("  经验原则：");
        System.out.println("    < 1MB 顺序读写    → BufferedInputStream/OutputStream");
        System.out.println("    需要随机访问      → FileChannel + position()");
        System.out.println("    文件传输到网络    → FileChannel.transferTo()（零拷贝）");
        System.out.println("    大文件随机读（GB）→ FileChannel.map()（内存映射，见下一部分）");
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
    }
}

