# 📚 Java 语言核心 & 计算机基础理论 学习项目

<div align="center">

![Status](https://img.shields.io/badge/状态-持续更新中-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)

</div>

---

## 📌 项目定位

本项目是三个关联学习工程中的**基础理论层**，专注 Java 语言核心机制与计算机基础理论。**不是真实的生产项目**，是学习与实验载体。

目标是把「知其然」变成「知其所以然」——在学 Spring、分布式、大数据之前，先把 Java 语言本身、JVM 原理、IO 体系、并发深度，以及操作系统、计算机网络、计算机原理、编译原理等底层基础打牢。没有这层地基，工程实践中遇到问题只能靠搜索和猜测，而不是真正理解。

**核心学习方向（14 大板块）：**
`Java 语言核心` · `Java 并发深度` · `JVM 原理` · `IO / NIO 体系` · `算法与数据结构` · `设计模式` · `计算机基础理论` · `优秀源码研读` · `信息安全基础` · `软件工程基础` · `数学基础` · `性能工程` · `调试与问题排查` · `前沿技术趋势`

> ⚠️ **关联项目分工说明**
>
> 本人维护三个相互补充、各有侧重的学习仓库，共同构成完整的知识体系：
>
> | 项目 | 定位 | 核心聚焦 |
> |------|:----:|---------|
> | **本项目**（`java_study`） | 🔵 基础理论 | Java 语言核心、并发深度、JVM 原理、IO/NIO 体系、数据结构与算法、设计模式、计算机基础理论、信息安全基础、软件工程基础、性能工程、调试排查——**打地基** |
> | [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) | 🟠 工程实践 | Spring 生态、分布式架构、大数据、存储中间件、风控爬虫、数据分析、多语言、AI/Agent 系统接入、测试工程——**做系统** |
> | [`ai_coding_harness_engineering_study`](../ai_coding_harness_engineering_study) | 🟣 AI 工程 | AI 编程方法论、上下文工程、AI 工具工程化、Harness 理论、AI Coding 工具链、大模型与 Agent 开发——**用 AI 提效** |

> 🔵 **本项目与 `java_fullstack_ai_agent_study` 的边界原则**
>
> | 知识点 | 本项目（原理地基）| `java_fullstack_ai_agent_study`（工程应用）|
> |--------|:---------------:|:----------------------------------------:|
> | JDK 动态代理 / CGLIB 字节码增强**原理** | ✅ | — |
> | Spring AOP 框架使用 / 源码 / 生产场景 | — | ✅ |
> | `java.nio` 原生 API（Buffer / Channel / Selector）| ✅ | — |
> | Netty 框架实战（自定义协议 / 长连接 / 编解码器）| — | ✅ |
> | Kafka 网络层（NIO 印证）/ 存储层（mmap 印证）| ✅ | — |
> | Kafka 业务特性（幂等 / 事务 / 消息积压 / Spring 集成）| — | ✅ |
> | RocketMQ CommitLog（mmap 印证）/ Netty 通信层（印证）| ✅ | — |
> | RocketMQ 事务消息 / 延迟消息（业务特性）| — | ✅ |
> | gRPC HTTP/2 帧结构 / Protobuf 二进制协议**原理** | ✅ | — |
> | gRPC 服务治理（拦截器 / 负载均衡 / 微服务集成）| — | ✅ |

---

## 🗺️ 技术版图

> 💡 **如何使用这张地图**：先看「学习路线总览」确认当前位置和下一步，再进入对应章节看详细要点，最后在「学习进度」表里打卡。

### 📍 学习路线总览

> 优先级说明：🔴 核心必学 ｜ 🟠 重要补充 ｜ 🟡 按需拓展 ｜ 🔵 了解即可

| # | 板块 | 优先级 | 前置依赖 | 学习目标 |
|:-:|------|:------:|---------|---------|
| 1 | 🏗️ **Java 语言核心** | 🔴 核心 | 无 | 吃透类型系统、泛型、反射、异常、资源管理等语言机制，是所有后续学习的语言基础 |
| 2 | ⚡ **Java 并发深度** | 🔴 核心 | Java 语言核心 | 从 `volatile`/JMM 到 AQS/CAS，理解 Java 并发工具的底层实现，而非仅会用 API |
| 3 | ☕ **JVM 原理** | 🔴 核心 | Java 语言核心 | 理解类加载、对象内存布局、GC 算法与调优、JIT 编译，是排查线上问题的根本 |
| 4 | 🔌 **IO / NIO 体系** | 🔴 核心 | Java 语言核心 + 操作系统基础 | 彻底理解 BIO→NIO→零拷贝→Netty 设计原理，是 Java 后端性能优化的核心竞争力 |
| 5 | 🧮 **算法与数据结构** | 🔴 核心 | Java 语言核心 | 掌握核心数据结构与算法，为集合框架源码阅读、系统设计与面试打基础 |
| 6 | 🎨 **设计模式** | 🟠 重要 | Java 语言核心 + 并发 | 掌握 23 种 GoF 模式 + 架构设计原则（SOLID / DRY / KISS），理解其在 JDK 和 Spring 中的真实应用 |
| 7 | 🖥️ **计算机基础理论** | 🟠 重要 | 无，可穿插学习 | 操作系统 / 计算机网络 / 计算机原理 / 编译原理四大基础，是理解 Java 和分布式系统的根因 |
| 8 | 🔬 **优秀源码研读** | 🟡 进阶 | 上述 1~7 板块综合 | 以「印证底层原理」为目标阅读 JDK / Kafka / RocketMQ / RPC 源码，从顶级工程师代码中建立工业级审美 |
| 9 | 🔐 **信息安全基础** | 🟠 重要 | 计算机网络 + 数学基础 | 掌握密码学原理、常见攻防模型、Java 安全 API，理解 HTTPS/JWT/OAuth 背后的数学支撑 |
| 10 | 🏛️ **软件工程基础** | 🟠 重要 | Java 语言核心 | 掌握 SOLID/DRY/KISS 原则、测试理论（TDD/BDD）、代码质量工程化，形成工程素养 |
| 11 | 📐 **数学基础** | 🟡 按需 | 无 | 离散数学（图论/逻辑）、概率统计、线性代数基础，支撑算法、密码学、AI 等方向 |
| 12 | 🚀 **性能工程** | 🟡 进阶 | JVM + IO + 并发 + 算法 | 系统性能分析方法论、基准测试（JMH）、性能剖析工具链，做到「有数据的优化」 |
| 13 | 🐛 **调试与问题排查** | 🟠 重要 | JVM + 并发 + IO | 掌握 JVM 诊断工具链（jstack/jmap/Arthas）、线上故障定位方法论，是生产必备硬技能 |
| 14 | 🔭 **前沿技术趋势** | 🔵 了解 | 上述 1~13 综合 | 追踪 Java 生态最新演进（Loom/Panama/Valhalla）、云原生基础、WebAssembly、Rust 设计思想对 Java 的影响 |

> 📌 **推荐学习主线**：`1 Java 语言核心` → `3 JVM 原理` → `2 Java 并发深度` → `4 IO/NIO 体系` → `5 算法与数据结构` → `6 设计模式 & 软件工程基础` → `7 计算机基础理论（穿插）` → `9 信息安全基础` → `12 性能工程` → `13 调试排查` → `8 源码研读` → `14 前沿趋势`

---

### 🏗️ Java 语言核心

> 这是一切的起点——把 Java 语言机制本身吃透，后面学框架才不会「知其然不知其所以然」。

| 方向 | 学习要点 |
|------|---------|
| 类型系统 | 基本类型 vs 包装类 / 自动装箱拆箱 / 类型转换 / 浮点精度陷阱 / 数值溢出 |
| 泛型 | 泛型类 / 泛型方法 / 类型擦除机制 / 通配符（`? extends` / `? super`）/ 泛型与数组 / 桥接方法 |
| 内部类 | 成员内部类 / 静态内部类 / 匿名内部类 / 局部内部类 / 与外部类的关系 |
| 面向对象深度 | `equals` & `hashCode` 契约 / `Comparable` vs `Comparator` / 多态与方法分派 / 虚方法表 / 动态绑定 / 接口默认方法冲突解决 |
| 反射与元数据 | `Class` 对象 / 反射获取方法与字段 / 注解处理（APT）/ `MethodHandle` / `VarHandle` |
| 动态代理与字节码增强 | JDK 动态代理原理（`InvocationHandler`）/ CGLIB 字节码增强 / ASM / Javassist / ByteBuddy 基础 |
| SPI 机制 | `ServiceLoader` 原理 / META-INF/services 约定 / SPI vs API / SPI 在 JDBC / SLF4J / Dubbo 中的应用 |
| 异常处理 | checked vs unchecked / `try-with-resources` / `AutoCloseable` / 资源管理模式（RAII）/ 异常链 / 自定义异常设计 |
| 函数式编程 | Lambda 表达式 / 方法引用 / `Stream` API（中间操作 / 终止操作 / 并行流）/ `Optional` / 函数式接口 / 组合器模式 |
| 字符串 | `String` 不可变性原理 / `StringPool` / `StringBuilder` vs `StringBuffer` / 正则表达式原理 |
| 序列化与反序列化 | Java 原生序列化（`Serializable` / `transient` / `serialVersionUID`）/ 序列化安全风险 / JSON 序列化原理对比 |
| 集合框架使用 | `List` / `Set` / `Map` / `Queue` / `Deque` 的适用场景与选型决策 / `Collections` 工具类 |
| 模块化与新特性 | Java 模块系统（JPMS）/ `record` 类型 / `sealed` 类 / `pattern matching` / 文本块 / `switch` 表达式增强 |

#### 🔑 重点知识深度清单

| 知识点 | 为什么重要 |
|--------|----------|
| 类型擦除 | 高频面试题，理解后再看集合框架源码事半功倍 |
| `equals` / `hashCode` 契约 | `HashMap` / `HashSet` 正确工作的前提，必须同时重写 |
| Lambda & Stream | 现代 Java 编程风格主力，减少样板代码 |
| `try-with-resources` | 资源安全管理，避免连接泄漏 |
| 反射 | Spring IoC / AOP 的底层机制，理解框架原理的入口 |
| 动态代理 / 字节码增强 | Spring AOP 的实现根基，面试高频考点 |
| SPI 机制 | JDBC Driver / SLF4J / Dubbo 等框架插件化的基础 |
| 序列化 | 分布式通信 / 缓存序列化 / 反序列化漏洞的根本 |

---

### ⚡ Java 并发深度

> 并发是 Java 后端最难啃的领域，也是最有含金量的领域。目标是从「会用 API」升级到「理解底层机制」。

| 方向 | 学习要点 |
|------|---------|
| 线程基础 | `Thread` 生命周期 / `Runnable` vs `Callable` / `join` / `interrupt` 中断机制 / 守护线程 |
| 线程池 | `ThreadPoolExecutor` 七个核心参数 / 四种拒绝策略 / 线程池监控 / 合理配置线程数 / 自定义线程工厂 |
| `ThreadLocal` | 原理与内存泄漏（弱引用 key + 强引用 value）/ 父子线程传递（TTL）/ `InheritableThreadLocal` |
| 异步编排 | `Future` / `CompletableFuture` 全部 API / 异步异常处理 / 链式编排 / 组合多个异步任务 |
| `volatile` 与 JMM | `volatile` 内存语义 / happens-before 规则 / 指令重排 / 内存可见性 |
| Java 内存模型（JMM）| 主内存与工作内存 / 八种原子操作 / 内存屏障（LoadLoad / StoreStore 等）|
| `synchronized` | 对象锁 vs 类锁 / 锁升级过程（偏向锁 → 轻量级锁 → 重量级锁）/ 对象头 Mark Word |
| CAS 与原子类 | Compare-And-Swap 原理 / `Unsafe` 类 / `AtomicInteger` / `AtomicReference` / ABA 问题 / `LongAdder` 分段累加 |
| AQS | `AbstractQueuedSynchronizer` CLH 队列 / 独占锁 vs 共享锁 / `state` 状态机 / `ConditionObject` 条件变量 |
| 并发工具类 | `ReentrantLock` / `ReentrantReadWriteLock` / `StampedLock` / `Semaphore` / `CountDownLatch` / `CyclicBarrier` / `Phaser` / `Exchanger` |
| 并发集合 | `ConcurrentHashMap` / `CopyOnWriteArrayList` / `BlockingQueue` 系列 / `ConcurrentLinkedQueue` / `DelayQueue` |
| `ForkJoinPool` | 工作窃取算法 / `RecursiveTask` / `RecursiveAction` / `CompletableFuture` 的底层线程池 |
| 并发问题经典模型 | 哲学家就餐 / 读者-写者 / 生产者-消费者 / 银行家算法 / 死锁 / 活锁 / 饥饿的区别 |
| 并发设计模式 | 生产者-消费者 / 读写锁模式 / 线程安全单例（DCL + `volatile`）/ 对象池 / Reactor 模式基础 |
| 虚拟线程（JDK 21）| Project Loom 原理 / 虚拟线程 vs 平台线程 / 结构化并发（`StructuredTaskScope`）/ 适用场景 |
| 响应式编程基础 | `Flow` API（JDK 9+）/ Reactive Streams 规范 / 背压（backpressure）原理 / 与传统线程模型对比 |

#### 并发深度学习路线

```
volatile / happens-before（JMM 语言层基础）
    ↓
synchronized 锁升级（对象头 / Mark Word / 偏向锁 → 轻量级 → 重量级）
    ↓
CAS 原子操作 / Unsafe 类 / AtomicXxx 实现
    ↓
AQS 核心原理（CLH 队列 / 独占模式 / 共享模式 / 条件变量 Condition）
    ↓
基于 AQS 的工具类（ReentrantLock / Semaphore / CountDownLatch / CyclicBarrier）
    ↓
并发集合（ConcurrentHashMap 分段机制 / BlockingQueue 实现）
    ↓
ForkJoinPool / 工作窃取算法（CompletableFuture 的底层）
```

> 💡 `volatile` / `synchronized` 的硬件根因（内存屏障 / CPU 流水线）见第七板块「计算机原理」，两者互相呼应。

---

### ☕ JVM 原理

> 你写的每一行 Java 代码最终都运行在 JVM 之上。理解 JVM 是排查 OOM、GC 问题、理解 Spring 框架行为的根本。

| 方向 | 学习要点 |
|------|---------|
| 类加载机制 | 类加载过程（加载→验证→准备→解析→初始化）/ 双亲委派模型 / 打破双亲委派（Tomcat / SPI）/ 自定义 `ClassLoader` |
| 类初始化 | `<clinit>` 方法 / 静态块执行时机 / 六种触发类初始化的情形 |
| 运行时数据区 | 堆（Heap）/ 栈（JVM Stack）/ 方法区 / 元空间 / PC 寄存器 / 本地方法栈 |
| 对象内存布局 | 对象头（Mark Word + 类型指针）/ 实例数据 / 对齐填充 / `synchronized` 锁存储原理 |
| 堆内存分代 | Eden / Survivor / 老年代 / TLAB 本地分配缓冲 / 对象晋升规则 |
| 逃逸分析 | 对象逃逸检测 / 栈上分配 / 标量替换 / 同步消除 |
| GC 基础 | 可达性分析 / GC Roots 枚举 / 四种引用（强/软/弱/虚）/ `finalize` 机制 |
| GC 算法 | 标记-清除 / 标记-整理 / 复制算法 / 分代收集策略 |
| 垃圾收集器 | Serial / Parallel / CMS（并发标记清除）/ G1（Region / 预期停顿）/ ZGC / Shenandoah |
| GC 调优 | GC 日志解读 / 常见 OOM 场景排查 / JVM 参数体系（`-Xmx` / `-XX:+UseG1GC` 等）|
| 字节码与 JIT | `.class` 文件结构 / 常量池 / 字节码指令集 / 解释执行 vs JIT / C1 / C2 分层编译 |
| JIT 优化 | 热点代码探测（计数器）/ 方法内联 / 逃逸分析 / 锁消除 / 标量替换 |
| AOT 与 GraalVM | Ahead-of-Time 编译 / GraalVM Native Image / 冷启动优化 |

#### JVM 问题对照表

| 遇到这个问题时 | 去看 JVM 哪个模块 |
|--------------|----------------|
| 为什么不同 ClassLoader 加载同一个类会 `ClassCastException`？ | 类加载 / 双亲委派 |
| Spring Boot 为什么要自定义 ClassLoader？ | 打破双亲委派 |
| `synchronized` 偏向锁是怎么存在对象里的？ | 对象头 / Mark Word |
| `ThreadLocal` 弱引用 key，为什么 value 还能泄漏？ | 堆 / Reference 引用链 |
| 线上 `java.lang.OutOfMemoryError: Java heap space`？ | GC 调优 / 堆分代 |
| G1 的 `-XX:MaxGCPauseMillis` 是怎么工作的？ | G1 收集器原理 |
| JVM 为什么比解释型语言快很多？ | JIT 编译 / 热点代码优化 |
| `invokedynamic` / Lambda 字节码是什么样的？ | 字节码指令集 |

---

### 🔌 IO / NIO 体系

> Java 后端性能优化的核心竞争力。从 BIO 到 NIO 再到 Netty，每一层都建立在前一层之上。

| 方向 | 学习要点 |
|------|---------|
| IO 模型理论 | 五种 IO 模型（同步阻塞 / 同步非阻塞 / IO 多路复用 / 信号驱动 / 异步 IO）|
| BIO | 单线程阻塞 / 每连接一线程模型 / 线程爆炸问题 |
| NIO 基础 | `Buffer` 三指针状态机（position / limit / capacity）/ `flip` / `compact` |
| NIO Channel | `FileChannel` / `SocketChannel` / `ServerSocketChannel` / `DatagramChannel` |
| NIO Selector | `Selector` + epoll 原理 / `SelectionKey` 四种就绪事件 / 完整 NIO Server 实现 |
| 零拷贝 | 传统拷贝（4次拷贝）/ `transferTo`（sendfile）/ `MappedByteBuffer`（mmap）/ 适用场景与陷阱 |
| NIO 文件 IO | `FileChannel` 本地 IO / `MappedByteBuffer` 内存映射大文件 / `NIO.2`（`Files` / `Path` / `WatchService`）|
| Netty 设计原理 | EventLoop 线程模型 / `ChannelPipeline` 责任链 / `ByteBuf` 设计 / 粘包拆包原理 |
| 协议编解码 | 固定长度帧 / 分隔符帧 / 长度字段帧 / 自定义协议设计 |
| 开源框架 IO 解析 | Kafka NIO Selector 网络层 / RocketMQ mmap 存储层 / 四大框架横向对比 |
| JDK 新特性 | `NIO.2` 进阶（异步 `AsynchronousFileChannel`）/ 虚拟线程（Project Loom / JDK 21）|

#### IO 体系七层递进路线

```
BIO（阻塞模型，理解痛点）
    ↓
NIO 地基：Buffer 三指针状态机 / Channel 类型体系
    ↓
NIO 网络 IO：Selector + epoll / SelectionKey / 完整 NIO Server
    ↓
零拷贝：transferTo 原理 / mmap 原理 / 小文件陷阱 / 实战对比
    ↓
NIO 文件 IO：FileChannel / MappedByteBuffer / NIO.2 Path API
    ↓
Netty 设计原理：EventLoop 线程模型 / Pipeline 责任链 / 粘包拆包（理解「为什么这样设计」）
    ↓
开源框架解析：Kafka IO 体系 / 四大框架横向对比（Netty / Kafka / RocketMQ / Nginx）
    ↓
前沿新特性：NIO.2 进阶 / JDK 21 虚拟线程（Project Loom）
```

> ⚠️ **Netty 边界说明**：本板块的 Netty 学习聚焦「设计原理」——EventLoop 为什么用单线程、Pipeline 为什么是责任链、粘包拆包的根因是什么。Netty 框架实战（自定义协议开发 / 心跳长连接管理 / 生产级编解码器）详见 [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) 网络与通信模块。

---

### 🧮 算法与数据结构

> 掌握核心数据结构与算法，是理解 JDK 集合框架源码、系统设计选型、技术面试的基础。

| 方向 | 学习要点 |
|------|---------|
| 复杂度分析 | 时间复杂度 / 空间复杂度 / Big-O 表示法 / 均摊分析 |
| 线性结构 | 数组 / 链表（单向/双向/循环）/ 栈 / 队列 / 双端队列 |
| 树结构 | 二叉树 / BST / AVL 树 / 红黑树 / B 树 / B+ 树 / 前缀树（Trie）|
| 堆与优先队列 | 最大堆 / 最小堆 / 堆排序 / `PriorityQueue` 实现原理 |
| 哈希 | 哈希函数设计 / 冲突解决（链地址 / 开放寻址）/ 负载因子 / `HashMap` 扩容原理 |
| 图 | 有向图 / 无向图 / BFS / DFS / 最短路径（Dijkstra / Bellman-Ford）/ 拓扑排序 |
| 排序算法 | 冒泡/插入/选择 / 归并排序 / 快速排序 / 堆排序 / 计数/基数/桶排序 / 稳定性分析 |
| 搜索算法 | 二分搜索 / 深度优先 / 广度优先 / A* 算法基础 |
| 动态规划 | DP 基本思路 / 记忆化搜索 / 背包问题 / 最长公共子序列 / 最短路径 |
| 回溯 | 全排列 / 子集 / N 皇后 / 剪枝策略 |
| 贪心 | 区间调度 / Huffman 编码 / 最小生成树（Kruskal / Prim）|
| 字符串算法 | KMP / Rabin-Karp / 正则表达式基础 |

#### JDK 集合框架对照

| 数据结构 | JDK 实现 | 底层原理 |
|---------|---------|---------|
| 动态数组 | `ArrayList` | 对象数组 + 1.5 倍扩容 |
| 双向链表 | `LinkedList` | Node 双向指针 |
| 红黑树 + 链表 | `HashMap` / `LinkedHashMap` | 数组 + 链表 + 红黑树，负载因子 0.75 |
| 红黑树 | `TreeMap` | 有序 Map，`Comparable` / `Comparator` |
| 数组 + 红黑树 | `ConcurrentHashMap` | JDK 8 之后 CAS + synchronized 细粒度锁 |
| 最大堆 | `PriorityQueue` | 完全二叉树的数组表示 |
| 双端队列 | `ArrayDeque` | 循环数组，无锁 |

---

### 🎨 设计模式

> 设计模式不是要死记名字，而是理解「它解决了什么问题」。结合 JDK 和 Spring 的真实应用场景对照理解收益最大。

| 类别 | 模式 | JDK / Spring 中的真实应用 |
|------|------|------------------------|
| **创建型** | 单例模式 | Spring Bean（`@Scope("singleton")`）/ `Runtime.getRuntime()` |
| | 工厂方法 | `Calendar.getInstance()` / Spring `FactoryBean` |
| | 抽象工厂 | `DocumentBuilderFactory` / `ConnectionFactory` |
| | 建造者 | `StringBuilder` / Lombok `@Builder` / `HttpClient.newBuilder()` |
| | 原型 | `Object.clone()` / Spring Prototype Scope |
| **结构型** | 代理模式 | JDK 动态代理 / CGLIB / Spring AOP |
| | 装饰器 | `BufferedInputStream` 包装 `FileInputStream` / Spring `HandlerInterceptor` |
| | 适配器 | `Arrays.asList()` / Spring `HandlerAdapter` |
| | 外观 | SLF4J 日志门面 / Spring `JdbcTemplate` |
| | 组合 | 文件系统（文件 + 目录）/ Swing 组件树 |
| | 桥接 | JDBC Driver / 跨平台 UI 框架 |
| | 享元 | `Integer.valueOf()` 缓存（-128~127）/ `String.intern()` |
| **行为型** | 责任链 | Netty `ChannelPipeline` / Servlet Filter / Spring Security 过滤链 |
| | 观察者 | Spring 事件机制（`ApplicationEvent`）/ Java `EventListener` |
| | 策略 | `Comparator` / Spring `HandlerMapping` 策略 |
| | 命令 | `Runnable` / `Callable` / 撤销操作实现 |
| | 模板方法 | `InputStream.read()` / Spring `JdbcTemplate` / `AbstractApplicationContext.refresh()` |
| | 迭代器 | `Iterator` 接口 / `for-each` 语法糖 |
| | 状态 | TCP 连接状态机 / 订单状态流转 |
| | 访问者 | `FileVisitor` / AST 节点处理 |
| | 解释器 | 正则表达式引擎 / SpEL 表达式 |
| | 中介者 | Spring `ApplicationContext` 作为中介 |
| | 备忘录 | 撤销/重做功能 / 游戏存档 |

#### 🏛️ 面向对象设计原则

| 原则 | 全称 | 核心思想 | 典型应用 |
|------|------|---------|---------|
| **S** - 单一职责 | Single Responsibility | 一个类只有一个引起变化的原因 | `InputStream` 只负责读取字节 |
| **O** - 开放封闭 | Open/Closed | 对扩展开放，对修改封闭 | 策略模式 / Spring `BeanPostProcessor` |
| **L** - 里氏替换 | Liskov Substitution | 子类必须能替换父类而不破坏程序 | 集合框架 `List` / `AbstractList` 继承体系 |
| **I** - 接口隔离 | Interface Segregation | 客户端不应依赖它不需要的接口 | `Readable` / `Writable` / `Closeable` 分离 |
| **D** - 依赖倒置 | Dependency Inversion | 依赖抽象，不依赖具体 | Spring IoC / JDBC `Driver` 接口 |
| **DRY** | Don't Repeat Yourself | 消除重复，提高可维护性 | 模板方法 / 抽象类抽取公共逻辑 |
| **KISS** | Keep It Simple, Stupid | 保持简洁，避免过度设计 | — |
| **YAGNI** | You Aren't Gonna Need It | 不要超前设计不需要的功能 | — |
| **迪米特法则** | Law of Demeter | 最少知识原则，减少耦合 | 外观模式 / 中介者模式 |
| **组合优于继承** | Composition over Inheritance | 用组合代替深层继承，更灵活 | 装饰器模式 / 策略模式 |

#### 🏗️ 架构模式基础

| 模式 | 核心思想 | Java 相关实践 |
|------|---------|-------------|
| 分层架构 | Controller → Service → Repository 层次分明，单向依赖 | Spring MVC 经典三层 |
| 六边形架构（端口与适配器）| 业务核心与外部系统解耦，依赖倒置 | Spring + 接口适配器 |
| 事件驱动架构基础 | 生产者-消费者解耦，异步化 | Spring `ApplicationEvent` / 消息队列基础 |
| CQRS 基础 | 命令（写）与查询（读）分离 | 与 DDD 结合理解 |
| 管道-过滤器 | 数据流经一系列处理单元 | `Stream` API / Netty Pipeline / Servlet Filter |

---

### 🖥️ 计算机基础理论（穿插学习）

> 不是单独学，而是遇到疑问时对照查。先从 Java 语言层建立感性认知，遇到「为什么」时来这里找根因。

#### 操作系统

| 方向 | 学习要点 |
|------|---------|
| 进程与线程 | 进程 vs 线程 / 上下文切换成本 / 协程 / 用户态 vs 内核态 / 系统调用（syscall）|
| 内存管理 | 虚拟内存 / 分页机制（TLB）/ 段页式 / Page Cache / mmap 原理 / 内存映射文件 / OOM Killer |
| IO 多路复用 | `select` / `poll` / `epoll`（LT / ET 两种触发模式）/ `io_uring`（最新异步 IO）/ 与 NIO Selector 的关系 |
| 进程间通信 | 管道（匿名/命名）/ 信号 / 消息队列 / 共享内存 / Socket / 信号量 |
| 文件系统 | VFS 抽象层 / inode / 日志文件系统 / 顺序读写 vs 随机读写性能差异 / 文件描述符限制 |
| 进程调度 | CPU 调度算法（FIFO / Round-Robin / CFS）/ 优先级与时间片 / 上下文切换 |
| 死锁 | 死锁四条件 / 死锁检测与预防 / Java 死锁排查（`jstack`）|
| 同步原语 | 互斥锁 / 自旋锁 / 条件变量 / 信号量 / 读写锁 的操作系统实现 |

#### 计算机网络

| 方向 | 学习要点 |
|------|---------|
| 协议分层 | OSI 七层 / TCP/IP 四层 / 各层协议作用 / 数据封装与解封 |
| TCP | 三次握手 / 四次挥手 / 滑动窗口 / 流量控制 / 拥塞控制（慢启动 / 拥塞避免 / 快重传 / CUBIC）/ TIME_WAIT / Keep-Alive |
| UDP | UDP 特性 / UDP 可靠传输的工程实现（QUIC）|
| HTTP | HTTP/1.0 vs HTTP/1.1（持久连接 / 管线化）/ HTTP/2（多路复用 / 头部压缩 HPACK / 服务器推送）/ HTTP/3（QUIC / 0-RTT）|
| HTTPS / TLS | 对称加密 / 非对称加密 / 数字证书 / TLS 1.2 vs TLS 1.3 握手过程 / CA 链验证 / 证书透明（CT）|
| WebSocket | 握手协议 / 帧格式 / 与 HTTP 长轮询的对比 |
| DNS | 递归查询 vs 迭代查询 / TTL / DNS 劫持 / DNSSEC / DoH（DNS over HTTPS）|
| 网络安全基础 | ARP 欺骗 / DNS 劫持 / IP 欺骗 / 中间人攻击（MITM）|
| 网络工具 | `tcpdump` / `Wireshark` / `netstat` / `ss` / `ping` / `traceroute` 抓包与分析 |

#### 计算机原理

| 方向 | 学习要点 |
|------|---------|
| CPU 与内存 | CPU 多级缓存（L1/L2/L3）/ 缓存行（Cache Line）/ 伪共享（False Sharing）/ NUMA 架构 |
| CPU 流水线 | 流水线冒险（数据/控制/结构冒险）/ 分支预测 / 超标量 |
| 内存屏障 | StoreStore / LoadLoad / StoreLoad / LoadStore 四类屏障 / `volatile` 的硬件实现 / MESI 协议 |
| 指令重排 | CPU 乱序执行 / 编译器重排 / `happens-before` 的硬件根因 |
| DMA 与零拷贝 | DMA 控制器原理 / `sendfile` 系统调用 / 「2 次 DMA 拷贝，0 次 CPU 拷贝」 |
| 总线与 IO | PCIe 总线 / 中断机制 / 轮询 vs 中断 / MMIO |
| 数值表示 | 整数补码 / 浮点数 IEEE 754 / 精度损失 / 大端 vs 小端 |

#### 编译原理

| 方向 | 学习要点 |
|------|---------|
| 编译流程 | 词法分析（Lexer）/ 语法分析（Parser）/ 语义分析 / AST（抽象语法树）/ 符号表 |
| 中间代码与优化 | IR 生成 / SSA 形式 / 常量折叠 / 死代码消除 / 循环优化 |
| 运行时 | 活动记录（栈帧）/ 动态链接 / 尾调用优化 |
| Java 相关 | `javac` 编译过程 / 字节码生成 / APT 注解处理器 / Lombok 原理 / JSR-269 插件化 |
| JIT 与 AOT | JIT 即时编译（与 JVM 板块呼应）/ GraalVM AOT / `invokedynamic` 指令 |
| 正则表达式引擎 | NFA vs DFA / 回溯问题 / 灾难性回溯（ReDoS）|

> 📌 **穿插学习指南**
>
> | 遇到这个问题时 | 去看这个模块 |
> |--------------|------------|
> | epoll 为什么比 select 快？NIO Selector 怎么工作？ | 操作系统（IO 多路复用）|
> | mmap 原理是什么？Page Cache 怎么加速 Kafka？ | 操作系统（内存管理）|
> | TCP 粘包 / 拆包是什么？为什么 Netty 要做 FrameDecoder？ | 计算机网络（TCP）|
> | HTTPS TLS 握手是怎么建立的？证书链如何验证？ | 计算机网络（HTTPS/TLS）|
> | `volatile` 为什么能保证可见性？DCL 为什么要加 `volatile`？ | 计算机原理（内存屏障）|
> | CPU 流水线是什么？为什么指令会被重排？ | 计算机原理（指令重排）|
> | DMA 是什么？零拷贝为什么说「2 次 DMA，0 次 CPU 拷贝」？ | 计算机原理（DMA）|
> | `javac` 怎么把源码变成字节码？JIT 是什么？ | 编译原理（与 JVM 板块呼应）|

---

### 🔬 优秀源码研读（进阶）

> **阅读原则**：每个方向都有明确的「印证目标」——印证本项目前面学到的哪个底层原理。与框架使用相关的业务特性，不在本项目深入。

#### 🔑 JDK 核心源码

| 方向 | 重点类 / 包 | 印证目标 |
|------|----------|--------|
| 集合框架 | `HashMap` / `ConcurrentHashMap` / `ArrayList` / `TreeMap` / `PriorityQueue` | 数据结构理论 → JDK 实现（红黑树 / CAS 分段锁 / 扩容）|
| 并发工具 | `AQS` / `ReentrantLock` / `Semaphore` / `CountDownLatch` / `ThreadPoolExecutor` | 并发深度（AQS / CAS / CLH 队列）→ 真实实现 |
| IO 体系 | `InputStream` 装饰器链 / `FileChannel` / `Selector` 实现 | IO/NIO 体系 → 装饰器模式与 NIO 底层 |
| JVM 相关 | `String` 不可变性 / `Integer` 缓存 / `Reference` 引用链（`WeakReference` / `SoftReference`）| JVM（GC Roots / 四种引用）→ 真实应用 |
| 函数式 | `Stream` 实现 / `CompletableFuture` / `ForkJoinPool` | 函数式编程 + 并发 → 内部实现 |

**入口建议**：先读 `HashMap` → `ConcurrentHashMap` → `AQS`，这条线把集合 + 并发的核心设计串起来。

#### 📨 Kafka 源码（聚焦 IO 与存储原理印证）

| 方向 | 关键模块 | 印证目标 |
|------|---------|--------|
| 存储层 | `Log` / `LogSegment` / `OffsetIndex` | 印证：mmap / 顺序写 / 稀疏索引设计 |
| 网络层 | `Selector`（基于 NIO）/ `KafkaChannel` / `Processor` | 印证：NIO Selector 与 epoll 原理 |

> ⚠️ Kafka 的副本机制 / 消费者协调 / 幂等与事务消息 / 消息积压，属于消息队列**业务特性**，详见 [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) 消息队列模块。

**入口建议**：先看存储层（`Log` + `LogSegment`），与 NIO `MappedByteBuffer` 章节对照阅读收益最大。

#### 📦 RocketMQ 源码（聚焦 mmap 与 Netty 网络层印证）

| 方向 | 关键模块 | 印证目标 |
|------|---------|--------|
| 消息存储 | `CommitLog`（mmap 写）/ `ConsumeQueue` / `IndexFile` | 印证：mmap 在 TB 级存储中的实际应用 |
| 网络通信 | Netty 作为通信层 / `RemotingCommand` 协议 | 印证：Netty Pipeline / 协议编解码原理 |

> ⚠️ RocketMQ 的事务消息 / 延迟消息（时间轮）属于消息队列**业务特性**，详见 [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) 消息队列模块。

**入口建议**：先看 `CommitLog`，与 NIO `MappedByteBuffer` 章节对照，体会 mmap 在工业级系统的实战应用。

#### 🔌 RPC 框架源码（Dubbo / gRPC）

| 方向 | 关键模块 | 印证目标 |
|------|---------|--------|
| Dubbo SPI 机制 | `ServiceConfig` / `Invoker` / `Protocol` SPI | 理解：微内核 + SPI 扩展设计模式 |
| Dubbo 网络层 | `NettyServer` / `NettyChannel` / `DubboCodec` | 印证：Netty 协议编解码设计原理 |
| gRPC 协议原理 | `Channel` / `ManagedChannel` / Protobuf 序列化 | 印证：HTTP/2 帧结构 / 二进制协议原理 |

> ⚠️ gRPC 的服务治理（拦截器 / 负载均衡 / 微服务集成）属于框架**工程应用**，详见 [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) 网络与通信模块。

#### 💡 源码阅读方法论

> 1. **带问题读**：先提一个问题（如「HashMap 扩容为什么是 2 倍？」），再去源码里找答案
> 2. **对照 DEBUG**：写 Demo 断点进入源码，比直接看源码效率高 3 倍
> 3. **先主干后细节**：第一遍只看核心流程，忽略异常处理和边界条件
> 4. **画时序图**：复杂模块边读边画，画完就记住了

---

### 🔐 信息安全基础

> 密码学是 HTTPS、JWT、OAuth、数字签名等一切「安全基础设施」的底层。理解这些原理，才能真正理解安全协议，而不只是会配置。

#### 密码学基础

| 方向 | 学习要点 |
|------|---------|
| 对称加密 | AES（ECB / CBC / GCM 模式）/ DES / 流加密 vs 块加密 / 初始向量（IV）/ 填充方案（PKCS7）|
| 非对称加密 | RSA 原理（大整数分解）/ 椭圆曲线密码学（ECC / ECDH / ECDSA）/ 密钥交换（DH）|
| 哈希函数 | MD5 / SHA-1 / SHA-256 / SHA-3 / 哈希碰撞 / 抗碰撞性 / 生日悖论 |
| 消息认证码 | HMAC 原理 / 数字签名（Sign / Verify）/ MAC vs 签名的区别 |
| 公钥基础设施 | 数字证书（X.509）/ CA 信任链 / CSR / 证书吊销（CRL / OCSP）|
| 随机数安全 | 伪随机数（PRNG）vs 密码学安全随机数（CSPRNG）/ `SecureRandom` |
| 密钥派生 | PBKDF2 / bcrypt / Argon2 / 密码安全存储 |

#### 常见攻击与防御

| 攻击类型 | 原理 | Java 防御手段 |
|---------|------|-------------|
| SQL 注入 | 拼接 SQL 执行恶意语句 | PreparedStatement / ORM 框架 |
| XSS 跨站脚本 | 注入恶意 JS 到页面 | HTML 转义 / CSP 响应头 |
| CSRF 跨站请求伪造 | 利用 Cookie 自动携带冒充用户 | CSRF Token / SameSite Cookie |
| 反序列化漏洞 | 反序列化恶意字节流执行代码 | 白名单过滤 / 避免原生序列化 |
| 路径遍历 | `../` 访问非授权文件 | 路径规范化 / 白名单校验 |
| 计时攻击 | 利用响应时间差推断密钥 | `MessageDigest.isEqual()`（常量时间比较）|
| 中间人攻击 | 劫持通信偷听/篡改 | TLS 证书钉扎（Certificate Pinning）|

#### Java 安全 API

| 方向 | 学习要点 |
|------|---------|
| `java.security` | `MessageDigest` / `Cipher` / `KeyPairGenerator` / `Signature` |
| `javax.crypto` | `SecretKey` / `KeyAgreement` / `Mac` |
| TLS / SSL | `SSLContext` / `TrustManager` / `KeyManager` / JSSE 原理 |
| JWT 原理 | Header.Payload.Signature 结构 / 签名验证流程 / `alg:none` 漏洞 |
| OAuth 2.0 原理 | 四种授权模式 / Access Token / Refresh Token / PKCE |
| Java 安全管理器 | `SecurityManager`（历史背景）/ Java 9+ 模块化安全 |

> 💡 **与「计算机网络」的呼应**：TLS 握手过程 = 非对称加密（密钥交换）+ 对称加密（数据传输）+ HMAC（完整性验证）+ 数字证书（身份认证），四种密码学工具的组合应用。

---

### 🏛️ 软件工程基础

> 代码写得好不仅靠 JVM 原理，更靠工程素养。这个板块聚焦「可维护、可测试、可扩展」的工程化能力。

#### 测试理论与实践

| 方向 | 学习要点 |
|------|---------|
| 单元测试 | 测试金字塔 / JUnit 5（`@Test` / `@ParameterizedTest` / `@BeforeEach`）/ 测试命名规范 |
| Mock 技术 | Mockito（`mock` / `spy` / `verify`）/ 打桩（`when...thenReturn`）/ 测试替身类型 |
| 集成测试 | Spring Boot Test / Testcontainers / 数据库集成测试 |
| 测试驱动开发（TDD）| Red-Green-Refactor 循环 / 先写测试再实现 / 测试即文档 |
| 行为驱动开发（BDD）| Given-When-Then 格式 / Cucumber / 可读性优先 |
| 测试覆盖率 | 行覆盖 / 分支覆盖 / JaCoCo / 覆盖率陷阱（覆盖率高 ≠ 测试好）|
| 契约测试 | Pact 框架 / 服务间接口契约验证 |

#### 代码质量工程

| 方向 | 学习要点 |
|------|---------|
| 代码异味 | 过长方法 / 过大类 / 重复代码 / 过深嵌套 / 魔法数字 / 注释即代码异味 |
| 重构手法 | 提取方法 / 提取类 / 移动方法 / 以多态替换条件 / 引入参数对象 |
| 静态分析工具 | Checkstyle / PMD / SpotBugs / SonarQube / ArchUnit（架构测试）|
| 代码审查 | CR 原则 / 有效反馈 / 自动化 lint 前置 |
| 技术债务 | 技术债务分类 / 债务管理 / 重构时机决策 |

#### 版本控制与工程协作

| 方向 | 学习要点 |
|------|---------|
| Git 深度 | 三棵树（工作区 / 暂存区 / 提交历史）/ `rebase` vs `merge` / `cherry-pick` / `bisect` |
| 分支策略 | Git Flow / GitHub Flow / Trunk Based Development / 特性开关（Feature Flags）|
| CI/CD 基础 | 持续集成概念 / 构建流水线 / 测试自动化 / 制品管理 / 蓝绿部署概念 |
| 语义化版本 | SemVer 规范（MAJOR.MINOR.PATCH）/ 版本兼容性约定 |

#### 文档与知识管理

| 方向 | 学习要点 |
|------|---------|
| 代码文档 | Javadoc 规范 / `@param` / `@return` / `@throws` / API 文档生成 |
| 架构决策记录 | ADR（Architecture Decision Record）/ 记录「为什么」而不只是「是什么」|
| README 工程化 | 清晰的项目定位 / 快速上手 / 示例代码 / 贡献指南 |

---

### 📐 数学基础

> 不需要研究生数学，但需要支撑算法、密码学、AI 理解的「工程数学」基础。

#### 离散数学

| 方向 | 学习要点 |
|------|---------|
| 集合论 | 集合运算（并/交/差/补）/ 幂集 / 笛卡尔积 / 关系与函数 |
| 逻辑 | 命题逻辑 / 谓词逻辑 / 推理规则 / 真值表 |
| 图论 | 有向图 / 无向图 / 连通性 / 树与森林 / 二部图 / 图的矩阵表示 |
| 组合数学 | 排列与组合 / 容斥原理 / 鸽巢原理 / 生成函数基础 |
| 数论基础 | 整除 / 素数 / 最大公因数（辗转相除）/ 模运算 / 费马小定理（密码学基础）|
| 布尔代数 | 逻辑门 / 布尔运算 / 卡诺图 / 位运算与布尔代数的关系 |

#### 概率与统计

| 方向 | 学习要点 |
|------|---------|
| 概率基础 | 概率公理 / 条件概率 / 贝叶斯定理 / 独立事件 |
| 随机变量 | 离散 / 连续随机变量 / 期望 / 方差 / 常见分布（均匀/正态/泊松/指数）|
| 统计推断 | 置信区间 / 假设检验基础 / p 值 |
| 工程应用 | 布隆过滤器（概率数据结构）/ 一致性哈希（均匀分布）/ 随机化算法（跳表 / 随机 QuickSort）|

#### 线性代数基础

| 方向 | 学习要点 |
|------|---------|
| 向量与矩阵 | 向量运算 / 矩阵乘法 / 转置 / 行列式 |
| 线性变换 | 特征值与特征向量 / SVD 分解基础 |
| 工程应用 | AI/ML 特征向量理解 / 图像处理矩阵变换 / PageRank 算法 |

---

### 🚀 性能工程

> 性能优化不是靠感觉和经验，而是靠**测量 → 分析 → 优化 → 验证**的科学方法论。

#### 性能分析方法论

| 方向 | 学习要点 |
|------|---------|
| 性能分析模型 | USE 方法（利用率/饱和度/错误率）/ RED 方法（速率/错误/延迟）/ Amdahl 定律 |
| 延迟 vs 吞吐量 | 两者的权衡关系 / 利特尔法则（Little's Law）/ 排队论基础 |
| 火焰图 | 调用栈采样原理 / On-CPU vs Off-CPU 火焰图 / async-profiler |
| 性能剖析工具 | `perf`（Linux）/ VisualVM / JFR（Java Flight Recorder）/ async-profiler |

#### JVM 性能调优

| 方向 | 学习要点 |
|------|---------|
| JVM 参数体系 | 堆大小（`-Xmx/-Xms`）/ GC 选择（`-XX:+UseG1GC`）/ GC 日志（`-Xlog:gc*`）|
| GC 调优 | GC 停顿分析 / 对象分配优化 / 晋升速率 / TLAB 调优 |
| JIT 编译优化 | 方法内联阈值 / 分层编译控制 / 编译日志解读 |
| 内存泄漏 | Heap Dump 分析 / MAT（Memory Analyzer Tool）/ 常见泄漏模式 |

#### 基准测试

| 方向 | 学习要点 |
|------|---------|
| JMH | JMH 框架（Java Microbenchmark Harness）/ `@Benchmark` / `@Warmup` / `@Fork` |
| 基准测试陷阱 | JIT 热点编译影响 / 死代码消除 / 内存分配干扰 / 结果解读 |
| 压测工具 | Gatling / JMeter / wrk / ab / 负载模型设计 |

#### 系统性能优化

| 方向 | 学习要点 |
|------|---------|
| 算法层 | 数据结构选型 / 算法复杂度优化 / 缓存友好代码 |
| JVM 层 | 对象池 / 零拷贝 / 减少装箱 / StringPool 使用 |
| IO 层 | 批量读写 / 异步非阻塞 / 连接池 / 缓冲区调优 |
| 并发层 | 减少锁粒度 / 无锁数据结构 / 线程亲和性 / 避免伪共享 |

---

### 🐛 调试与问题排查

> 这是区分「Junior」和「Senior」的分水岭。不光要能写代码，更要能快速定位生产问题。

#### JVM 诊断工具链

| 工具 | 用途 | 核心用法 |
|------|------|---------|
| `jps` | 查看 Java 进程 | `jps -lv` 列出进程号和参数 |
| `jstack` | 线程栈快照 | 死锁检测 / 线程阻塞分析 |
| `jmap` | 堆内存信息 | `jmap -heap` / `jmap -histo` / 生成 Heap Dump |
| `jstat` | JVM 统计信息 | GC 频率 / 堆使用率实时监控 |
| `jcmd` | 多功能诊断命令 | 触发 GC / 生成 JFR 飞行记录 / 类直方图 |
| `jconsole` / `VisualVM` | 可视化监控 | 实时 Heap / CPU / 线程监控 |
| **Arthas** | 线上诊断神器 | 动态跟踪方法调用 / 类加载信息 / 字节码反编译 / 热更新 |
| **JFR + JMC** | 低开销生产剖析 | Java Flight Recorder 飞行记录 + Java Mission Control 分析 |
| async-profiler | 性能剖析 / 火焰图 | CPU / 内存分配 / 锁竞争火焰图 |

#### 常见问题排查手册

| 问题现象 | 排查方向 | 核心工具 |
|---------|---------|---------|
| CPU 突刺 100% | `top` → 找高 CPU 线程 → `jstack` 确认 | `top -Hp PID` + `jstack` |
| 内存持续增长 / OOM | GC 日志 → Heap Dump → MAT 分析 | `jmap` + MAT |
| 线程死锁 | `jstack` 中找 `Found deadlock` | `jstack` / Arthas |
| 响应变慢（无明显错误）| 火焰图 → 找热点方法 → SQL 慢查询 | async-profiler + Arthas trace |
| Full GC 频繁 | GC 日志 → 老年代 → 大对象 / 内存泄漏 | GC log + `jmap -histo` |
| ClassNotFoundException | ClassLoader 层级 / 依赖冲突 | Arthas `classloader` / `jad` |
| 端口占用 / 连接泄漏 | `netstat -anp` / `ss -tp` | 系统工具 + 代码审查 |

#### Linux 生产环境排查

| 方向 | 学习要点 |
|------|---------|
| 系统监控 | `top` / `htop` / `vmstat` / `iostat` / `sar` |
| 网络诊断 | `tcpdump` / `netstat` / `ss` / `lsof` |
| 日志分析 | `grep` / `awk` / `sed` / `tail -f` / ELK 体系基础 |
| 系统调用跟踪 | `strace` / `ltrace` / `perf trace` |
| 内核参数调优 | 文件描述符限制（`ulimit`）/ TCP 内核参数（`tcp_backlog` / `somaxconn`）|

---

### 🔭 前沿技术趋势

> 了解即可，不需要深入，但要建立「这些方向是什么、和 Java 生态的关系是什么」的认知地图。

#### 📋 JDK 版本特性速查

> Java 采用半年一次的节奏发版（JDK 9 起），每 3 年发布一个 LTS（长期支持版本）。生产环境主流：**JDK 8**（历史遗留）→ **JDK 11**（第一个广泛替换 8 的 LTS）→ **JDK 17**（当前主流 LTS）→ **JDK 21**（最新 LTS，强烈推荐升级）。

##### JDK 8（LTS，2014，经典奠基版）

| 特性 | 要点 |
|------|------|
| **Lambda 表达式** | 函数式编程语法糖 / `(params) -> body` / 类型推断 |
| **Stream API** | 链式操作（`map` / `filter` / `reduce` / `collect`）/ 惰性求值 / 并行流 |
| **函数式接口** | `Function` / `Consumer` / `Supplier` / `Predicate` / `@FunctionalInterface` |
| **Optional** | 避免 NPE / `map` / `flatMap` / `orElse` / `orElseThrow` |
| **方法引用** | `类::方法` / `对象::方法` / `类::new` 四种形式 |
| **默认方法 & 静态方法** | 接口可有实现 / 多重继承冲突解决规则 |
| **新日期 API（java.time）** | `LocalDate` / `LocalDateTime` / `ZonedDateTime` / `Duration` / `Period` / 线程安全、不可变设计 |
| **Nashorn JS 引擎** | 内嵌 JS 脚本执行（JDK 15 已废弃）|
| **元空间替换永久代** | `PermGen` → `Metaspace`，类元数据移出堆，由本地内存管理 |
| **并发增强** | `CompletableFuture` / `StampedLock` / `LongAdder` |

##### JDK 11（LTS，2018，首个收费拆分版本）

| 特性 | 要点 |
|------|------|
| **HTTP Client（标准化）** | 替代 `HttpURLConnection` / 支持 HTTP/2 / 异步非阻塞 / `WebSocket` |
| **`var` 类型推断（局部变量）** | 已从 JDK 10 引入 / Lambda 参数中可用 `var` / 增强可读性 |
| **字符串新方法** | `isBlank()` / `strip()` / `lines()` / `repeat(n)` / `stripLeading()` / `stripTrailing()` |
| **`Files` 新方法** | `Files.readString()` / `Files.writeString()` / `Files.isSameFile()` |
| **Epsilon GC** | 无操作 GC（用于测试/性能基准，不回收内存）|
| **ZGC（实验）** | 低延迟 GC，STW 不超过 10ms（JDK 15 生产可用）|
| **废弃 Nashorn JS 引擎** | 建议迁移 GraalVM |
| **Oracle JDK 商业授权变化** | OpenJDK 与 Oracle JDK 二进制等同；Oracle JDK 商业使用需付费 |

##### JDK 17（LTS，2021，当前最主流生产版本）

| 特性 | 要点 |
|------|------|
| **密封类（sealed class）** | `sealed` / `permits`，限制子类范围 / 与 `pattern matching` 配合 |
| **`record` 类型** | 不可变数据载体 / 自动生成 `equals` / `hashCode` / `toString` / 访问器 |
| **文本块（Text Block）** | 多行字符串 `"""..."""` / 自动对齐 / 无需转义 |
| **`switch` 表达式增强** | 箭头语法 `case X -> expr` / `yield` 返回值 / 无 `fallthrough` |
| **模式匹配 instanceof** | `if (obj instanceof String s)` 自动转型绑定 |
| **`instanceof` 增强** | 消除繁琐的强制转型 |
| **强封装 JDK 内部 API** | `--add-opens` 必须显式声明；废弃 `sun.misc.Unsafe` 部分接口 |
| **ZGC / Shenandoah 生产可用** | G1 仍是默认 GC |
| **弃用 Applet API** | Applet 彻底退出历史舞台 |
| **Spring Boot 3.x / Spring 6 最低要求** | 框架生态纷纷要求 JDK 17+ |

##### JDK 21（LTS，2023，当前最新 LTS，强烈推荐）

| 特性 | 要点 |
|------|------|
| **虚拟线程（Virtual Threads，Project Loom ✅）** | 轻量级线程 / 数百万并发 / `Thread.ofVirtual()` / 同步阻塞 API 不阻塞平台线程 / 适合 IO 密集型 |
| **结构化并发（StructuredTaskScope）** | 子任务生命周期与父作用域绑定 / 避免泄漏 / 简化错误处理 |
| **序列化集合（Sequenced Collections）** | `SequencedCollection` / `SequencedMap` 接口 / 统一 `first` / `last` 语义 |
| **`switch` 模式匹配（标准化）** | 可 `switch` 任意对象 / 与 sealed class 完美配合 / 解构绑定 |
| **`record` 模式（标准化）** | `instanceof Point(var x, var y)` 解构 |
| **字符串模板（预览）** | `STR."Hello \{name}"` 插值语法（JDK 23 重设计中）|
| **未命名模式 `_`（预览）** | 忽略不需要的变量绑定 |
| **分代 ZGC（默认启用）** | ZGC 分代支持 / 显著降低内存占用和吞吐量损失 |
| **`HashMap.newHashMap(capacity)`** | 直接指定预期容量，内部自动计算避免 rehash |
| **Key Encapsulation Mechanism（KEM）** | 后量子密码学 API 基础 |

##### JDK 22 / 23 / 24（特性版本，非 LTS）

| 特性 | 版本 | 要点 |
|------|:----:|------|
| **未命名变量 `_`（标准化）** | 22 | 忽略不用的绑定变量 |
| **字符串模板（预览重设计）** | 22–23 | 更灵活的插值方式（持续演进）|
| **`Stream.gather()` API** | 22 | 自定义中间流操作，超越现有 `map`/`filter` 表达能力 |
| **前置声明（Ahead-of-Time Class Loading）** | 22–24 | Project Leyden 初步落地，减少 JVM 启动热身时间 |
| **外部函数与内存 API（标准化，Project Panama ✅）** | 22 | `MemorySegment` / `MethodHandle` 替代 `Unsafe` / 安全访问本地内存 |
| **向量 API（持续孵化）** | 22–24 | SIMD 指令级并行 / 数值计算加速 |
| **模块导入声明** | 23 | `import module java.base;` 批量导入 |
| **`Scoped Values`（标准化）** | 24 | 替代 `ThreadLocal` / 适配虚拟线程的上下文传播 |

##### 版本选型建议

| 场景 | 推荐版本 | 理由 |
|------|:------:|------|
| 新项目（2024+）| **JDK 21** | 最新 LTS / 虚拟线程 / 全特性 / 主流框架已全面支持 |
| 存量项目维护 | **JDK 17** | 当前最广泛 LTS / 密封类 / record / 文本块 |
| 遗留系统 | **JDK 11** | 第一个 LTS 替代品，脱离 JDK 8 的第一步 |
| 面试备考 | **JDK 17 + 21** | 高频考点：record/密封类（17）、虚拟线程/模式匹配（21）|

---

#### Java 平台演进（Project 计划）

| Project | 目标 | 当前状态 |
|---------|------|---------|
| **Loom** | 虚拟线程 / 结构化并发 | ✅ JDK 21 正式发布 |
| **Panama** | Java 与本地代码（C/C++/Rust）互操作 | 🔥 JDK 22+ 进行中 |
| **Valhalla** | 值类型（Value Types）/ 原始类泛型，消除泛型装箱开销 | 🔥 进行中 |
| **Amber** | 语言特性增强（record / pattern matching / sealed）| ✅ 持续交付 |
| **Leyden** | 启动时间优化 / 减少 JVM 热身时间 | 🔜 早期阶段 |

#### 云原生基础

| 方向 | 学习要点 |
|------|---------|
| 容器基础 | Docker 原理（namespace / cgroup）/ 容器 vs 虚拟机 / 镜像层 |
| Kubernetes 基础 | Pod / Service / Deployment 核心概念 / K8s 调度原理 |
| 服务网格基础 | Sidecar 模式 / Istio 基础概念 / 可观测性（Metrics / Traces / Logs）|
| GraalVM Native Image | AOT 编译 / 冷启动优化 / 反射限制与配置 |

#### 跨语言视野

| 方向 | 学习要点 |
|------|---------|
| Rust 设计思想 | 所有权 / 借用检查器 / 无 GC 内存安全 / 对 Java GC 设计的启发 |
| Go 并发模型 | goroutine / channel / CSP 模型 / 与 Java 虚拟线程的对比 |
| WebAssembly | WASM 基础 / 与 JVM 的对比 / 在浏览器与服务端的应用 |
| Python 生态 | Python GIL / CPython 内存管理 / 与 Java 互操作（Jython / GraalPy）|

#### AI 时代的 Java 开发

| 方向 | 学习要点 |
|------|---------|
| LLM 在 Java 开发中的应用 | AI 代码生成辅助 / 代码 Review 自动化 / 测试用例生成 |
| Java AI 框架基础 | Spring AI / LangChain4j 基础概念 / 向量数据库原理 |
| AI 对语言设计的影响 | Copilot 友好的 API 设计 / 可测试性 / 代码可解释性 |

---

## 📁 项目结构

> 🚧 目录结构规划中，随学习进度逐步创建与完善。已有代码的目录为实际存在，其余为规划目录。

```
java_study/
├── README.md                                    # 项目总览（本文件）
│
└── src/main/java/org/example/
    │
    ├── java_base_test/                          # 🏗️ Java 语言核心 + ⚡ 并发 + 🔌 IO（实验代码）
    │   ├── *.java                               #   Java 语言核心（类型系统/泛型/反射/内部类/面向对象等）
    │   ├── proxy_aop/                           #   动态代理与字节码增强 / AOP 原理
    │   │   ├── ProxyFullDemo.java               #     静态代理 → JDK 动态代理 → CGLIB
    │   │   ├── AopDemo.java                     #     Spring AOP 五种通知类型
    │   │   ├── AopAdviceTypesDemo.java          #     AOP 通知类型详解
    │   │   └── SpringAopScenariosDemo.java      #     AOP 生产场景演示
    │   ├── thread_aync/                         #   ⚡ Java 并发深度（多线程 / 线程池 / 异步）
    │   │   ├── ThreadPoolDemo.java              #     ThreadPoolExecutor 七参数演示
    │   │   ├── ThreadLocalDemo.java             #     ThreadLocal 内存泄漏分析
    │   │   ├── FutureDemo.java                  #     CompletableFuture 异步编排
    │   │   └── CrossThreadTransmitDemo.java     #     跨线程 ThreadLocal 传递（TTL）
    │   ├── io/                                  #   🔌 IO / NIO 体系（核心重点）
    │   │   ├── bio/showcase/                    #     BIO 阻塞模型演示
    │   │   │   ├── BIOServer_NoThread.java      #       单线程阻塞服务端
    │   │   │   ├── BIOServer_WithThread.java    #       每连接一线程模型
    │   │   │   └── BIOClient.java              #       配套客户端
    │   │   ├── nio/NIODemo.java                 #     NIO 全体系单文件完整版（Part1~23）
    │   │   ├── nio/show_multi_agent/            #     多 Agent 重构版，每 Part 独立文件
    │   │   │   ├── Part1_FiveIOModels.java      #       五种 IO 模型
    │   │   │   ├── Part4_BufferStateMachine.java#       Buffer 三指针状态机
    │   │   │   ├── Part6~Part8                  #       Selector + NIO Server（最难，反复看）
    │   │   │   ├── Part9~Part12                 #       零拷贝原理与实战
    │   │   │   ├── Part17~Part19                #       Netty 架构原理
    │   │   │   ├── Part20~Part21                #       Kafka IO + 四大框架对比
    │   │   │   └── Part22~Part23                #       NIO.2 进阶 / 虚拟线程
    │   │   └── zerocopy/ZeroCopyDemo.java       #     零拷贝性能对比演示
    │   └── multi_agent/                         #   多 Agent 系统演示（Orchestrator-Worker）
    │
    ├── algorithm/                               # 🧮 算法与数据结构 — 算法（LeetCode 风格练习 + 理论）
    ├── datastructures/                          # 🧮 算法与数据结构 — 数据结构（手写实现 + 原理分析）
    ├── designmodel/                             # 🎨 设计模式（23 种 GoF 模式 + SOLID 原则）
    │
    ├── operate_system/                          # 🖥️ 计算机基础理论 — 操作系统
    ├── compute_network/                         # 🖥️ 计算机基础理论 — 计算机网络
    ├── computer_principle/                      # 🖥️ 计算机基础理论 — 计算机原理
    ├── compiler_principle/                      # 🖥️ 计算机基础理论 — 编译原理
    │
    ├── source_code_study/                       # 🔬 优秀源码研读（以底层原理印证为目标）
    │   ├── jdk/                                 #   JDK 核心源码（集合/并发/IO/JVM）
    │   ├── kafka/                               #   Kafka（NIO 印证 / mmap 印证）
    │   ├── rocketmq/                            #   RocketMQ（CommitLog mmap / Netty 通信层）
    │   └── rpc/                                 #   RPC（Dubbo SPI / gRPC HTTP/2 协议）
    │
    ├── jvm/                                     # ☕ JVM 原理（规划中）
    │   ├── classloading/                        #   类加载机制（双亲委派 / 自定义类加载器）
    │   ├── memory/                              #   内存模型（堆/栈/方法区/直接内存）
    │   ├── gc/                                  #   GC 算法（G1 / ZGC / Shenandoah）
    │   └── jit/                                 #   JIT 编译（逃逸分析 / 内联优化）
    │
    ├── security/                                # 🔐 信息安全基础（规划中）
    │   ├── cryptography/                        #   密码学基础（对称/非对称/哈希/数字签名）
    │   ├── network-security/                    #   网络安全（TLS/HTTPS 握手原理）
    │   ├── attack-defense/                      #   攻防模型（XSS/CSRF/SQL注入/SSRF原理）
    │   └── java-security-api/                   #   Java 安全 API（JCA/JCE/JSSE）
    │
    ├── software-engineering/                    # 🏛️ 软件工程基础（规划中）
    │   ├── testing/                             #   测试理论（TDD/BDD/单元测试/Mock）
    │   ├── refactoring/                         #   重构技法（代码坏味道 / 安全重构步骤）
    │   ├── code-quality/                        #   代码质量（静态分析 / CheckStyle / SonarQube）
    │   └── ci-basics/                           #   持续集成基础（流水线设计 / 测试覆盖率）
    │
    ├── math/                                    # 📐 数学基础（规划中）
    │   ├── discrete-math/                       #   离散数学（图论 / 逻辑 / 集合论）
    │   ├── probability-stats/                   #   概率论与统计（随机变量 / 分布 / 假设检验）
    │   └── linear-algebra/                      #   线性代数（向量 / 矩阵 / 特征值）
    │
    ├── performance/                             # 🚀 性能工程（规划中）
    │   ├── jmh/                                 #   JMH 基准测试（Throughput / AverageTime / 陷阱规避）
    │   ├── profiling/                           #   性能剖析（Async-Profiler / JFR / 火焰图）
    │   └── optimization/                        #   性能优化实战（CPU/内存/IO/GC 专项调优）
    │
    ├── debugging/                               # 🐛 调试与问题排查（规划中）
    │   ├── jvm-tools/                           #   JVM 诊断工具（jstack/jmap/jstat/jcmd）
    │   ├── arthas/                              #   Arthas 线上诊断实践（trace/watch/classloader）
    │   └── incident/                            #   故障排查方法论（OOM/死锁/CPU飙高/线程泄漏）
    │
    └── frontier/                                # 🔭 前沿技术趋势（规划中）
        ├── project-loom/                        #   Project Loom（虚拟线程 / 结构化并发）
        ├── project-panama/                      #   Project Panama（外部函数接口 / 向量 API）
        ├── project-valhalla/                    #   Project Valhalla（值类型 / 内联类）
        ├── cloud-native-java/                   #   云原生 Java（GraalVM 原生镜像 / 启动优化）
        └── wasm-rust-insights/                  #   WebAssembly / Rust 设计思想对 Java 的影响
```

---

## 📊 学习进度

> 各模块学习状态持续更新。状态说明：🔜 待开始 ｜ 🔥 进行中 ｜ ✅ 已完成 ｜ ⏸️ 暂停

<table>
  <thead>
    <tr>
      <th>主模块</th>
      <th>子模块 / 知识点</th>
      <th>状态</th>
      <th>备注</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td rowspan="10"><b>🏗️ Java 语言核心</b></td>
      <td>类型系统（基本类型 / 包装类 / 自动装箱 / 浮点精度）</td>
      <td>🔥 进行中</td>
      <td></td>
    </tr>
    <tr>
      <td>泛型（泛型类 / 泛型方法 / 类型擦除 / 通配符）</td>
      <td>🔥 进行中</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>内部类（成员 / 静态 / 匿名 / 局部）</td>
      <td>🔥 进行中</td>
      <td></td>
    </tr>
    <tr>
      <td>面向对象（equals / hashCode 契约 / Comparable vs Comparator）</td>
      <td>🔥 进行中</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>反射与元数据（Class 对象 / 反射获取方法字段 / 注解处理）</td>
      <td>🔥 进行中</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>异常处理（checked vs unchecked / try-with-resources / 资源管理）</td>
      <td>🔥 进行中</td>
      <td></td>
    </tr>
    <tr>
      <td>函数式编程（Lambda / 方法引用 / Stream API / Optional）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>字符串（String 不可变性 / StringPool / StringBuilder）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>集合框架使用（List / Set / Map / Queue / Deque 选型）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>动态代理与字节码增强（JDK 动态代理 / CGLIB / ASM / ByteBuddy）</td>
      <td>🔥 进行中</td>
      <td>🔑 重点，AOP 实现根基</td>
    </tr>
    <tr>
      <td>SPI 机制（ServiceLoader / JDBC / SLF4J / Dubbo 应用）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>序列化与反序列化（Serializable / serialVersionUID / 安全风险）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>Java 新特性（record / sealed / pattern matching / 文本块）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td rowspan="15"><b>⚡ Java 并发深度</b></td>
      <td>线程基础（生命周期 / interrupt / join / Callable）</td>
      <td>🔥 进行中</td>
      <td></td>
    </tr>
    <tr>
      <td>线程池（ThreadPoolExecutor 七参数 / 四种拒绝策略 / 合理配置）</td>
      <td>🔥 进行中</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>ThreadLocal（原理 / 弱引用内存泄漏 / TTL 跨线程传递）</td>
      <td>🔥 进行中</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>CompletableFuture（全部 API / 异步异常 / 链式编排）</td>
      <td>🔥 进行中</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>volatile 与 JMM（内存语义 / happens-before / 指令重排）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>Java 内存模型（主内存 / 工作内存 / 八种原子操作 / 内存屏障）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>synchronized 锁升级（对象头 / Mark Word / 偏向→轻量级→重量级）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>CAS 与原子类（Unsafe / AtomicInteger / ABA 问题）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>AQS 核心原理（CLH 队列 / 独占锁 / 共享锁 / Condition）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>并发工具类（ReentrantLock / ReadWriteLock / Semaphore / CountDownLatch / CyclicBarrier）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>并发集合（ConcurrentHashMap / CopyOnWriteArrayList / BlockingQueue）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>ForkJoinPool（工作窃取算法 / RecursiveTask / RecursiveAction）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>并发问题经典模型（哲学家就餐 / 死锁 / 活锁 / 饥饿）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>并发设计模式（生产者-消费者 / 线程安全单例 DCL / 对象池）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>虚拟线程（JDK 21 / Project Loom / 结构化并发）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点，JDK 21+ 必学</td>
    </tr>
    <tr>
      <td>响应式编程基础（Flow API / Reactive Streams / 背压原理）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td rowspan="13"><b>☕ JVM 原理</b></td>
      <td>类加载过程（加载→验证→准备→解析→初始化）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>双亲委派模型 / 打破双亲委派（Tomcat / SPI）/ 自定义 ClassLoader</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>类初始化时机（&lt;clinit&gt; / 静态块 / 六种触发情形）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>运行时数据区（堆 / 栈 / 方法区 / 元空间 / PC 寄存器）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>对象内存布局（对象头 / Mark Word / 实例数据 / 对齐填充）</td>
      <td>🔜 待开始</td>
      <td>synchronized 锁升级的基础</td>
    </tr>
    <tr>
      <td>堆内存分代（Eden / Survivor / 老年代 / TLAB / 对象晋升）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>逃逸分析（栈上分配 / 标量替换 / 同步消除）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>GC 基础（可达性分析 / GC Roots / 四种引用 / finalize）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>GC 算法（标记-清除 / 标记-整理 / 复制算法）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>垃圾收集器（Serial / Parallel / CMS / G1 / ZGC / Shenandoah）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>GC 调优（GC 日志解读 / OOM 排查 / JVM 参数体系）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点，生产必备</td>
    </tr>
    <tr>
      <td>字节码与 JIT（.class 格式 / 字节码指令 / C1/C2 分层编译 / 热点探测）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>JIT 优化（方法内联 / 逃逸分析 / 锁消除 / 标量替换）/ AOT / GraalVM</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td rowspan="11"><b>🔌 IO / NIO 体系</b></td>
      <td>IO 模型理论（五种 IO 模型概览）</td>
      <td>🔥 进行中</td>
      <td>Part1</td>
    </tr>
    <tr>
      <td>BIO（单线程阻塞 / 每连接一线程 / 线程爆炸问题）</td>
      <td>🔥 进行中</td>
      <td>Part2~Part3</td>
    </tr>
    <tr>
      <td>NIO 基础（Buffer 三指针状态机 / Channel 类型体系）</td>
      <td>🔥 进行中</td>
      <td>Part4~Part5，🔑 重点</td>
    </tr>
    <tr>
      <td>NIO 网络 IO（Selector + epoll / SelectionKey / 完整 NIO Server）</td>
      <td>🔥 进行中</td>
      <td>Part6~Part8，最难，反复看</td>
    </tr>
    <tr>
      <td>零拷贝（transferTo / mmap / 小文件陷阱 / 性能对比）</td>
      <td>🔥 进行中</td>
      <td>Part9~Part13，🔑 重点</td>
    </tr>
    <tr>
      <td>NIO 文件 IO（FileChannel / MappedByteBuffer / NIO.2 Path API）</td>
      <td>🔥 进行中</td>
      <td>Part14~Part16</td>
    </tr>
    <tr>
      <td>Netty 设计原理（EventLoop 线程模型 / Pipeline / ByteBuf / 粘包拆包）</td>
      <td>🔥 进行中</td>
      <td>Part17~Part19，仅原理层</td>
    </tr>
    <tr>
      <td>开源框架 IO 解析（Kafka IO 体系 / 四大框架横向对比）</td>
      <td>🔥 进行中</td>
      <td>Part20~Part21</td>
    </tr>
    <tr>
      <td>NIO.2 进阶（异步 AsynchronousFileChannel / WatchService）</td>
      <td>🔥 进行中</td>
      <td>Part22</td>
    </tr>
    <tr>
      <td>虚拟线程（Project Loom / JDK 21 Virtual Thread）</td>
      <td>🔥 进行中</td>
      <td>Part23</td>
    </tr>
    <tr>
      <td>零拷贝性能对比演示（传统拷贝 vs transferTo vs mmap）</td>
      <td>🔥 进行中</td>
      <td>ZeroCopyDemo</td>
    </tr>
    <tr>
      <td rowspan="12"><b>🧮 算法与数据结构</b></td>
      <td>复杂度分析（时间 / 空间 / Big-O / 均摊分析）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>线性结构（数组 / 链表 / 栈 / 队列 / 双端队列）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>树结构（二叉树 / BST / AVL 树 / 红黑树 / B+ 树 / Trie）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>堆与优先队列（最大堆 / 最小堆 / 堆排序 / PriorityQueue 原理）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>哈希表（哈希函数 / 冲突解决 / 负载因子 / HashMap 扩容原理）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>图（BFS / DFS / Dijkstra / 拓扑排序）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>排序算法（归并 / 快排 / 堆排 / 计数 / 基数 / 稳定性分析）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>搜索算法（二分搜索 / DFS / BFS）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>动态规划（DP 思路 / 记忆化搜索 / 背包 / LCS）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>回溯（全排列 / 子集 / N 皇后 / 剪枝）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>贪心（区间调度 / Huffman / 最小生成树）/ 字符串算法（KMP）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td rowspan="9"><b>🎨 设计模式</b></td>
      <td>创建型（单例 / 工厂方法 / 抽象工厂 / 建造者 / 原型）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>结构型（代理 / 装饰器 / 适配器 / 外观 / 组合 / 桥接 / 享元）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>行为型（责任链 / 观察者 / 策略 / 命令 / 模板方法）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>行为型（迭代器 / 状态 / 访问者 / 解释器 / 中介者 / 备忘录）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>模式在 JDK 中的应用（BufferedInputStream / PriorityQueue / Iterator 等）</td>
      <td>🔜 待开始</td>
      <td>结合源码理解</td>
    </tr>
    <tr>
      <td>模式在 Spring 中的应用（AOP / IoC / MVC / 事件机制）</td>
      <td>🔜 待开始</td>
      <td>原理层了解</td>
    </tr>
    <tr>
      <td>并发场景下的设计模式（线程安全单例 DCL / 生产者消费者）</td>
      <td>🔜 待开始</td>
      <td>与并发深度结合</td>
    </tr>
    <tr>
      <td>面向对象设计原则（SOLID / DRY / KISS / YAGNI / 迪米特法则）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点，工程素养核心</td>
    </tr>
    <tr>
      <td>架构模式基础（分层架构 / 六边形 / 事件驱动 / CQRS / 管道过滤器）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td rowspan="20"><b>🖥️ 计算机基础理论</b></td>
      <td>操作系统：进程 vs 线程 / 上下文切换 / 协程 / 用户态 vs 内核态</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>操作系统：内存管理 / 虚拟内存 / 分页机制 / Page Cache / mmap</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>操作系统：IO 多路复用（select / poll / epoll LT & ET 两种模式）</td>
      <td>🔜 待开始</td>
      <td>穿插学习，🔑 重点</td>
    </tr>
    <tr>
      <td>操作系统：进程调度（CFS / 优先级 / 时间片）/ 同步原语（互斥锁/自旋锁/信号量）</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>操作系统：进程间通信 / 文件系统 / 死锁四条件与排查</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机网络：协议分层（OSI / TCP/IP）</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机网络：TCP（三次握手 / 四次挥手 / 流量控制 / 拥塞控制）</td>
      <td>🔜 待开始</td>
      <td>穿插学习，🔑 重点</td>
    </tr>
    <tr>
      <td>计算机网络：HTTP/1.1 vs HTTP/2 vs HTTP/3（QUIC）</td>
      <td>🔜 待开始</td>
      <td>穿插学习，🔑 重点</td>
    </tr>
    <tr>
      <td>计算机网络：HTTPS / TLS 握手原理 / 证书链验证</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机网络：UDP 特性 / QUIC 可靠传输实现</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机网络：WebSocket 握手协议 / 帧格式</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机网络：DNS / 网络工具（tcpdump / Wireshark）</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机网络：网络安全基础（ARP 欺骗 / DNS 劫持 / MITM）</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机原理：CPU 多级缓存 / 缓存行 / 伪共享（False Sharing）</td>
      <td>🔜 待开始</td>
      <td>穿插学习，volatile 硬件基础</td>
    </tr>
    <tr>
      <td>计算机原理：内存屏障（StoreStore / LoadLoad 等）/ volatile 实现</td>
      <td>🔜 待开始</td>
      <td>穿插学习，🔑 重点</td>
    </tr>
    <tr>
      <td>计算机原理：CPU 指令重排 / 乱序执行 / happens-before 硬件根因</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机原理：CPU 流水线（冒险/分支预测/超标量）/ 数值表示（IEEE 754 / 大小端）</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>计算机原理：DMA 原理 / sendfile 系统调用 / 零拷贝硬件基础</td>
      <td>🔜 待开始</td>
      <td>穿插学习</td>
    </tr>
    <tr>
      <td>编译原理：词法分析 / 语法分析 / AST / 语义分析</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>编译原理：javac 编译过程 / APT 注解处理器 / Lombok 原理</td>
      <td>🔜 待开始</td>
      <td>了解为主，与 JVM 呼应</td>
    </tr>
    <tr>
      <td>编译原理：JIT 即时编译（与 JVM 板块呼应）/ invokedynamic</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>编译原理：运行时（栈帧/动态链接）/ 正则表达式引擎（NFA vs DFA / ReDoS）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td rowspan="11"><b>🔬 优秀源码研读</b></td>
      <td>JDK — 集合框架（HashMap / ConcurrentHashMap / ArrayList / TreeMap）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点，印证数据结构</td>
    </tr>
    <tr>
      <td>JDK — 并发工具（AQS / ReentrantLock / ThreadPoolExecutor）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点，印证并发深度</td>
    </tr>
    <tr>
      <td>JDK — IO 体系（InputStream 装饰器链 / FileChannel / Selector）</td>
      <td>🔜 待开始</td>
      <td>印证 IO/NIO 体系</td>
    </tr>
    <tr>
      <td>JDK — JVM 相关（String / Integer 缓存 / Reference 引用链）</td>
      <td>🔜 待开始</td>
      <td>印证 JVM 原理</td>
    </tr>
    <tr>
      <td>JDK — 函数式（Stream 实现 / CompletableFuture / ForkJoinPool）</td>
      <td>🔜 待开始</td>
      <td>印证函数式 + 并发</td>
    </tr>
    <tr>
      <td>Kafka — 存储层（Log / LogSegment / OffsetIndex）</td>
      <td>🔜 待开始</td>
      <td>印证 mmap / 顺序写</td>
    </tr>
    <tr>
      <td>Kafka — 网络层（Selector / KafkaChannel / Processor）</td>
      <td>🔜 待开始</td>
      <td>印证 NIO Selector 原理</td>
    </tr>
    <tr>
      <td>RocketMQ — 存储（CommitLog mmap 写 / ConsumeQueue / IndexFile）</td>
      <td>🔜 待开始</td>
      <td>印证 mmap 实战应用</td>
    </tr>
    <tr>
      <td>RocketMQ — 网络通信（Netty 通信层 / RemotingCommand 协议）</td>
      <td>🔜 待开始</td>
      <td>印证 Netty Pipeline 原理</td>
    </tr>
    <tr>
      <td>RPC — Dubbo SPI 机制（ServiceConfig / Invoker / Protocol SPI）</td>
      <td>🔜 待开始</td>
      <td>理解微内核 + SPI 设计</td>
    </tr>
    <tr>
      <td>RPC — gRPC 协议原理（HTTP/2 帧结构 / Protobuf 二进制协议）</td>
      <td>🔜 待开始</td>
      <td>印证 HTTP/2 协议原理</td>
    </tr>
    <tr>
      <td rowspan="7"><b>🔐 信息安全基础</b></td>
      <td>密码学基础（对称加密 AES / 非对称加密 RSA / ECC）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>哈希函数（SHA-256 / SHA-3）/ 消息认证码（HMAC）/ 数字签名</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>公钥基础设施（X.509 证书 / CA 链 / 证书吊销）/ 密钥派生（PBKDF2 / bcrypt）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>常见攻击与防御（SQL 注入 / XSS / CSRF / 反序列化漏洞 / 计时攻击）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>Java 安全 API（MessageDigest / Cipher / SSLContext / JSSE）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>JWT 原理（Header.Payload.Signature / alg:none 漏洞）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>OAuth 2.0 原理（四种授权模式 / Access Token / PKCE）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td rowspan="6"><b>🏛️ 软件工程基础</b></td>
      <td>测试理论（测试金字塔 / JUnit 5 / Mockito / TDD / BDD）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>测试覆盖率（JaCoCo）/ 契约测试（Pact）/ 集成测试（Testcontainers）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>代码质量工程（代码异味 / 重构手法 / SonarQube / SpotBugs / ArchUnit）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>Git 深度（三棵树 / rebase vs merge / bisect）/ 分支策略（Trunk Based）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>CI/CD 基础（构建流水线 / 制品管理 / 蓝绿部署概念）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>技术债务管理 / ADR 架构决策记录 / Javadoc 规范</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td rowspan="6"><b>📐 数学基础</b></td>
      <td>离散数学（集合论 / 逻辑 / 图论基础）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>组合数学（排列组合 / 容斥原理 / 鸽巢原理）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>数论基础（模运算 / 费马小定理 / 辗转相除）密码学支撑</td>
      <td>🔜 待开始</td>
      <td>🔑 重点（密码学基础）</td>
    </tr>
    <tr>
      <td>概率统计（贝叶斯 / 常见分布 / 期望方差）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>概率工程应用（布隆过滤器 / 一致性哈希 / 随机化算法）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>线性代数基础（矩阵/特征值/SVD）工程应用（AI/PageRank）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td rowspan="6"><b>🚀 性能工程</b></td>
      <td>性能分析方法论（USE / RED / Amdahl 定律 / 利特尔法则）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>JMH 基准测试（@Benchmark / @Warmup / 常见陷阱）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>火焰图与性能剖析（async-profiler / JFR / VisualVM）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>JVM 性能调优（GC 日志解读 / Heap Dump / MAT / TLAB 调优）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点，生产必备</td>
    </tr>
    <tr>
      <td>系统性能优化（算法层 / JVM 层 / IO 层 / 并发层 / 缓存友好代码）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td>压测工具（Gatling / JMeter / wrk）/ 负载模型设计</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td rowspan="5"><b>🐛 调试与问题排查</b></td>
      <td>JVM 诊断工具链（jstack / jmap / jstat / jcmd / jconsole）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点，生产必备</td>
    </tr>
    <tr>
      <td>Arthas 线上诊断（方法追踪 / 字节码反编译 / 热更新 / 类加载分析）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>JFR + JMC 低开销生产剖析 / 常见问题排查手册（CPU 100% / OOM / 死锁）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>Linux 生产排查（top / vmstat / iostat / strace / tcpdump）</td>
      <td>🔜 待开始</td>
      <td>🔑 重点</td>
    </tr>
    <tr>
      <td>日志分析（grep / awk / tail -f / ELK 体系基础）/ 内核参数调优（ulimit / tcp_backlog）</td>
      <td>🔜 待开始</td>
      <td></td>
    </tr>
    <tr>
      <td rowspan="4"><b>🔭 前沿技术趋势</b></td>
      <td>Java 平台演进（Loom ✅ / Panama / Valhalla / Amber / Leyden）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>云原生基础（Docker 原理 / K8s 核心概念 / 服务网格 / GraalVM Native）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>跨语言视野（Rust 所有权 / Go goroutine / WASM / Python GIL）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
    <tr>
      <td>AI 时代的 Java 开发（Spring AI / LangChain4j / Copilot 友好设计）</td>
      <td>🔜 待开始</td>
      <td>了解为主</td>
    </tr>
  </tbody>
</table>

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

## 🔗 关联项目

| 项目 | 定位 | 内容 |
|------|------|------|
| **本项目**（`java_study`） | 基础理论 | Java 语言核心（含 SPI/字节码增强/序列化）、JVM 原理、IO/NIO 体系、并发深度（含虚拟线程/响应式）、数据结构与算法、设计模式与设计原则、计算机基础理论（OS/网络/原理/编译）、信息安全基础（密码学/攻防）、软件工程基础（测试/重构/CI）、数学基础、性能工程（JMH/火焰图）、调试与问题排查（Arthas/JFR）、前沿技术趋势 |
| [`java_fullstack_ai_agent_study`](../java_fullstack_ai_agent_study) | 工程实践 | Spring 生态、分布式架构、大数据生态、存储中间件、风控爬虫、数据分析、多语言、前端、AI/Agent 系统接入（做系统）|
| [`ai_coding_harness_engineering_study`](../ai_coding_harness_engineering_study) | AI 工程 | AI 编程方法论、上下文工程、AI 工具工程化、Harness 理论、AI Coding 工具链（Claude Code / Cursor / Codex）、AI 算法与大模型理论、Agent 开发（用 AI 提效）|

---

## 📅 更新记录

| 日期 | 版本 | 内容 |
|------|------|------|
| 2025-05 | v0.1.0 | 初始化项目，建立基础目录结构 |
| 2025-05 | v0.2.0 | 完成 IO/NIO 全体系（Part1~Part23），含七层递进路线图与生活场景类比 |
| 2025-05 | v0.3.0 | 新增多 Agent 系统架构演示（`multi_agent/`），Orchestrator-Worker 模式 |
| 2025-05 | v0.4.0 | 重构 NIODemo 为多 Agent 模式（`io/nio/show_multi_agent/`），23 个 Part 拆分为独立文件 |
| 2025-05 | v0.5.0 | 建立各模块 README，新增编译原理模块（`compiler_principle/`）|
| 2025-05 | v0.6.0 | 新增优秀源码研读方向（`source_code_study/`），规划 JDK / Kafka / RocketMQ / RPC 四条源码学习路线 |
| 2026-05 | v0.7.0 | 完善 README 地图：补充 Java 语言基础知识点清单、修正阶段编号、细化 IO 目录说明 |
| 2026-06-10 | v0.8.0 | 同步关联项目分工说明：对齐三个项目最新定位描述 |
| 2026-06-10 | v1.0.0 | **全面重构 README**：参照项目二、三的格式风格，以项目定位为核心重新梳理全部学习内容；新增「技术版图」章节（学习路线总览 + 8 大板块详细内容）；新增 JVM 原理完整板块（类加载/内存模型/GC/JIT）；拆分并深化 Java 并发深度（volatile/JMM/AQS/CAS/ForkJoin）；重新组织算法与数据结构 + 设计模式板块；补全计算机基础理论（HTTPS/TLS / CPU 流水线 / 内存屏障）；重构学习进度为 HTML 大表格；明确各方向与项目二的边界；新增项目结构树形图 |
| 2026-06-10 | v1.1.0 | **知识体系全面扩充**：新增 6 大板块（🔐 信息安全基础 / 🏛️ 软件工程基础 / 📐 数学基础 / 🚀 性能工程 / 🐛 调试与问题排查 / 🔭 前沿技术趋势）；完善已有板块子方向（Java 语言核心新增 SPI/字节码增强/序列化；并发深度新增虚拟线程/响应式/经典并发模型；设计模式新增 SOLID 原则表 + 架构模式基础；计算机基础理论新增进程调度/同步原语/UDP/WebSocket/CPU 流水线/数值表示/正则引擎等）；学习路线总览扩展至 14 大板块；更新学习进度表新增 6 个板块进度跟踪 |
| 2026-06-10 | v1.2.0 | **迭代「项目结构」树形图**：补充 6 个新增板块规划目录（`jvm/` / `security/` / `software-engineering/` / `math/` / `performance/` / `debugging/` / `frontier/`）；完善各目录子目录与注释；修正计算机基础理论目录表示（各模块独立同级）；为已有目录补充板块标注 Emoji；添加「已有 vs 规划」说明注释 |
| 2026-06-10 | v1.3.0 | **新增「📋 JDK 版本特性速查」子节**：在「🔭 前沿技术趋势」板块中新增 JDK 8 / JDK 11 / JDK 17 / JDK 21 / JDK 22-24 各版本关键特性速查表，涵盖 Lambda/Stream（8）、HTTP Client/var（11）、record/sealed/文本块（17）、虚拟线程/结构化并发/模式匹配（21）、Panama/Gather/ScopedValues（22-24）；新增版本选型建议表 |

---

<div align="center">
  <sub>持续更新中 🚀 · 打好地基，才能盖高楼</sub>
</div>
