package org.example.java_base_test.io.nio;

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
 * 本文件按 Part1→Part23 严格顺序系统覆盖本地 IO 和网络 IO 全部核心知识点，
 * 每一层知识都在前一层基础上递进：
 *
 * ── 第一层：基础理论 ──────────────────────────────────────────────────
 * Part1  - 五种 IO 模型对比（BIO/NIO轮询/IO多路复用/信号驱动/AIO）
 * Part2  - BIO 的根本问题：一连接一线程
 * Part3  - BufferedInputStream 为什么能提速（系统调用次数 & 装饰器模式）
 *
 * ── 第二层：NIO 核心基础 ──────────────────────────────────────────────
 * Part4  - Buffer 三指针状态机（flip/clear/compact）← NIO 绝对地基
 * Part5  - Channel 类型体系与 FileChannel 不能非阻塞的原因
 *
 * ── 第三层：NIO 网络 IO ────────────────────────────────────────────────
 * Part6  - Selector + epoll 工作原理（epoll vs select/poll）
 * Part7  - SelectionKey 四种事件详解（含 OP_WRITE 陷阱）
 * Part8  - 完整 NIO Server（Reactor 单线程模式）
 * Part9  - transferTo 从 Java 到内核的完整调用链（零拷贝入门）
 * Part10 - 内核缓冲区满 & 网络拥塞导致 transferTo 传不完
 * Part11 - 小文件零拷贝反而慢的原因分析
 * Part12 - Kafka / Nginx / RocketMQ 案例：零拷贝的实际应用
 * Part13 - NIO 常见误区 & 选型指南（BIO vs NIO vs Netty 怎么选）
 *
 * ── 第四层：NIO 本地文件 IO ───────────────────────────────────────────
 * Part14 - FileChannel 本地文件 IO（read/write/position/force/truncate/FileLock）
 * Part15 - MappedByteBuffer 内存映射（mmap 原理 + 实战 + RocketMQ 源码）
 * Part16 - NIO.2 基础：Path/Files/WatchService 文件系统 API（替代 java.io.File）
 *
 * ── 第五层：Netty 高级网络框架 ───────────────────────────────────────
 * Part17 - Netty 核心架构（原生NIO七宗罪/EventLoop/Pipeline/ByteBuf）
 * Part18 - Netty 粘包拆包（FrameDecoder全家桶/自定义RPC协议/心跳）
 * Part19 - Netty 实战（Echo Server/HTTP Server/ChannelFuture/连接池）
 *
 * ── 第六层：开源框架 IO 深度解析 ─────────────────────────────────────
 * Part20 - Kafka 完整 IO 体系（存储结构/写入链路/sendfile消费/性能数字）
 * Part21 - 四大框架 IO 横向对比（Netty/Kafka/RocketMQ/Nginx）& 选型指南
 *
 * ── 第七层：NIO.2 进阶 & 新版 JDK IO 新特性 ──────────────────────────
 * Part22 - NIO.2 深度进阶（文件属性视图/符号链接/SeekableByteChannel/AIO/FileStore）
 * Part23 - JDK9~21 IO 新特性（transferTo/readNBytes/HttpClient/虚拟线程/结构化并发）
 *
 * 运行方式：执行 main() 方法，按提示查看各部分输出
 *
 * ============================================================
 * 七层进阶路线图（学习指南）
 * ============================================================
 *
 * 学习顺序遵循：问题 → 基础工具 → 网络应用 → 本地文件 → 工业级框架 → 开源实战 → 前沿新特性
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  第一层：基础理论（Part1 ~ Part3）                                │
 * │  "先搞清楚问题，再学解决方案"                                      │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Part1 - 五种IO模型对比                                           │
 * │    建立全局观，知道 BIO/NIO/AIO 是什么，为什么有这些东西              │
 * │  Part2 - BIO 的根本问题                                           │
 * │    知道老方案的痛点（一个连接占一个线程，1000连接就要1000线程）         │
 * │  Part3 - BufferedInputStream 为何能提速                           │
 * │    理解"减少系统调用次数"这个最基础的IO优化思路                       │
 * │                                                                  │
 * │  比喻：就像学开车前，先了解汽油车/电动车/混动车的区别，               │
 * │        以及为什么老式汽车费油。                                     │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  第二层：NIO 核心基础（Part4 ~ Part5）                             │
 * │  "NIO 的两块基石，不懂这两个后面全看不懂"                            │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Part4 - Buffer 三指针状态机（position / limit / capacity）        │
 * │    NIO 所有读写都靠 Buffer，这是绝对地基                            │
 * │    flip()  = 从"写入模式"切换到"读取模式"                           │
 * │    clear() = 重置，准备下一轮写入                                   │
 * │    compact() = 保留未读数据，继续追加写入                            │
 * │  Part5 - Channel 类型体系                                         │
 * │    数据通道是什么，FileChannel 为何不能非阻塞                        │
 * │                                                                  │
 * │  比喻：Buffer 就像一个"有刻度的水杯"                                │
 * │    position = 喝到哪了；limit = 能喝到哪；capacity = 杯子总容量     │
 * │    flip() = 从"倒水模式"切换到"喝水模式"                            │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  第三层：NIO 网络 IO（Part6 ~ Part13）                            │
 * │  "NIO 最核心的用武之地——高并发网络服务器"                            │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Part6  - Selector + epoll 原理                                  │
 * │    一个线程监控 N 个连接，NIO 能处理海量连接的秘密                    │
 * │  Part7  - SelectionKey 四种事件                                   │
 * │    连接/可读/可写/accept 事件怎么用，OP_WRITE 陷阱                  │
 * │  Part8  - 完整 NIO Server                                        │
 * │    把 Part4~Part7 串起来，写一个真正能跑的服务器                     │
 * │  Part9  - transferTo 零拷贝入门                                   │
 * │    传统方式拷贝4次，零拷贝只需2次，原理是什么                         │
 * │  Part10 - 缓冲区满 & 网络拥塞                                      │
 * │    transferTo 可能传不完，这个坑很多人踩                             │
 * │  Part11 - 小文件零拷贝反而慢                                       │
 * │    零拷贝不是万能的，什么时候用有讲究                                 │
 * │  Part12 - Kafka/Nginx 零拷贝案例                                  │
 * │    看真实开源项目怎么用这些技术                                      │
 * │  Part13 - 常见误区 & 选型指南                                      │
 * │    BIO/NIO/Netty 该怎么选，避免踩坑                                │
 * │                                                                  │
 * │  比喻：Selector 就像前台接待员，同时管理1000个客户的号码牌，           │
 * │    哪个客户有动静就处理哪个，而不是为每人单独安排一个服务员（BIO）       │
 * │                                                                  │
 * │  ⚠️  重点提示：这一层内容最多也最难，Part6~Part8 反复看几遍            │
 * │     再往下走，后面的零拷贝（Part9~Part12）理解起来会轻松很多            │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  第四层：NIO 本地文件 IO（Part14 ~ Part16）                        │
 * │  "NIO 操作磁盘文件的高级姿势"                                       │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Part14 - FileChannel 本地文件读写                                 │
 * │    文件锁 / 定位 / 强制刷盘等专业操作                                │
 * │  Part15 - MappedByteBuffer 内存映射（mmap）                       │
 * │    让文件"看起来像内存"，RocketMQ 就是这样存消息的                    │
 * │  Part16 - NIO.2 Path/Files/WatchService                          │
 * │    现代文件系统 API，监听文件变化，比 java.io.File 强大得多           │
 * │                                                                  │
 * │  比喻：MappedByteBuffer 就像把书的某几页"撕下来放在桌上"，            │
 * │    直接在上面写写画画，不用每次都翻书（不需要反复系统调用）              │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  第五层：Netty 高级网络框架（Part17 ~ Part19）                      │
 * │  "实际工作中没人裸写 NIO，Netty 才是主力"                            │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Part17 - Netty 核心架构                                          │
 * │    原生 NIO 有七个大坑，Netty 是怎么解决的                           │
 * │    EventLoop / Pipeline / ByteBuf 工作原理                        │
 * │  Part18 - 粘包拆包                                                │
 * │    TCP 是流式协议，一条"消息"可能被拆成几段收到                       │
 * │    Netty 的 FrameDecoder 全家桶怎么处理                             │
 * │  Part19 - Netty 实战                                              │
 * │    Echo 服务器 / HTTP 服务器，ChannelFuture 异步编程                │
 * │                                                                  │
 * │  为什么第五层才学 Netty？                                           │
 * │    不懂 NIO 的人学 Netty 只会"照猫画虎"，不知道为什么这样设计。        │
 * │    懂了 NIO 再学 Netty，会发现每个设计都在解决 NIO 的具体痛点。        │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  第六层：开源框架 IO 深度解析（Part20 ~ Part21）                    │
 * │  "看顶级开源项目如何把这些知识用到极致"                               │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Part20 - Kafka 完整 IO 体系                                      │
 * │    顺序写磁盘 + Page Cache + sendfile，为何能支撑百万 QPS            │
 * │  Part21 - 四大框架横向对比                                         │
 * │    Netty / Kafka / RocketMQ / Nginx 的 IO 设计对比（面试神器）      │
 * │                                                                  │
 * │  比喻：就像学会了烹饪基础后，去看米其林厨师的菜谱，                    │
 * │    理解专业厨师为什么每一步都这样做。                                 │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  第七层：NIO.2 进阶 & JDK 新特性（Part22 ~ Part23）               │
 * │  "紧跟 Java 技术演进，了解最新方向"                                 │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Part22 - NIO.2 深度进阶                                          │
 * │    文件属性视图 / 符号链接 / AIO 异步IO / FileStore 磁盘信息         │
 * │  Part23 - JDK9~21 IO 新特性                                       │
 * │    HttpClient / 虚拟线程（Project Loom）/ 结构化并发                │
 * │                                                                  │
 * │  ★ 虚拟线程是重磅更新（JDK21）：                                    │
 * │    让 BIO 风格的代码也能达到 NIO 的并发性能，                        │
 * │    是对整个 IO 体系的一次颠覆性补充。                                │
 * │    放在最后学，才能真正理解"为什么这是革命性的"。                      │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ============================================================
 */
public class NIODemo {

    public static void main(String[] args) throws Exception {
        System.out.println("============================================================");
        System.out.println("  Java IO / NIO 全体系深度演示");
        System.out.println("============================================================");
        System.out.println();

        // ── 第一层：基础理论（Part1 ~ Part3）────────────────────────────
        Part1_FiveIOModels.explain();                  // Part1  五种IO模型对比
        Part2_BIOProblem.explain();                    // Part2  BIO根本问题
        Part3_BufferedInputStreamSpeedup.explain();    // Part3  BufferedInputStream提速原理

        // ── 第二层：NIO 核心基础（Part4 ~ Part5）─────────────────────
        Part4_BufferStateMachine.demonstrate();        // Part4  Buffer三指针状态机
        Part5_ChannelTypes.explain();                  // Part5  Channel类型体系

        // ── 第三层：NIO 网络 IO（Part6 ~ Part13）─────────────────────
        Part6_SelectorEpoll.explain();                 // Part6  Selector+epoll原理
        Part7_SelectionKeyEvents.explain();            // Part7  SelectionKey四种事件
        Part8_NIOServerExplained.explain();            // Part8  完整NIO Server
        Part9_TransferToCallChain.explain();           // Part9  transferTo调用链（零拷贝）
        Part10_BufferFullAndCongestion.explain();      // Part10 缓冲区满&网络拥塞
        Part11_SmallFileSlower.explain();              // Part11 小文件零拷贝反而慢
        Part12_KafkaNginxCases.explain();              // Part12 Kafka/Nginx零拷贝案例
        Part13_MistakesAndSelection.explain();         // Part13 NIO常见误区&选型指南

        // ── 第四层：NIO 本地文件 IO（Part14 ~ Part16）────────────────
        Part14_FileChannelLocalIO.demonstrate();       // Part14 FileChannel本地文件IO
        Part15_MappedByteBuffer.demonstrate();         // Part15 MappedByteBuffer内存映射
        Part16_NIO2FilesAndWatch.demonstrate();        // Part16 NIO.2 Path/Files/WatchService

        // ── 第五层：Netty 高级网络框架（Part17 ~ Part19）─────────────
        Part17_NettyArchitecture.explain();            // Part17 Netty核心架构
        Part18_NettyFrameDecoder.explain();            // Part18 Netty粘包拆包
        Part19_NettyPractice.explain();                // Part19 Netty实战

        // ── 第六层：开源框架 IO 深度解析（Part20 ~ Part21）───────────
        Part20_KafkaIODeepDive.explain();              // Part20 Kafka完整IO体系
        Part21_FrameworkIOComparison.explain();        // Part21 四大框架IO横向对比

        // ── 第七层：NIO.2 进阶 & 新版 JDK 新特性（Part22 ~ Part23）──
        Part22_NIO2Advanced.demonstrate();             // Part22 NIO.2深度进阶/AIO/FileStore
        Part23_ModernJdkIOFeatures.demonstrate();      // Part23 JDK9~21 IO新特性

        System.out.println("============================================================");
        System.out.println("  全部演示完毕（共 23 个部分，七层进阶 Part1→Part23）");
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
        System.out.println("═══ 🍜 生活场景类比：用手机点外卖，理解五种 IO 模型 ═══");
        System.out.println();
        System.out.println("  你在办公室饿了，要点外卖。 '你' = 应用程序，'外卖' = 数据");
        System.out.println("  '外卖做好' = 数据到达内核缓冲区，'取到手' = 数据拷贝到你手里");
        System.out.println();
        System.out.println("  模型① BIO  → 你打电话给前台：「我要一份外卖，好了叫我」");
        System.out.println("             然后你就站在前台等，什么都不干，直到外卖到了才走");
        System.out.println("             （线程完全阻塞，CPU 全程白白浪费）");
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

// ====================================================================
// 第二部分：BIO 的根本问题——一个连接一个线程
// ====================================================================
class Part2_BIOProblem {

    static void explain() {
        System.out.println("【第二部分：BIO 的根本问题——一连接一线程】");
        System.out.println();
        System.out.println("═══ 🏦 生活场景：想象一家银行网点 ═══");
        System.out.println();
        System.out.println("  BIO 的工作方式 = 老式银行的「一对一服务」：");
        System.out.println();
        System.out.println("  【BIO 模式】");
        System.out.println("  进来1个客户 → 柜台叫一个服务员专门陪着他");
        System.out.println("  进来10个客户 → 派10个服务员，每人盯着一个");
        System.out.println("  进来1000个客户 → 需要1000个服务员！！！");
        System.out.println();
        System.out.println("  问题1【内存不够】：");
        System.out.println("    每个服务员（线程）要占一间休息室（内存 ~1MB 栈）");
        System.out.println("    1000个服务员 = 需要1000间休息室（1GB内存）→ 撑不住");
        System.out.println();
        System.out.println("  问题2【大部分时间在摸鱼】：");
        System.out.println("    99%的时间，服务员坐在那等客户开口（阻塞在 read()）");
        System.out.println("    真正在处理业务的时间 < 1%");
        System.out.println("    1000个服务员，990个在睡觉，10个在干活");
        System.out.println("    还要频繁切换谁来工作（上下文切换开销巨大）");
        System.out.println();
        System.out.println("  【NIO 模式（对比）】");
        System.out.println("  一个「大堂经理」(Selector 线程) 管理所有客户取号");
        System.out.println("  谁的号到了（数据就绪）就叫谁，后台只需少量处理人员");
        System.out.println("  10个人轻松处理1000个客户！");
        System.out.println();
        System.out.println("═══ 以下是技术代码分析 ═══");
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

// ====================================================================
// 第四部分：Buffer 三指针状态机（flip / clear / compact）
//
// ── 这部分讲什么？干什么用？ ──────────────────────────────────────────
//
// NIO 与传统 BIO 最核心的区别之一就是：
//   BIO：直接操作 InputStream/OutputStream，数据像"水流"一样流动
//   NIO：所有数据的读写都必须经过 Buffer，数据先装进桶，再统一处理
//
// ByteBuffer 就是那个"桶"。
// 但这个桶不是普通的桶，它内部有 3 个指针（游标）来精确控制：
//   - 现在写到哪了？
//   - 现在读到哪了？
//   - 总共能装多少？
//
// flip() / clear() / compact() 就是用来切换这 3 个指针状态的操作。
// 搞不懂这个，NIO 的所有代码都看不懂，这是 NIO 的绝对地基。
//
// ── ByteBuffer 的内存结构（源码中的字段） ─────────────────────────────
//
// java.nio.Buffer 源码（JDK 8）：
//
//   private int mark = -1;   // 标记位，调用 mark() 时记录当前 position
//   private int position = 0; // 当前游标：下一次 get/put 从这里开始
//   private int limit;        // 有效边界：不能读/写超过这里
//   private int capacity;     // 总容量：创建后不变
//
// 四者关系必须满足：0 <= mark <= position <= limit <= capacity
//
// 可以这样理解——把 ByteBuffer 想象成一个停车场：
//
//   capacity = 停车场总车位数（10个）
//   limit    = 今天开放的车位上限（可能只开放了前8个）
//   position = 下一辆车停的位置（当前游标）
//   mark     = 某个"书签"位置（可以随时回来）
//
// ── 两种"模式"的概念 ──────────────────────────────────────────────────
//
// ByteBuffer 没有显式的"读模式/写模式"标志位，
// 但通过约定，三个指针的值不同代表不同用途：
//
// 【写模式】刚 allocate 或 clear() 后：
//   position = 0（从头写）
//   limit    = capacity（全部空间都能写）
//   图示：
//   [0][1][2][3][4][5][6][7][8][9]
//    P                             L=C
//   (position=0, limit=10, capacity=10)
//
// 往里写 3 个字节 put('H'), put('i'), put('!') 后：
//   [H][i][!][3][4][5][6][7][8][9]
//               P                 L=C
//   (position=3, limit=10)
//   position 自动向右移动到 3
//
// 【读模式】flip() 后：
//   limit    = position（之前写到哪，读就只能读到哪）
//   position = 0（从头读）
//   图示：
//   [H][i][!][3][4][5][6][7][8][9]
//    P        L
//   (position=0, limit=3)
//   limit 收紧到 3，防止读到没写过的垃圾数据
//
// ────────────────────────────────────────────────────────────────────
// ====================================================================
class Part4_BufferStateMachine {

    static void demonstrate() {
        System.out.println("【第四部分：Buffer 三指针状态机（flip / clear / compact）】");
        System.out.println();
        System.out.println("═══ 🎙️ 生活场景：把 ByteBuffer 想象成一盒录音磁带 ═══");
        System.out.println();
        System.out.println("  想象一盒旧式录音磁带，你要用它来录音、再播放：");
        System.out.println();
        System.out.println("  【磁带的三个指针】");
        System.out.println("  capacity  = 磁带总长度（买来是多少就是多少，不变）");
        System.out.println("  position  = 录音/播放头现在在哪个位置（当前游标）");
        System.out.println("  limit     = 这次最多能录/能播到哪里（有效边界）");
        System.out.println();
        System.out.println("  【录音阶段 = 写模式】刚买来的磁带：");
        System.out.println("    position = 0（从头录）");
        System.out.println("    limit = capacity（整盘磁带都能录）");
        System.out.println("    你开始录音「Hello」，录音头自动往后走5格");
        System.out.println("    position 变成 5，剩余的磁带还空着");
        System.out.println();
        System.out.println("  【flip() = 录完了，切换成播放模式】");
        System.out.println("    把磁带倒回到开头（position = 0）");
        System.out.println("    同时告诉播放器「只播到第5格就停」（limit = 5）");
        System.out.println("    → 防止播放到后面空白区域（对应未写入数据）");
        System.out.println();
        System.out.println("  【播放中 = 读模式】");
        System.out.println("    播放头从 0 开始往后走，播到 limit=5 停止");
        System.out.println("    播放头走到哪就是 position");
        System.out.println();
        System.out.println("  【clear() = 播完了，重新录制（磁带不擦，只归零）】");
        System.out.println("    position = 0，limit = capacity");
        System.out.println("    ⚠️ 磁带上旧内容还在，只是录音头回到开头，下次录会覆盖");
        System.out.println("    就像磁带「清空」其实没有消磁，只是重新从头录而已");
        System.out.println();
        System.out.println("  【compact() = 播了一半，中途要接着录】");
        System.out.println("    场景：磁带上有 ABC 三首歌，播完 AB 后想接着录新歌 D");
        System.out.println("         但 C 还没播完，不能直接清空！");
        System.out.println("    compact() 做的事：把 C 移到最前面，然后录音头从 C 后面开始");
        System.out.println("    这样下次可以先听 C 再听 D（粘包处理的核心！）");
        System.out.println();
        System.out.println("═══ 以下是技术演示（对照上面的比喻来看）═══");
        System.out.println();
        System.out.println("━━━ ByteBuffer 的内存结构 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ByteBuffer 底层就是一个 byte[]，");
        System.out.println("  配合 4 个指针精确控制读写位置：");
        System.out.println();
        System.out.println("  capacity  = 总容量，创建后永远不变");
        System.out.println("  limit     = 有效边界（写模式=capacity；读模式=已写入量）");
        System.out.println("  position  = 当前游标（下一次 get/put 的位置）");
        System.out.println("  mark      = 书签（调用 mark() 记录，reset() 跳回）");
        System.out.println();
        System.out.println("  约束：0 <= mark <= position <= limit <= capacity");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 1：完整生命周期：allocate → put → flip → get → clear
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示1：完整生命周期 allocate→put→flip→get→clear ━━━━━━━━━━━");
        System.out.println();

        // Step1: allocate
        ByteBuffer buf = ByteBuffer.allocate(10);
        System.out.println("  Step1: ByteBuffer.allocate(10)");
        System.out.println("  底层创建了 byte[10]，指针初始化：");
        printBufferDiagram("allocate后【写模式】", buf, "← P（下一次put从这里写）");
        // 图示：capacity=10, limit=10, position=0
        // [_][_][_][_][_][_][_][_][_][_]
        //  P                             L=C

        // Step2: put
        buf.put((byte) 'H');
        buf.put((byte) 'i');
        buf.put((byte) '!');
        System.out.println("  Step2: put('H'), put('i'), put('!') 各一次");
        System.out.println("  每次 put 都把 position+1，游标右移：");
        printBufferDiagram("put 3字节后【写模式】", buf, "← P（下一次put从这里写）");
        // [H][i][!][_][_][_][_][_][_][_]
        //           P                    L=C

        // Step3: flip
        System.out.println("  Step3: buf.flip()");
        System.out.println("  flip() 源码等价于：");
        System.out.println("    limit = position;  // 把 limit 收紧到已写位置");
        System.out.println("    position = 0;      // 游标归零，从头读");
        System.out.println("    mark = -1;         // 清除书签");
        System.out.println("  作用：切换为【读模式】，limit 就是防止读到未写区域的护栏");
        buf.flip();
        printBufferDiagram("flip()后【读模式】", buf, "← P（下一次get从这里读）");
        // [H][i][!][_][_][_][_][_][_][_]
        //  P        L

        // Step4: get
        byte b1 = buf.get(); // 读 'H'
        byte b2 = buf.get(); // 读 'i'
        System.out.println("  Step4: get() 两次，读到：'" + (char)b1 + "' 和 '" + (char)b2 + "'");
        System.out.println("  每次 get 把 position+1，游标右移：");
        printBufferDiagram("get两次后【读模式，'!'未读】", buf, "← P（下一次get从这里读）");
        // [H][i][!][_][_][_][_][_][_][_]
        //       P  L

        // Step5: clear
        System.out.println("  Step5: buf.clear()");
        System.out.println("  clear() 源码等价于：");
        System.out.println("    position = 0;       // 游标归零");
        System.out.println("    limit = capacity;   // limit 恢复到最大");
        System.out.println("    mark = -1;          // 清除书签");
        System.out.println("  ⚠ 重要：clear() 并不清空 byte[] 里的数据！");
        System.out.println("    '!' 还安静地躺在 index=2 里，只是下次 put 会覆盖它");
        System.out.println("    所以叫 clear 有点误导，准确说是「重置指针，假装清空」");
        buf.clear();
        printBufferDiagram("clear()后【写模式，数据未清】", buf, "← P（重新从头写）");
        // [H][i][!][_][_][_][_][_][_][_]  ← 数据还在！只是指针归零了
        //  P                              L=C
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 2：compact —— NIO 粘包处理的核心操作
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示2：compact() —— 粘包场景下的救命操作 ━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  真实场景：TCP 是流式协议，没有消息边界");
        System.out.println("  一次 channel.read(buf) 可能读到：");
        System.out.println("    「消息A 完整数据(3字节)」+「消息B 的半包(2字节)」");
        System.out.println("  处理完消息A后，消息B的半包不能丢，不能用 clear()！");
        System.out.println("  这时就要用 compact()");
        System.out.println();

        ByteBuffer buf2 = ByteBuffer.allocate(10);
        // 模拟 channel.read(buf) 一次读进来 5 字节
        buf2.put(new byte[]{'A', 'A', 'A', 'B', 'B'}); // AAA=消息A，BB=消息B半包
        buf2.flip();
        printBufferDiagram("channel.read后 flip【读模式】", buf2, "← P（准备读取）");
        // [A][A][A][B][B][_][_][_][_][_]
        //  P              L

        // 处理消息A：读3字节
        buf2.get(); buf2.get(); buf2.get();
        printBufferDiagram("读完消息A(3字节)，BB是消息B半包", buf2, "← P（'B','B'还没读）");
        // [A][A][A][B][B][_][_][_][_][_]
        //           P   L

        System.out.println("  此时如果用 clear()，'B','B' 就丢了！");
        System.out.println("  应该用 compact()：");
        System.out.println("  compact() 做的事：");
        System.out.println("    1. 把 [position, limit) 之间的未读数据复制到数组头部");
        System.out.println("    2. position = 未读数据量（2）");
        System.out.println("    3. limit = capacity（开放全部空间，准备继续写入）");
        buf2.compact();
        printBufferDiagram("compact()后【写模式，BB保留在头部】", buf2, "← P（从这里继续写新数据）");
        // [B][B][A][B][B][_][_][_][_][_]  ← 前2位是BB，后面是旧数据残留（会被覆盖）
        //       P                          L=C
        System.out.println("  下次 channel.read(buf) 会把新数据追加到 position=2 之后");
        System.out.println("  再次 flip() 后，可以读到完整的 消息B（BB + 新到的数据）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 3：三个操作的对比表
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示3：flip / clear / compact 对比速查表 ━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌───────────┬──────────────────────────────┬──────────────────────────────┐");
        System.out.println("  │  操作     │  指针变化                    │  使用场景                    │");
        System.out.println("  ├───────────┼──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ flip()    │ limit=position, position=0   │ 写完数据，准备读取/发送      │");
        System.out.println("  │           │ 写模式 → 读模式              │ channel.write(buf)之前必调   │");
        System.out.println("  ├───────────┼──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ clear()   │ position=0, limit=capacity   │ 数据已全部读完，复用Buffer   │");
        System.out.println("  │           │ 任意 → 写模式（不清数据）    │ ⚠ 未读数据会丢失！          │");
        System.out.println("  ├───────────┼──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ compact() │ 未读数据移到头部             │ 处理粘包/半包，保留未读数据  │");
        System.out.println("  │           │ position=剩余量,limit=cap    │ 读模式 → 写模式（保留数据）  │");
        System.out.println("  └───────────┴──────────────────────────────┴──────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 4：Heap Buffer vs Direct Buffer
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示4：Heap Buffer vs Direct Buffer ━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ByteBuffer.allocate(1024)       ← Heap Buffer（堆内存）");
        System.out.println("  ByteBuffer.allocateDirect(1024) ← Direct Buffer（堆外内存）");
        System.out.println();
        System.out.println("  【Heap Buffer】");
        System.out.println("  数据存在 JVM 堆里，GC 会管理、移动它的内存地址。");
        System.out.println("  做 IO 时（如 channel.write(buf)），JVM 需要：");
        System.out.println("    ① 先把堆内数据拷贝一份到堆外（固定地址）");
        System.out.println("    ② 再让内核从堆外地址做 DMA");
        System.out.println("  多了一次内存拷贝的原因：GC 随时可能移动堆对象，");
        System.out.println("  DMA 是异步操作，它不认识会移动的地址");
        System.out.println();
        System.out.println("  【Direct Buffer】");
        System.out.println("  数据存在堆外（直接调用 OS 的 malloc），地址固定不动。");
        System.out.println("  内核 DMA 可以直接操作这块内存，省去堆→堆外的拷贝。");
        System.out.println("  代价：分配/释放慢（走 OS），释放依赖 GC 触发 Cleaner，");
        System.out.println("        不适合频繁创建小 Buffer");
        System.out.println();
        System.out.println("  【内存路径对比】");
        System.out.println("  Heap Buffer IO：  磁盘/网卡 → 内核缓冲区 → 堆外临时区 → JVM堆  (3次拷贝)");
        System.out.println("  Direct Buffer IO：磁盘/网卡 → 内核缓冲区 → 堆外Direct区         (2次拷贝)");
        System.out.println();
        System.out.println("  【Netty 的解法：内存池 PooledByteBufAllocator】");
        System.out.println("  Direct Buffer 创建慢的问题，Netty 用内存池解决：");
        System.out.println("  预先 allocateDirect 一大块，然后切分复用，避免频繁 malloc/free");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 5：最经典的 Bug：写完忘了 flip()
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示5：最经典 Bug —— 写完忘了 flip() ━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        ByteBuffer bugBuf = ByteBuffer.allocate(10);
        bugBuf.put("hello".getBytes());
        // ↑ 正常应该调用 flip()，这里故意不调用，模拟 Bug

        System.out.println("  写入 'hello'（5字节），忘了调用 flip()，直接发送给 channel：");
        System.out.println("    position=" + bugBuf.position()
                + ", limit=" + bugBuf.limit()
                + ", remaining=" + bugBuf.remaining());
        System.out.println();
        System.out.println("  问题：channel.write(bugBuf) 或 bugBuf.get() 从 position=5 开始读");
        System.out.println("        但 limit=10，remaining=5，看起来像还有5字节可读");
        System.out.println("        实际读到的是 index[5]~[9]，全是初始值 0x00！");
        System.out.println("        对端收到的是 5 个 \\0 字节，'hello' 一个字没发出去！");
        System.out.println();
        System.out.println("  ✅ 正确写法：");
        System.out.println("    ByteBuffer buf = ByteBuffer.allocate(10);");
        System.out.println("    buf.put(\"hello\".getBytes());");
        System.out.println("    buf.flip();                          // ← 关键！切换为读模式");
        System.out.println("    channel.write(buf);                  // 现在才会读 [0,5) 的数据");
        System.out.println();

        // 演示正确写法
        ByteBuffer correctBuf = ByteBuffer.allocate(10);
        correctBuf.put("hello".getBytes());
        correctBuf.flip(); // 正确！
        byte[] readData = new byte[correctBuf.remaining()];
        correctBuf.get(readData);
        System.out.println("  验证正确写法，flip后 get 到的内容：\"" + new String(readData) + "\"");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 演示 6：mark() & reset() 书签功能
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 演示6：mark() & reset() 书签功能 ━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  场景：先读几个字节判断消息类型，再重新从头解析完整消息");
        ByteBuffer markBuf = ByteBuffer.allocate(10);
        markBuf.put(new byte[]{'M', 'S', 'G', ':', 'H', 'i'});
        markBuf.flip();

        System.out.println("  数据：[M][S][G][:][H][i]，flip后准备读取");
        printBufferDiagram("flip后", markBuf, "← P");

        // 读3字节判断消息头
        byte t1 = markBuf.get();
        byte t2 = markBuf.get();
        byte t3 = markBuf.get();
        System.out.println("  读前3字节判断类型：'" + (char)t1 + (char)t2 + (char)t3 + "'");

        // 在读完头部之后，mark 当前位置（第4字节 ':'）
        markBuf.mark();
        System.out.println("  mark() 在 position=" + markBuf.position() + " 打上书签");

        // 继续读
        markBuf.get(); // ':'
        markBuf.get(); // 'H'
        System.out.println("  继续读了2字节，现在 position=" + markBuf.position());

        // 发现需要重新从':' 开始解析，reset回去
        markBuf.reset();
        System.out.println("  reset() 跳回书签位置，position=" + markBuf.position());
        byte back = markBuf.get();
        System.out.println("  再次 get() 读到：'" + (char)back + "'（正是 ':' ）");
        System.out.println();

        NIODemo.printSeparator();
    }

    // 打印 Buffer 的可视化状态图
    static void printBufferDiagram(String label, ByteBuffer buf, String ignored) {
        int cap  = buf.capacity();
        int lim  = buf.limit();
        int pos  = buf.position();

        System.out.println("  [" + label + "]");

        // 第一行：数组内容（直接访问 array()，不受 limit 限制）
        byte[] array = buf.array();
        StringBuilder cells = new StringBuilder("    |");
        for (int i = 0; i < cap; i++) {
            byte b = array[i];
            String cell = b == 0 ? " _ " : " " + (char) b + " ";
            cells.append(cell).append("|");
        }
        System.out.println(cells);

        // 第二行：指针标记
        StringBuilder pointers = new StringBuilder("     ");
        for (int i = 0; i < cap; i++) {
            String marker = "   ";
            if (i == pos && i == lim) marker = "P=L";
            else if (i == pos)        marker = " P ";
            else if (i == lim)        marker = " L ";
            else                      marker = "   ";
            pointers.append(marker).append(" ");
        }
        // 如果 pos == cap，P 打在末尾
        if (pos == cap) pointers.append(" P");
        if (lim == cap) pointers.append("(L=C)");
        System.out.println(pointers);

        System.out.println("    capacity=" + cap + "  limit=" + lim
                + "  position=" + pos + "  remaining=" + buf.remaining());
        System.out.println();
    }
}

// ====================================================================
// 第五部分：Channel 类型体系
// ====================================================================
class Part5_ChannelTypes {

    static void explain() {
        System.out.println("【第五部分：Channel 类型体系】");
        System.out.println();
        System.out.println("═══ 🚰 生活场景：Stream vs Channel 的本质区别 ═══");
        System.out.println();
        System.out.println("  传统 IO 的 Stream（流）：");
        System.out.println("    就像家里的水龙头：");
        System.out.println("    - 要么只能放水（OutputStream）");
        System.out.println("    - 要么只能接水（InputStream）");
        System.out.println("    - 不能暂停，数据像流水一样单向流动");
        System.out.println("    - 只能顺着读，不能跳到中间某个位置");
        System.out.println();
        System.out.println("  NIO 的 Channel（通道）：");
        System.out.println("    就像家里的城市供水管道（双向、有阀门、可精确控制）：");
        System.out.println("    - 双向：同一根管道既能进水也能出水（FileChannel 可读可写）");
        System.out.println("    - 可以跳到指定位置（position(long) 直接跳到文件第N字节）");
        System.out.println("    - 必须配合「水桶」（Buffer）使用，不能直接拧开就用");
        System.out.println("    - 可以非阻塞（网络Channel），可以注册到Selector统一管理");
        System.out.println();
        System.out.println("  Buffer（缓冲区）= 装水的桶：");
        System.out.println("    数据不能直接从 Channel 流向你，");
        System.out.println("    必须先装进「桶」（Buffer），你再从桶里取");
        System.out.println("    Channel → Buffer → 你的代码   (读)");
        System.out.println("    你的代码 → Buffer → Channel   (写)");
        System.out.println();
        System.out.println("  ★ 关键区别记忆：");
        System.out.println("    Stream = 单向水龙头，直接流，不需要桶");
        System.out.println("    Channel = 双向管道，必须配合桶（Buffer），功能更强大");
        System.out.println();
        System.out.println("═══ 以下是各种 Channel 类型的详细介绍 ═══");
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
        System.out.println("═══ 🍽️ 生活场景：餐厅取号系统 理解 Selector+epoll ═══");
        System.out.println();
        System.out.println("  想象一家超级火爆的餐厅，同时有1000桌客人在等候：");
        System.out.println();
        System.out.println("  【老方案 select/poll = 服务员轮流问每桌】");
        System.out.println("    服务员从第1桌到第1000桌，挨个问：「你要点菜了吗？」");
        System.out.println("    大多数桌还没准备好，但还是要问一遍");
        System.out.println("    1000桌全问完一轮 → 才能知道谁准备好了");
        System.out.println("    客人越多，轮一圈越慢 → 这就是 O(n) 的问题");
        System.out.println();
        System.out.println("  【新方案 epoll = 餐厅取号+呼叫器系统】");
        System.out.println("    每桌客人拿到一个呼叫器（fd 注册到 epoll）");
        System.out.println("    服务员（线程）只管坐在前台休息（epoll_wait 休眠）");
        System.out.println("    哪桌准备好了，呼叫器自动响（内核硬件中断 → 就绪链表）");
        System.out.println("    服务员只去响了的那桌（只处理就绪的 fd）");
        System.out.println("    1000桌只有5桌准备好了 → 服务员只跑5趟！O(1)");
        System.out.println();
        System.out.println("  【Selector 就是这个「呼叫器管理中心」】");
        System.out.println("    channel.register(selector, OP_READ)  = 给这桌发呼叫器");
        System.out.println("    selector.select()                     = 服务员坐着等");
        System.out.println("    selectedKeys()                        = 当前响了的呼叫器列表");
        System.out.println();
        System.out.println("  【重要理解：select() 的「阻塞」≠ 傻等】");
        System.out.println("    服务员等呼叫器响 = 睡觉（CPU利用率接近0%，省电！）");
        System.out.println("    不是服务员站着等（那是非阻塞轮询，浪费体力/CPU）");
        System.out.println("    和 Thread.sleep() 一样，都是真正休眠，被动等待唤醒");
        System.out.println();
        System.out.println("═══ 以下是 epoll 的技术细节 ═══");
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
        System.out.println("═══ 📮 生活场景：快递驿站的四种状态通知 ═══");
        System.out.println();
        System.out.println("  把 NIO Server 想象成一个快递驿站，SelectionKey 的四种事件就是：");
        System.out.println();
        System.out.println("  OP_ACCEPT（有新快递到站需要签收）");
        System.out.println("    驿站大门口来了一辆快递车（新的TCP连接请求）");
        System.out.println("    驿站老板（ServerSocketChannel）要出来签收（accept()）");
        System.out.println("    签收后，这个包裹变成一个独立的格子（SocketChannel）");
        System.out.println();
        System.out.println("  OP_CONNECT（客户连线成功通知）");
        System.out.println("    你主动打电话给驿站预约（connect()），");
        System.out.println("    驿站接通了，你收到通知（finishConnect()确认）");
        System.out.println("    ⚠️ 必须打这个确认电话，否则通话建立不完整");
        System.out.println();
        System.out.println("  OP_READ（格子里有包裹可以取了）");
        System.out.println("    「您有一个包裹，可以来取了」（Socket缓冲区有数据）");
        System.out.println("    你去格子里拿（channel.read(buffer)）");
        System.out.println("    ⚠️ 如果 read() 返回 -1，说明快递员「消失了」（对端关闭连接）");
        System.out.println("       必须取消这个格子（key.cancel()）并关闭（channel.close()）");
        System.out.println();
        System.out.println("  OP_WRITE（你的包裹可以寄出去了）= 最容易误用的事件！");
        System.out.println("    「发件窗口有空位，可以过来发快递了」");
        System.out.println("    ⚠️ 陷阱：发件窗口几乎一直有空位！");
        System.out.println("         一直订阅这个通知 = 驿站每秒钟发100条短信骚扰你");
        System.out.println("         导致你的手机（CPU）爆满，100%！");
        System.out.println("    ✅ 正确用法：只有快递太多寄不完（write() 写了一半），");
        System.out.println("               才订阅 OP_WRITE 等空位通知，");
        System.out.println("               寄完立刻取消订阅！");
        System.out.println();
        System.out.println("═══ 以下是技术细节 ═══");
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
        System.out.println("═══ ✈️ 生活场景：机场塔台调度 理解 Reactor 模式 ═══");
        System.out.println();
        System.out.println("  把整个 NIO Server 想象成一座繁忙机场的调度系统：");
        System.out.println();
        System.out.println("  【机场结构对应关系】");
        System.out.println("  塔台（1个调度员）    = 1个 Selector 线程（Reactor 核心）");
        System.out.println("  机场大门（一个）      = ServerSocketChannel（监听新连接）");
        System.out.println("  每架飞机             = 每个 SocketChannel（客户端连接）");
        System.out.println("  飞机呼叫塔台         = 数据就绪事件（OP_READ/OP_WRITE）");
        System.out.println("  塔台给飞机许可       = 处理完毕，继续监听");
        System.out.println();
        System.out.println("  【整个运作流程】");
        System.out.println("  1. 塔台开门营业（selector = Selector.open()）");
        System.out.println("  2. 大门注册「有新飞机要进来就通知我」（register OP_ACCEPT）");
        System.out.println("  3. 塔台调度员开始值班等消息（while(true) { selector.select() }）");
        System.out.println("     → 调度员在休息室睡觉（线程休眠），有事才醒");
        System.out.println("  4. 有新飞机进港（OP_ACCEPT）：");
        System.out.println("     登记这架飞机（accept() 得到 SocketChannel）");
        System.out.println("     给它分配频道（register OP_READ），以后听它说话");
        System.out.println("  5. 飞机有呼叫（OP_READ）：read() 读取数据，处理，回复");
        System.out.println("  6. 处理完回到步骤3等下一个事件");
        System.out.println();
        System.out.println("  【1个调度员为什么能管100架飞机？】");
        System.out.println("  因为大部分时间飞机不说话（连接空闲），");
        System.out.println("  调度员只处理「正在说话」的飞机，其余时间休息。");
        System.out.println("  这就是单线程能处理高并发的核心原理！");
        System.out.println();
        System.out.println("  【iter.remove() 为什么必须有？】");
        System.out.println("  机场记录本（selectedKeys）记录了「今天响应的飞机」");
        System.out.println("  处理完一架不 remove()，下次还会误以为它在呼叫！");
        System.out.println("  → 重复处理同一事件 = 飞机被叫了两次，大乱！");
        System.out.println();
        System.out.println("═══ 以下是完整 NIO Server 代码讲解 ═══");
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
        System.out.println("═══ 🚗 生活场景：高速公路收费站 理解缓冲区满和网络拥塞 ═══");
        System.out.println();
        System.out.println("  把数据传输想象成高速公路运输：");
        System.out.println("  你的程序 = 货主，数据 = 货物，TCP连接 = 高速公路");
        System.out.println();
        System.out.println("  【发送缓冲区 = 高速公路入口收费站的停车区】");
        System.out.println("  你（程序）把货（数据）送到收费站停车区（发送缓冲区 128KB）");
        System.out.println("  货车（TCP）陆续从停车区出发，按限速（接收窗口）行驶");
        System.out.println("  停车区满了 → 你的货车进不来 → transferTo 传不完！");
        System.out.println();
        System.out.println("  【接收窗口 rwnd = 对端的接收停车区容量】");
        System.out.println("  对端也有个停车区（接收缓冲区），它满了就通知你：");
        System.out.println("  「我这边停满了，你先别发了」（TCP ACK 携带 rwnd=0）");
        System.out.println("  就像目的地仓库满了，入口立了个牌子「暂停接收」");
        System.out.println();
        System.out.println("  【拥塞窗口 cwnd = 高速公路限速标志】");
        System.out.println("  即使你和对端都有空位，高速公路本身也可能堵了");
        System.out.println("  TCP 自动降速（慢启动/拥塞避免），就像雾天限速60km/h");
        System.out.println("  有事故（丢包） → 限速更严格（cwnd 砍半）");
        System.out.println();
        System.out.println("  【为什么一定要写 while 循环？】");
        System.out.println("  你要运1000吨货，但每次最多只能过去30吨（缓冲区/窗口限制）");
        System.out.println("  不写循环 = 只运了第一批30吨，剩下970吨直接消失，还不报错！");
        System.out.println("  写了循环 = 分批运，直到1000吨全部到对端");
        System.out.println();
        System.out.println("═══ 以下是技术细节 ═══");
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
        System.out.println("═══ 🏭 生活场景：超级仓库的高效发货秘密 ═══");
        System.out.println();
        System.out.println("  Kafka/Nginx 的 IO 设计就像世界一流的物流仓库运作方式：");
        System.out.println();
        System.out.println("  【Kafka = 亚马逊仓库（高吞吐消息队列）】");
        System.out.println("  秘密1「顺序收货」：所有货物（消息）只追加到货架尾端");
        System.out.println("    不乱插，不随机放 = 磁盘顺序写（600MB/s）");
        System.out.println("    vs 随机写（100KB/s），差了6000倍！");
        System.out.println("    就像流水线作业 vs 东翻西找，效率天差地别");
        System.out.println();
        System.out.println("  秘密2「暂存室中转」：货物先放暂存室（Page Cache），不直接上货架");
        System.out.println("    暂存室是内存，极快！货架是磁盘，慢");
        System.out.println("    仓库工人（OS）闲时把暂存室货物整理到货架（异步刷盘）");
        System.out.println("    顾客不用等上架，秒收货！");
        System.out.println();
        System.out.println("  秘密3「直发模式」：顾客来取货，仓库直接把货架地址告诉快递员");
        System.out.println("    快递员（DMA）自己去取，不需要工人（CPU）搬进搬出");
        System.out.println("    这就是 sendfile 零拷贝，Kafka官方数据：吞吐量提升6倍！");
        System.out.println();
        System.out.println("  【Nginx = 便利店（高并发静态资源）】");
        System.out.println("  Nginx 发静态文件 = 便利店把货直接从仓库传送带发给顾客");
        System.out.println("  Java Spring 发静态文件 = 服务员把货搬到柜台再给顾客（多了CPU搬运）");
        System.out.println("  → 静态资源放CDN或Nginx，别让Java服务直接发！");
        System.out.println();
        System.out.println("═══ 以下是技术细节 ═══");
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
        System.out.println("═══ 🔧 生活场景：选对工具，事半功倍 ═══");
        System.out.println();
        System.out.println("  BIO/NIO/Netty 的选择，就像搬家选交通工具：");
        System.out.println();
        System.out.println("  搬一件行李（<100个连接）→ 打车（BIO）最省事");
        System.out.println("    代码简单，够用就行，不要过度设计");
        System.out.println();
        System.out.println("  搬全家（1000+并发连接）→ 叫货拉拉（NIO+Selector）");
        System.out.println("    一辆大货车效率远超打1000辆出租车");
        System.out.println();
        System.out.println("  搬整栋楼的东西（生产级高并发服务）→ 专业搬家公司（Netty）");
        System.out.println("    自己开货拉拉太费心（原生NIO七大坑），交给专业的");
        System.out.println("    Netty帮你搞定：粘包/拆包/心跳/编解码/连接池...");
        System.out.println();
        System.out.println("  常见误区就像「用错了工具」：");
        System.out.println("  ❌ 用大货车搬一件行李（小项目用NIO，代码复杂了结果一点不快）");
        System.out.println("  ❌ 注册了 OP_WRITE 忘取消（相当于叫货拉拉到了不让走，堵路）");
        System.out.println("  ❌ Netty里做阻塞操作（专业搬家工人被你绑在厕所里，全公司停摆）");
        System.out.println();
        System.out.println("═══ 以下是详细误区分析和选型表 ═══");
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

// ====================================================================
// 第十四部分：FileChannel 本地文件 IO 完整操作
//
// ── 这部分讲什么？ ─────────────────────────────────────────────────────
// FileChannel 是 NIO 操作本地文件的核心 API，是 InputStream/OutputStream 的替代方案。
// 相比传统 IO：
//   - 双向读写（一个 Channel 既能读也能写）
//   - 支持随机访问（通过 position 游标直接跳到文件任意位置）
//   - 支持零拷贝 transferTo
//   - 支持内存映射 map()
//   - 支持文件锁 lock()
//   - ⚠ 但不支持非阻塞（不能注册 Selector）
// ====================================================================
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
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十五部分：MappedByteBuffer —— 内存映射文件（mmap）
//
// ── 这部分讲什么？ ─────────────────────────────────────────────────────
// mmap 是操作系统的一项核心技术：把磁盘文件的一段内容直接映射到进程的虚拟地址空间。
// 之后「访问内存」就等于「访问文件」，OS 负责按需把文件页加载到 Page Cache。
//
// Java 对应 API：FileChannel.map() → 返回 MappedByteBuffer
//
// 典型使用者：
//   - RocketMQ：CommitLog 写入用 mmap，省去 write() 系统调用
//   - Lucene（ElasticSearch底层）：索引文件大量用 mmap 做随机读
//   - Disruptor：JVM 内高性能无锁队列也依赖类似原理
// ====================================================================
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
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十六部分：NIO.2 —— Path/Files/WatchService 文件系统 API
//
// ── 这部分讲什么？ ─────────────────────────────────────────────────────
// JDK 7 引入了 NIO.2（JSR-203），彻底重新设计了文件系统 API：
//   旧 API（java.io.File）：方法返回值混乱（失败返回 false 不抛异常），
//                           不支持符号链接，不支持文件属性，跨平台差
//   新 API（java.nio.file）：
//     Path      替代 File，表示文件路径
//     Files     工具类，静态方法替代 File 的各种操作
//     Paths     创建 Path 的工厂（JDK11 可直接 Path.of()）
//     WatchService  目录监听，可监控文件创建/修改/删除
// ====================================================================
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
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第二十二部分：NIO.2 深度进阶
//
// ── 这部分讲什么？ ─────────────────────────────────────────────────────
// Part16 讲了 NIO.2 的基础（Path/Files/WatchService）。
// 这部分深入 NIO.2 的高级特性，很多人不知道但实际工作中很有用：
//
//   1. 文件属性视图体系（BasicFileAttributes / PosixFileAttributes）
//      精确获取创建时间、修改时间、文件大小、权限，跨平台差异处理
//
//   2. 符号链接（Symbolic Link）精确控制
//      NIO.2 对 symlink 的感知能力 vs java.io.File 的混乱
//
//   3. SeekableByteChannel：随机读写的统一接口
//      FileChannel 和 SocketChannel 的公共父接口
//
//   4. AsynchronousFileChannel（AIO）
//      JDK7 引入的异步文件 IO，完整异步，不阻塞任何线程
//      vs NIO（同步非阻塞）vs BIO（同步阻塞）的对比
//
//   5. FileStore 磁盘信息查询
//      查询磁盘容量、剩余空间，比 File.getTotalSpace() 更精确
// ====================================================================
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

// ====================================================================
// 第二十三部分：JDK 9~21 NIO 相关新特性
//
// ── 这部分讲什么？ ─────────────────────────────────────────────────────
// JDK 在 9~21 的演进中，持续对 IO 相关 API 进行增强：
//
//   JDK9：  InputStream.transferTo()，Stream API 增强
//   JDK10： 无 IO 直接新特性（局部变量推断 var 间接影响写法）
//   JDK11： Files.writeString / readString / isSameFile 改进，
//            InputStream.readNBytes()
//   JDK12~16：无重大 IO 新特性，switch 表达式/record 影响代码写法
//   JDK17：  密封类（Sealed Class），与 IO 框架设计相关
//   JDK19~21：虚拟线程（Project Loom）——对 IO 编程模型的革命！
//             io_uring 支持（Linux 内核5.1+，真正异步IO）
// ====================================================================
class Part23_ModernJdkIOFeatures {

    static void demonstrate() throws Exception {
        System.out.println("【第二十三部分：JDK 9~21 NIO 相关新特性】");
        System.out.println();
        System.out.println("═══ 🚀 生活场景：手机系统的持续进化 理解JDK IO新特性 ═══");
        System.out.println();
        System.out.println("  把 JDK 的版本升级想象成手机系统（iOS/Android）的持续迭代：");
        System.out.println("  基础功能早就有了，每个新版本都在某些地方让「用户体验更好」：");
        System.out.println();
        System.out.println("  JDK9 = 系统更新：「分享」功能从三步简化成一步");
        System.out.println("    InputStream.transferTo()：以前要写10行的「读到内存再写出去」，");
        System.out.println("    现在一行：in.transferTo(out)  就像分享一张图片变成了直接「一键分享」");
        System.out.println();
        System.out.println("  JDK11 = 系统更新：「便签」应用终于支持「直接保存到文件」了");
        System.out.println("    Files.writeString/readString：以前写文件要转 byte[]，很繁琐");
        System.out.println("    现在：Files.writeString(path, \"内容\") 直接写，Files.readString(path) 直接读");
        System.out.println("    内置 HttpClient：以前要用第三方库（OkHttp），现在系统自带，支持 HTTP/2");
        System.out.println();
        System.out.println("  JDK15 = 系统更新：「备忘录」终于支持多行格式，不再丢失换行");
        System.out.println("    Text Block（文本块）：写 JSON/SQL 不再需要一堆 \\n 和引号转义");
        System.out.println("    直接用三引号包裹，所见即所得");
        System.out.println();
        System.out.println("  JDK21 = 重大系统升级：「多任务处理」彻底重构");
        System.out.println("    虚拟线程（Virtual Threads）：让 IO 阻塞不再是性能瓶颈");
        System.out.println("    就像手机从「单核」升级到「多核」，同样的写法性能大幅提升");
        System.out.println("    结构化并发：并发任务管理从「危险的传真」变成「安全的项目管理系统」");
        System.out.println();
        System.out.println("  这些改进的共同主题：让代码更简单、更安全、更高效");
        System.out.println("  每个新特性都有「以前的痛点」→「新的解法」两步理解法");
        System.out.println();
        System.out.println("═══ 以下是各版本新特性技术细节 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：JDK9 - InputStream.transferTo()
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. JDK9：InputStream.transferTo()（BIO 也有零拷贝语义了）━━━");
        System.out.println();
        System.out.println("  JDK9 之前，从 InputStream 读数据到 OutputStream 要写循环：");
        System.out.println();
        System.out.println("  // JDK8 写法（繁琐）");
        System.out.println("  byte[] buf = new byte[8192];");
        System.out.println("  int n;");
        System.out.println("  while ((n = in.read(buf)) != -1) {");
        System.out.println("      out.write(buf, 0, n);");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // JDK9 写法（一行搞定）");
        System.out.println("  in.transferTo(out);  // ← InputStream 新增方法");
        System.out.println();
        System.out.println("  实际演示：");

        // 用字节数组模拟 InputStream
        byte[] data = "Hello transferTo! This is JDK9 new feature.".getBytes();
        java.io.InputStream inStream = new java.io.ByteArrayInputStream(data);
        java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream();
        // JDK9+ transferTo，JDK8 用循环兜底
        long transferred;
        int jv9 = getJavaVersion();
        if (jv9 >= 9) {
            try {
                java.lang.reflect.Method m = java.io.InputStream.class
                    .getMethod("transferTo", java.io.OutputStream.class);
                transferred = (Long) m.invoke(inStream, outStream);
            } catch (Exception e) { transferred = doCopy(inStream, outStream); }
        } else {
            transferred = doCopy(inStream, outStream);
        }
        System.out.println("    transferTo() 传输了 " + transferred + " 字节");
        System.out.println("    输出内容: \"" + outStream.toString() + "\"");
        System.out.println();
        System.out.println("  注意：InputStream.transferTo() ≠ FileChannel.transferTo()");
        System.out.println("    InputStream.transferTo()：简化写法，底层还是 read/write 循环");
        System.out.println("    FileChannel.transferTo()：底层调 sendfile()，真正零拷贝");
        System.out.println("    前者是编程便利，后者是 IO 性能优化，两者不一样！");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：JDK11 - Files.writeString/readString + InputStream增强
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. JDK11：Files.writeString / readString（文件 IO 更简洁）━━━");
        System.out.println();
        System.out.println("  JDK8 写字符串到文件：");
        System.out.println("    Files.write(path, content.getBytes(StandardCharsets.UTF_8));");
        System.out.println("  JDK11：");
        System.out.println("    Files.writeString(path, content);  // 默认 UTF-8");
        System.out.println("    Files.writeString(path, content, Charset.forName(\"GBK\")); // 指定编码");
        System.out.println();
        System.out.println("  JDK8 读文件到字符串：");
        System.out.println("    new String(Files.readAllBytes(path), StandardCharsets.UTF_8)");
        System.out.println("  JDK11：");
        System.out.println("    Files.readString(path);  // 默认 UTF-8，一行搞定");
        System.out.println();

        // 运行时检测 JDK 版本（JDK8 兼容的写法用反射调用）
        int javaVersion = getJavaVersion();
        System.out.println("  当前 JDK 版本: " + javaVersion);
        java.nio.file.Path jdk11TestFile = java.nio.file.Files.createTempFile("jdk11_", ".txt");
        try {
            if (javaVersion >= 11) {
                // 使用反射调用 JDK11 API，避免 JDK8 编译失败
                // 实际项目直接写：Files.writeString(path, "content")
                java.lang.reflect.Method writeString =
                    java.nio.file.Files.class.getMethod("writeString",
                        java.nio.file.Path.class, CharSequence.class,
                        java.nio.file.OpenOption[].class);
                writeString.invoke(null, jdk11TestFile, "JDK11 writeString test",
                    new java.nio.file.OpenOption[0]);

                java.lang.reflect.Method readString =
                    java.nio.file.Files.class.getMethod("readString", java.nio.file.Path.class);
                String read = (String) readString.invoke(null, jdk11TestFile);
                System.out.println("    Files.writeString + Files.readString 演示: \"" + read + "\"");
            } else {
                // JDK8 回退方案
                java.nio.file.Files.write(jdk11TestFile,
                    "JDK8 fallback write".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String read = new String(
                    java.nio.file.Files.readAllBytes(jdk11TestFile),
                    java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("    JDK8 写法（JDK11 Files.writeString/readString 不可用）: \""
                    + read + "\"");
            }
        } finally {
            java.nio.file.Files.deleteIfExists(jdk11TestFile);
        }
        System.out.println();

        System.out.println("  JDK11 InputStream 新方法：");
        System.out.println();
        System.out.println("  readNBytes(int len)：精确读取 N 字节（不像 read() 可能读少）");
        System.out.println("    // JDK8：");
        System.out.println("    byte[] buf = new byte[len];");
        System.out.println("    int totalRead = 0;");
        System.out.println("    while (totalRead < len) {");
        System.out.println("        int n = in.read(buf, totalRead, len - totalRead);");
        System.out.println("        if (n == -1) break;");
        System.out.println("        totalRead += n;");
        System.out.println("    }");
        System.out.println("    // JDK11：");
        System.out.println("    byte[] buf = in.readNBytes(len);  // 一行搞定！");
        System.out.println();
        System.out.println("  readAllBytes()：读全部（JDK9 已有，但 JDK11 性能改进）");
        System.out.println("  skipNBytes(long n)：精确跳过 N 字节（JDK12+，不会少跳）");
        System.out.println();

        // 演示 readNBytes（JDK11+，JDK8 用循环兜底）
        byte[] src = "0123456789ABCDEF".getBytes();
        java.io.InputStream is = new java.io.ByteArrayInputStream(src);
        byte[] first5 = readNBytesCompat(is, 5);
        System.out.println("  readNBytes(5) 演示: \"" + new String(first5) + "\"  (精确读5字节)");
        byte[] rest = readAllBytesCompat(is);
        System.out.println("  readAllBytes() 读剩余: \"" + new String(rest) + "\"");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：JDK12~16 - Stream 增强 + 文本块 + Record 对 IO 代码的影响
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. JDK12~16：代码层面改进（影响 IO 代码写法）━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  这些版本没有重大 IO API 新特性，但以下改进影响 IO 代码写法：");
        System.out.println();
        System.out.println("  ① JDK13~15 文本块（Text Block）——多行字符串不再痛苦");
        System.out.println("     写 JSON/SQL/协议体时不再需要大量转义：");
        System.out.println();
        System.out.println("  // JDK8（痛苦）");
        System.out.println("  String json = \"{\\\"name\\\": \\\"Alice\\\",\" +");
        System.out.println("                \"\\\"age\\\": 30}\";");
        System.out.println();
        System.out.println("  // JDK15（文本块）");
        System.out.println("  String json = \"\"\"");
        System.out.println("      {");
        System.out.println("          \\\"name\\\": \\\"Alice\\\",");
        System.out.println("          \\\"age\\\": 30");
        System.out.println("      }");
        System.out.println("      \"\"\";");
        System.out.println("  // 直接发给 HttpClient 或写到文件，不再需要字符串拼接");
        System.out.println();
        System.out.println("  ② JDK14~16 Record（不可变数据类）——网络消息对象更简洁");
        System.out.println("     协议消息对象用 record 定义，替代 Builder 模式：");
        System.out.println();
        System.out.println("  // JDK8 写法（要写 getter/equals/hashCode/toString）");
        System.out.println("  public class LoginRequest {");
        System.out.println("      private final String username;");
        System.out.println("      private final String password;");
        System.out.println("      // 几十行 getter/constructor/toString...");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // JDK16 Record 写法（一行）");
        System.out.println("  record LoginRequest(String username, String password) {}");
        System.out.println("  // 自动生成 getter/equals/hashCode/toString");
        System.out.println("  // 非常适合作为网络消息 DTO（Data Transfer Object）");
        System.out.println();
        System.out.println("  ③ JDK14~21 switch 表达式 / 模式匹配 instanceof");
        System.out.println("     处理不同 IO 类型时代码更简洁：");
        System.out.println();
        System.out.println("  // JDK8（繁琐的 instanceof + cast）");
        System.out.println("  Object channel = getChannel();");
        System.out.println("  if (channel instanceof FileChannel) {");
        System.out.println("      FileChannel fc = (FileChannel) channel;");
        System.out.println("      fc.transferTo(...);");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // JDK16+（模式匹配 instanceof）");
        System.out.println("  if (channel instanceof FileChannel fc) {");
        System.out.println("      fc.transferTo(...);  // fc 自动绑定，不需要强转");
        System.out.println("  }");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：JDK19~21 虚拟线程（Project Loom）——IO 编程模型革命
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. JDK19~21：虚拟线程（Virtual Threads）—— IO 编程的革命 ━━━");
        System.out.println();
        System.out.println("  虚拟线程是 JDK21 最重大的特性（Project Loom），");
        System.out.println("  它彻底改变了 Java IO 编程模型的选择逻辑。");
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │               平台线程 vs 虚拟线程                           │");
        System.out.println("  ├───────────────────────────┬──────────────────────────────────┤");
        System.out.println("  │ 平台线程（OS Thread）     │ 虚拟线程（Virtual Thread）      │");
        System.out.println("  ├───────────────────────────┼──────────────────────────────────┤");
        System.out.println("  │ 1个JVM线程 = 1个OS线程    │ N个虚拟线程 = M个OS线程(N>>M)   │");
        System.out.println("  │ 栈内存：~1MB/线程         │ 栈内存：~几KB/虚拟线程（动态）  │");
        System.out.println("  │ 10万线程 → 100GB内存 崩溃 │ 100万虚拟线程 → 几GB 正常运行  │");
        System.out.println("  │ BIO阻塞 → OS线程挂起浪费  │ BIO阻塞 → 虚拟线程挂起，OS继续 │");
        System.out.println("  └───────────────────────────┴──────────────────────────────────┘");
        System.out.println();
        System.out.println("  虚拟线程的核心原理：");
        System.out.println("    每个虚拟线程运行在「载体线程」（平台线程）上");
        System.out.println("    当虚拟线程遇到阻塞 IO → JVM 自动把它从载体线程卸载");
        System.out.println("    载体线程继续运行其他虚拟线程（不阻塞！）");
        System.out.println("    IO 完成后 → JVM 把虚拟线程重新挂载到某个载体线程继续执行");
        System.out.println();
        System.out.println("  ═══ 🏭 生活场景：工厂流水线 理解虚拟线程 ═══");
        System.out.println();
        System.out.println("  假设你开了一家工厂，有 1000 个订单要处理：");
        System.out.println();
        System.out.println("  旧方式（平台线程 BIO，一个订单一个专属工人）：");
        System.out.println("    雇 1000 个工人，每个工人负责一个订单从头到尾");
        System.out.println("    订单处理中需要「等待供货商发货」（IO阻塞），工人就坐在那傻等");
        System.out.println("    1000 个工人大部分时间都在傻等供货商……");
        System.out.println("    问题：1000个工人工资（内存）、管理成本（线程切换）极高");
        System.out.println("          工人数量还有上限（OS线程数有限），超过就崩溃");
        System.out.println();
        System.out.println("  更好的方式（NIO，用 Selector 一个工人管多个订单）：");
        System.out.println("    只雇 8 个多面手工人（CPU核心数），");
        System.out.println("    工人不傻等，有哪个订单「可以继续做了」才去做哪个");
        System.out.println("    但问题是：写「哪个订单可以继续做了」的逻辑很复杂（NIO编程难）");
        System.out.println("    代码里全是 callback、CompletableFuture，难以理解和调试");
        System.out.println();
        System.out.println("  虚拟线程方式（JDK21，完美结合两者优点）：");
        System.out.println("    雇 8 个实际工人（平台线程，载体线程）");
        System.out.println("    为每个订单创建一张「任务便条」（虚拟线程），100万张便条内存够用");
        System.out.println("    工人拿起便条 A 开始干，遇到「等待供货」（IO阻塞）：");
        System.out.println("      JVM：「订单A在等货，先把便条 A 放一边（挂起）」");
        System.out.println("      JVM：「工人别闲着，去处理便条 B」");
        System.out.println("    货到了（IO完成）：");
        System.out.println("      JVM：「便条 A 的货到了，哪个工人空了去处理它」");
        System.out.println("    每张便条记录了「当前做到哪步了」（虚拟线程的调用栈/上下文）");
        System.out.println();
        System.out.println("  核心优势：");
        System.out.println("    ✅ 代码像 BIO 一样简单直观（一个虚拟线程处理一个请求，可以傻等）");
        System.out.println("    ✅ 性能像 NIO 一样高效（底层工人不傻等，IO阻塞自动切换）");
        System.out.println("    ✅ 便条（虚拟线程）可以有 100 万个，内存只占几 KB 每个");
        System.out.println("    ✅ 不需要 callback，代码从上往下顺序读懂，不再是「回调地狱」");
        System.out.println();
        System.out.println("  对比图：");
        System.out.println("    BIO + 平台线程：1000个请求 → 需要1000个OS线程 → 内存爆炸");
        System.out.println("    NIO + Selector：1000个请求 → 8个线程够用，但代码极复杂");
        System.out.println("    虚拟线程（JDK21）：1000个请求 → 1000个虚拟线程 → 8个工人，代码简单！");
        System.out.println();
        System.out.println("  类比：");
        System.out.println("    平台线程 = 真实员工（贵，10万人工资撑不住）");
        System.out.println("    虚拟线程 = 任务清单上的条目（便宜，100万条都行）");
        System.out.println("    载体线程 = 实际干活的工人（固定几个，CPU核数倍）");
        System.out.println("    阻塞 = 这个任务在等快递（IO），工人先去干别的任务");
        System.out.println("    IO 完成 = 快递到了，工人回来继续这个任务");
        System.out.println();
        System.out.println("  虚拟线程的使用方式：");
        System.out.println();
        System.out.println("  // 方式1：Thread.ofVirtual（JDK21）");
        System.out.println("  Thread vt = Thread.ofVirtual().start(() -> {");
        System.out.println("      // 这里可以写阻塞 BIO！");
        System.out.println("      String result = httpClient.get(\"https://api.example.com\"); // 阻塞");
        System.out.println("      System.out.println(result);");
        System.out.println("  });");
        System.out.println();
        System.out.println("  // 方式2：虚拟线程线程池（处理并发请求）");
        System.out.println("  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();");
        System.out.println("  // 每个任务都在独立虚拟线程中运行");
        System.out.println("  executor.submit(() -> {");
        System.out.println("      // BIO 数据库查询（可以阻塞，不浪费平台线程）");
        System.out.println("      User user = jdbc.query(\"select * from users where id=?\", 1);");
        System.out.println("      return user;");
        System.out.println("  });");
        System.out.println();

        // 实际创建虚拟线程演示（用反射兼容 JDK8 编译）
        System.out.println("  实际演示：");
        if (javaVersion >= 21) {
            try {
                // Thread.ofVirtual().start(Runnable) —— 通过反射调用
                java.lang.reflect.Method ofVirtual = Thread.class.getMethod("ofVirtual");
                Object builder = ofVirtual.invoke(null);
                java.lang.reflect.Method start = builder.getClass()
                    .getMethod("start", Runnable.class);
                Thread vt = (Thread) start.invoke(builder, (Runnable) () -> {
                    try {
                        boolean isVirtual = (Boolean) Thread.class
                            .getMethod("isVirtual").invoke(Thread.currentThread());
                        System.out.println("    虚拟线程执行中，isVirtual=" + isVirtual
                            + "  name=" + Thread.currentThread().getName());
                    } catch (Exception ex) {
                        System.out.println("    虚拟线程执行中，name=" + Thread.currentThread().getName());
                    }
                });
                vt.join();
            } catch (Exception e) {
                System.out.println("    反射调用虚拟线程失败: " + e.getMessage());
            }
        } else if (javaVersion >= 19) {
            System.out.println("    JDK19/20: 虚拟线程是 preview 特性，需要 --enable-preview 参数");
        } else {
            System.out.println("    当前 JDK" + javaVersion + " 不支持虚拟线程（需要 JDK21+）");
            System.out.println("    模拟普通线程: Thread.ofPlatform().start(() -> {...})");
        }
        System.out.println();

        System.out.println("  ★ 虚拟线程对 IO 编程模型的革命性影响：");
        System.out.println();
        System.out.println("  在虚拟线程之前（JDK8~17）：");
        System.out.println("    处理高并发 IO，必须用 NIO/Netty 的异步模型");
        System.out.println("    原因：BIO 阻塞 → 平台线程挂起 → 10万并发需要10万线程 → OOM");
        System.out.println("    代价：异步代码（Callback/CompletableFuture）极难调试");
        System.out.println();
        System.out.println("  有了虚拟线程（JDK21）：");
        System.out.println("    可以用 BIO 的写法处理高并发！");
        System.out.println("    BIO 阻塞 → 虚拟线程挂起（轻量级，不浪费平台线程）");
        System.out.println("    可以开 100万个虚拟线程，每个线程用同步阻塞 BIO");
        System.out.println("    代码简单如 BIO，性能接近 NIO");
        System.out.println();
        System.out.println("  对各框架的影响：");
        System.out.println("    Spring Boot 3.2+：默认支持虚拟线程（Tomcat 每个请求一个虚拟线程）");
        System.out.println("    Netty：仍然有价值（极致低延迟场景，Netty 比虚拟线程 BIO 快）");
        System.out.println("    JDBC：虚拟线程 + BIO JDBC 可以替代复杂的异步数据库客户端");
        System.out.println("    R2DBC（响应式DB驱动）：虚拟线程兴起后，R2DBC 的优势下降");
        System.out.println();
        System.out.println("  什么时候还需要 NIO/Netty（即使有了虚拟线程）：");
        System.out.println("    ① 极致低延迟：Netty 的 EventLoop 无上下文切换，比虚拟线程快");
        System.out.println("    ② 精确控制协议：自定义二进制协议、WebSocket 等");
        System.out.println("    ③ 大量短连接：每个请求一个虚拟线程，还是有切换开销");
        System.out.println("    ④ 现有 Netty 代码库：迁移成本高，不值得为了虚拟线程重写");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：JDK21 结构化并发 + Scoped Values
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. JDK21：结构化并发（Structured Concurrency）━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  结构化并发解决的问题：");
        System.out.println("    一个 HTTP 请求需要并发调用两个微服务（用户服务 + 订单服务）");
        System.out.println("    用 CompletableFuture 写法复杂，还要处理：");
        System.out.println("      - 其中一个失败要取消另一个");
        System.out.println("      - 所有子任务完成才能结束父任务");
        System.out.println("      - 子任务的日志/追踪要关联到父任务");
        System.out.println();
        System.out.println("  ═══ 👷 生活场景：工程项目管理 理解结构化并发 ═══");
        System.out.println();
        System.out.println("  想象你是一个项目经理（主任务），负责完成一个装修项目：");
        System.out.println("  你派了两个施工队同时干活（两个子任务）：");
        System.out.println("    施工队A：负责刷墙（查用户信息）");
        System.out.println("    施工队B：负责铺地板（查订单信息）");
        System.out.println();
        System.out.println("  旧方式（CompletableFuture，就像传真机时代的管理）：");
        System.out.println("    你发了两份传真（submit 两个Future），然后你继续干别的");
        System.out.println("    问题来了：施工队A出事故了（任务失败），你根本不知道");
        System.out.println("    施工队B还在继续铺地板（白干了，浪费资源）");
        System.out.println("    你要手动写代码：「A失败了，取消B」——极其繁琐");
        System.out.println("    更糟的是：如果项目经理（主任务）提前退出，施工队还在后台乱跑！");
        System.out.println("    这就是「线程泄漏」——子任务脱离了父任务的控制");
        System.out.println();
        System.out.println("  结构化并发（StructuredTaskScope，就像现代项目管理系统）：");
        System.out.println("    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {");
        System.out.println("    // 「开工！」——创建一个「工程围栏」");
        System.out.println("    // 派出两个施工队，他们在「围栏」内工作");
        System.out.println("        Subtask<User> A = scope.fork(() -> 刷墙())");
        System.out.println("        Subtask<Order> B = scope.fork(() -> 铺地板())");
        System.out.println("        scope.join()  // 「等两队都完工」");
        System.out.println("        scope.throwIfFailed()  // 「任一失败，全停工（另一队自动收到停工令）」");
        System.out.println("        return 装修完成(A.get(), B.get())");
        System.out.println("    } // 「围栏关闭」——保证离开前所有施工队都已撤场，不会有队伍游荡在外");
        System.out.println();
        System.out.println("  核心保证（围栏的作用）：");
        System.out.println("    ✅ 任一失败 → 另一个自动停止（不浪费资源）");
        System.out.println("    ✅ 离开 try 块 → 所有子任务必然已完成/取消（不会线程泄漏）");
        System.out.println("    ✅ 子任务的异常/日志自动关联到父任务（追踪方便）");
        System.out.println("    ✅ 代码结构就像「进去→并发干活→等结束→出来」，逻辑清晰");
        System.out.println();
        System.out.println("  ShutdownOnFailure vs ShutdownOnSuccess：");
        System.out.println("    ShutdownOnFailure：任一失败，全部停（装修任一队出事，整个项目暂停）");
        System.out.println("    ShutdownOnSuccess：任一成功，全部停（多个备用服务竞速，谁快用谁）");
        System.out.println("    例子：同时请求3个城市的天气API，哪个先回来就用哪个");
        System.out.println();
        System.out.println("  JDK8 写法（CompletableFuture，复杂）：");
        System.out.println("    CompletableFuture<User> userFuture = CompletableFuture");
        System.out.println("        .supplyAsync(() -> userService.getUser(userId));");
        System.out.println("    CompletableFuture<Order> orderFuture = CompletableFuture");
        System.out.println("        .supplyAsync(() -> orderService.getOrders(userId));");
        System.out.println("    CompletableFuture.allOf(userFuture, orderFuture).thenRun(() -> {");
        System.out.println("        // 两个完成了再合并，但异常处理和取消逻辑很复杂...");
        System.out.println("    });");
        System.out.println();
        System.out.println("  JDK21 结构化并发写法（简洁）：");
        System.out.println("    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {");
        System.out.println("        // 并发提交两个子任务（都在虚拟线程里执行）");
        System.out.println("        Subtask<User> userTask  = scope.fork(() -> userService.getUser(id));");
        System.out.println("        Subtask<Order> orderTask = scope.fork(() -> orderService.getOrders(id));");
        System.out.println();
        System.out.println("        scope.join();           // 等两个都完成");
        System.out.println("        scope.throwIfFailed();  // 任一失败就抛出（另一个自动取消）");
        System.out.println();
        System.out.println("        // 两个都成功，安全获取结果");
        System.out.println("        return new Response(userTask.get(), orderTask.get());");
        System.out.println("    }  // 自动关闭 scope，确保子任务不泄漏");
        System.out.println();
        System.out.println("  ShutdownOnFailure（任一失败，全部取消）vs");
        System.out.println("  ShutdownOnSuccess（任一成功，全部取消——适合竞速场景）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第六节：各 JDK 版本 IO 相关改进汇总表
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 6. JDK 版本 IO 相关改进汇总表 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌────────┬──────────────────────────────────────────────────────┐");
        System.out.println("  │ 版本   │ IO / 并发相关新特性                                  │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK7   │ NIO.2（Path/Files/WatchService/AIO）                 │");
        System.out.println("  │        │ try-with-resources（AutoCloseable，IO资源自动关闭）   │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK8   │ Files.lines() Stream（懒加载大文件，避免OOM）         │");
        System.out.println("  │        │ CompletableFuture（异步编程）                        │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK9   │ InputStream.transferTo(OutputStream)                 │");
        System.out.println("  │        │ 模块系统（module-info.java，影响类加载和反射）        │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK11  │ Files.writeString/readString/isSameFile                │");
        System.out.println("  │        │ InputStream.readNBytes()/readAllBytes() 改进            │");
        System.out.println("  │        │ HttpClient（标准化，支持 HTTP/2 和 WebSocket）          │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK12+ │ InputStream.skipNBytes(long)（精确跳过N字节）          │");
        System.out.println("  │        │ switch 表达式（简化 IO 事件处理分支）                  │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK14+ │ Record（简化网络消息 DTO）                             │");
        System.out.println("  │        │ 模式匹配 instanceof（简化 Channel 类型判断）           │");
        System.out.println("  │        │ Text Block（简化 JSON/XML 协议体构建）                 │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK19  │ 虚拟线程 Preview（--enable-preview）                  │");
        System.out.println("  │        │ Structured Concurrency Preview                        │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK21  │ ★ 虚拟线程 GA（Thread.ofVirtual()，无需 preview）    │");
        System.out.println("  │        │ ★ 结构化并发 Preview（StructuredTaskScope）           │");
        System.out.println("  │        │ Scoped Values Preview（替代 ThreadLocal 的轻量方案）   │");
        System.out.println("  │        │ Sequenced Collections（有序集合 API，影响 IO 缓冲区）  │");
        System.out.println("  └────────┴──────────────────────────────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第七节：JDK11 HttpClient（标准化 HTTP 客户端）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 7. JDK11：HttpClient（内置 HTTP/2 + 异步，替代 HttpURLConnection）");
        System.out.println();
        System.out.println("  HttpURLConnection（JDK 老 API）的问题：");
        System.out.println("    ① 不支持 HTTP/2（只有 HTTP/1.1）");
        System.out.println("    ② 只有同步阻塞 API");
        System.out.println("    ③ 连接复用（keep-alive）行为不透明");
        System.out.println("    ④ 代码极繁琐（开/关流/设头部/读响应...）");
        System.out.println();
        System.out.println("  JDK11 HttpClient（一步到位）：");
        System.out.println();
        System.out.println("  // 同步 GET（最简单）");
        System.out.println("  HttpClient client = HttpClient.newHttpClient();");
        System.out.println("  HttpRequest request = HttpRequest.newBuilder()");
        System.out.println("      .uri(URI.create(\"https://httpbin.org/get\"))");
        System.out.println("      .header(\"Accept\", \"application/json\")");
        System.out.println("      .GET()");
        System.out.println("      .build();");
        System.out.println("  HttpResponse<String> response =");
        System.out.println("      client.send(request, HttpResponse.BodyHandlers.ofString());");
        System.out.println("  System.out.println(response.statusCode()); // 200");
        System.out.println("  System.out.println(response.body());       // JSON 响应体");
        System.out.println();
        System.out.println("  // 异步 GET（非阻塞，返回 CompletableFuture）");
        System.out.println("  CompletableFuture<HttpResponse<String>> future =");
        System.out.println("      client.sendAsync(request, HttpResponse.BodyHandlers.ofString());");
        System.out.println("  future.thenApply(HttpResponse::body)");
        System.out.println("        .thenAccept(System.out::println);");
        System.out.println();
        System.out.println("  // POST JSON");
        System.out.println("  HttpRequest postReq = HttpRequest.newBuilder()");
        System.out.println("      .uri(URI.create(\"https://api.example.com/users\"))");
        System.out.println("      .header(\"Content-Type\", \"application/json\")");
        System.out.println("      .POST(HttpRequest.BodyPublishers.ofString(\"{\\\"name\\\":\\\"Alice\\\"}\"))");
        System.out.println("      .build();");
        System.out.println();
        System.out.println("  // 下载大文件（流式，不 OOM）");
        System.out.println("  HttpResponse<Path> fileResp = client.send(");
        System.out.println("      HttpRequest.newBuilder().uri(bigFileUri).build(),");
        System.out.println("      HttpResponse.BodyHandlers.ofFile(Paths.get(\"/tmp/download.zip\")));");
        System.out.println("  // 数据直接写入文件，不会把整个文件读进内存");
        System.out.println();
        System.out.println("  HttpClient 底层 IO：");
        System.out.println("    基于 NIO（java.nio.channels.SocketChannel）");
        System.out.println("    内置连接池，自动复用 keep-alive 连接");
        System.out.println("    支持 HTTP/2 多路复用（一个 TCP 连接并发多个请求）");
        System.out.println("    HTTP/2 底层：TLS + ALPN 协商协议，一个 SocketChannel 复用");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第八节：虚拟线程 + Netty：选哪个？
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 8. 虚拟线程 + Netty 选型决策树（JDK21 时代）━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  场景1：普通微服务（Spring Boot + HTTP API + 数据库）");
        System.out.println("    → ✅ 虚拟线程（Spring Boot 3.2+）");
        System.out.println("    原因：业务 IO 等待（DB/HTTP）是瓶颈，虚拟线程解决了等待浪费问题");
        System.out.println("    代码：和 JDK8 BIO 一样简单，自动享受高并发");
        System.out.println();
        System.out.println("  场景2：高性能 RPC 框架 / 网关 / 消息中间件");
        System.out.println("    → ✅ Netty（NIO + EventLoop）");
        System.out.println("    原因：需要极致低延迟、自定义二进制协议、背压控制");
        System.out.println("    虚拟线程每次切换都有 ~μs 级开销，Netty EventLoop 是纳秒级");
        System.out.println();
        System.out.println("  场景3：文件批处理 / 日志处理（大量本地文件 IO）");
        System.out.println("    → ✅ 虚拟线程 + BIO（或 NIO）");
        System.out.println("    虚拟线程遇到文件读写阻塞时挂起，不浪费平台线程");
        System.out.println("    简洁的同步代码即可处理大量并发文件 IO");
        System.out.println();
        System.out.println("  场景4：大文件传输（网盘/CDN/下载服务）");
        System.out.println("    → ✅ FileChannel.transferTo()（零拷贝）+ NIO 或 Netty");
        System.out.println("    虚拟线程在这里没有优势（bottleneck 是磁盘/网络带宽，不是线程等待）");
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │ 总结：虚拟线程不是 Netty 的替代品，而是互补关系            │");
        System.out.println("  │       虚拟线程解决「线程数限制」问题                        │");
        System.out.println("  │       Netty 解决「极致性能 + 自定义协议」问题               │");
        System.out.println("  │       两者各有适用场景，未来可能会共存很长时间              │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        System.out.println();
        NIODemo.printSeparator();
    }

    /** 获取当前 JDK 主版本号（JDK8=8, JDK11=11, JDK21=21...） */
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            // JDK8 及之前：1.8.x -> 8
            return Integer.parseInt(version.substring(2, 3));
        } else {
            // JDK9+：11.0.2 -> 11, 21.0.1 -> 21
            int dot = version.indexOf('.');
            return Integer.parseInt(dot > 0 ? version.substring(0, dot) : version);
        }
    }

    /** 兼容 JDK8 的 InputStream 拷贝（替代 JDK9 transferTo） */
    private static long doCopy(java.io.InputStream in, java.io.OutputStream out)
            throws java.io.IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            total += n;
        }
        return total;
    }

    /** 兼容 JDK8 的 readNBytes（精确读N字节） */
    private static byte[] readNBytesCompat(java.io.InputStream in, int len)
            throws java.io.IOException {
        try {
            java.lang.reflect.Method m = java.io.InputStream.class
                .getMethod("readNBytes", int.class);
            return (byte[]) m.invoke(in, len);
        } catch (Exception e) {
            // JDK8 兜底：手动读 N 字节
            byte[] buf = new byte[len];
            int total = 0;
            while (total < len) {
                int n = in.read(buf, total, len - total);
                if (n == -1) break;
                total += n;
            }
            if (total < len) {
                byte[] trimmed = new byte[total];
                System.arraycopy(buf, 0, trimmed, 0, total);
                return trimmed;
            }
            return buf;
        }
    }

    /** 兼容 JDK8 的 readAllBytes */
    private static byte[] readAllBytesCompat(java.io.InputStream in)
            throws java.io.IOException {
        try {
            java.lang.reflect.Method m = java.io.InputStream.class.getMethod("readAllBytes");
            return (byte[]) m.invoke(in);
        } catch (Exception e) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            doCopy(in, bos);
            return bos.toByteArray();
        }
    }
}

// ====================================================================
// 第十七部分：Netty 核心架构深度解析
//
// ── 这部分讲什么？ ─────────────────────────────────────────────────────
// 原生 NIO 写起来极其痛苦（flip/clear 容易搞错、粘包要自己处理、
// epoll 空轮询 Bug、异常处理繁琐...），Netty 是对原生 NIO 的工业级封装。
//
// 理解 Netty 的关键：它不是发明了新东西，而是把原生 NIO 的七宗罪全部
// 封装掉，让你专注写业务逻辑，而不是和操作系统底层搏斗。
//
// 核心概念：
//   EventLoopGroup  → 线程池 + Selector 的封装
//   EventLoop       → 1个线程 + 1个 Selector（管 N 个 Channel）
//   Channel         → 连接的抽象（NioSocketChannel / NioServerSocketChannel）
//   Pipeline        → 处理器链（责任链模式，数据流水线）
//   ChannelHandler  → 处理一个具体的业务逻辑
//   ByteBuf         → 替代 ByteBuffer，双指针，无需 flip()
// ====================================================================
class Part17_NettyArchitecture {

    static void explain() {
        System.out.println("【第十七部分：Netty 核心架构深度解析】");
        System.out.println();
        System.out.println("═══ 🚚 生活场景：自建快递 vs 找顺丰 理解为什么用Netty ═══");
        System.out.println();
        System.out.println("  原生 NIO 就像「自己组建快递公司」：");
        System.out.println("  买车（Selector）、招司机（Thread）、建仓库（Buffer）...");
        System.out.println("  理论上能做，但你要搞定的事情超多：");
        System.out.println("  → 包裹破了怎么办？（粘包拆包）");
        System.out.println("  → 车坏了怎么办？（epoll 空轮询 Bug）");
        System.out.println("  → 客户一直不取件怎么处理？（心跳/空闲检测）");
        System.out.println("  → 包裹格式不对怎么办？（编解码）");
        System.out.println("  这些全要你自己搞，一不小心就出 Bug");
        System.out.println();
        System.out.println("  Netty 就像「直接用顺丰」：");
        System.out.println("  顺丰帮你处理好了所有烦恼，你只管告诉顺丰「寄什么、发哪里」");
        System.out.println("  专注业务逻辑，底层细节 Netty 全包");
        System.out.println();
        System.out.println("  【Netty 核心组件类比快递公司】");
        System.out.println("  EventLoopGroup（Boss）= 快递接单中心（只负责接新订单）");
        System.out.println("  EventLoopGroup（Worker）= 配送员团队（处理实际收发件）");
        System.out.println("  Channel = 每一个快递包裹（每个网络连接）");
        System.out.println("  Pipeline = 包裹处理流水线（分拣→称重→打包→发货）");
        System.out.println("  ChannelHandler = 流水线上的每个工序（你写的业务逻辑）");
        System.out.println("  ByteBuf = 改良版货箱（比ByteBuffer好用，不需要flip）");
        System.out.println();
        System.out.println("  【为什么 Netty 比原生 NIO 好？】");
        System.out.println("  就像「为什么用顺丰比自建快递好」：");
        System.out.println("  - 成熟可靠，Bug 已经被数百万用户验证过了");
        System.out.println("  - 功能完整，HTTP/WebSocket/自定义协议都支持");
        System.out.println("  - 性能极佳，内存池/零拷贝都已优化好");
        System.out.println("  Netty 是 Dubbo/gRPC/Kafka Client 的底层，经受了工业级考验");
        System.out.println();
        System.out.println("═══ 以下是 Netty 核心架构技术细节 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：原生 NIO 七宗罪 vs Netty 解决方案
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 原生 NIO 七宗罪 vs Netty 解决方案 ━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌────┬─────────────────────────────┬──────────────────────────────┐");
        System.out.println("  │ # │ 原生 NIO 痛点               │ Netty 解决方案               │");
        System.out.println("  ├────┼─────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ ① │ ByteBuffer flip/clear 易错  │ ByteBuf 双指针，无需 flip    │");
        System.out.println("  │ ② │ 粘包/拆包需自己处理         │ 内置 FrameDecoder 全家桶     │");
        System.out.println("  │ ③│ JDK epoll 空轮询 Bug        │ 检测计数，自动重建 Selector  │");
        System.out.println("  │ ④ │ 异常处理分散繁琐            │ Pipeline 统一 exceptionCaught│");
        System.out.println("  │ ⑤ │ 无连接池                   │ Channel 对象池               │");
        System.out.println("  │ ⑥ │ 无编解码                   │ 内置 HTTP/WebSocket/自定义   │");
        System.out.println("  │ ⑦ │ 无心跳机制                 │ IdleStateHandler             │");
        System.out.println("  └────┴─────────────────────────────┴──────────────────────────────┘");
        System.out.println();
        System.out.println("  类比：原生 NIO 是「毛坯房 + 一堆砖头」，Netty 是「精装修公寓」");
        System.out.println("        你进去就可以住（写业务），不用自己砌墙（处理底层细节）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：Netty 线程模型（Reactor 主从模式）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. Netty 线程模型：Reactor 主从模式 ━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  原生 NIO 单线程 Reactor（Part8 讲过）的问题：");
        System.out.println("    1个线程既 accept 又 read/write 又处理业务");
        System.out.println("    某个 Handler 执行慢 → 整个 Selector 被卡住 → 其他连接无法响应");
        System.out.println();
        System.out.println("  Netty 的 Reactor 主从模式（默认）：");
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │                    Netty Reactor 主从模式                    │");
        System.out.println("  │                                                              │");
        System.out.println("  │  BossGroup（通常1个线程）                                   │");
        System.out.println("  │  ┌─────────────────────────────────────┐                    │");
        System.out.println("  │  │ EventLoop-0                         │                    │");
        System.out.println("  │  │  Selector（只监听 OP_ACCEPT）       │                    │");
        System.out.println("  │  │  有新连接 → 把 SocketChannel 交给  │                    │");
        System.out.println("  │  │            WorkerGroup 中某个线程  │                    │");
        System.out.println("  │  └─────────────────────────────────────┘                    │");
        System.out.println("  │           ↓ （Round-Robin 分配）                            │");
        System.out.println("  │  WorkerGroup（CPU×2 个线程，默认）                         │");
        System.out.println("  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │");
        System.out.println("  │  │ EventLoop-0 │ │ EventLoop-1 │ │ EventLoop-2 │  ...       │");
        System.out.println("  │  │ Selector    │ │ Selector    │ │ Selector    │           │");
        System.out.println("  │  │ Channel A   │ │ Channel C   │ │ Channel E   │           │");
        System.out.println("  │  │ Channel B   │ │ Channel D   │ │ Channel F   │           │");
        System.out.println("  │  └─────────────┘ └─────────────┘ └─────────────┘           │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  关键设计：每个 Channel 绑定一个 EventLoop，整个生命周期不换");
        System.out.println("    优点：同一 Channel 的所有事件（read/write/连接/断开）");
        System.out.println("          都在同一线程处理 → 天然无锁，不需要 synchronized！");
        System.out.println("    类比：一个快递员（EventLoop）负责固定几栋楼（Channel）");
        System.out.println("          他认识每家住户，不需要和别人交接，效率高且不出错");
        System.out.println();
        System.out.println("  代码结构：");
        System.out.println("    EventLoopGroup bossGroup   = new NioEventLoopGroup(1);");
        System.out.println("    EventLoopGroup workerGroup = new NioEventLoopGroup(); // 默认 CPU×2");
        System.out.println("    ServerBootstrap bootstrap  = new ServerBootstrap();");
        System.out.println("    bootstrap");
        System.out.println("        .group(bossGroup, workerGroup)");
        System.out.println("        .channel(NioServerSocketChannel.class)  // 用 NIO");
        System.out.println("        .childHandler(new ChannelInitializer<SocketChannel>() {");
        System.out.println("            protected void initChannel(SocketChannel ch) {");
        System.out.println("                ch.pipeline()");
        System.out.println("                  .addLast(new LengthFieldDecoder(...)) // 拆包");
        System.out.println("                  .addLast(new MyBusinessHandler());    // 业务");
        System.out.println("            }");
        System.out.println("        });");
        System.out.println("    bootstrap.bind(8080).sync(); // 绑定端口，开始监听");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：Pipeline & ChannelHandler（责任链模式）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. Pipeline & ChannelHandler（责任链模式）━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  每个 Channel 都有一个 ChannelPipeline（固定的双向链表）");
        System.out.println("  数据进来（Inbound）从链表头往尾走");
        System.out.println("  数据出去（Outbound）从链表尾往头走");
        System.out.println();
        System.out.println("  [网络] ←→ [Head] → [H1] → [H2] → [H3] → [Tail]");
        System.out.println("            ←─────────────────────────────────────");
        System.out.println("  Inbound流 →  Head  →  H1  →  H2  →  H3  → Tail（数据读进来）");
        System.out.println("  Outbound流←  Head  ←  H1  ←  H2  ←  H3  ← Tail（数据写出去）");
        System.out.println();
        System.out.println("  生活类比：流水线工厂");
        System.out.println("    原材料进来：工位1（解码）→ 工位2（验签）→ 工位3（业务处理）");
        System.out.println("    成品出去：  工位3（生成响应）→ 工位2（加签）→ 工位1（编码）");
        System.out.println();
        System.out.println("  三种 Handler：");
        System.out.println("    ChannelInboundHandlerAdapter  - 只处理读进来的数据");
        System.out.println("    ChannelOutboundHandlerAdapter - 只处理写出去的数据");
        System.out.println("    ChannelDuplexHandler          - 双向都处理");
        System.out.println();
        System.out.println("  典型 Pipeline 配置（一个 RPC 服务器）：");
        System.out.println("    pipeline.addLast(new IdleStateHandler(60, 0, 0)); // 心跳检测");
        System.out.println("    pipeline.addLast(new LengthFieldBasedFrameDecoder(...)); // 拆包");
        System.out.println("    pipeline.addLast(new ProtobufDecoder(MyProto.getDefaultInstance())); // 解码");
        System.out.println("    pipeline.addLast(new LengthFieldPrepender(4));    // 加长度头");
        System.out.println("    pipeline.addLast(new ProtobufEncoder());          // 编码");
        System.out.println("    pipeline.addLast(new BusinessHandler());          // 业务逻辑");
        System.out.println();
        System.out.println("  ⚠ 黄金法则：Handler 里不能做阻塞操作（数据库/HTTP调用）！");
        System.out.println("    原因：Handler 在 EventLoop 线程里执行，阻塞 → 整个线程卡死");
        System.out.println("          EventLoop 线程卡死 → 所有绑定的 Channel 都无法响应！");
        System.out.println("    正确做法：把阻塞操作提交到独立的业务线程池");
        System.out.println("      ctx.channel().eventLoop().execute(() -> {");
        System.out.println("          // 阻塞操作（在 EventLoop 线程里，不推荐）");
        System.out.println("      });");
        System.out.println("      // 正确：提交到业务线程池");
        System.out.println("      businessExecutor.submit(() -> {");
        System.out.println("          String result = db.query(...); // 阻塞无所谓，独立线程");
        System.out.println("          ctx.writeAndFlush(result);     // 写回时回到 EventLoop");
        System.out.println("      });");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：ByteBuf vs ByteBuffer —— 双指针 vs 单指针
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. ByteBuf vs ByteBuffer —— 双指针彻底消灭 flip() ━━━━━━━━");
        System.out.println();
        System.out.println("  ByteBuffer（JDK 原生）的痛苦：");
        System.out.println("    只有 1 个 position 指针，读写共用");
        System.out.println("    写完 → flip() → 读，读完 → clear() → 再写");
        System.out.println("    忘了 flip() = 读到垃圾数据，是 NIO 新手最常见 Bug");
        System.out.println();
        System.out.println("  ByteBuf（Netty）的优雅：");
        System.out.println("    readerIndex ─── 下一次读从这里开始");
        System.out.println("    writerIndex ─── 下一次写从这里开始");
        System.out.println("    两个指针独立，读写互不干扰，永远不需要 flip()！");
        System.out.println();
        System.out.println("  ByteBuf 内存结构：");
        System.out.println("    ┌──────────────┬─────────────────┬──────────────────────────┐");
        System.out.println("    │ 已读（废弃区）│  可读数据区     │      可写空间            │");
        System.out.println("    └──────────────┴─────────────────┴──────────────────────────┘");
        System.out.println("    0          readerIndex       writerIndex                capacity");
        System.out.println();
        System.out.println("    readableBytes()  = writerIndex - readerIndex  （有多少可读）");
        System.out.println("    writableBytes()  = capacity - writerIndex      （还能写多少）");
        System.out.println();
        System.out.println("  ByteBuf 读写示例（对比 ByteBuffer）：");
        System.out.println();
        System.out.println("  // ByteBuffer（痛苦版）");
        System.out.println("  ByteBuffer buf = ByteBuffer.allocate(64);");
        System.out.println("  buf.put(\"hello\".getBytes());  // 写");
        System.out.println("  buf.flip();                    // ← 必须！否则读到 0");
        System.out.println("  byte[] data = new byte[buf.remaining()];");
        System.out.println("  buf.get(data);                 // 读");
        System.out.println("  buf.clear();                   // ← 准备下次写");
        System.out.println();
        System.out.println("  // ByteBuf（优雅版）");
        System.out.println("  ByteBuf buf = Unpooled.buffer(64);");
        System.out.println("  buf.writeBytes(\"hello\".getBytes()); // 写，writerIndex 自动增");
        System.out.println("  byte[] data = new byte[buf.readableBytes()];");
        System.out.println("  buf.readBytes(data);                 // 读，readerIndex 自动增");
        System.out.println("  // 不需要 flip()！不需要 clear()！");
        System.out.println();
        System.out.println("  ByteBuf 三种内存类型：");
        System.out.println("    堆内 ByteBuf：  Unpooled.heapBuffer()   在 JVM 堆，GC 管理");
        System.out.println("    堆外 ByteBuf：  Unpooled.directBuffer() 在 OS 内存，需手动释放");
        System.out.println("    复合 ByteBuf：  Unpooled.wrappedBuffer() 多个 buf 的逻辑合并");
        System.out.println("                   不发生内存拷贝！（类似指针数组）");
        System.out.println();
        System.out.println("  池化 vs 非池化：");
        System.out.println("    Unpooled.xxx()              非池化，用完就丢，GC 回收");
        System.out.println("    PooledByteBufAllocator.DEFAULT.buffer()  池化，借出用完还回去");
        System.out.println("    Netty 默认：堆外 + 池化（高并发下大幅减少 GC 压力）");
        System.out.println();
        System.out.println("  ⚠ 内存泄漏陷阱：ByteBuf 是引用计数的（refCnt）");
        System.out.println("    创建时 refCnt = 1，调用 release() → refCnt - 1 → 归零则回收");
        System.out.println("    如果忘了 release()，堆外内存泄漏，最终 OutOfMemoryError");
        System.out.println("    Netty 原则：谁最后用，谁 release()");
        System.out.println("    SimpleChannelInboundHandler 会自动 release inbound 消息");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：Netty 处理一个请求的完整流程
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. 从「客户端发数据」到「Handler 收到」的完整流程 ━━━━━━━");
        System.out.println();
        System.out.println("  Step1：客户端发送数据包");
        System.out.println("    网卡收到数据 → DMA 写入 Socket 接收缓冲区");
        System.out.println("    硬件中断 → 内核唤醒 epoll_wait → Selector.select() 返回");
        System.out.println();
        System.out.println("  Step2：EventLoop 线程处理 OP_READ 事件");
        System.out.println("    channel.read(ByteBuf)  ← 把数据从 Socket 缓冲区搬到 ByteBuf");
        System.out.println("    触发 pipeline.fireChannelRead(ByteBuf)");
        System.out.println();
        System.out.println("  Step3：ByteBuf 流过 Pipeline（Inbound 方向）");
        System.out.println("    [Head] → [LengthFieldDecoder] → [ProtobufDecoder] → [BusinessHandler]");
        System.out.println("    每个 Handler 处理完调用 ctx.fireChannelRead(msg) 传给下一个");
        System.out.println();
        System.out.println("  Step4：BusinessHandler 处理业务，写响应");
        System.out.println("    ctx.writeAndFlush(response)  ← 触发 Outbound 方向");
        System.out.println();
        System.out.println("  Step5：响应流过 Pipeline（Outbound 方向）");
        System.out.println("    [BusinessHandler] → [ProtobufEncoder] → [LengthPrepender] → [Head]");
        System.out.println("    Head 将数据写入 Socket 发送缓冲区，网卡 DMA 发出");
        System.out.println();
        System.out.println("  整条链：");
        System.out.println("    网卡 → Socket缓冲区 → ByteBuf → Pipeline(decode) → Handler");
        System.out.println("    Handler → Pipeline(encode) → Socket缓冲区 → 网卡 → 客户端");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十八部分：Netty 粘包/拆包处理
//
// ── 为什么会有粘包/拆包？ ──────────────────────────────────────────────
// TCP 是面向「字节流」的协议，不是面向「消息」的协议。
// 你发一条消息，TCP 不保证对方一次 read() 就能收完。
// 也不保证你发两条消息，对方不会在一次 read() 里全收到。
//
// 生活类比：
//   你往水管里灌了 3 桶水（3条消息），
//   对面的人用碗接水（read()），
//   可能接出来是：半桶+一桶半，也可能是两桶+一桶……
//   每次 read() 的结果不确定，这就是粘包/拆包。
// ====================================================================
class Part18_NettyFrameDecoder {

    static void explain() {
        System.out.println("【第十八部分：Netty 粘包/拆包处理】");
        System.out.println();
        System.out.println("═══ 🚰 生活场景：水管里的水 理解粘包/拆包 ═══");
        System.out.println();
        System.out.println("  想象你往水管里灌水（发送数据），对面用碗接水（接收数据）：");
        System.out.println();
        System.out.println("  你往水管里依次灌了 3 桶水（3 条消息）：");
        System.out.println("    第1桶：红色水（消息A）");
        System.out.println("    第2桶：绿色水（消息B）");
        System.out.println("    第3桶：蓝色水（消息C）");
        System.out.println();
        System.out.println("  对面用碗接水，可能出现这些情况：");
        System.out.println("    正常情况：三碗分别接到红/绿/蓝（理想，实际几乎不存在）");
        System.out.println("    粘包：第一碗同时接到了红色+绿色水混在一起（两条消息连在一起）");
        System.out.println("    拆包：第一碗只接到半碗红色，第二碗接到另半碗红+全部绿（一条消息被拆开）");
        System.out.println("    混合：最混乱的情况，各种颜色都可能混在一起");
        System.out.println();
        System.out.println("  问题根源：TCP 是「字节流」，不是「消息流」");
        System.out.println("    TCP 只保证数据按顺序、不丢失地到达，但「消息边界」它不管！");
        System.out.println("    就像水管里的水混在一起，你根本分不清哪段是「第1桶」哪段是「第2桶」");
        System.out.println();
        System.out.println("  解决方案（如何区分消息边界）：");
        System.out.println("    方法1：固定长度——每桶水规定必须是1升（FixedLengthFrameDecoder）");
        System.out.println("    方法2：特殊分隔符——每桶水里加一块石头作为「消息结束标志」(\\n)");
        System.out.println("    方法3：长度+内容——先告诉对方「这桶水有3升」，再灌水（最常用！）");
        System.out.println("    方法4：自定义协议——先加「桶的颜色（类型）」+「桶的容量」+水（RPC协议）");
        System.out.println();
        System.out.println("  Netty 内置了这 4 种方案的 FrameDecoder，你只需配置，不用自己写逻辑！");
        System.out.println();
        System.out.println("═══ 以下是粘包/拆包技术细节 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：粘包/拆包原理
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 粘包/拆包原理（彻底搞清楚）━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  发送方发了 3 条消息（A=5字节，B=3字节，C=4字节）：");
        System.out.println("  ┌─────┬───┬────┐");
        System.out.println("  │AAAAA│BBB│CCCC│");
        System.out.println("  └─────┴───┴────┘");
        System.out.println();
        System.out.println("  接收方可能的 read() 结果（4种情况）：");
        System.out.println();
        System.out.println("  ① 理想情况（不粘不拆）：");
        System.out.println("    read1: AAAAA   read2: BBB   read3: CCCC");
        System.out.println();
        System.out.println("  ② 粘包（两条消息粘在一起收到）：");
        System.out.println("    read1: AAAAABBB   read2: CCCC");
        System.out.println("    原因：发送端 Nagle 算法把小包合并，或接收端 read() 太慢");
        System.out.println();
        System.out.println("  ③ 拆包（一条消息被拆成两次收到）：");
        System.out.println("    read1: AAA   read2: AABBBCCCC");
        System.out.println("    原因：网络 MTU 限制，或发送缓冲区不够，TCP 把消息拆开发");
        System.out.println();
        System.out.println("  ④ 粘包+拆包混合（最头疼）：");
        System.out.println("    read1: AAABBB   read2: AACCCC");
        System.out.println("    前面 A 被拆了，同时 B 和下一个包头粘在一起");
        System.out.println();
        System.out.println("  关键结论：不能假设一次 read() = 一条完整消息！");
        System.out.println("           必须在应用层定义「消息边界」来分割字节流");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：四种解决方案对比
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. 四种消息边界方案对比 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  方案1：固定长度（FixedLengthFrameDecoder）");
        System.out.println("    每条消息固定 N 字节，不够 N 字节就等，凑满再交给上层");
        System.out.println("    ┌────────────────┐┌────────────────┐");
        System.out.println("    │   消息A (16B)  ││   消息B (16B)  │");
        System.out.println("    └────────────────┘└────────────────┘");
        System.out.println("    优点：简单   缺点：消息长度必须固定（浪费空间或截断）");
        System.out.println("    Netty：new FixedLengthFrameDecoder(16)");
        System.out.println();
        System.out.println("  方案2：特殊分隔符（DelimiterBasedFrameDecoder）");
        System.out.println("    用 \\n 或自定义字节序列作为消息结束标志");
        System.out.println("    ┌────────────┬──┐┌────────────┬──┐");
        System.out.println("    │  消息内容  │\\n││  消息内容  │\\n│");
        System.out.println("    └────────────┴──┘└────────────┴──┘");
        System.out.println("    优点：变长消息   缺点：消息内容不能含分隔符（或需转义）");
        System.out.println("    Netty：new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter())");
        System.out.println("    HTTP 的请求头用的就是这种方式（\\r\\n\\r\\n 分隔 header 和 body）");
        System.out.println();
        System.out.println("  方案3：长度字段（LengthFieldBasedFrameDecoder）← 工业最常用");
        System.out.println("    消息 = 长度头（N字节）+ 消息体");
        System.out.println("    ┌───────┬──────────────────────┐");
        System.out.println("    │ 长度  │      消息内容        │");
        System.out.println("    │ (4B)  │   (长度字段指定)     │");
        System.out.println("    └───────┴──────────────────────┘");
        System.out.println("    优点：变长，内容可含任意字节，性能最好");
        System.out.println("    Netty：LengthFieldBasedFrameDecoder（6个参数，下面详讲）");
        System.out.println("    使用者：Dubbo、gRPC、Thrift、自定义 RPC 协议都用这种");
        System.out.println();
        System.out.println("  方案4：自定义协议头（用 ReplayingDecoder）");
        System.out.println("    消息 = 魔数 + 版本 + 长度 + 类型 + 序列号 + 消息体");
        System.out.println("    最灵活，适合完整 RPC 协议设计");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：LengthFieldBasedFrameDecoder 六参数详解
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. LengthFieldBasedFrameDecoder 六参数详解 ━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  构造参数：");
        System.out.println("  new LengthFieldBasedFrameDecoder(");
        System.out.println("      maxFrameLength,    // ① 最大帧长度（防 OOM 攻击）");
        System.out.println("      lengthFieldOffset, // ② 长度字段从第几字节开始");
        System.out.println("      lengthFieldLength, // ③ 长度字段本身占几个字节（通常2或4）");
        System.out.println("      lengthAdjustment,  // ④ 长度字段的值需要加多少才是消息体长度");
        System.out.println("      initialBytesToStrip // ⑤ 解码后去掉前几个字节（去掉头部）");
        System.out.println("  )");
        System.out.println();
        System.out.println("  场景1：最简单，长度 = 消息体长度，4字节长度字段");
        System.out.println("    报文: ┌─── 4B ───┬─── N 字节 ───┐");
        System.out.println("          │  length  │    body      │");
        System.out.println("          └──────────┴──────────────┘");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4)");
        System.out.println("    → 读 4 字节得到 length，再读 length 字节，Strip 掉 4 字节头");
        System.out.println("    → Handler 收到的是纯 body ByteBuf");
        System.out.println();
        System.out.println("  场景2：长度包含了头部（Dubbo 协议风格）");
        System.out.println("    报文: ┌─ 2B ─┬──── 2B ────┬─── N 字节 ───┐");
        System.out.println("          │魔数  │ total_len  │    body      │");
        System.out.println("          └──────┴────────────┴──────────────┘");
        System.out.println("          其中 total_len = 4 (头) + N (body)");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 2, 2, -4, 0)");
        System.out.println("    lengthFieldOffset=2 : 跳过2字节魔数才是长度字段");
        System.out.println("    lengthAdjustment=-4 : 长度值包含了头部4字节，减掉才是body长");
        System.out.println("    initialBytesToStrip=0: 不剥离头部，Handler 收完整帧");
        System.out.println();
        System.out.println("  场景3：自定义RPC协议（最常见设计）");
        System.out.println("    报文: ┌─ 4B ─┬─ 1B ─┬─ 1B ─┬─ 4B ─┬─── N 字节 ───┐");
        System.out.println("          │魔数  │版本  │类型  │body长│    body      │");
        System.out.println("          └──────┴──────┴──────┴──────┴──────────────┘");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 6, 4, 0, 10)");
        System.out.println("    lengthFieldOffset=6  : 前6字节(魔数+版本+类型)跳过");
        System.out.println("    lengthFieldLength=4  : 长度字段4字节");
        System.out.println("    lengthAdjustment=0   : 长度字段的值就是body长度");
        System.out.println("    initialBytesToStrip=10: 剥离掉前10字节头部，Handler收纯body");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：自定义协议编解码完整示例
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. 自定义 RPC 协议完整设计示例 ━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  协议格式（共 12 字节头 + 变长 body）：");
        System.out.println("  ┌──────────┬──────┬──────┬──────────┬──────────┬─────────────┐");
        System.out.println("  │ 魔数(4B) │版本  │类型  │序列号(4B)│body长(4B)│   body(NB)  │");
        System.out.println("  │ 0xCAFEBABE│(1B) │(1B)  │          │          │             │");
        System.out.println("  └──────────┴──────┴──────┴──────────┴──────────┴─────────────┘");
        System.out.println("  消息类型：0x01=请求 0x02=响应 0x03=心跳 0x04=心跳响应");
        System.out.println();
        System.out.println("  编码器（MessageEncoder extends MessageToByteEncoder<Message>）：");
        System.out.println("    protected void encode(ctx, msg, out) {");
        System.out.println("        out.writeInt(0xCAFEBABE);        // 魔数");
        System.out.println("        out.writeByte(1);                // 版本");
        System.out.println("        out.writeByte(msg.getType());    // 类型");
        System.out.println("        out.writeInt(msg.getSeqId());    // 序列号");
        System.out.println("        byte[] body = serialize(msg);");
        System.out.println("        out.writeInt(body.length);       // body 长度");
        System.out.println("        out.writeBytes(body);            // body");
        System.out.println("    }");
        System.out.println();
        System.out.println("  解码器（LengthFieldBasedFrameDecoder 参数）：");
        System.out.println("    // lengthFieldOffset=10(跳过前10字节到body长字段)");
        System.out.println("    // lengthFieldLength=4, lengthAdjustment=0");
        System.out.println("    // initialBytesToStrip=0(保留完整帧，再自己解析头)");
        System.out.println("    new LengthFieldBasedFrameDecoder(65536, 10, 4, 0, 0)");
        System.out.println();
        System.out.println("  消息解码器（MessageDecoder extends ByteToMessageDecoder）：");
        System.out.println("    protected void decode(ctx, in, out) {");
        System.out.println("        int magic  = in.readInt();   // 0xCAFEBABE");
        System.out.println("        byte ver   = in.readByte();");
        System.out.println("        byte type  = in.readByte();");
        System.out.println("        int seqId  = in.readInt();");
        System.out.println("        int len    = in.readInt();");
        System.out.println("        byte[] body = new byte[len];");
        System.out.println("        in.readBytes(body);");
        System.out.println("        out.add(deserialize(body, type)); // 传给下一个 Handler");
        System.out.println("    }");
        System.out.println();
        System.out.println("  心跳机制（IdleStateHandler）：");
        System.out.println("    pipeline.addLast(new IdleStateHandler(0, 0, 30)); // 30s无读写触发");
        System.out.println("    // Handler 里：");
        System.out.println("    public void userEventTriggered(ctx, evt) {");
        System.out.println("        if (evt instanceof IdleStateEvent) {");
        System.out.println("            ctx.writeAndFlush(HEARTBEAT_FRAME); // 发心跳");
        System.out.println("            // 如果对端也没响应，就关闭连接");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第十九部分：Netty 实战——从 Echo Server 到 HTTP Server
//
// 这部分通过两个完整的真实 Netty 程序，演示 Netty 的编程模式。
// 代码以注释形式呈现（实际运行需要 Netty 依赖），重点讲清楚每一行的作用。
// ====================================================================
class Part19_NettyPractice {

    static void explain() {
        System.out.println("【第十九部分：Netty 实战——Echo Server 到 HTTP Server】");
        System.out.println();
        System.out.println("═══ 🏨 生活场景：连锁酒店的标准服务流程 理解Netty实战 ═══");
        System.out.println();
        System.out.println("  把 Netty 服务器想象成一家连锁酒店的运营模式：");
        System.out.println();
        System.out.println("  【Echo Server = 最简单的「回声酒店」】");
        System.out.println("  酒店只有一个服务：客人说什么，前台就复述什么（Echo = 回声）");
        System.out.println("  这是最基础的服务模式，目的是验证整个服务流程通不通");
        System.out.println("  就像学编程时写的第一个「Hello World」程序");
        System.out.println();
        System.out.println("  酒店运营对应 Netty 组件：");
        System.out.println("    大堂经理（BossGroup）：专门负责迎接新客人（accept 新连接）");
        System.out.println("      - 只管「开门迎客、分配房间」，不管具体服务内容");
        System.out.println("      - 通常只需要 1 个大堂经理（BossGroup 线程数 = 1）");
        System.out.println();
        System.out.println("    客房服务员（WorkerGroup）：负责实际服务每位客人");
        System.out.println("      - 每个服务员管理几个客房（每个 EventLoop 管理多个 Channel）");
        System.out.println("      - 客人有需求就服务（read 事件），服务完继续等待下一个需求");
        System.out.println("      - 服务员数量 = CPU核数×2（WorkerGroup 线程数）");
        System.out.println();
        System.out.println("    服务手册（Pipeline）：每个客房都有一套标准服务流程");
        System.out.println("      - 客人说话→翻译（StringDecoder）→服务员理解→回复→翻译（StringEncoder）");
        System.out.println("      - Pipeline 就是这套「服务流程书」，每个步骤是一个 Handler");
        System.out.println();
        System.out.println("  【HTTP Server = 「会处理具体请求」的酒店】");
        System.out.println("  更高级的版本：不只是复述，而是能处理各种具体业务请求");
        System.out.println("    GET /menu → 返回菜单（处理 GET 请求）");
        System.out.println("    POST /order → 下单（处理 POST 请求）");
        System.out.println("  Netty 内置了 HTTP 解码器，把「字节流」翻译成「HTTP请求对象」");
        System.out.println("  你只需要处理业务逻辑，不需要自己解析 HTTP 协议");
        System.out.println();
        System.out.println("  【ChannelFuture = 「快递单号」异步编程模型】");
        System.out.println("  酒店给你下单的快递单号（ChannelFuture）：");
        System.out.println("    sync()：你在前台等，直到快递真的送到（阻塞）");
        System.out.println("    addListener()：留下手机号，货到了打你电话（回调，推荐）");
        System.out.println("    CLOSE 监听器：快递送达后，直接关闭快递单（连接关闭）");
        System.out.println();
        System.out.println("═══ 以下是 Netty 实战代码（含逐行注释）═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：Netty Echo Server（最简单的 Netty 程序）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. Netty Echo Server（完整代码 + 逐行注释）━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  // ── 服务端启动类 ──────────────────────────────────────────");
        System.out.println("  public class NettyEchoServer {");
        System.out.println("      public static void main(String[] args) throws Exception {");
        System.out.println();
        System.out.println("          // BossGroup：只处理 accept（通常1个线程就够）");
        System.out.println("          // 类比：前台接待员，只负责开门迎客，登记分配房间");
        System.out.println("          EventLoopGroup boss   = new NioEventLoopGroup(1);");
        System.out.println();
        System.out.println("          // WorkerGroup：处理 read/write/业务（默认 CPU×2）");
        System.out.println("          // 类比：客房服务员，具体服务每个客户");
        System.out.println("          EventLoopGroup worker = new NioEventLoopGroup();");
        System.out.println();
        System.out.println("          try {");
        System.out.println("              ServerBootstrap b = new ServerBootstrap();");
        System.out.println("              b.group(boss, worker)");
        System.out.println("               // 指定服务端 Channel 类型（NIO 模式）");
        System.out.println("               .channel(NioServerSocketChannel.class)");
        System.out.println("               // 服务端 Socket 配置");
        System.out.println("               .option(ChannelOption.SO_BACKLOG, 128)");
        System.out.println("               // 客户端 Channel 配置（每个 accept 的连接都会应用）");
        System.out.println("               .childOption(ChannelOption.SO_KEEPALIVE, true)");
        System.out.println("               // ★ 核心：初始化每个新连接的 Pipeline");
        System.out.println("               .childHandler(new ChannelInitializer<SocketChannel>() {");
        System.out.println("                   @Override");
        System.out.println("                   protected void initChannel(SocketChannel ch) {");
        System.out.println("                       ch.pipeline()");
        System.out.println("                         // 行分隔符拆包（防粘包）");
        System.out.println("                         .addLast(new LineBasedFrameDecoder(1024))");
        System.out.println("                         // 字节转字符串");
        System.out.println("                         .addLast(new StringDecoder(CharsetUtil.UTF_8))");
        System.out.println("                         // 字符串转字节");
        System.out.println("                         .addLast(new StringEncoder(CharsetUtil.UTF_8))");
        System.out.println("                         // 业务 Handler");
        System.out.println("                         .addLast(new EchoServerHandler());");
        System.out.println("                   }");
        System.out.println("               });");
        System.out.println();
        System.out.println("              // 绑定端口，sync() 等待绑定完成（异步 → 同步）");
        System.out.println("              ChannelFuture f = b.bind(8080).sync();");
        System.out.println("              System.out.println(\"Server started on port 8080\");");
        System.out.println();
        System.out.println("              // 等待服务器 Channel 关闭（阻塞 main 线程）");
        System.out.println("              f.channel().closeFuture().sync();");
        System.out.println("          } finally {");
        System.out.println("              // 优雅关闭（等待已提交任务执行完）");
        System.out.println("              boss.shutdownGracefully();");
        System.out.println("              worker.shutdownGracefully();");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // ── EchoServerHandler ────────────────────────────────────");
        System.out.println("  // SimpleChannelInboundHandler<T> 会自动 release() inbound 的 ByteBuf");
        System.out.println("  // 如果用 ChannelInboundHandlerAdapter，要手动 ReferenceCountUtil.release(msg)");
        System.out.println("  public class EchoServerHandler");
        System.out.println("          extends SimpleChannelInboundHandler<String> {");
        System.out.println();
        System.out.println("      @Override");
        System.out.println("      protected void channelRead0(ChannelHandlerContext ctx,");
        System.out.println("                                  String msg) {");
        System.out.println("          System.out.println(\"收到: \" + msg);");
        System.out.println("          // writeAndFlush = write(msg) + flush()");
        System.out.println("          // write 只是写到 Netty 发送缓冲区，flush 才真正触发写 Socket");
        System.out.println("          ctx.writeAndFlush(msg + \"\\n\"); // 带换行符，客户端能拆包");
        System.out.println("      }");
        System.out.println();
        System.out.println("      @Override");
        System.out.println("      public void channelActive(ChannelHandlerContext ctx) {");
        System.out.println("          // 连接建立时触发");
        System.out.println("          System.out.println(\"新连接：\" + ctx.channel().remoteAddress());");
        System.out.println("      }");
        System.out.println();
        System.out.println("      @Override");
        System.out.println("      public void exceptionCaught(ChannelHandlerContext ctx,");
        System.out.println("                                   Throwable cause) {");
        System.out.println("          // 统一异常处理（不要让异常吃掉，要记日志）");
        System.out.println("          cause.printStackTrace();");
        System.out.println("          ctx.close(); // 关闭连接");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：Netty HTTP Server
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. Netty HTTP Server（理解 HTTP 协议处理流程）━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Netty 内置了完整的 HTTP 编解码器，让你轻松实现 HTTP 服务器。");
        System.out.println("  理解这段代码，也能帮你理解 Spring Boot 底层（它底层就是 Netty）");
        System.out.println();
        System.out.println("  // ── HTTP Server 启动 ─────────────────────────────────────");
        System.out.println("  .childHandler(new ChannelInitializer<SocketChannel>() {");
        System.out.println("      protected void initChannel(SocketChannel ch) {");
        System.out.println("          ch.pipeline()");
        System.out.println("            // HTTP 请求解码器：字节 → HttpRequest + HttpContent");
        System.out.println("            .addLast(new HttpRequestDecoder())");
        System.out.println("            // 把多个 HttpContent 合并成一个完整 FullHttpRequest");
        System.out.println("            .addLast(new HttpObjectAggregator(65536))");
        System.out.println("            // HTTP 响应编码器：HttpResponse → 字节");
        System.out.println("            .addLast(new HttpResponseEncoder())");
        System.out.println("            // 支持大文件 chunked 传输");
        System.out.println("            .addLast(new ChunkedWriteHandler())");
        System.out.println("            // 业务 Handler");
        System.out.println("            .addLast(new HttpServerHandler());");
        System.out.println("      }");
        System.out.println("  });");
        System.out.println();
        System.out.println("  // ── HTTP Handler ────────────────────────────────────────");
        System.out.println("  public class HttpServerHandler");
        System.out.println("          extends SimpleChannelInboundHandler<FullHttpRequest> {");
        System.out.println();
        System.out.println("      protected void channelRead0(ChannelHandlerContext ctx,");
        System.out.println("                                  FullHttpRequest request) {");
        System.out.println("          // 获取请求信息");
        System.out.println("          String uri    = request.uri();            // /api/hello");
        System.out.println("          HttpMethod method = request.method();     // GET/POST");
        System.out.println("          String body   = request.content().toString(UTF_8); // 请求体");
        System.out.println();
        System.out.println("          // 构造响应");
        System.out.println("          String responseBody = \"{\\\"msg\\\": \\\"hello\\\"}\";");
        System.out.println("          FullHttpResponse response = new DefaultFullHttpResponse(");
        System.out.println("              HTTP_1_1,");
        System.out.println("              HttpResponseStatus.OK,");
        System.out.println("              Unpooled.copiedBuffer(responseBody, UTF_8)");
        System.out.println("          );");
        System.out.println("          response.headers()");
        System.out.println("              .set(CONTENT_TYPE, \"application/json\")");
        System.out.println("              .setInt(CONTENT_LENGTH, response.content().readableBytes());");
        System.out.println();
        System.out.println("          // 判断是否 keep-alive");
        System.out.println("          boolean keepAlive = HttpUtil.isKeepAlive(request);");
        System.out.println("          if (keepAlive) {");
        System.out.println("              response.headers().set(CONNECTION, KEEP_ALIVE);");
        System.out.println("              ctx.writeAndFlush(response);");
        System.out.println("          } else {");
        System.out.println("              // 非 keep-alive，写完关闭");
        System.out.println("              ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：Netty Future 和 Promise（异步编程模型）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. Netty 的异步编程：ChannelFuture & Promise ━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Netty 所有 IO 操作都是异步的，返回 ChannelFuture。");
        System.out.println();
        System.out.println("  生活类比：网购下单");
        System.out.println("    传统同步：在收银台前等，货没到不能走（线程阻塞）");
        System.out.println("    Netty 异步：下单后立即拿到「快递单号」（ChannelFuture）");
        System.out.println("              你可以继续干别的，货到了系统自动通知你（回调）");
        System.out.println();
        System.out.println("  四种使用方式：");
        System.out.println();
        System.out.println("  // 方式1：sync()——把异步变同步（简单，但会阻塞 EventLoop 线程）");
        System.out.println("  ChannelFuture f = channel.write(msg);");
        System.out.println("  f.sync(); // 阻塞当前线程直到写完");
        System.out.println("  ⚠ 注意：不能在 EventLoop 线程里调 sync()，会死锁！");
        System.out.println();
        System.out.println("  // 方式2：addListener()——回调（推荐，不阻塞）");
        System.out.println("  channel.writeAndFlush(msg).addListener(future -> {");
        System.out.println("      if (future.isSuccess()) {");
        System.out.println("          System.out.println(\"写入成功\");");
        System.out.println("      } else {");
        System.out.println("          future.cause().printStackTrace();");
        System.out.println("          channel.close();");
        System.out.println("      }");
        System.out.println("  });");
        System.out.println();
        System.out.println("  // 方式3：结合 ChannelFutureListener 常量");
        System.out.println("  ctx.writeAndFlush(response)");
        System.out.println("     .addListener(ChannelFutureListener.CLOSE); // 写完关闭连接");
        System.out.println();
        System.out.println("  // 方式4：Promise——可手动设置结果（类似 CompletableFuture）");
        System.out.println("  Promise<String> promise = channel.eventLoop().newPromise();");
        System.out.println("  // 在某个异步操作完成后：");
        System.out.println("  promise.setSuccess(\"result\");");
        System.out.println("  // 等待结果：");
        System.out.println("  String result = promise.sync().get();");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：Netty 连接池（ChannelPool）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. Netty 连接管理：连接池 & 优雅关闭 ━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  RPC 客户端为什么需要连接池？");
        System.out.println("    每次请求都建一个新 TCP 连接：三次握手 ~1ms，太慢");
        System.out.println("    复用连接：只需要一次握手，后续请求直接发数据");
        System.out.println();
        System.out.println("  Netty 连接池（SimpleChannelPool）：");
        System.out.println("    Bootstrap bootstrap = new Bootstrap()...");
        System.out.println("    ChannelPool pool = new SimpleChannelPool(bootstrap,");
        System.out.println("        new AbstractChannelPoolHandler() {");
        System.out.println("            public void channelCreated(Channel ch) {");
        System.out.println("                ch.pipeline().addLast(new MyHandler());");
        System.out.println("            }");
        System.out.println("        }");
        System.out.println("    );");
        System.out.println("    // 从池中借一个 Channel");
        System.out.println("    Future<Channel> future = pool.acquire();");
        System.out.println("    Channel ch = future.sync().get();");
        System.out.println("    ch.writeAndFlush(msg).sync();");
        System.out.println("    pool.release(ch); // ★ 用完必须还！");
        System.out.println();
        System.out.println("  Dubbo / Spring Cloud 的连接池本质上就是这样工作的");
        System.out.println();
        System.out.println("  优雅关闭（生产必备）：");
        System.out.println("    // 不要直接 System.exit()！");
        System.out.println("    // 正确方式：让 EventLoop 处理完当前任务后关闭");
        System.out.println("    bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);");
        System.out.println("    workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);");
        System.out.println("    // 第一个参数 quietPeriod=0：没有新任务时立即关闭");
        System.out.println("    // 第二个参数 timeout=5：最多等 5 秒");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第二十部分：Kafka 完整 IO 体系深度解析
//
// ── 这部分讲什么？ ─────────────────────────────────────────────────────
// Kafka 是 IO 性能优化的教科书级别案例。
// 它同时用了：顺序写、PageCache、零拷贝、批量发送、压缩、mmap 索引……
// 每一项技术都是有原因的。这部分从整体架构出发，逐层分析每个 IO 决策。
// ====================================================================
class Part20_KafkaIODeepDive {

    static void explain() {
        System.out.println("【第二十部分：Kafka 完整 IO 体系深度解析】");
        System.out.println();
        System.out.println("═══ 📰 生活场景类比：报社印刷厂，理解 Kafka 完整 IO 体系 ═══");
        System.out.println();
        System.out.println("  把 Kafka 想象成一家「日报印刷厂」：");
        System.out.println();
        System.out.println("  ① 存储结构（.log + .index 文件）= 印刷厂的「流水号报纸仓库」");
        System.out.println("    仓库按「期号」分区存放，每摞报纸（Segment）最多堆1米高（1GB）");
        System.out.println("    每摞第一张报纸的期号就是文件名（方便快速定位：二分查找）");
        System.out.println("    .index 文件 = 每摞旁边贴的「抽查清单」：第100张在哪页？");
        System.out.println();
        System.out.println("  ② 顺序写磁盘 = 印刷机「连续出纸」，不跳格不倒退");
        System.out.println("    连续出纸（顺序写）比「随机跳页印刷」快 100 倍");
        System.out.println("    磁盘顺序写速度可媲美内存随机写，这是 Kafka 高吞吐的第一个秘密");
        System.out.println();
        System.out.println("  ③ Page Cache = 印刷厂门口的「展示架」");
        System.out.println("    最新出炉的报纸直接摆展示架（OS Page Cache），客户直接拿走");
        System.out.println("    根本不用进仓库找（跳过磁盘 IO），Kafka 消费者几乎消费 Page Cache 里的数据");
        System.out.println();
        System.out.println("  ④ sendfile 零拷贝消费 = 印刷厂直接用「传送带」把报纸送上卡车");
        System.out.println("    不经过「搬运工手中转」（用户态），直接从展示架（Page Cache）传送带到卡车（网卡）");
        System.out.println("    Consumer 消费时，数据：Page Cache → 网卡（2次DMA，0次CPU拷贝）");
        System.out.println();
        System.out.println("  ⑤ Producer 写入链路 = 记者投稿 → 编辑暂存 → 批量印刷");
        System.out.println("    记者（Producer）发稿 → 编辑桌（OS Page Cache）暂存");
        System.out.println("    积攒一批后一次性送印刷机（fsync刷盘），而不是每篇稿子都印一次");
        System.out.println();
        System.out.println("═══ 以上是 Kafka IO 的「印刷厂全流程」，技术细节见下 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：Kafka 存储结构（IO 优化的基础）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. Kafka 存储结构（为 IO 优化而设计）━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Kafka 数据目录结构：");
        System.out.println("    /kafka/data/");
        System.out.println("      ├── my-topic-0/          ← Partition 0");
        System.out.println("      │     ├── 00000000000000000000.log     ← 消息数据文件");
        System.out.println("      │     ├── 00000000000000000000.index   ← 稀疏偏移量索引");
        System.out.println("      │     ├── 00000000000000000000.timeindex ← 时间索引");
        System.out.println("      │     ├── 00000000000001000000.log     ← 下一个 Segment");
        System.out.println("      │     └── 00000000000001000000.index");
        System.out.println("      └── my-topic-1/          ← Partition 1");
        System.out.println();
        System.out.println("  文件名 = 该 Segment 第一条消息的 offset（方便二分查找定位）");
        System.out.println("  一个 Partition 由多个 Segment（默认 1GB 或 7天）组成");
        System.out.println();
        System.out.println("  .log 文件格式（每条消息）：");
        System.out.println("    ┌──────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("    │offset(8B)│size(4B)  │CRC(4B)   │attributes│key/value │");
        System.out.println("    └──────────┴──────────┴──────────┴──────────┴──────────┘");
        System.out.println("    消息追加写，永远是顺序写！");
        System.out.println();
        System.out.println("  .index 文件（稀疏索引，mmap 加载）：");
        System.out.println("    ┌──────────────────┬──────────────────┐");
        System.out.println("    │ relativeOffset(4B)│ position(4B)    │");
        System.out.println("    └──────────────────┴──────────────────┘");
        System.out.println("    每隔 N 条消息记一个索引（稀疏），不是每条都有索引");
        System.out.println("    用 mmap 加载到内存，二分查找速度极快");
        System.out.println("    定位消息：二分查找 index → 找到 position → 顺序扫描 log 文件");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：Producer 写入流程（完整 IO 链路）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. Producer 写入流程（完整 IO 链路）━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Producer → Broker 的完整数据流：");
        System.out.println();
        System.out.println("  ① Producer 批量发送（batch.size + linger.ms）");
        System.out.println("     消息不是一条条发的！Producer 有 RecordAccumulator 累积器");
        System.out.println("     batch.size=16KB：积满 16KB 才发一批");
        System.out.println("     linger.ms=5：或者等 5ms 后不管有没满都发");
        System.out.println("     类比：快递公司不是收到一个包裹就发车，而是装满一车再发");
        System.out.println();
        System.out.println("  ② Producer 压缩（可选，减少网络 IO）");
        System.out.println("     compression.type = snappy/lz4/zstd");
        System.out.println("     压缩在 Producer 端进行，Broker 存压缩后的数据");
        System.out.println("     Consumer 解压，Broker 不解压（节省 CPU）");
        System.out.println("     lz4 压缩率约 50%，吞吐量提升约 50%（网络省一半）");
        System.out.println();
        System.out.println("  ③ Broker 接收数据（网络 IO）");
        System.out.println("     Kafka 网络层：1个 Acceptor + N个 Processor + M个 Handler");
        System.out.println("     Acceptor（1线程）：accept 新连接，分发给 Processor");
        System.out.println("     Processor（默认3线程）：Selector 管理连接，读取请求数据");
        System.out.println("     Handler（默认8线程）：真正处理请求（写文件等）");
        System.out.println("     这是 Reactor 多线程模式，和 Netty 的 Boss+Worker 思路一样");
        System.out.println();
        System.out.println("  ④ Broker 写入 Page Cache（关键！不是直接写磁盘）");
        System.out.println("     Log.append() → 写入 .log 文件（FileChannel.write）");
        System.out.println("     实际上写到 OS Page Cache，OS 异步刷盘（dirty page 回写）");
        System.out.println("     acks=1：写到 Leader Page Cache 就返回成功");
        System.out.println("     acks=all：Leader + ISR 副本都写 Page Cache 才返回");
        System.out.println();
        System.out.println("  ⑤ Broker 更新 .index（稀疏索引，每 N 字节写一条）");
        System.out.println("     .index 文件用 mmap 映射，写 = 写内存（极快）");
        System.out.println();
        System.out.println("  ⑥ 刷盘策略（可配置）");
        System.out.println("     log.flush.interval.messages=100000（每10万条强制刷盘）");
        System.out.println("     log.flush.interval.ms=1000（每1秒强制刷盘）");
        System.out.println("     默认：不主动刷盘，完全依赖 OS Page Cache（性能最好，但有风险）");
        System.out.println("     Kafka 高可用靠多副本（Replication），而不是依赖本地刷盘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：Consumer 消费流程（完整 IO 链路）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. Consumer 消费流程（sendfile 零拷贝的实战）━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Consumer 发送 FETCH 请求 → Broker 响应数据");
        System.out.println();
        System.out.println("  定位消息的过程：");
        System.out.println("    Consumer 告诉 Broker：我要 topic-0，从 offset=12345 开始");
        System.out.println("    Step1：在 /data/topic-0/ 目录下，二分查找 Segment 文件名");
        System.out.println("           找到 00000000000010000.log（第10000条消息开始的文件）");
        System.out.println("    Step2：mmap 加载 .index 文件，二分查找 offset=12345 的位置");
        System.out.println("           找到最近的索引：offset=12300 → file_position=5678");
        System.out.println("    Step3：从 position=5678 开始顺序扫描 .log 文件");
        System.out.println("           找到 offset=12345 的具体字节位置");
        System.out.println("    整个定位过程：约 3次磁盘IO（2次二分查找 + 1次顺序扫描）");
        System.out.println();
        System.out.println("  发送数据（零拷贝）：");
        System.out.println("    FileRecords.writeTo() 调用 FileChannel.transferTo()");
        System.out.println("    → sendfile()：Page Cache → 网卡（0次 CPU 拷贝）");
        System.out.println();
        System.out.println("  Page Cache 的巨大作用：");
        System.out.println("    消费者通常消费最新的消息（Consumer lag 不大）");
        System.out.println("    刚写入的消息还在 Page Cache 里（还没刷到磁盘！）");
        System.out.println("    transferTo 直接从 Page Cache 发给网卡，根本没有磁盘 IO！");
        System.out.println("    类比：外卖刚到手（Page Cache），直接给来取的客人（Consumer）");
        System.out.println("          根本没有放进冰箱（磁盘）再取出来的步骤");
        System.out.println();
        System.out.println("  为什么 Kafka 官方建议给 Broker 留大内存？");
        System.out.println("    不是给 JVM，而是给 OS Page Cache！");
        System.out.println("    JVM 堆建议只给 6~8GB（Kafka 本身不缓存数据在堆上）");
        System.out.println("    剩余内存全给 OS 当 Page Cache");
        System.out.println("    如果 Producer 生产速度 < Cache 命中率，消费者永远不读磁盘！");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：Kafka 性能数字全景
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. Kafka 性能数字全景（各项技术叠加的效果）━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  单机 Kafka（8核 64GB 1G网卡）典型吞吐量：");
        System.out.println("    写入：~600MB/s（接近网卡上限）");
        System.out.println("    读取：~600MB/s（接近网卡上限）");
        System.out.println("    写+读同时：~900MB/s（接近网卡满载）");
        System.out.println();
        System.out.println("  各项优化贡献：");
        System.out.println("  ┌──────────────────────────────┬──────────────────────────────┐");
        System.out.println("  │ 优化技术                     │ 贡献                         │");
        System.out.println("  ├──────────────────────────────┼──────────────────────────────┤");
        System.out.println("  │ 顺序写（vs 随机写）          │ 吞吐提升 6000 倍（HDD）      │");
        System.out.println("  │ Page Cache（不直接写磁盘）   │ 写延迟降低 10~100x           │");
        System.out.println("  │ 零拷贝（sendfile）           │ CPU 使用率降低 80%           │");
        System.out.println("  │ 批量发送（batch.size）       │ 网络 IO 次数降低 100x        │");
        System.out.println("  │ 压缩（lz4/snappy）           │ 网络带宽节省 50~70%          │");
        System.out.println("  │ Partition 并行               │ 吞吐线性扩展（分区越多越快）  │");
        System.out.println("  └──────────────────────────────┴──────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：Kafka 网络层源码导读
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. Kafka 网络层源码导读（Java NIO 的实际应用）━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Kafka 网络层是手写的 Java NIO，没有用 Netty（故意的）");
        System.out.println("  原因：Kafka 要精确控制每个 IO 细节，不想让框架层引入不确定性");
        System.out.println();
        System.out.println("  核心类：");
        System.out.println("    SocketServer.java   → 管理所有网络连接（Acceptor + Processors）");
        System.out.println("    Selector.java       → 封装 java.nio.Selector（Kafka 自己的！）");
        System.out.println("    KafkaChannel.java   → 单个 TCP 连接的抽象");
        System.out.println("    NetworkReceive.java → 负责接收完整消息（处理粘包/拆包）");
        System.out.println();
        System.out.println("  NetworkReceive（处理粘包/拆包）核心逻辑：");
        System.out.println("    // Kafka 用「4字节长度头 + body」的方式解决粘包");
        System.out.println("    if (size == null) {");
        System.out.println("        // 先读 4 字节长度");
        System.out.println("        if (sizeBuffer.hasRemaining()) {");
        System.out.println("            channel.read(sizeBuffer); // NIO 非阻塞读");
        System.out.println("        }");
        System.out.println("        if (!sizeBuffer.hasRemaining()) {");
        System.out.println("            sizeBuffer.flip();");
        System.out.println("            size = sizeBuffer.getInt(); // 长度");
        System.out.println("            buffer = ByteBuffer.allocate(size); // 按长度分配");
        System.out.println("        }");
        System.out.println("    } else {");
        System.out.println("        // 读 body（可能多次才读完）");
        System.out.println("        if (buffer.hasRemaining()) {");
        System.out.println("            channel.read(buffer);");
        System.out.println("        }");
        System.out.println("        if (!buffer.hasRemaining()) {");
        System.out.println("            // 一条完整消息收齐了！");
        System.out.println("            buffer.flip();");
        System.out.println("            complete = true;");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("    // 这就是手动实现了 LengthFieldBasedFrameDecoder 的功能！");
        System.out.println();
        NIODemo.printSeparator();
    }
}

// ====================================================================
// 第二十一部分：Nginx / RocketMQ / Netty / Kafka IO 横向对比与选型总结
//
// 把前面 20 个 Part 的知识串联起来，形成一个完整的知识体系。
// 用「为什么」驱动：为什么这个框架选择这种 IO 方式？
// ====================================================================
class Part21_FrameworkIOComparison {

    static void explain() {
        System.out.println("【第二十一部分：四大框架 IO 横向对比与选型总结】");
        System.out.println();
        System.out.println("═══ 🚗 生活场景类比：四种运输公司，理解四大框架 IO 选型 ═══");
        System.out.println();
        System.out.println("  把四个框架想象成四家「专业运输公司」，各自有不同的核心竞争力：");
        System.out.println();
        System.out.println("  🚕 Netty = 「豪华专车平台」（司机接单 + 行程管理 + 服务标准化）");
        System.out.println("    专注「乘客运输」（网络通信），不管货运（文件IO）");
        System.out.println("    特色：统一服务标准（Pipeline）、私家车（线程池）随叫随到");
        System.out.println("    适合：需要实时通信的场景——IM、游戏、RPC 框架");
        System.out.println();
        System.out.println("  📰 Kafka = 「报纸批量印发公司」（大批量、高频次、讲究顺序）");
        System.out.println("    专注「批量派送」（高吞吐消息），能同时服务百万订阅用户");
        System.out.println("    特色：顺序写磁盘+sendfile零拷贝，100万 QPS 不是梦");
        System.out.println("    适合：日志收集、流式数据管道、事件溯源");
        System.out.println();
        System.out.println("  📦 RocketMQ = 「精准速递公司」（既要快，还要保证每单必达）");
        System.out.println("    用 mmap 写消息（超快落盘）+ sendfile 消费（零拷贝）");
        System.out.println("    特色：事务消息、延迟消息、消费位点精确管理");
        System.out.println("    适合：电商订单、支付、需要强一致性的金融业务");
        System.out.println();
        System.out.println("  🏎️ Nginx = 「高速公路收费站」（同时服务海量过路车，自己不运货）");
        System.out.println("    用 epoll + sendfile，C 语言实现，几乎没有框架开销");
        System.out.println("    特色：1个 Worker 进程扛 1万+ 并发，静态文件发送极致快");
        System.out.println("    适合：反向代理、负载均衡、静态资源 CDN 源站");
        System.out.println();
        System.out.println("  选型口诀：");
        System.out.println("    需要「实时双向通信」  → Netty（你的 RPC/IM/游戏首选）");
        System.out.println("    需要「超高吞吐消息」  → Kafka（日志/流式管道）");
        System.out.println("    需要「业务消息可靠性」→ RocketMQ（电商/支付）");
        System.out.println("    需要「接入层/静态服务」→ Nginx（反向代理/CDN）");
        System.out.println();
        System.out.println("═══ 以上是四大框架的「运输公司」类比，技术对比见下表 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：四大框架 IO 技术栈横向对比
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. 四大框架 IO 技术栈横向对比 ━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌───────────┬──────────┬───────────────┬─────────────┬────────────┐");
        System.out.println("  │ 框架      │ 网络IO   │ 文件IO        │ 零拷贝      │ 特殊技术   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ Netty     │ NIO+epoll│ N/A（专注网络）│ ByteBuf零拷 │ PooledBuf  │");
        System.out.println("  │           │ Reactor  │               │ CompositeBuf│ Pipeline   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ Kafka     │ 手写NIO  │ FileChannel   │ sendfile    │ PageCache  │");
        System.out.println("  │           │ Reactor  │ 顺序写        │ transferTo  │ 稀疏索引   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ RocketMQ  │ Netty    │ mmap          │ mmap写      │ CommitLog  │");
        System.out.println("  │           │          │ FileChannel   │ sendfile读  │ ConsumeQ   │");
        System.out.println("  ├───────────┼──────────┼───────────────┼─────────────┼────────────┤");
        System.out.println("  │ Nginx     │ epoll    │ sendfile      │ sendfile    │ 事件驱动   │");
        System.out.println("  │           │ (C语言)  │               │             │ 进程模型   │");
        System.out.println("  └───────────┴──────────┴───────────────┴─────────────┴────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：为什么各框架选择不同的 IO 方案（深度分析）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. 为什么各框架选择不同的 IO 方案 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        System.out.println("  【Netty：为什么用 Reactor 主从模式 + ByteBuf 池化】");
        System.out.println("  核心诉求：通用高性能网络框架，支持任意协议");
        System.out.println("  为什么 Reactor 主从？");
        System.out.println("    Boss 线程只 accept，不做任何耗时操作");
        System.out.println("    accept 是非常快的操作（O(1)），1个线程足够");
        System.out.println("    Worker 线程专注 IO+业务，数量可配置");
        System.out.println("  为什么 ByteBuf 池化？");
        System.out.println("    高并发场景：每秒可能处理 10万+ 请求");
        System.out.println("    每个请求需要分配/释放 ByteBuf（DirectBuffer）");
        System.out.println("    DirectBuffer 分配涉及系统调用，极其昂贵（~μs 级）");
        System.out.println("    池化后：从内存池取用，不涉及系统调用（~ns 级）");
        System.out.println("    池化 DirectBuffer = 减少 GC + 减少系统调用，双重收益");
        System.out.println();

        System.out.println("  【Kafka：为什么顺序写 + PageCache + sendfile 的组合】");
        System.out.println("  核心诉求：最高消息吞吐量，消息持久化");
        System.out.println("  为什么顺序写？");
        System.out.println("    持久化意味着要写磁盘，随机写 HDD：0.1 MB/s，顺序写：600 MB/s");
        System.out.println("    Kafka 把所有消息追加写到文件末尾，永远是顺序写");
        System.out.println("    即使是 SSD，顺序写也比随机写快 3~10 倍（闪存特性）");
        System.out.println("  为什么 PageCache 而不是 Java 内存？");
        System.out.println("    PageCache 是 OS 管理的，进程重启不会丢（OS 还在）");
        System.out.println("    Java 堆对象重启就没了，还要 GC");
        System.out.println("    PageCache 可以和 sendfile 配合做零拷贝，Java 堆不行");
        System.out.println("  为什么不用 mmap（RocketMQ 用了，Kafka 没用）？");
        System.out.println("    Kafka：写入是顺序追加，FileChannel 已经够快");
        System.out.println("           mmap 的优势是随机访问，顺序写用不到");
        System.out.println("    RocketMQ：CommitLog 写入后还要构建 ConsumeQueue（需随机访问）");
        System.out.println("              用 mmap 做随机写更高效");
        System.out.println();

        System.out.println("  【RocketMQ：为什么 mmap 写 + sendfile 读的混合方案】");
        System.out.println("  核心诉求：低延迟写入 + 事务消息 + 顺序消息");
        System.out.println("  为什么写用 mmap？");
        System.out.println("    CommitLog 写入：put() 到 mmap 内存 = 没有 write() 系统调用");
        System.out.println("    比 Kafka 的 FileChannel.write() 还要快（省一次系统调用）");
        System.out.println("    延迟更低：适合对延迟敏感的场景（金融、实时通知）");
        System.out.println("  为什么读用 sendfile？");
        System.out.println("    Consumer 消费：文件 → 网卡，经典零拷贝场景");
        System.out.println("    mmap 读也可以，但 sendfile 对大文件顺序读更有优势");
        System.out.println();

        System.out.println("  【Nginx：为什么用 C 而不是 Java + 多进程而不是多线程】");
        System.out.println("  为什么 C？");
        System.out.println("    Nginx 是 Web 服务器，对每字节的内存占用极敏感");
        System.out.println("    10万并发：Java JVM 光 GC overhead 就不能接受");
        System.out.println("    C 没有 GC，内存精确控制，单 Worker 进程只占几 MB");
        System.out.println("  为什么多进程而不是多线程（Java 是多线程）？");
        System.out.println("    C 的多线程编程极容易出 bug（race condition）");
        System.out.println("    多进程：进程间独立，一个 Worker 崩溃不影响其他 Worker");
        System.out.println("    Master 进程负责管理 Worker，Worker 崩溃自动重启");
        System.out.println("    Java 框架（Netty）用多线程是因为 JVM 线程更轻（没有进程级开销）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：IO 知识体系全景图（把所有 Part 串起来）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. IO 知识体系全景图（21 个 Part 串联）━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │                    Java IO 完整知识体系                      │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 基础层（操作系统原理）                                       │");
        System.out.println("  │   Part1  五种IO模型  Part2  BIO问题  Part3  BufferedStream    │");
        System.out.println("  │   用户态/内核态  Page Cache  DMA  系统调用                   │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ NIO 核心层                                                   │");
        System.out.println("  │   Part4  Buffer三指针  Part5  Channel体系                    │");
        System.out.println("  │   Part6  Selector+epoll  Part7  SelectionKey事件             │");
        System.out.println("  │   Part8  Reactor单线程NIO Server                            │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 本地文件 IO 层                                               │");
        System.out.println("  │   Part14 FileChannel  Part15 MappedByteBuffer(mmap)          │");
        System.out.println("  │   Part16 NIO.2 Path/Files/WatchService                      │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 零拷贝层                                                     │");
        System.out.println("  │   Part9  transferTo调用链  Part10 缓冲区满/拥塞              │");
        System.out.println("  │   Part11 小文件零拷贝反而慢  Part12 Kafka/Nginx案例          │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ Netty 层（工业级封装）                                       │");
        System.out.println("  │   Part17 架构(EventLoop/Pipeline/ByteBuf)                   │");
        System.out.println("  │   Part18 粘包拆包(FrameDecoder/自定义协议)                  │");
        System.out.println("  │   Part19 实战(Echo/HTTP/Future/连接池)                      │");
        System.out.println("  ├──────────────────────────────────────────────────────────────┤");
        System.out.println("  │ 框架应用层                                                   │");
        System.out.println("  │   Part20 Kafka完整IO体系（顺序写/PageCache/sendfile）       │");
        System.out.println("  │   Part21 四大框架IO横向对比（选型指南）                     │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：面试高频问题 + 标准答案
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. 面试高频问题 & 标准答案 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Q1：BIO 和 NIO 的区别？");
        System.out.println("  A：BIO 是阻塞 IO，每个连接需要一个线程；");
        System.out.println("     NIO 是非阻塞 IO，一个线程（Selector）可以管理多个连接。");
        System.out.println("     BIO 瓶颈：线程数（内存、上下文切换）；");
        System.out.println("     NIO 瓶颈：CPU 处理能力（无线程浪费，但业务逻辑要快）。");
        System.out.println();
        System.out.println("  Q2：Kafka 为什么快？");
        System.out.println("  A：四个核心原因：");
        System.out.println("     ①顺序写（磁盘寻道时间为0）；");
        System.out.println("     ②写 Page Cache（不直接写磁盘，内存速度）；");
        System.out.println("     ③sendfile 零拷贝（读时 0次CPU拷贝）；");
        System.out.println("     ④批量+压缩（减少网络 IO 次数和带宽）。");
        System.out.println();
        System.out.println("  Q3：Netty 的 ByteBuf 和 JDK ByteBuffer 有什么区别？");
        System.out.println("  A：ByteBuffer 是单指针（position），读写需要 flip/clear 切换；");
        System.out.println("     ByteBuf 是双指针（readerIndex/writerIndex），读写独立，无需 flip；");
        System.out.println("     ByteBuf 支持池化（PooledByteBufAllocator），减少 GC；");
        System.out.println("     ByteBuf 支持复合（CompositeByteBuf），零拷贝合并 buf；");
        System.out.println("     ByteBuf 有引用计数，用完必须 release()，否则内存泄漏。");
        System.out.println();
        System.out.println("  Q4：什么是零拷贝？transferTo() 怎么实现零拷贝？");
        System.out.println("  A：零拷贝 = 减少 CPU 参与数据搬运（不是真的0次拷贝）。");
        System.out.println("     传统读文件到 Socket：4次拷贝（DMA+CPU拷贝各2次）。");
        System.out.println("     transferTo 底层调用 sendfile()：只有2次DMA拷贝，0次CPU拷贝。");
        System.out.println("     文件数据路径：磁盘→PageCache（DMA）→网卡（DMA，Linux2.4+）。");
        System.out.println("     Kafka 的 FileRecords.writeTo() 就是用 transferTo() 实现的。");
        System.out.println();
        System.out.println("  Q5：Netty 的 EventLoop 为什么不能做阻塞操作？");
        System.out.println("  A：一个 EventLoop 绑定多个 Channel，一个线程管理多个连接。");
        System.out.println("     如果 EventLoop 线程阻塞（等数据库/HTTP），");
        System.out.println("     所有绑定的 Channel 都无法处理读写事件。");
        System.out.println("     解决：把阻塞操作提交到独立的业务线程池执行。");
        System.out.println();
        System.out.println("  Q6：TCP 粘包是什么？Netty 怎么解决？");
        System.out.println("  A：TCP 是字节流协议，多条消息可能在一次 read() 里全收到（粘包）");
        System.out.println("     或一条消息需要多次 read() 才能收完（拆包）。");
        System.out.println("     Netty 解决方案：");
        System.out.println("     ①固定长度：FixedLengthFrameDecoder");
        System.out.println("     ②分隔符：DelimiterBasedFrameDecoder");
        System.out.println("     ③长度字段（最常用）：LengthFieldBasedFrameDecoder");
        System.out.println("     ④自定义协议头：ReplayingDecoder");
        System.out.println();
        System.out.println("  Q7：mmap 内存映射适合什么场景？不适合什么场景？");
        System.out.println("  A：适合：大文件随机读（如 ES/Lucene 索引）、高频写（RocketMQ）");
        System.out.println("     不适合：小文件（mmap() 系统调用开销 > 节省的拷贝开销）");
        System.out.println("             需要精确管理内存的场景（mmap 无法主动 GC）");
        System.out.println("             Windows 上需要删除/重命名映射文件（句柄被占用）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：推荐学习路线（更新版）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. 完整学习路线（结合本文件 21 个 Part）━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  第1阶段：基础（Part1-3）");
        System.out.println("    目标：理解 IO 的本质，知道为什么需要 NIO");
        System.out.println("    重点：五种IO模型 + BIO的线程开销 + syscall减少原理");
        System.out.println("    实践：写一个 BIO 聊天室，亲身感受它的问题");
        System.out.println();
        System.out.println("  第2阶段：NIO 核心（Part4-8, Part14-16）");
        System.out.println("    目标：能写出正确的 NIO 代码");
        System.out.println("    重点：Buffer三指针 + Selector + FileChannel + NIO.2 Files");
        System.out.println("    实践：把 BIO 聊天室改成 NIO 版本，感受差距");
        System.out.println();
        System.out.println("  第3阶段：零拷贝（Part9-12, Part15）");
        System.out.println("    目标：理解文件传输的 IO 链路");
        System.out.println("    重点：sendfile vs mmap + Page Cache + 什么时候用零拷贝");
        System.out.println("    实践：写一个文件服务器，对比 FileChannel vs transferTo 性能");
        System.out.println();
        System.out.println("  第4阶段：Netty（Part17-19）");
        System.out.println("    目标：能用 Netty 写生产级代码");
        System.out.println("    重点：EventLoop + Pipeline + ByteBuf + 粘包拆包");
        System.out.println("    实践：写一个 Netty Echo Server，然后加 LengthField 拆包");
        System.out.println("          再实现一个简单的 HTTP Server");
        System.out.println();
        System.out.println("  第5阶段：框架源码（Part20-21）");
        System.out.println("    目标：看懂开源框架的 IO 设计思路");
        System.out.println("    重点：Kafka IO 链路 + RocketMQ mmap + 四框架横向对比");
        System.out.println("    实践：读 Kafka FileRecords.writeTo() 源码");
        System.out.println("          读 RocketMQ MappedFile.appendMessage() 源码");
        System.out.println("          读 Netty NioEventLoop.processSelectedKeys() 源码");
        System.out.println();
        System.out.println("  通关标志：");
        System.out.println("    能清晰回答：「Kafka 为什么快」（要讲到 Page Cache 和 sendfile 级别）");
        System.out.println("    能清晰回答：「Netty 如何处理10万并发」（要讲到 EventLoop 无锁化）");
        System.out.println("    能手写出：正确的 NIO Echo Server（Buffer 不出错，事件不漏处理）");
        System.out.println();
        NIODemo.printSeparator();
    }
}

