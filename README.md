# Java + 计算机基础理论 学习项目

系统性学习 Java 核心知识体系与计算机基础理论，按「先打地基 → 再学工具 → 最后看工业实践」的顺序递进。

> **项目定位说明**
> - 本项目（`java_study`）：专注 Java 语言核心 + 计算机基础理论
> - `java_fullstack_ai_agent_study`（待建）：Java 全栈 + AI 与 Agent 工程学习
> - `ai_coding_harness_engineering_study`（待建）：AI Coding Harness 工程实践

---

## 学习模块总览

```
java_study/
│
├── java_base_test/                  Java 基础核心
│   ├── *.java                         ├─ Java 语言基础（泛型/集合/反射/内部类/基本类型等）
│   ├── io/                            ├─ IO / NIO 体系（核心重点）
│   │   ├── bio/                       │    ├─ BIO 阻塞模型
│   │   ├── nio/                       │    ├─ NIO 全体系（Part1~Part23，七层递进）
│   │   └── zerocopy/                  │    └─ 零拷贝专题
│   ├── thread_aync/                   ├─ 多线程 & 异步编程
│   ├── proxy_aop/                     ├─ 代理模式 & AOP
│   └── multi_agent/                   └─ 多 Agent 系统架构（演示用）
│
├── algorithm/                       算法
├── datastructures/                  数据结构
├── designmodel/                     设计模式
│
├── operate_system/                  计算机基础理论
├── compute_network/                   ├─ 操作系统
└── computer_principle/                ├─ 计算机网络
                                       └─ 计算机原理
```

各模块详细说明见对应目录下的 README.md。

---

## 推荐学习顺序

### 第一阶段：Java 语言地基
> 目标：把 Java 语言本身的核心机制吃透，后面学框架才不会「知其然不知其所以然」

```
Java 语言基础（泛型/equals/基本类型/内部类/反射）
    ↓
多线程 & 异步（线程池/ThreadLocal/CompletableFuture）
    ↓
代理模式 & AOP（静态代理 → JDK动态代理 → CGLIB → Spring AOP）
```

**学习重点：**
- `泛型`：类型擦除是高频面试题，理解后再看集合框架源码事半功倍
- `equals/hashCode`：必须同时重写，是 HashMap 正确工作的前提
- `ThreadLocal 内存泄漏`：弱引用 key + 强引用 value 的经典陷阱
- `CompletableFuture`：现代 Java 异步编程主力，掌握后可替代大量手动 Thread 写法
- `JDK动态代理 vs CGLIB`：Spring AOP 的底层，面试必考

---

### 第二阶段：IO 体系（重点 & 难点）
> 目标：彻底理解 Java IO 全体系，这是 Java 后端性能优化的核心竞争力

```
BIO（阻塞模型，理解痛点）
    ↓
NIO 地基：Buffer 三指针状态机 / Channel 类型体系
    ↓
NIO 网络 IO：Selector + epoll / SelectionKey 四种事件 / 完整 NIO Server
    ↓
零拷贝：transferTo 调用链 / 小文件陷阱 / Kafka·Nginx 实战
    ↓
NIO 文件 IO：FileChannel / MappedByteBuffer(mmap) / NIO.2
    ↓
Netty 框架：核心架构 / 粘包拆包 / Echo·HTTP Server 实战
    ↓
开源框架解析：Kafka 完整 IO 体系 / 四大框架横向对比（Netty/Kafka/RocketMQ/Nginx）
    ↓
前沿新特性：NIO.2 进阶 / JDK9~21 / 虚拟线程（Project Loom）
```

**入口文件：** `java_base_test/io/nio/NIODemo.java`（Part1 → Part23 依次运行）

> ⚠️ **学习建议：** Part6~Part8（Selector+NIO Server）是整个体系最难的部分，反复看几遍再继续。
> 零拷贝（Part9~Part12）和 Netty（Part17~Part19）理解起来以前面为基础，不要跳读。

---

### 第三阶段：算法与设计
> 目标：提升系统设计能力，建立「用合适的设计解决合适问题」的工程直觉

```
数据结构（链表/树/堆/哈希表）→ 算法（排序/二分/动态规划/图）
    ↓
设计模式（先学高频：单例/工厂/装饰器/代理/观察者/策略/责任链）
    ↓
多 Agent 系统（Orchestrator-Worker 架构演示）
```

**学习建议：** 设计模式不要死记名字，重点理解「它解决了什么问题」，结合已学的 IO 和 Spring AOP 场景对照理解（装饰器 = BufferedInputStream，责任链 = Netty Pipeline，策略 = Spring HandlerMapping）。

---

### 第四阶段：计算机基础理论（穿插学习）
> 目标：理解 Java 底层行为的硬件/OS 根因，不是单独学，而是遇到疑问时对照查

| 遇到这个问题时 | 去看这个模块 |
|--------------|------------|
| epoll 为什么比 select 快？NIO Selector 怎么工作？ | `operate_system/`（IO多路复用） |
| mmap 原理是什么？Page Cache 怎么加速 Kafka？ | `operate_system/`（内存管理） |
| TCP 粘包/拆包是什么？为什么 Netty 要做 FrameDecoder？ | `compute_network/`（传输层） |
| volatile 为什么能保证可见性？DCL 为什么要加 volatile？ | `computer_principle/`（内存屏障） |
| DMA 是什么？零拷贝为什么说「2次DMA拷贝，0次CPU拷贝」？ | `computer_principle/`（总线&DMA） |

---

## 运行方式

```bash
# 编译整个项目
mvn compile

# 运行 IO/NIO 全体系演示（推荐从这里开始）
java -cp target/classes org.example.java_base_test.io.nio.NIODemo

# 运行多 Agent 系统演示
java -cp target/classes org.example.java_base_test.multi_agent.MultiAgentDemo

