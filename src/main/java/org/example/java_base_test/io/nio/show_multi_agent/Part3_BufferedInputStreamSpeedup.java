package org.example.java_base_test.io.nio.show_multi_agent;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class Part3_BufferedInputStreamSpeedup {

    static void explain() throws Exception {
        System.out.println("【第三部分：BufferedInputStream 为什么能大幅提速】");
        System.out.println();
        System.out.println("═══ 📦 生活场景：快递小哥送货 vs 货车批量运货 ═══");
        System.out.println();
        System.out.println("  没有 BufferedInputStream，就像：");
        System.out.println("  「快递小哥每次只拿一个包裹，从仓库跑到你家，放下，");
        System.out.println("   再跑回仓库拿下一个，再跑过来...」");
        System.out.println("  1MB数据 = 跑 100万趟！（每趟都是一次系统调用，超慢）");
        System.out.println();
        System.out.println("  加了 BufferedInputStream，就像：");
        System.out.println("  「货车一次拉 8192 个包裹（8KB），一趟送完，省了大量来回路程」");
        System.out.println("  1MB数据 = 只需跑 128 趟（系统调用减少了 8192 倍！）");
        System.out.println();
        System.out.println("  关键洞察：「路程本身（系统调用上下文切换）」比「搬包裹（拷贝数据）」");
        System.out.println("           耗时得多！所以减少次数才是王道。");
        System.out.println();
        System.out.println("  装饰器模式 = 就像给货车加装备：");
        System.out.println("    FileInputStream        = 原始货车（能跑但没缓冲）");
        System.out.println("    BufferedInputStream    = 加装大货箱（一次多拉货）");
        System.out.println("    DataInputStream        = 再加智能分拣系统（能解析Java基本类型）");
        System.out.println("    层层包装 = 层层加能力，这就是装饰器模式");
        System.out.println();
        System.out.println("═══ 以下是技术细节和实测数据 ═══");
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

