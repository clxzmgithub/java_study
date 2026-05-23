package org.example.java_base_test.io.nio.show_multi_agent;

class Part16_NIO2FilesAndWatch {

    static void demonstrate() throws Exception {
        System.out.println("【第十六部分：NIO.2 —— Path/Files/WatchService】");
        System.out.println();
        System.out.println("═══ 📁 生活场景：从「老式纸质档案」升级到「智能数字档案系统」═══");
        System.out.println();
        System.out.println("  java.io.File（旧API）就像用纸质档案管理系统：");
        System.out.println("  问题1：找文件失败只告诉你「失败了」，不说为什么");
        System.out.println("         就像档案员说「没找到」，但不说是被借走了还是根本没有");
        System.out.println("  问题2：不认识「快捷方式」（符号链接），操作快捷方式=操作原文件");
        System.out.println("  问题3：无法知道文件的详细信息（权限/所有者/精确修改时间）");
        System.out.println("  问题4：不能「监听」文件夹变化");
        System.out.println();
        System.out.println("  java.nio.file（NIO.2）就像智能数字档案系统：");
        System.out.println("  ✅ Path = 精确的文件「门牌号」（比 File 功能更强）");
        System.out.println("  ✅ Files = 一本操作手册，所有操作失败都告诉你原因");
        System.out.println("  ✅ WatchService = 摄像头，实时监控文件夹有没有新档案进来");
        System.out.println();
        System.out.println("  【WatchService 的典型应用场景】");
        System.out.println("  就像公司前台设了「收件箱摄像头」：");
        System.out.println("  一旦有新文件放进来（配置文件修改），立刻通知相关部门（热重载）");
        System.out.println("  开发工具（IDE）的「文件变更自动刷新」就是这个原理！");
        System.out.println("  Spring Boot 的 devtools 热重载，Webpack 的 watch 模式，都是它");
        System.out.println();
        System.out.println("═══ 以下是代码演示 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示1：Path 基本操作
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. Path 基本操作 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        java.nio.file.Path p = java.nio.file.Paths.get("/Users/demo/data/test.txt");
        System.out.println("  Path p = Paths.get(\"/Users/demo/data/test.txt\")");
        System.out.println("  p.getFileName()   = " + p.getFileName());
        System.out.println("  p.getParent()     = " + p.getParent());
        System.out.println("  p.getRoot()       = " + p.getRoot());
        System.out.println("  p.getNameCount()  = " + p.getNameCount() + "  （路径分量数）");
        System.out.println("  p.getName(1)      = " + p.getName(1) + "  （第2段路径）");
        System.out.println("  p.isAbsolute()    = " + p.isAbsolute());
        System.out.println();

        // resolve：拼接路径
        java.nio.file.Path base = java.nio.file.Paths.get("/Users/demo");
        java.nio.file.Path resolved = base.resolve("data/test.txt");
        System.out.println("  base.resolve(\"data/test.txt\") = " + resolved);

        // relativize：求相对路径
        java.nio.file.Path from = java.nio.file.Paths.get("/Users/demo/data");
        java.nio.file.Path to   = java.nio.file.Paths.get("/Users/demo/logs/app.log");
        System.out.println("  from.relativize(to) = " + from.relativize(to));
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示2：Files 常用操作
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. Files 常用操作（替代 File 的各种骚操作） ━━━━━━━━━━━━━━");
        System.out.println();

        java.nio.file.Path tmpDir  = java.nio.file.Files.createTempDirectory("nio2_demo_");
        java.nio.file.Path tmpFile = tmpDir.resolve("hello.txt");

        try {
            // 写文件
            java.nio.file.Files.write(tmpFile, "Hello NIO.2!\nLine 2\nLine 3".getBytes());
            System.out.println("  Files.write(path, bytes) 写入完成");

            // 读全部内容
            String content = new String(java.nio.file.Files.readAllBytes(tmpFile));
            System.out.println("  Files.readAllBytes(path) = \"" + content.replace("\n", "\\n") + "\"");

            // 按行读
            java.util.List<String> lines = java.nio.file.Files.readAllLines(tmpFile);
            System.out.println("  Files.readAllLines(path) 行数 = " + lines.size());
            lines.forEach(l -> System.out.println("    └─ \"" + l + "\""));

            // 文件属性
            System.out.println("  Files.size(path)         = " + java.nio.file.Files.size(tmpFile) + " bytes");
            System.out.println("  Files.exists(path)       = " + java.nio.file.Files.exists(tmpFile));
            System.out.println("  Files.isRegularFile(path)= " + java.nio.file.Files.isRegularFile(tmpFile));

            // 复制
            java.nio.file.Path copyFile = tmpDir.resolve("hello_copy.txt");
            java.nio.file.Files.copy(tmpFile, copyFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("  Files.copy(src, dst) 复制成功");

            // 移动/重命名
            java.nio.file.Path movedFile = tmpDir.resolve("hello_moved.txt");
            java.nio.file.Files.move(copyFile, movedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("  Files.move(src, dst) 移动成功");

            // 追加写
            java.nio.file.Files.write(tmpFile, "\nLine 4 appended".getBytes(),
                    java.nio.file.StandardOpenOption.APPEND);
            System.out.println("  Files.write(path, bytes, APPEND) 追加成功");
            System.out.println("  追加后内容行数 = "
                    + java.nio.file.Files.readAllLines(tmpFile).size());

            // createDirectories（递归建目录，不像 mkdir 要先建父目录）
            java.nio.file.Path nestedDir = tmpDir.resolve("a/b/c");
            java.nio.file.Files.createDirectories(nestedDir);
            System.out.println("  Files.createDirectories(\"a/b/c\") 递归创建成功");
            System.out.println();

            // ════════════════════════════════════════════════════════════
            // 演示3：Files.walk 遍历目录树
            // ════════════════════════════════════════════════════════════
            System.out.println("━━━ 3. Files.walk / walkFileTree 遍历目录树 ━━━━━━━━━━━━━━━");
            System.out.println();

            // 在临时目录建几个文件
            java.nio.file.Files.write(tmpDir.resolve("a/b/file1.txt"), "f1".getBytes());
            java.nio.file.Files.write(tmpDir.resolve("a/b/c/file2.txt"), "f2".getBytes());

            System.out.println("  Files.walk(tmpDir) 遍历所有文件：");
            try (java.util.stream.Stream<java.nio.file.Path> stream =
                         java.nio.file.Files.walk(tmpDir)) {
                stream.forEach(ep -> System.out.println("    " + tmpDir.relativize(ep)));
            }
            System.out.println();

            // 只找 .txt 文件
            System.out.println("  只找 .txt 文件：");
            try (java.util.stream.Stream<java.nio.file.Path> stream =
                         java.nio.file.Files.walk(tmpDir)) {
                stream.filter(ep -> ep.toString().endsWith(".txt"))
                      .forEach(ep -> System.out.println("    " + tmpDir.relativize(ep)));
            }
            System.out.println();

        } finally {
            // 递归删除临时目录
            try (java.util.stream.Stream<java.nio.file.Path> stream =
                         java.nio.file.Files.walk(tmpDir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .forEach(ep -> {
                          try { java.nio.file.Files.deleteIfExists(ep); } catch (Exception ignored) {}
                      });
            }
        }

        // ════════════════════════════════════════════════════════════════
        // 演示4：WatchService 目录监听
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. WatchService 目录监听 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  WatchService 原理：");
        System.out.println("    Java API         → OS 内核 inotify（Linux）/ FSEvents（macOS）");
        System.out.println("    注册目录           内核监控目录下的文件系统事件");
        System.out.println("    watcher.take()    → 阻塞等待事件（类似 select/epoll）");
        System.out.println("    有事件发生          → 返回 WatchKey，里面有事件列表");
        System.out.println();
        System.out.println("  三种事件类型：");
        System.out.println("    StandardWatchEventKinds.ENTRY_CREATE  文件/目录被创建");
        System.out.println("    StandardWatchEventKinds.ENTRY_MODIFY  文件被修改");
        System.out.println("    StandardWatchEventKinds.ENTRY_DELETE  文件/目录被删除");
        System.out.println();
        System.out.println("  代码模板：");
        System.out.println();
        System.out.println("  Path dir = Paths.get(\"/watch/this/dir\");");
        System.out.println("  WatchService watcher = FileSystems.getDefault().newWatchService();");
        System.out.println();
        System.out.println("  // 注册监听（可同时注册多种事件）");
        System.out.println("  dir.register(watcher,");
        System.out.println("      StandardWatchEventKinds.ENTRY_CREATE,");
        System.out.println("      StandardWatchEventKinds.ENTRY_MODIFY,");
        System.out.println("      StandardWatchEventKinds.ENTRY_DELETE);");
        System.out.println();
        System.out.println("  // 事件循环（通常在独立线程里）");
        System.out.println("  while (true) {");
        System.out.println("      WatchKey key = watcher.take(); // 阻塞，等待事件");
        System.out.println("      for (WatchEvent<?> event : key.pollEvents()) {");
        System.out.println("          WatchEvent.Kind<?> kind = event.kind();");
        System.out.println("          Path changed = (Path) event.context(); // 变化的文件名");
        System.out.println("          System.out.println(kind.name() + \": \" + changed);");
        System.out.println("      }");
        System.out.println("      // ★ 必须 reset()，否则 key 失效，后续事件收不到");
        System.out.println("      boolean valid = key.reset();");
        System.out.println("      if (!valid) break; // 目录被删除，监听失效");
        System.out.println("  }");
        System.out.println();
        System.out.println("  实际运行演示（后台线程监听，主线程触发文件变化）：");

        java.nio.file.Path watchDir = java.nio.file.Files.createTempDirectory("watch_");
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(2);
        java.util.List<String> events = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        // 启动监听线程
        Thread watchThread = new Thread(() -> {
            try (java.nio.file.WatchService watcher2 =
                         java.nio.file.FileSystems.getDefault().newWatchService()) {
                watchDir.register(watcher2,
                        java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
                        java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY,
                        java.nio.file.StandardWatchEventKinds.ENTRY_DELETE);
                while (true) {
                    java.nio.file.WatchKey key = watcher2.poll(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (key == null) break; // 超时退出
                    for (java.nio.file.WatchEvent<?> e : key.pollEvents()) {
                        String msg = e.kind().name() + ": " + e.context();
                        events.add(msg);
                        latch.countDown();
                    }
                    key.reset();
                }
            } catch (Exception ignored) {}
        });
        watchThread.setDaemon(true);
        watchThread.start();

        Thread.sleep(100); // 等监听线程就绪

        // 主线程触发事件
        java.nio.file.Path newFile = watchDir.resolve("test.txt");
        java.nio.file.Files.write(newFile, "created".getBytes());
        Thread.sleep(100);
        java.nio.file.Files.write(newFile, "modified".getBytes(), java.nio.file.StandardOpenOption.APPEND);

        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println("  捕获到的文件事件：");
        events.forEach(e -> System.out.println("    → " + e));

        // 清理
        java.nio.file.Files.deleteIfExists(newFile);
        java.nio.file.Files.deleteIfExists(watchDir);
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示5：File 的致命缺陷（和 Path/Files 对比）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. 为什么要从 java.io.File 迁移到 NIO.2 ━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  java.io.File 的历史缺陷：");
        System.out.println();
        System.out.println("  ① 方法失败只返回 false，不抛异常，无法知道原因");
        System.out.println("       file.delete()   失败 → 返回 false（权限问题？文件被占用？）");
        System.out.println("       file.mkdir()    失败 → 返回 false（父目录不存在？已存在？）");
        System.out.println("    Files 版本：Files.delete(path) 失败 → 抛 IOException，附带原因");
        System.out.println();
        System.out.println("  ② 不支持符号链接（symlink）");
        System.out.println("       file.isDirectory() 会跟随符号链接，无法判断「链接本身」");
        System.out.println("    Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) 可控制");
        System.out.println();
        System.out.println("  ③ 不支持文件属性（权限/所有者/创建时间）");
        System.out.println("    Files.getAttribute(path, \"unix:uid\")");
        System.out.println("    Files.getPosixFilePermissions(path)");
        System.out.println();
        System.out.println("  ④ 没有目录监听");
        System.out.println("    只有 WatchService 才能监听文件变化");
        System.out.println();
        System.out.println("  ⑤ File 和 Path 互转（与老代码兼容）：");
        System.out.println("    File → Path：  file.toPath()");
        System.out.println("    Path → File：  path.toFile()");
        System.out.println();
        System.out.println("  结论：新代码一律用 Path + Files，File 只用于和老 API 桥接");
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
    }
}

