# 操作系统专题

操作系统核心概念，理解 Java 运行时行为（线程调度、GC、IO多路复用）的底层支撑。

---

## 核心知识点

| 方向 | 重点内容 |
|------|---------|
| **进程 & 线程** | 进程/线程区别，上下文切换开销，用户态 vs 内核态 |
| **内存管理** | 虚拟内存，Page Cache，mmap 原理，内存映射文件 |
| **IO 模型** | 同步/异步，阻塞/非阻塞，select/poll/epoll 对比 |
| **文件系统** | 磁盘顺序写 vs 随机写，inode，符号链接 |
| **调度算法** | 时间片轮转，优先级调度，多核 CPU 亲和性 |

---

## 与 Java 的关联

- `epoll` 原理 → 直接影响 NIO Selector 性能（详见 `io/nio/` Part6）
- `mmap` 原理 → MappedByteBuffer 和 RocketMQ CommitLog（详见 `io/nio/` Part15）
- 用户态 / 内核态切换 → 理解零拷贝为什么能提速（详见 `io/nio/` Part9）
- 虚拟内存 / Page Cache → 理解 Kafka 为什么快（详见 `io/nio/` Part20）

建议在学完 `io/nio/` 第一~三层后，再系统学习本专题，结合代码理解会深刻很多。

