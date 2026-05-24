# 优秀源码研读

从「会用」到「看懂顶级工程师怎么写」，建立工业级代码审美与架构直觉。

> 本模块是所有基础学习的**收敛层**——学完 Java 基础、IO/NIO、并发、Netty 之后，回到这里在真实工业级源码中印证、深化和拔高。

---

## 模块结构

```
source_code_study/
├── jdk/        JDK 核心源码（集合 / 并发 / IO / JVM）
├── kafka/      Kafka 源码（存储 / 网络 / 副本 / 消费者）
├── rocketmq/   RocketMQ 源码（消息存储 / 事务 / 延迟消息）
└── rpc/        RPC 框架源码（Dubbo / gRPC 核心链路）
```

各子模块详细说明见对应目录下的 README.md。

---

## 推荐阅读顺序

```
JDK 核心源码（集合 → 并发）     ← 最先读，基础最扎实
    ↓
Kafka 源码（存储层 → 网络层）   ← IO/NIO 学完后读，印证效果最好
    ↓
RocketMQ 源码（CommitLog）     ← mmap/Netty 学完后读
    ↓
RPC 框架源码（Dubbo / gRPC）   ← Netty + 序列化学完后读
```

> 💡 **各模块之间的知识依赖**
>
> | 源码 | 需要先掌握的基础 |
> |------|----------------|
> | JDK 集合/并发 | Java 基础、多线程 & 异步 |
> | Kafka | NIO 体系（Part1~Part23）、零拷贝、Netty |
> | RocketMQ | MappedByteBuffer (mmap)、Netty、分布式事务概念 |
> | RPC | Netty、Protobuf 序列化概念、微服务基础 |

---

## 源码阅读方法论

1. **带问题读**：先从使用层面提出一个问题（如「HashMap 扩容为什么是 2 倍？」），再去源码里找答案
2. **对照 DEBUG**：写一个 Demo，断点进入源码，比直接看源码效率高 3 倍
3. **先主干后细节**：第一遍只看核心流程，忽略异常处理和边界条件
4. **画时序图**：复杂模块边读边画，画完就记住了
5. **与已学知识对照**：每看一个模块，有意识地回想「这用了哪个我学过的底层知识？」

---

## 与其他模块的关联

| 源码模块 | 印证哪些已学知识 |
|---------|----------------|
| JDK HashMap | 哈希冲突解决、红黑树（数据结构模块） |
| JDK AQS | CAS、volatile、线程状态机（多线程模块） |
| Kafka 网络层 | NIO Selector、epoll（NIODemo Part5~Part8） |
| Kafka 存储层 | 零拷贝 sendfile（NIODemo Part9~Part12） |
| RocketMQ CommitLog | MappedByteBuffer mmap（NIODemo Part15） |
| Dubbo/gRPC 网络层 | Netty Pipeline、编解码（NIODemo Part17~Part19） |

