package org.example.java_base_test.io.nio.show_multi_agent;

/**
 * ============================================================
 * Java IO / NIO 全体系深度演示 — 多 Agent 重构版
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
 * 多 Agent 重构说明
 * ============================================================
 *
 * 本包（show_multi_agent）是对 nio/NIODemo.java 的重构演示：
 *
 * 重构方式：
 *   原始文件：NIODemo.java（单文件，5000+ 行，23 个 Part 全在一起）
 *   重构后：  每个 Part 拆分为独立的 .java 文件，本文件作为统一入口
 *
 * 重构过程采用「多 Agent 并行」模式：
 *   - Orchestrator（主 Agent）：分析行号边界，拆分任务，分发给三组子 Agent
 *   - 子 Agent 组 A：并行创建 Part1  ~ Part8  共 8 个文件
 *   - 子 Agent 组 B：并行创建 Part9  ~ Part16 共 8 个文件
 *   - 子 Agent 组 C：并行创建 Part17 ~ Part23 共 7 个文件
 *   - 主 Agent 汇聚结果，创建本入口文件
 *
 * 包结构：
 *   show_multi_agent/
 *     ├── NIODemoMain.java          ← 本文件（统一入口，main 方法）
 *     ├── NIODemo.java              ← 工具类（printSeparator 辅助方法）
 *     ├── Part1_FiveIOModels.java
 *     ├── Part2_BIOProblem.java
 *     ├── Part3_BufferedInputStreamSpeedup.java
 *     ├── Part4_BufferStateMachine.java
 *     ├── Part5_ChannelTypes.java
 *     ├── Part6_SelectorEpoll.java
 *     ├── Part7_SelectionKeyEvents.java
 *     ├── Part8_NIOServerExplained.java
 *     ├── Part9_TransferToCallChain.java
 *     ├── Part10_BufferFullAndCongestion.java
 *     ├── Part11_SmallFileSlower.java
 *     ├── Part12_KafkaNginxCases.java
 *     ├── Part13_MistakesAndSelection.java
 *     ├── Part14_FileChannelLocalIO.java
 *     ├── Part15_MappedByteBuffer.java
 *     ├── Part16_NIO2FilesAndWatch.java
 *     ├── Part17_NettyArchitecture.java
 *     ├── Part18_NettyFrameDecoder.java
 *     ├── Part19_NettyPractice.java
 *     ├── Part20_KafkaIODeepDive.java
 *     ├── Part21_FrameworkIOComparison.java
 *     ├── Part22_NIO2Advanced.java
 *     └── Part23_ModernJdkIOFeatures.java
 *
 * ============================================================
 */
public class NIODemoMain {

    public static void main(String[] args) throws Exception {
        System.out.println("============================================================");
        System.out.println("  Java IO / NIO 全体系深度演示（多 Agent 重构版）");
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
        System.out.println("  【多 Agent 重构版：每个 Part 为独立文件，便于单独阅读和修改】");
        System.out.println("============================================================");
    }
}

