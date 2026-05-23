package org.example.java_base_test.io.nio.show_multi_agent;

class Part22_NIO2Advanced {

    static void demonstrate() throws Exception {
        System.out.println("【第二十二部分：NIO.2 深度进阶】");
        System.out.println();
        System.out.println("═══ 🗂 生活场景：从「手写档案馆」到「智能档案系统」理解NIO.2进阶 ═══");
        System.out.println();
        System.out.println("  NIO.2 是 JDK7 引入的现代文件系统 API，就像把「手写纸质档案馆」");
        System.out.println("  升级成「智能数字化档案管理系统」，每项能力的对比：");
        System.out.println();
        System.out.println("  ① 文件属性（第一节）= 档案封面上的「元数据卡」");
        System.out.println("    旧档案：只知道「这份档案的创建日期」（java.io.File 有限信息）");
        System.out.println("    新系统：档案卡有所有信息——创建时间、最后修改时间、文件类型、");
        System.out.println("            大小、Unix权限（谁可读写执行），还能自定义扩展字段");
        System.out.println();
        System.out.println("  ② 符号链接（第二节）= 档案馆里的「转发卡/别名索引」");
        System.out.println("    「客户信息」档案夹里有一张卡，写着「请看→市场部第5柜第3格」");
        System.out.println("    这张「转发卡」就是符号链接（symlink）");
        System.out.println("    旧方式：你以为自己在看「客户信息」，其实看的是「市场部第5柜」");
        System.out.println("    新系统：你可以精确控制「跟着转发卡走」还是「只看转发卡本身」");
        System.out.println();
        System.out.println("  ③ SeekableByteChannel（第三节）= 可以「任意翻到某页」的高级阅读器");
        System.out.println("    旧方式（InputStream）：只能从头往后读，像卡带机，不能快进到中间");
        System.out.println("    SeekableByteChannel：像 DVD 播放机，可以直接跳到任意位置读写");
        System.out.println();
        System.out.println("  ④ AIO（第四节）= 请「快递员」代取档案，取好了电话通知你");
        System.out.println("    NIO：取档案通知你来取，你自己走过去拿");
        System.out.println("    AIO：档案取好了直接送到你手上，你全程不参与");
        System.out.println();
        System.out.println("  ⑤ FileStore（第六节）= 档案馆「空间管理员」的看板");
        System.out.println("    实时显示：哪个档案柜（磁盘分区）还有多少空间、什么类型的柜子");
        System.out.println();
        System.out.println("═══ 以下是各节技术细节 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：文件属性视图体系（最完整的文件元数据 API）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 文件属性视图体系（BasicFileAttributes / PosixFileAttributes）━");
        System.out.println();
        System.out.println("  NIO.2 属性视图层次：");
        System.out.println("    AttributeView（顶层接口）");
        System.out.println("      └── FileAttributeView");
        System.out.println("            ├── BasicFileAttributeView  → 跨平台基础属性（所有OS都有）");
        System.out.println("            ├── PosixFileAttributeView  → Unix权限/所有者（Linux/macOS）");
        System.out.println("            ├── DosFileAttributeView    → Windows DOS属性（只读/隐藏/系统）");
        System.out.println("            ├── AclFileAttributeView    → Windows ACL 访问控制列表");
        System.out.println("            └── UserDefinedFileAttributeView → 用户自定义扩展属性");
        System.out.println();
        System.out.println("  生活类比：就像身份证有基础信息（BasicView），");
        System.out.println("           美国护照还有签证页（PosixView），Windows 文档还有只读属性（DosView）");
        System.out.println();

        // BasicFileAttributes 演示
        System.out.println("  ① BasicFileAttributes（所有平台通用）：");
        java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("nio2_attr_", ".txt");
        java.nio.file.Files.write(tmpFile, "test content for attributes demo".getBytes());

        try {
            java.nio.file.attribute.BasicFileAttributes basic =
                java.nio.file.Files.readAttributes(tmpFile,
                    java.nio.file.attribute.BasicFileAttributes.class);

            System.out.println("    creationTime()    = " + basic.creationTime());
            System.out.println("    lastModifiedTime()= " + basic.lastModifiedTime());
            System.out.println("    lastAccessTime()  = " + basic.lastAccessTime());
            System.out.println("    size()            = " + basic.size() + " bytes");
            System.out.println("    isRegularFile()   = " + basic.isRegularFile());
            System.out.println("    isDirectory()     = " + basic.isDirectory());
            System.out.println("    isSymbolicLink()  = " + basic.isSymbolicLink());
            System.out.println("    fileKey()         = " + basic.fileKey() + "  （文件唯一标识符，inode号）");
            System.out.println();

            // 修改时间（java.io.File 根本没有 setLastModifiedTime，只有 setLastModified long）
            System.out.println("  修改文件时间戳（java.io.File 无法做到的）：");
            java.nio.file.attribute.BasicFileAttributeView view =
                java.nio.file.Files.getFileAttributeView(tmpFile,
                    java.nio.file.attribute.BasicFileAttributeView.class);
            java.nio.file.attribute.FileTime newTime =
                java.nio.file.attribute.FileTime.fromMillis(
                    System.currentTimeMillis() - 86400_000L); // 改成昨天
            view.setTimes(newTime, newTime, null); // (lastModified, lastAccess, creation)
            System.out.println("    Files.getFileAttributeView(...).setTimes(yesterday, ...)");
            System.out.println("    lastModifiedTime 现在 = " +
                java.nio.file.Files.getLastModifiedTime(tmpFile));
            System.out.println();

            // PosixFileAttributes（Linux/macOS）
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("windows")) {
                System.out.println("  ② PosixFileAttributes（Linux/macOS 专属）：");
                java.nio.file.attribute.PosixFileAttributes posix =
                    java.nio.file.Files.readAttributes(tmpFile,
                        java.nio.file.attribute.PosixFileAttributes.class);
                System.out.println("    owner()      = " + posix.owner().getName());
                System.out.println("    group()      = " + posix.group().getName());
                System.out.println("    permissions()= " + posix.permissions() + "  （rwxr-xr-x 格式）");
                System.out.println();

                // 修改权限
                java.util.Set<java.nio.file.attribute.PosixFilePermission> perms =
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--");
                java.nio.file.Files.setPosixFilePermissions(tmpFile, perms);
                System.out.println("    setPosixFilePermissions(\"rw-r--r--\") 设置成功");
                System.out.println();
            }

            // 批量读取多个属性（性能更好，一次系统调用）
            System.out.println("  ③ 批量读取属性（推荐方式，避免多次系统调用）：");
            System.out.println("    // 用逗号分隔多个属性名，一次调用读取");
            java.util.Map<String, Object> attrs =
                java.nio.file.Files.readAttributes(tmpFile, "basic:size,lastModifiedTime,isDirectory");
            System.out.println("    Files.readAttributes(path, \"basic:size,lastModifiedTime,isDirectory\")");
            attrs.forEach((k, v) -> System.out.println("      " + k + " = " + v));
            System.out.println();

        } finally {
            java.nio.file.Files.deleteIfExists(tmpFile);
        }

        // ════════════════════════════════════════════════════════════════
        // 第二节：符号链接精确控制
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. 符号链接（Symbolic Link）精确控制 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  java.io.File 的问题：");
        System.out.println("    file.isDirectory() 会「跟随」符号链接（FOLLOW_LINKS）");
        System.out.println("    你无法判断「这个路径本身是不是一个符号链接」");
        System.out.println("    导致：操作符号链接 = 操作了它指向的真实文件");
        System.out.println();
        System.out.println("  NIO.2 精确控制：LinkOption.NOFOLLOW_LINKS");
        System.out.println();
        System.out.println("  // 判断路径本身是否是符号链接（不跟随）");
        System.out.println("  Files.isSymbolicLink(path)          → 是否是 symlink 本身");
        System.out.println("  Files.readSymbolicLink(path)        → 读取 symlink 指向的路径");
        System.out.println();
        System.out.println("  // FOLLOW_LINKS（默认）vs NOFOLLOW_LINKS 对比：");
        System.out.println("  Files.exists(symlink)                          → 检查目标文件是否存在");
        System.out.println("  Files.exists(symlink, LinkOption.NOFOLLOW_LINKS)→ 检查 symlink 本身是否存在");
        System.out.println();
        System.out.println("  Files.isDirectory(symlink)                      → 跟随，判断目标是否是目录");
        System.out.println("  Files.isDirectory(symlink, NOFOLLOW_LINKS)      → 不跟随，判断链接本身");
        System.out.println();
        System.out.println("  // 删除符号链接本身（不删除目标文件）");
        System.out.println("  Files.delete(symlinkPath)     → 只删 symlink，不影响真实文件");
        System.out.println();

        // 实际演示（仅 Linux/macOS）
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("windows")) {
            java.nio.file.Path realFile = java.nio.file.Files.createTempFile("real_", ".txt");
            java.nio.file.Files.write(realFile, "real file".getBytes());
            java.nio.file.Path symlink = realFile.getParent().resolve("symlink_" + System.nanoTime());
            try {
                java.nio.file.Files.createSymbolicLink(symlink, realFile);
                System.out.println("  实际演示：");
                System.out.println("    createSymbolicLink(symlink → " + realFile.getFileName() + ")");
                System.out.println("    isSymbolicLink(symlink) = " + java.nio.file.Files.isSymbolicLink(symlink));
                System.out.println("    readSymbolicLink(symlink) = " + java.nio.file.Files.readSymbolicLink(symlink));
                System.out.println("    isRegularFile(symlink, FOLLOW)      = " +
                    java.nio.file.Files.isRegularFile(symlink)); // 跟随 → true
                System.out.println("    isRegularFile(symlink, NOFOLLOW)    = " +
                    java.nio.file.Files.isRegularFile(symlink,
                        java.nio.file.LinkOption.NOFOLLOW_LINKS)); // 不跟随 → false
            } finally {
                java.nio.file.Files.deleteIfExists(symlink);
                java.nio.file.Files.deleteIfExists(realFile);
            }
        } else {
            System.out.println("  （符号链接演示：Windows 环境跳过）");
        }
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：SeekableByteChannel——随机读写的统一接口
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. SeekableByteChannel —— 随机读写的统一接口 ━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  继承关系：");
        System.out.println("    Channel");
        System.out.println("      └── ReadableByteChannel");
        System.out.println("      └── WritableByteChannel");
        System.out.println("            └── ByteChannel");
        System.out.println("                  └── SeekableByteChannel  ← NIO.2 新增（JDK7）");
        System.out.println("                        └── FileChannel     ← 实现了 SeekableByteChannel");
        System.out.println();
        System.out.println("  SeekableByteChannel 新增了「定位」能力：");
        System.out.println("    position()       获取当前读写位置");
        System.out.println("    position(long)   跳到指定字节位置（随机访问）");
        System.out.println("    size()           文件大小");
        System.out.println("    truncate(long)   截断到指定大小");
        System.out.println();
        System.out.println("  用 Files.newByteChannel() 打开（更简洁的方式）：");

        java.nio.file.Path tmpSbc = java.nio.file.Files.createTempFile("sbc_", ".dat");
        try {
            // 写入数据
            try (java.nio.channels.SeekableByteChannel sbc =
                     java.nio.file.Files.newByteChannel(tmpSbc,
                         java.nio.file.StandardOpenOption.WRITE,
                         java.nio.file.StandardOpenOption.READ)) {

                // 写 "Hello World!"
                sbc.write(java.nio.ByteBuffer.wrap("Hello World!".getBytes()));
                System.out.println();
                System.out.println("    写入 \"Hello World!\"  position=" + sbc.position()
                    + "  size=" + sbc.size());

                // 随机跳到第 6 字节，覆盖写 "NIO2!"
                sbc.position(6);
                sbc.write(java.nio.ByteBuffer.wrap("NIO2!".getBytes()));
                System.out.println("    position(6) → 覆盖写 \"NIO2!\"  position=" + sbc.position());

                // 回到开头读全部
                sbc.position(0);
                java.nio.ByteBuffer readBuf = java.nio.ByteBuffer.allocate((int) sbc.size());
                sbc.read(readBuf);
                readBuf.flip();
                System.out.println("    回到 position(0) 读全部 = \""
                    + new String(readBuf.array(), 0, readBuf.limit()) + "\"");
                System.out.println("    （\"Hello \" + \"NIO2!\" = \"Hello NIO2!\"）");
            }
        } finally {
            java.nio.file.Files.deleteIfExists(tmpSbc);
        }
        System.out.println();
        System.out.println("  ⚠ Files.newByteChannel vs FileChannel.open 选型：");
        System.out.println("    Files.newByteChannel()：返回 SeekableByteChannel（更通用）");
        System.out.println("    FileChannel.open()：     返回 FileChannel（含 transferTo/map 等高级特性）");
        System.out.println("    如果只需要简单读写：用 Files.newByteChannel 更简洁");
        System.out.println("    如果需要 transferTo/mmap/FileLock：用 FileChannel.open");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：AsynchronousFileChannel（AIO）——真正的异步文件 IO
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. AsynchronousFileChannel（AIO）—— 真正的异步文件 IO ━━━━━━━━");
        System.out.println();
        System.out.println("  ═══ 🍕 生活场景：点外卖的三种等待方式，深入理解AIO ═══");
        System.out.println();
        System.out.println("  你在家饿了，想点一份外卖（程序要读一个文件）：");
        System.out.println();
        System.out.println("  BIO（同步阻塞）— 你站在门口等：");
        System.out.println("    你打了电话点餐，然后就守在门口等，外卖没到你哪儿也去不了");
        System.out.println("    外卖到了你才能干别的事（线程阻塞，IO完成才继续执行）");
        System.out.println("    适合：偶尔等一次无所谓，但如果同时等1000份外卖你会累死");
        System.out.println();
        System.out.println("  NIO（同步非阻塞，IO多路复用）— 你在家干活，时不时去门口看看：");
        System.out.println("    你点了餐，回到书桌继续工作，每隔几分钟去门口看有没有外卖");
        System.out.println("    Selector 就是「外卖平台的振动手环」：哪个外卖到了它就振动通知你");
        System.out.println("    振动了，你还是要自己走到门口取（read() 这步你自己做）");
        System.out.println("    「取外卖」这个动作（数据从内核搬到你的碗里）你自己完成");
        System.out.println();
        System.out.println("  AIO（异步 IO）— 你请了跑腿小哥帮你全程处理：");
        System.out.println("    你告诉跑腿：「帮我点一份宫保鸡丁，直接放到我桌上，放好了来敲门通知我」");
        System.out.println("    然后你完全不管了，继续工作、睡觉、打游戏……");
        System.out.println("    外卖好了，跑腿取了，跑腿帮你摆好了，然后「叩叩叩」：「放好了！」");
        System.out.println("    这个「叩叩叩」就是 CompletionHandler.completed() 回调！");
        System.out.println("    两个阶段（等外卖好、取外卖）都是跑腿（OS）做的，你完全不参与");
        System.out.println();
        System.out.println("  ─────────────────────────────────────────────────────────");
        System.out.println("  关键区别：谁来「取外卖」（谁来把数据从内核缓冲区搬到你的buffer）？");
        System.out.println("    BIO/NIO：你自己取（调 read()，这步是同步的）");
        System.out.println("    AIO：跑腿帮你取（OS帮你搬，搬完回调通知你）");
        System.out.println("  ─────────────────────────────────────────────────────────");
        System.out.println();
        System.out.println("  AIO 的代码模式（对应跑腿场景）：");
        System.out.println("    「你告诉跑腿」= afc.read(buffer, position, attachment, handler)");
        System.out.println("    「这是我的地址/碗」= buffer（OS把食物放进这个碗里）");
        System.out.println("    「放好了来敲门」= handler.completed(bytesRead, attachment) 回调");
        System.out.println("    「如果出问题」= handler.failed(exception, attachment) 回调");
        System.out.println();
        System.out.println("  ⚠ AIO 的实际状况（Linux 的跑腿是「假跑腿」）：");
        System.out.println("    Windows：IOCP，真正的 OS 级异步，OS 全程帮你搬，性能很好");
        System.out.println("    Linux JDK7~18：底层用线程池模拟！跑腿其实是「另一个人」替你等、替你取");
        System.out.println("    效果和「你叫了一个朋友帮你站门口等、取了送给你」一样");
        System.out.println("    线程池模拟 = 换了个人做阻塞 IO，并不比 NIO 快");
        System.out.println("    所以 Netty 基于 NIO 而非 AIO（Linux 下 AIO 优势不明显）");
        System.out.println();
        System.out.println("  五种 IO 模型回顾（Part1 的内容，现在可以深刻理解了）：");
        System.out.println();
        System.out.println("  BIO（同步阻塞）：");
        System.out.println("    read() → 线程阻塞，直到数据就绪 + 数据搬运完成");
        System.out.println("    类比：你站在打印机旁边等，打印完才能走");
        System.out.println();
        System.out.println("  NIO（同步非阻塞）：");
        System.out.println("    channel.read() → 立即返回 0（没数据）或 n（有数据）");
        System.out.println("    Selector.select() → 等待「数据就绪」通知，然后你自己 read（搬运）");
        System.out.println("    类比：打印机好了来叫你（select），你再自己走过去取（read）");
        System.out.println("    注意：read() 这一步本身还是同步的（你在等搬运完成）");
        System.out.println();
        System.out.println("  AIO（异步 IO）：");
        System.out.println("    asyncChannel.read(buf, ..., handler) → 立即返回");
        System.out.println("    OS 完成数据搬运后，回调 handler.completed()");
        System.out.println("    类比：打印完了送货上门（handler 回调），你完全不用管");
        System.out.println("    两个阶段都不阻塞：等待就绪 + 数据搬运，都是 OS 做的");
        System.out.println();
        System.out.println("  ┌──────────────┬──────────────┬──────────────────────┐");
        System.out.println("  │ 模型         │ 等待数据就绪 │ 数据搬运（内核→用户）│");
        System.out.println("  ├──────────────┼──────────────┼──────────────────────┤");
        System.out.println("  │ BIO          │ 阻塞         │ 阻塞                 │");
        System.out.println("  │ NIO（轮询）  │ 非阻塞轮询   │ 阻塞                 │");
        System.out.println("  │ IO 多路复用  │ 阻塞在select │ 阻塞                 │");
        System.out.println("  │ 信号驱动IO   │ 非阻塞       │ 阻塞                 │");
        System.out.println("  │ AIO          │ 非阻塞       │ 非阻塞（OS 完成）    │");
        System.out.println("  └──────────────┴──────────────┴──────────────────────┘");
        System.out.println();
        System.out.println("  AsynchronousFileChannel 代码演示：");

        java.nio.file.Path tmpAio = java.nio.file.Files.createTempFile("aio_", ".dat");
        java.nio.file.Files.write(tmpAio, "AsyncIO Test Content - Hello AIO World!".getBytes());

        try (java.nio.channels.AsynchronousFileChannel afc =
                 java.nio.channels.AsynchronousFileChannel.open(tmpAio,
                     java.nio.file.StandardOpenOption.READ)) {

            java.nio.ByteBuffer aioBuffer = java.nio.ByteBuffer.allocate(64);
            java.util.concurrent.CountDownLatch aioLatch =
                new java.util.concurrent.CountDownLatch(1);
            final String[] result = {""};

            // 方式1：CompletionHandler 回调（最常用）
            System.out.println();
            System.out.println("  方式1：CompletionHandler 回调");
            System.out.println("  afc.read(buffer, position=0, attachment, new CompletionHandler<>() {");
            System.out.println("      public void completed(Integer bytesRead, Object attachment) {");
            System.out.println("          // 这里是回调，在 OS 的 IO 线程里执行，不阻塞你的线程");
            System.out.println("          buffer.flip();");
            System.out.println("          String content = new String(buffer.array(), 0, bytesRead);");
            System.out.println("          System.out.println(\"读到: \" + content);");
            System.out.println("      }");
            System.out.println("      public void failed(Throwable exc, Object attachment) { ... }");
            System.out.println("  });");

            afc.read(aioBuffer, 0, null,
                new java.nio.channels.CompletionHandler<Integer, Object>() {
                    @Override
                    public void completed(Integer bytesRead, Object attachment) {
                        aioBuffer.flip();
                        result[0] = new String(aioBuffer.array(), 0, bytesRead);
                        aioLatch.countDown();
                    }
                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        exc.printStackTrace();
                        aioLatch.countDown();
                    }
                });

            // 这里 main 线程可以继续干别的（没有阻塞）
            System.out.println("  → read() 调用完立即返回，main 线程继续执行其他任务...");
            aioLatch.await(3, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("  → 回调触发，读到内容: \"" + result[0] + "\"");
            System.out.println();

            // 方式2：Future 方式（可 get() 等待）
            System.out.println("  方式2：Future 方式（可以 get() 等结果）");
            java.nio.ByteBuffer buf2 = java.nio.ByteBuffer.allocate(64);
            java.util.concurrent.Future<Integer> future = afc.read(buf2, 0);
            // 可以在这里做其他事情，然后再 get()
            int bytesRead = future.get(); // 等待 IO 完成
            buf2.flip();
            System.out.println("  future.get() = " + bytesRead + " bytes");
            System.out.println("  内容 = \"" + new String(buf2.array(), 0, bytesRead) + "\"");
            System.out.println();

        } finally {
            java.nio.file.Files.deleteIfExists(tmpAio);
        }

        System.out.println("  AsynchronousFileChannel 写入：");
        System.out.println("    AsynchronousFileChannel afc = AsynchronousFileChannel.open(path,");
        System.out.println("        StandardOpenOption.WRITE, StandardOpenOption.CREATE);");
        System.out.println("    ByteBuffer data = ByteBuffer.wrap(\"hello async\".getBytes());");
        System.out.println("    afc.write(data, 0, null, new CompletionHandler<>() {");
        System.out.println("        public void completed(Integer written, Object att) {");
        System.out.println("            System.out.println(\"写入完成: \" + written + \" bytes\");");
        System.out.println("        }");
        System.out.println("        public void failed(Throwable exc, Object att) { ... }");
        System.out.println("    });");
        System.out.println();
        System.out.println("  ⚠ AIO 文件 IO 在生产中的注意事项：");
        System.out.println("    Linux：底层使用 io_uring（JDK19+）或 线程池模拟（JDK7~18）");
        System.out.println("    macOS：使用 kqueue + 线程池模拟");
        System.out.println("    Windows：使用 IOCP（真正内核异步，性能最好）");
        System.out.println("    ⚠ Linux JDK7~18 的 AIO 文件 IO 底层是线程池模拟的！");
        System.out.println("       并不比 NIO + 线程池性能更好，甚至更差");
        System.out.println("       真正受益的是 Windows（IOCP）和 JDK19+（io_uring）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：AsynchronousServerSocketChannel（AIO 网络 IO）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. AsynchronousServerSocketChannel（AIO 网络 IO）━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  NIO（Selector/epoll）vs AIO（AsynchronousChannel）网络 IO 对比：");
        System.out.println();
        System.out.println("  NIO Server（Selector）：");
        System.out.println("    while(true) {");
        System.out.println("        selector.select();  // 阻塞等「就绪」");
        System.out.println("        // 遍历就绪的 Channel，然后手动 read/write");
        System.out.println("    }");
        System.out.println("    你还是要自己调 read()（数据搬运这步你做）");
        System.out.println();
        System.out.println("  AIO Server（AsynchronousServerSocketChannel）：");
        System.out.println("    AsynchronousServerSocketChannel server =");
        System.out.println("        AsynchronousServerSocketChannel.open()");
        System.out.println("            .bind(new InetSocketAddress(8080));");
        System.out.println();
        System.out.println("    // accept 也是异步的！");
        System.out.println("    server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {");
        System.out.println("        public void completed(AsynchronousSocketChannel client, Object att) {");
        System.out.println("            // 有新连接了，可以在这里读数据");
        System.out.println("            ByteBuffer buf = ByteBuffer.allocate(1024);");
        System.out.println("            client.read(buf, buf, new CompletionHandler<>() {");
        System.out.println("                public void completed(Integer n, ByteBuffer b) {");
        System.out.println("                    b.flip();");
        System.out.println("                    // 数据已经搬运到 buf 里了！OS 帮我们搬的");
        System.out.println("                    process(b);");
        System.out.println("                    // 继续 accept 下一个连接");
        System.out.println("                    server.accept(null, this);");
        System.out.println("                }");
        System.out.println("                public void failed(Throwable e, ByteBuffer b) { ... }");
        System.out.println("            });");
        System.out.println("        }");
        System.out.println("        public void failed(Throwable exc, Object att) { ... }");
        System.out.println("    });");
        System.out.println();
        System.out.println("  AIO 网络 IO 的问题：");
        System.out.println("    ① 回调嵌套地狱（Callback Hell），代码可读性极差");
        System.out.println("    ② Linux 下用线程池模拟，和 NIO 性能差不多");
        System.out.println("    ③ Netty 基于 NIO 而非 AIO（Netty 团队评估后放弃了 AIO）");
        System.out.println("    → 实际生产：网络 IO 用 Netty（NIO），不用 AIO");
        System.out.println("    → AIO 的价值在 Windows IOCP 场景，或未来 io_uring 成熟后");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第六节：FileStore——磁盘信息查询
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 6. FileStore —— 磁盘信息查询 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  java.io.File.getTotalSpace() 的问题：");
        System.out.println("    只能查到「文件所在分区」的总大小，不能指定任意路径");
        System.out.println("    不知道文件系统类型（ext4？ntfs？tmpfs？）");
        System.out.println();

        java.nio.file.Path currentPath = java.nio.file.Paths.get(
            System.getProperty("java.io.tmpdir"));
        try {
            java.nio.file.FileStore store = java.nio.file.Files.getFileStore(currentPath);

            System.out.println("  查询 \"" + currentPath + "\" 所在磁盘：");
            System.out.println("    store.name()          = " + store.name());
            System.out.println("    store.type()          = " + store.type() + "  （文件系统类型）");
            System.out.println("    store.isReadOnly()    = " + store.isReadOnly());
            long gb = 1024L * 1024 * 1024;
            System.out.printf("    getTotalSpace()       = %.1f GB%n",
                (double) store.getTotalSpace() / gb);
            System.out.printf("    getUsableSpace()      = %.1f GB  （应用可用）%n",
                (double) store.getUsableSpace() / gb);
            System.out.printf("    getUnallocatedSpace() = %.1f GB  （未分配）%n",
                (double) store.getUnallocatedSpace() / gb);
            System.out.println();

            // 检查是否支持特定属性视图
            System.out.println("    支持的属性视图：");
            System.out.println("      supportsFileAttributeView(\"basic\")  = " +
                store.supportsFileAttributeView("basic"));
            System.out.println("      supportsFileAttributeView(\"posix\")  = " +
                store.supportsFileAttributeView("posix"));
            System.out.println("      supportsFileAttributeView(\"dos\")    = " +
                store.supportsFileAttributeView("dos"));
            System.out.println();

            System.out.println("  遍历系统所有磁盘：");
            java.nio.file.FileSystems.getDefault().getFileStores().forEach(fs -> {
                try {
                    System.out.printf("    %-20s type=%-8s total=%.1fGB usable=%.1fGB%n",
                        fs.name(), fs.type(),
                        (double) fs.getTotalSpace() / gb,
                        (double) fs.getUsableSpace() / gb);
                } catch (java.io.IOException ignored) {}
            });
        } catch (java.io.IOException e) {
            System.out.println("    (FileStore 查询失败: " + e.getMessage() + ")");
        }
        System.out.println();
        NIODemo.printSeparator();
    }
}

