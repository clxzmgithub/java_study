# 📚 Java + 计算机基础理论 学习项目

<div align="center">

![Status](https://img.shields.io/badge/状态-持续更新中-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)

</div>

---

## 📌 项目定位

本项目是三个关联学习工程中的**基础理论层**，专注 Java 语言核心机制与计算机基础理论。

目标是把「知其然」变成「知其所以然」——在学 Spring、分布式、大数据之前，先把 Java 语言本身、IO 体系、并发模型，以及操作系统、计算机网络、计算机原理、编译原理等底层基础打牢。没有这层地基，工程实践中遇到问题只能靠搜索和猜测，而不是真正理解。

> ⚠️ **关联项目分工说明**
>
> 本人维护三个相互补充、各有侧重的学习仓库，共同构成完整的知识体系：
>
> | 项目 | 定位 | 核心聚焦 |
> |------|:----:|---------|
> | **本项目**（`java_study`） | 🔵 基础理论 | Java 语言核心、数据结构与算法、IO/NIO 体系、操作系统、计算机网络、计算机原理、编译原理——**打地基** |
> | [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) | 🟠 工程实践 | Spring 生态、分布式架构、大数据、存储中间件、风控爬虫、数据分析、多语言、AI/Agent——**做系统** |
> | [`ai_coding_harness_engineering_study`](../ai_coding_harness_engineering_study) | 🟣 AI 工程 | AI 编程方法论、上下文工程、Harness 理论、AI Coding 工具链、大模型与 Agent 开发——**用 AI 提效** |

---

## 🗺️ 学习模块总览

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
├── computer_principle/                ├─ 计算机网络
└── compiler_principle/                ├─ 计算机原理
                                       └─ 编译原理
```

各模块详细说明见对应目录下的 README.md。

---

## 📊 学习进度

> 各模块学习状态持续更新。

| 模块 | 状态 | 开始时间 | 备注 |
|------|:----:|---------|------|
| Java 语言基础 | 🔥 进行中 | 2025-05 | 泛型/反射/集合/内部类等 |
| IO / NIO 体系 | 🔥 进行中 | 2025-05 | Part1~Part23，七层递进 |
| 多线程 & 异步 | 🔥 进行中 | 2025-05 | 线程池/ThreadLocal/CompletableFuture |
| 代理模式 & AOP | 🔥 进行中 | 2025-05 | JDK动态代理/CGLIB/Spring AOP |
| 数据结构 | 🔜 待开始 | - | |
| 算法 | 🔜 待开始 | - | |
| 设计模式 | 🔜 待开始 | - | |
| 操作系统 | 🔜 待开始 | - | 穿插学习 |
| 计算机网络 | 🔜 待开始 | - | 穿插学习 |
| 计算机原理 | 🔜 待开始 | - | 穿插学习 |
| 编译原理 | 🔜 待开始 | - | 了解为主，打好概念基础 |

> 状态说明：🔜 待开始 ｜ 🔥 进行中 ｜ ✅ 已完成 ｜ ⏸️ 暂停

---

## 📖 推荐学习顺序

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
> 目标：理解程序运行的底层根因，不是单独学，而是遇到疑问时对照查

| 遇到这个问题时 | 去看这个模块 |
|--------------|------------|
| epoll 为什么比 select 快？NIO Selector 怎么工作？ | `operate_system/`（IO多路复用） |
| mmap 原理是什么？Page Cache 怎么加速 Kafka？ | `operate_system/`（内存管理） |
| TCP 粘包/拆包是什么？为什么 Netty 要做 FrameDecoder？ | `compute_network/`（传输层） |
| volatile 为什么能保证可见性？DCL 为什么要加 volatile？ | `computer_principle/`（内存屏障） |
| DMA 是什么？零拷贝为什么说「2次DMA拷贝，0次CPU拷贝」？ | `computer_principle/`（总线&DMA） |
| javac 怎么把源码变成字节码？JIT 是什么？词法/语法分析怎么工作？ | `compiler_principle/` |

> 📌 **关于编译原理**
> 编译原理是计算机与软件领域最核心的基础理论课之一，涵盖词法分析、语法分析、语义分析、中间代码生成、代码优化、目标代码生成等完整流程。
> 理解编译原理能让你真正看懂 JVM 的字节码编译（javac）、JIT 即时编译、注解处理器（APT）、Lombok 等工具的工作原理，也是理解 LLM 如何「理解」代码的重要基础。
> 本项目已建立该模块目录（`compiler_principle/`），代码内容后续视学习进度补充。

---

## 🚀 运行方式

```bash
# 编译整个项目
mvn compile

# 运行 IO/NIO 全体系演示（推荐从这里开始）
java -cp target/classes org.example.java_base_test.io.nio.NIODemo

# 运行多 Agent 系统演示
java -cp target/classes org.example.java_base_test.multi_agent.MultiAgentDemo
```

---

## 📝 更新记录

| 日期 | 更新内容 |
|------|---------|
| 2025-05 | 初始化项目，建立基础目录结构 |
| 2025-05 | 完成 IO/NIO 全体系（Part1~Part23），含七层递进路线图与生活场景类比 |
| 2025-05 | 新增多 Agent 系统架构演示（`multi_agent/`），Orchestrator-Worker 模式 |
| 2025-05 | 重构 NIODemo 为多 Agent 模式（`io/nio/show_multi_agent/`），23个Part拆分为独立文件 |
| 2025-05 | 建立各模块 README，新增编译原理模块（`compiler_principle/`） |

---

## 🔗 关联项目

| 项目 | 定位 | 内容 |
|------|------|------|
| **本项目**（`java_study`） | 基础理论 | Java 语言核心、数据结构与算法、IO/NIO 体系、操作系统、计算机网络、计算机原理、编译原理 |
| [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) | 工程实践 | Spring 生态、分布式架构、大数据生态、存储中间件、多语言、前端、AI/Agent 工程化 |
| [`ai_coding_harness_engineering_study`](../ai_coding_harness_engineering_study) | AI 工程 | AI 编程、Harness 工程理论、AI Coding 工具链、大模型应用及 Agent 开发知识沉淀 |

---

<div align="center">
  <sub>持续更新中 🚀 · 打好地基，才能盖高楼</sub>
</div>

