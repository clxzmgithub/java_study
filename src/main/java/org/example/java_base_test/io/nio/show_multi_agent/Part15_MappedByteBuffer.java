package org.example.java_base_test.io.nio.show_multi_agent;

class Part15_MappedByteBuffer {

    static void demonstrate() throws Exception {
        System.out.println("【第十五部分：MappedByteBuffer —— 内存映射文件（mmap）】");
        System.out.println();
        System.out.println("═══ 🗺️ 生活场景：地图上直接标记 vs 抄一份回来再改 ═══");
        System.out.println();
        System.out.println("  【传统 read/write 方式】");
        System.out.println("  就像查地图时的老方法：");
        System.out.println("  1. 去图书馆借地图（磁盘 → 内核Page Cache）");
        System.out.println("  2. 把整张地图抄一份带回家（Page Cache → JVM堆 CPU拷贝）");
        System.out.println("  3. 在抄回来的副本上标记（修改JVM堆中的数据）");
        System.out.println("  4. 再把修改的地方誊回图书馆原本（JVM堆 → 磁盘 CPU拷贝）");
        System.out.println("  → 每次改个小地方都要「抄一遍、改、再抄一遍」，超慢！");
        System.out.println();
        System.out.println("  【mmap 方式（MappedByteBuffer）】");
        System.out.println("  就像图书馆直接把地图借给你放在桌上：");
        System.out.println("  1. 你直接在图书馆原版地图上画（映射到你的内存地址空间）");
        System.out.println("  2. 在上面标记 A、B、C 景点（直接写 Page Cache，无需拷贝）");
        System.out.println("  3. 图书馆（OS）晚上自动把你的修改存档（异步刷盘）");
        System.out.println("  → 省去了「抄一遍」的步骤，直接操作原版！");
        System.out.println();
        System.out.println("  【首次访问「缺页中断」= 图书馆查档案】");
        System.out.println("  你拿到了地图的「座位号」（虚拟地址），但地图还在仓库");
        System.out.println("  第一次翻到某页 → 图书馆去仓库取那页（缺页中断，加载Page Cache）");
        System.out.println("  之后再翻同一页 → 直接从桌上拿（纯内存操作，极快）");
        System.out.println();
        System.out.println("  【为什么 RocketMQ 用 mmap？— 写消息速度飞快】");
        System.out.println("  写一条消息 = 在地图上画一个点（直接写内存地址）");
        System.out.println("  完全不需要 write() 系统调用！");
        System.out.println("  RocketMQ 写消息性能：mmap > write() + Page Cache > 直接写磁盘");
        System.out.println();
        System.out.println("  【mmap 的代价 — 借了地图不还的问题】");
        System.out.println("  MappedByteBuffer 借了图书馆地图，不还（GC无法直接释放）");
        System.out.println("  Windows上：地图还没还，图书馆就不让别人改它（文件被占用）");
        System.out.println("  解决：主动归还（强制 clean()）或等图书馆打扫卫生（GC触发）");
        System.out.println();
        System.out.println("═══ 以下是代码演示 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示1：mmap 原理（与传统 read/write 的对比）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. mmap 原理 vs 传统 read/write ━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  传统 read() 路径：");
        System.out.println("    磁盘 ──DMA──► Page Cache（内核）──CPU 拷贝──► 用户缓冲区（JVM堆）");
        System.out.println("    2次拷贝 + 2次上下文切换（用户态→内核态→用户态）");
        System.out.println();
        System.out.println("  mmap 路径：");
        System.out.println("    mmap() 系统调用：在进程虚拟地址空间建立一块映射区域");
        System.out.println("    此区域直接对应文件的 Page Cache（不是独立副本！）");
        System.out.println();
        System.out.println("    首次访问 mappedBuf[i]（Page Fault）：");
        System.out.println("      CPU 发现该虚拟页没有对应物理页 → 触发缺页中断");
        System.out.println("      OS 内核把文件对应的页加载到 Page Cache");
        System.out.println("      建立虚拟页 → 物理页（Page Cache）的映射");
        System.out.println("      之后访问同一页：直接读内存，无任何系统调用！");
        System.out.println();
        System.out.println("    写入 mappedBuf[i] = x：");
        System.out.println("      直接写 Page Cache 对应的物理页（视为内存写）");
        System.out.println("      OS 异步刷盘（或手动 force()/msync()）");
        System.out.println("      省去了 write() 系统调用 + 内核缓冲区拷贝");
        System.out.println();
        System.out.println("  ┌─────────────────┬────────────────────┬──────────────────────┐");
        System.out.println("  │ 操作            │ 传统 read/write    │ mmap                 │");
        System.out.println("  ├─────────────────┼────────────────────┼──────────────────────┤");
        System.out.println("  │ 内存拷贝次数    │ 读：2次 写：2次    │ 读：1次 写：0次      │");
        System.out.println("  │ 系统调用次数    │ 每次 read/write    │ 只有初始化 mmap()    │");
        System.out.println("  │ 用户空间缓冲区  │ 需要（JVM堆）      │ 不需要（直接操作）   │");
        System.out.println("  │ 随机访问性能    │ 差（需seek+read）  │ 极好（直接寻址）     │");
        System.out.println("  └─────────────────┴────────────────────┴──────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示2：MappedByteBuffer 代码演示
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. MappedByteBuffer 代码演示 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("mmap_", ".dat");
        try {
            // 先写入初始数据
            java.nio.file.Files.write(tmpFile, "Hello, mmap world!".getBytes());

            try (java.nio.channels.FileChannel fc =
                     java.nio.channels.FileChannel.open(tmpFile,
                             java.nio.file.StandardOpenOption.READ,
                             java.nio.file.StandardOpenOption.WRITE)) {

                long fileSize = fc.size();
                System.out.println("  文件大小：" + fileSize + " 字节");

                // ★ 核心 API：fc.map(mode, position, size)
                // MapMode.READ_WRITE：可读可写（对应 mmap PROT_READ|PROT_WRITE）
                // MapMode.READ_ONLY ：只读
                // MapMode.PRIVATE   ：写时拷贝（不影响原文件，类似 Copy-On-Write）
                java.nio.MappedByteBuffer mbb = fc.map(
                        java.nio.channels.FileChannel.MapMode.READ_WRITE,
                        0,        // 从文件第 0 字节开始
                        fileSize  // 映射 fileSize 字节
                );
                System.out.println("  mmap 映射完成，mappedByteBuffer capacity = " + mbb.capacity());

                // 直接读（不需要 read() 系统调用）
                byte[] readArr = new byte[(int) fileSize];
                mbb.get(readArr);
                System.out.println("  直接读出内容：\"" + new String(readArr) + "\"");

                // 跳回开头，覆盖写
                mbb.position(0);
                mbb.put("MODIFIED_CONTENT!".getBytes());
                System.out.println("  直接写入 \"MODIFIED_CONTENT!\"（类似写内存，极快）");

                // force() 相当于 msync()，强制刷盘
                mbb.force();
                System.out.println("  mbb.force() 强制将修改刷到磁盘（对应 msync()）");
            }

            // 验证结果
            byte[] result = java.nio.file.Files.readAllBytes(tmpFile);
            System.out.println("  从磁盘读取验证：\"" + new String(result) + "\"");

        } finally {
            java.nio.file.Files.deleteIfExists(tmpFile);
        }
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示3：mmap 适合大文件随机读写（结合 position 原理说明）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. 为什么 mmap 特别适合大文件随机读写 ━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  场景：1GB 索引文件，查询时需要随机跳到文件的任意位置读 16 字节");
        System.out.println();
        System.out.println("  传统方式：");
        System.out.println("    RandomAccessFile raf = new RandomAccessFile(file, \"r\");");
        System.out.println("    raf.seek(offset);    // ① 系统调用 lseek()");
        System.out.println("    raf.read(buf);       // ② 系统调用 read()（可能触发磁盘IO）");
        System.out.println("    每次随机读 = 2次系统调用 + 上下文切换");
        System.out.println();
        System.out.println("  mmap 方式：");
        System.out.println("    MappedByteBuffer mbb = fc.map(READ_ONLY, 0, fileSize);");
        System.out.println("    // 之后随机读就是普通内存操作！");
        System.out.println("    mbb.position(offset);");
        System.out.println("    mbb.get(buf, 0, 16); // 无系统调用，直接读 Page Cache");
        System.out.println();
        System.out.println("  热点页（频繁访问的文件区域）会常驻 Page Cache");
        System.out.println("  OS 自动缓存，多次读同一区域 = 纯内存速度（ns 级别）");
        System.out.println();
        System.out.println("  Lucene 索引读取就是这么做的：");
        System.out.println("    每个索引段文件在 JVM 进程里都有一个 MappedByteBuffer");
        System.out.println("    搜索时直接用内存寻址跳转，不走 read() 系统调用");
        System.out.println("    ES 节点内存给 50% 给 JVM，另 50% 专门留给 OS Page Cache 缓存索引");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示4：mmap 的注意事项和坑
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. mmap 的注意事项 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  坑1：MappedByteBuffer 无法被 GC 直接释放！");
        System.out.println("    MappedByteBuffer 是 Direct Buffer");
        System.out.println("    底层的 munmap() 系统调用在 Cleaner 里，依赖 GC 触发");
        System.out.println("    文件被映射期间，文件句柄被占用，Windows 上无法删除/重命名！");
        System.out.println();
        System.out.println("    强制释放（Hack，JDK8）：");
        System.out.println("      sun.misc.Cleaner cleaner = ((DirectBuffer) mbb).cleaner();");
        System.out.println("      if (cleaner != null) cleaner.clean();");
        System.out.println("    JDK9+：");
        System.out.println("      ((sun.nio.ch.DirectBuffer) mbb).cleaner().clean();");
        System.out.println("      或用 ByteBuffer API (Java 9+):");
        System.out.println("      // 没有直接 API，Netty 用 PlatformDependent.freeDirectBuffer(buf)");
        System.out.println();
        System.out.println("  坑2：映射大小不能超过 Integer.MAX_VALUE（2GB）");
        System.out.println("    MappedByteBuffer 继承 ByteBuffer，position/limit 是 int");
        System.out.println("    超过 2GB 需要分段映射（每段 < 2GB）");
        System.out.println("    RocketMQ CommitLog 每个文件固定 1GB 就是这个原因");
        System.out.println();
        System.out.println("  坑3：映射文件大小必须 > 0，且 map() 的 size 参数要和实际写入大小匹配");
        System.out.println("    常见做法：新建文件时先用 channel.write(0) 预分配空间，再 map");
        System.out.println();
        System.out.println("  坑4：mmap 不适合频繁小文件 IO");
        System.out.println("    mmap() 系统调用本身开销比 read() 高（建立 VMA 虚拟内存区域）");
        System.out.println("    文件 < 4KB（一个 Page）时，传统方式反而更快");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示5：RocketMQ mmap 写入核心逻辑（源码级）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. RocketMQ CommitLog 怎么用 mmap（源码级） ━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  RocketMQ MappedFile.java（简化）：");
        System.out.println();
        System.out.println("  class MappedFile {");
        System.out.println("      private MappedByteBuffer mappedByteBuffer; // 映射整个 CommitLog 文件");
        System.out.println("      private int wrotePosition;                  // 当前写入位置（AtomicInteger）");
        System.out.println();
        System.out.println("      void init(String fileName, int fileSize) throws IOException {");
        System.out.println("          this.fileChannel = new RandomAccessFile(file, \"rw\").getChannel();");
        System.out.println("          // 映射整个 1GB 文件（或指定大小）");
        System.out.println("          this.mappedByteBuffer = fileChannel.map(");
        System.out.println("              MapMode.READ_WRITE, 0, fileSize);");
        System.out.println("      }");
        System.out.println();
        System.out.println("      // 写消息（不需要 write() 系统调用！）");
        System.out.println("      boolean appendMessage(byte[] data) {");
        System.out.println("          int currentPos = wrotePosition;");
        System.out.println("          if (currentPos + data.length > fileSize) return false;");
        System.out.println("          // ★ 直接写内存地址 = 写 Page Cache");
        System.out.println("          ByteBuffer slice = mappedByteBuffer.slice();");
        System.out.println("          slice.position(currentPos);");
        System.out.println("          slice.put(data);                 // 零系统调用！");
        System.out.println("          wrotePosition += data.length;    // 更新写指针");
        System.out.println("          return true;");
        System.out.println("      }");
        System.out.println();
        System.out.println("      // 刷盘（可配置同步/异步）");
        System.out.println("      void flush() {");
        System.out.println("          mappedByteBuffer.force(); // 对应 msync(MS_SYNC)");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  这就是为什么 RocketMQ 写入速度极快：");
        System.out.println("    写消息 = 直接 put() 到内存（无系统调用）");
        System.out.println("    OS 异步将 Page Cache 刷盘（不阻塞消息写入）");
        System.out.println("    需要强一致时才调用 force() = msync()");
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
    }
}

