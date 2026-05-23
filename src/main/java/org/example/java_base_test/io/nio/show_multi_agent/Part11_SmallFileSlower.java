package org.example.java_base_test.io.nio.show_multi_agent;

class Part11_SmallFileSlower {

    static void explain() {
        System.out.println("【第十一部分：小文件零拷贝为什么反而可能更慢】");
        System.out.println();
        System.out.println("═══ 🎁 生活场景：寄一张贺卡 vs 寄一整套家具 ═══");
        System.out.println();
        System.out.println("  零拷贝就像「专业搬家公司」：有固定的上门费（系统调用固定成本）");
        System.out.println("  传统拷贝就像「你自己搬」：每次搬运要出力（CPU拷贝），但没有上门费");
        System.out.println();
        System.out.println("  【小文件（4KB贺卡）场景】");
        System.out.println("  叫专业搬家公司搬一张贺卡：");
        System.out.println("    上门费 500元（sendfile固定开销 ~1μs）+ 搬卡费 0元");
        System.out.println("    总费用 500元");
        System.out.println("  自己搬一张贺卡：");
        System.out.println("    走路2分钟（read+write，~400ns）+ 体力消耗可忽略");
        System.out.println("  结论：小文件自己搬更划算，零拷贝反而亏了！");
        System.out.println();
        System.out.println("  【大文件（1GB家具）场景】");
        System.out.println("  叫专业搬家公司搬全套家具：");
        System.out.println("    上门费 500元（固定开销）+ 搬家费（节省大量CPU力气）");
        System.out.println("    总费用远低于自己搬（CPU可以去干其他事）");
        System.out.println("  自己搬一套家具：累死人（大量CPU拷贝，还霸占CPU干不了其他事）");
        System.out.println("  结论：大文件或大量请求时，零拷贝明显合算！");
        System.out.println();
        System.out.println("  【Nginx/Kafka 为什么还是用零拷贝服务小文件？】");
        System.out.println("  因为它们同时在发10万个请求！");
        System.out.println("  单次可能不省事，但省下的CPU能同时服务更多人");
        System.out.println("  零拷贝的价值：「提升系统整体吞吐量」不是「单次更快」");
        System.out.println();
        System.out.println("═══ 以下是技术细节 ═══");
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
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
    }
}

