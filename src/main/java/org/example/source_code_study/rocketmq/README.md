# RocketMQ 源码研读

从消息存储与可靠性视角切入 RocketMQ 源码，理解「金融级消息队列」背后的 mmap 存储、事务消息与高可用设计。

> **前置建议**：先完成 NIODemo Part15（MappedByteBuffer mmap）和 Part17~Part19（Netty 框架），再读本模块源码，印证效果最好。

---

## 推荐阅读路线

```
存储层：CommitLog（mmap顺序写）/ ConsumeQueue / IndexFile
    ↓
消息写入链路：Broker.putMessage → CommitLog.putMessage → MappedFile
    ↓
消息消费链路：DefaultMessageStore.getMessage → ConsumeQueue → CommitLog
    ↓
网络通信层：Netty 封装 / RemotingCommand 协议 / NettyRemotingServer
    ↓
事务消息：TransactionalMessageBridge / 二阶段提交 / Half消息
    ↓
延迟消息：ScheduleMessageService / 18级延迟时间轮
    ↓
高可用：主从同步 / DLedger（Raft协议）/ 自动切换
```

---

## 核心模块详解

### 💾 存储层（最先读）

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `CommitLog` | `putMessage` / `MappedFile` mmap 写入 / `flush` | 为什么用 mmap 而不是 FileChannel 直接写？ |
| `MappedFile` | `appendMessage` / `flush` / `warmMappedFile` | 文件预热（mlock）的目的是什么？ |
| `ConsumeQueue` | 定长 20 字节消息索引 / `putMessagePositionInfo` | ConsumeQueue 为什么设计成定长？如何快速定位消息？ |
| `IndexFile` | 哈希索引 / `putKey` / `selectPhyOffset` | 按消息 Key 查询是如何实现 O(1) 定位的？ |
| `DefaultMessageStore` | 统筹 CommitLog + ConsumeQueue + IndexFile | 三个文件如何协同？哪个是主文件？ |

**入口**：`CommitLog.putMessage()` → `MappedFile.appendMessage()` → `MappedByteBuffer.put()`，这条链读完就理解了「mmap 顺序写」的完整实现。

**对照 NIODemo**：
- mmap 写入 → Part15 MappedByteBuffer 专题
- ConsumeQueue sendfile 消费 → Part9 transferTo 零拷贝

---

### 🌐 网络通信层

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `NettyRemotingServer` | Boss/Worker EventLoopGroup / Pipeline 配置 | RocketMQ 如何在 Netty 基础上封装 RPC 框架？ |
| `RemotingCommand` | 协议头（固定16字节）/ 序列化 / requestCode | 自定义二进制协议如何设计 Header + Body？ |
| `NettyDecoder` | 基于帧长度的拆包解码 | 粘包拆包如何解决？与 Netty LengthFieldDecoder 对比 |
| `BrokerController` | 各处理器（Processor）注册 / 请求路由 | 一个 requestCode 是如何路由到对应处理器的？ |

**对照 NIODemo**：Part17~Part19 Netty 架构、Pipeline、粘包拆包专题。

---

### 📨 事务消息

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `TransactionalMessageBridge` | Half 消息写入 `RMQ_SYS_TRANS_HALF_TOPIC` | 为什么要先写 Half 消息再写真正的消息？ |
| `TransactionalMessageServiceImpl` | `check` / 回查机制 / 超时处理 | 如果生产者没有 commit/rollback，Broker 怎么处理？ |
| `EndTransactionProcessor` | commit → 恢复真实消息 / rollback → 删除 Half 消息 | 二阶段提交的原子性如何保证？ |

---

### ⏰ 延迟消息

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ScheduleMessageService` | 18 个延迟级别 / `Timer` 轮询 | 为什么是固定 18 级而不是任意时间？ |
| `DeliverDelayedMessageTimerTask` | 检查到期消息 / 恢复到真实 Topic | 延迟消息如何从 `SCHEDULE_TOPIC` 转移到真实 Topic？ |

---

### 🔄 高可用

| 机制 | 关键类 | 核心收益 |
|------|--------|---------|
| 主从同步 | `HAService` / `HAConnection` / `WaitNotifyObject` | 理解同步复制 vs 异步复制的实现差异 |
| DLedger（Raft） | `DLedgerServer` / `QuorumAckChecker` | 理解 Raft 选举与日志复制的工业级实现 |

---

## 与已学知识的关联

| RocketMQ 源码 | 已学模块 |
|-------------|---------|
| `MappedFile`（mmap写） | NIODemo Part15 MappedByteBuffer |
| `ConsumeQueue`（sendfile消费） | NIODemo Part9~Part12 零拷贝 |
| `NettyRemotingServer` | NIODemo Part17~Part19 Netty 框架 |
| 事务消息二阶段提交 | 分布式事务概念（`java_fullstack_ai_agent_study`） |
| DLedger Raft 协议 | `operate_system/`（一致性算法） |

