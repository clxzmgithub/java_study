package org.example.java_base_test.io.nio.show_multi_agent;

class Part9_TransferToCallChain {

    static void explain() {
        System.out.println("【第九部分：transferTo() 完整调用链 Java→JVM→OS→内核】");
        System.out.println();
        System.out.println("═══ 📚 生活场景：图书馆借书 理解零拷贝 ═══");
        System.out.println();
        System.out.println("  场景：你要把图书馆的一本书「发给」你的朋友（网络传输文件）");
        System.out.println();
        System.out.println("  【传统方式（非零拷贝）= 4次搬运】");
        System.out.println("  步骤1：图书馆员（内核/DMA）从书架（磁盘）取书放到阅览室（内核缓冲区）");
        System.out.println("  步骤2：你（CPU）从阅览室走过去，抱着书回到你的座位（JVM堆）");
        System.out.println("  步骤3：你（CPU）再抱着书走到邮寄窗口（Socket发送缓冲区）");
        System.out.println("  步骤4：邮递员（内核/DMA）把书从窗口发走（发给网络/对端）");
        System.out.println("  → 你亲自搬了2次！（CPU做了2次内存拷贝）");
        System.out.println();
        System.out.println("  【零拷贝（transferTo/sendfile）= 2次搬运，你不用动】");
        System.out.println("  步骤1：图书馆员（内核/DMA）从书架取书放到阅览室（内核缓冲区）");
        System.out.println("  步骤2：你只是告诉邮递员「阅览室的那本书直接发走」（传文件描述符）");
        System.out.println("         邮递员（内核/DMA）自己去阅览室取，直接发走");
        System.out.println("  → 你全程没搬书（CPU 0次拷贝！）");
        System.out.println();
        System.out.println("  【为什么节省的这两次搬运很重要？】");
        System.out.println("  Kafka 每秒要发几百万本书，每次少搬2次 = 每秒省几百万次CPU操作");
        System.out.println("  → 这就是 Kafka 为什么能达到百万 QPS 的秘密之一！");
        System.out.println();
        System.out.println("  【transferTo 为什么要套 while 循环？】");
        System.out.println("  就像邮寄一套《四库全书》，邮政规定每次最多寄5kg，");
        System.out.println("  你得分多次寄（TCP缓冲区有限），不写循环就只寄了第一箱，其余丢失！");
        System.out.println();
        System.out.println("═══ 以下是 transferTo() 的完整调用链（从Java到硬件）═══");
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
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
    }
}

