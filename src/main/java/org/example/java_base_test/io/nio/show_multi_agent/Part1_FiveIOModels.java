package org.example.java_base_test.io.nio.show_multi_agent;

class Part1_FiveIOModels {

    static void explain() {
        System.out.println("【第一部分：五种 IO 模型】");
        System.out.println();
        System.out.println("理解 IO 模型是理解 BIO/NIO/AIO 的前提。");
        System.out.println("所有 IO 操作都要经历两个阶段：");
        System.out.println("  阶段1：等待数据就绪（数据从硬件到内核缓冲区）");
        System.out.println("  阶段2：数据拷贝（内核缓冲区 → 用户空间）");
        System.out.println();
        System.out.println("═══ 🍜 生活场景类比：用手机点外卖，理解五种 IO 模型 ═══");
        System.out.println();
        System.out.println("  你在办公室饿了，要点外卖。 '你' = 应用程序，'外卖' = 数据");
        System.out.println("  '外卖做好' = 数据到达内核缓冲区，'取到手' = 数据拷贝到你手里");
        System.out.println();
        System.out.println("  模型① BIO  → 你打电话给前台：「我要一份外卖，好了叫我」");
        System.out.println("             然后你就站在前台等，什么都不干，直到外卖到了才走");
        System.out.println("             （线程全程阻塞挂起，无法服务其他连接，线程资源白白占用）");
        System.out.println();
        System.out.println("  模型② NIO轮询 → 你回工位，每隔30秒起来跑一趟前台问：「外卖到了吗？」");
        System.out.println("             没到就回去，30秒后再跑一趟。如此反复。");
        System.out.println("             （CPU 一直忙着轮询，浪费在大量无效的「没到」）");
        System.out.println();
        System.out.println("  模型③ IO多路复用 → 外卖平台给你一个取餐器（振动手环），");
        System.out.println("             你回工位安心工作，多个外卖同时等，哪个振了去取哪个。");
        System.out.println("             （Selector！1个线程监控N个连接，有就绪才处理）");
        System.out.println();
        System.out.println("  模型④ 信号驱动 → 你把手机号给餐厅，外卖好了餐厅打你电话通知你");
        System.out.println("             但你还是要自己下楼去取（阶段2数据搬运自己做）");
        System.out.println();
        System.out.println("  模型⑤ AIO   → 你叫了跑腿，跑腿帮你等、取、送货上门，全程不用管");
        System.out.println("             外卖放桌上了才通知你（两阶段都是OS帮你完成）");
        System.out.println();
        System.out.println("═══ 以上就是5种IO模型的核心区别，下面是技术细节 ═══");
        System.out.println();

        System.out.println("模型① 阻塞 IO（BIO）← Java 传统 IO / BIO 服务器");
        System.out.println();
        System.out.println("  用户进程             内核");
        System.out.println("  ─────────            ────────────────────────");
        System.out.println("  read()  ──syscall──► 等待数据（阻塞中...）");
        System.out.println("  （线程挂起）             数据就绪，拷贝到用户空间");
        System.out.println("  read() 返回 ◄──────  返回");
        System.out.println("  特点：两个阶段都阻塞，线程全程等待");
        System.out.println("  🍜类比：你站在前台等外卖，外卖没到你哪也去不了，白白浪费时间");
        System.out.println();

        System.out.println("模型② 非阻塞 IO（NIO 轮询，注意不是 Java NIO！）");
        System.out.println();
        System.out.println("  用户进程             内核");
        System.out.println("  read()  ──────────► 没好，返回 EAGAIN");
        System.out.println("  read()  ──────────► 没好，返回 EAGAIN");
        System.out.println("  read()  ──────────► 就绪！拷贝数据，返回");
        System.out.println("  特点：阶段1不阻塞，但 CPU 一直轮询（忙等，浪费）");
        System.out.println("  🍜类比：每隔30秒跑一趟前台问「外卖到了吗」，没到就走，");
        System.out.println("         来回跑路消耗的精力比干正事还多（CPU空转）");
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
        System.out.println("  🍜类比：外卖振动手环（Selector），可以同时等10个外卖，");
        System.out.println("         哪个振了去取哪个，其余时间安心工作。");
        System.out.println("         ★ 这是Java NIO的核心！Selector就是振动手环");
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
        System.out.println("  🍜类比：叫了跑腿服务，跑腿帮你等外卖、取外卖、送到工位桌上");
        System.out.println("         放好了打电话通知你（回调），全程你不用动。");
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

