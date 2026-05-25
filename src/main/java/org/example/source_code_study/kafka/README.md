# Kafka 源码研读

从 IO 性能的视角切入 Kafka 源码，理解「百万 QPS 消息队列」背后的存储、网络与协调设计。

> **前置建议**：先完成 NIODemo Part1~Part23（特别是 Part6 Selector、Part9~Part12 零拷贝、Part20 Kafka IO 体系），再来读源码，印证效果最好。

---

## 推荐阅读路线

```
存储层：Log / LogSegment / OffsetIndex（顺序写 + 稀疏索引）
    ↓
网络层：Selector（NIO封装）/ KafkaChannel / Processor / Acceptor
    ↓
副本机制：ReplicaManager / Partition / ISR 收缩与扩展
    ↓
生产者：RecordAccumulator / Sender / ProducerBatch（批量发送）
    ↓
消费者：ConsumerCoordinator / OffsetManager / Fetcher（分区再平衡）
    ↓
Controller：KafkaController / ZkClient（选举 + 元数据管理）
```

---

## 核心模块详解

### 💾 存储层（最先读）

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `Log` | `append` / `read` / `roll`（段滚动）| 为什么每个 Partition 是独立的 Log 目录？ |
| `LogSegment` | `.log` 文件顺序写 / `.index` 稀疏索引 | 稀疏索引如何用二分查找定位消息？ |
| `OffsetIndex` | `lookup` / `append` / mmap 实现 | 索引文件为什么用 mmap 而不是普通 IO？ |
| `FileRecords` | `writeTo`（sendfile 零拷贝）| 消费者读消息时如何触发 sendfile？ |

**入口**：`Log.append()` → `LogSegment.append()` → `FileRecords.writeTo()`，这条链读完就理解了「顺序写 + sendfile 消费」的完整闭环。

**对照 NIODemo**：
- 顺序写 → Part20 Kafka IO 体系（印刷厂类比）
- sendfile 消费 → Part9~Part12 零拷贝专题

---

### 🌐 网络层

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `KafkaSelector` | 基于 `java.nio.Selector` 封装 / `poll` | Kafka 如何在 NIO 基础上封装出自己的 Selector？ |
| `KafkaChannel` | `read` / `write` / `TransportLayer` | 明文 vs TLS 如何通过 TransportLayer 抽象隔离？ |
| `Acceptor` | `accept` / 分发给 `Processor` | 1 个 Acceptor + N 个 Processor 的 Reactor 模型 |
| `Processor` | `selector.poll` / 请求队列 / 响应队列 | 请求如何从网络线程流转到 IO 线程再回来？ |
| `RequestChannel` | `RequestQueue` / `ResponseQueue` | 网络线程与处理线程如何解耦？ |

**对照 NIODemo**：Part6~Part8 NIO Server 实现，Kafka 的 Acceptor+Processor 是其工业级版本。

---

### 📋 副本机制

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ReplicaManager` | `appendRecords` / `fetchMessages` / `becomeLeaderOrFollower` | Leader 和 Follower 的读写路径有何区别？ |
| `Partition` | `ISR` 维护 / `highWatermark` 更新 | HW 是如何保证消费者只看到已提交数据的？ |
| `ReplicaFetcherThread` | `processPartitionData` / lag 计算 | Follower 如何追赶 Leader？ISR 缩减的触发条件？ |

---

### 📤 生产者

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `RecordAccumulator` | `append` / `ProducerBatch` / `drain` | 消息如何在内存中批量积攒？ |
| `Sender` | `sendProducerData` / 幂等序号 / 事务状态 | 幂等生产者如何通过序号去重？ |

---

### 📥 消费者

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ConsumerCoordinator` | `JoinGroup` / `SyncGroup` / `onPartitionsRevoked` | 再平衡的完整流程是什么？ |
| `OffsetManager` | `__consumer_offsets` Topic / 提交与拉取 | 为什么 offset 要存在 Kafka 自己的 Topic 里？ |
| `Fetcher` | `sendFetches` / `fetchedRecords` | 消费者如何预拉取数据提高吞吐？ |

---

## 与已学知识的关联

| Kafka 源码 | 已学模块 |
|-----------|---------|
| `KafkaSelector`（NIO封装） | NIODemo Part6 Selector + epoll |
| `FileRecords.writeTo`（sendfile） | NIODemo Part9~Part12 零拷贝 |
| `OffsetIndex`（mmap） | NIODemo Part15 MappedByteBuffer |
| Acceptor + Processor（Reactor模型）| NIODemo Part7~Part8 NIO Server |
| ISR + HW（高可用设计） | `operate_system/`（一致性模型） |

