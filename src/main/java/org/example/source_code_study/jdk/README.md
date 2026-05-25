# JDK 核心源码研读

深入 JDK 源码，理解 Java 标准库的设计思想与实现细节，把「会用」升级为「看懂为什么这么设计」。

---

## 推荐阅读路线

```
HashMap（哈希表 + 红黑树）
    ↓
ConcurrentHashMap（CAS + 分段思想）
    ↓
AQS（AbstractQueuedSynchronizer）
    ↓
ReentrantLock / Semaphore / CountDownLatch
    ↓
ThreadPoolExecutor（线程池核心）
    ↓
InputStream 装饰器链 / FileChannel / Selector 实现
    ↓
String 不可变性 / Integer 缓存 / Reference 引用链
```

---

## 核心模块详解

### 🗂️ 集合框架

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `HashMap` | `putVal` / `resize` / `treeifyBin` | 扩容为什么是 2 倍？链表何时转红黑树？ |
| `ConcurrentHashMap` | `putVal` / `transfer` / `sizeCtl` | JDK8 如何用 CAS + `synchronized` 替代分段锁？ |
| `ArrayList` | `grow` / `System.arraycopy` | 扩容系数为什么是 1.5？ |
| `LinkedList` | `Node` 双向链表 / `Deque` 实现 | 为什么不推荐用 LinkedList 做随机访问？ |
| `PriorityQueue` | `siftUp` / `siftDown` | 堆化时间复杂度是 O(n) 还是 O(nlogn)？ |

**入口**：从 `HashMap.put()` 开始，一路跟进 `putVal → resize → treeifyBin`，三个方法读完基本掌握全貌。

---

### ⚙️ 并发工具

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `AQS` | `acquire` / `release` / `Node` 等待队列 | CLH 队列如何实现公平/非公平锁？ |
| `ReentrantLock` | `lock` / `tryAcquire` / `Condition` | 可重入如何计数？Condition 队列与 AQS 队列的关系？ |
| `CountDownLatch` | `await` / `countDown` / `Sync` | 如何用 AQS 的 state 实现倒计时？ |
| `Semaphore` | `acquire` / `release` | 公平模式与非公平模式的区别？ |
| `ThreadPoolExecutor` | `execute` / `addWorker` / `runWorker` / `processWorkerExit` | 线程池的 7 个参数如何联动？拒绝策略何时触发？ |

**入口**：先读 `AQS`，再读 `ReentrantLock`——理解了 AQS 的状态机，其他并发工具都是在它基础上的小变体。

---

### 📂 IO 体系（与 NIODemo 对照）

| 类 | 阅读重点 | 对照 NIODemo |
|----|---------|------------|
| `BufferedInputStream` | 装饰器模式 / `buf` 缓冲区设计 | Part3：BufferedInputStream 加速原理 |
| `FileChannel` | `read` / `write` / `transferTo` | Part9：transferTo 零拷贝调用链 |
| `MappedByteBuffer` | `map` / `force` / DirectByteBuffer | Part15：MappedByteBuffer mmap |
| `SelectorImpl` | `select` / `selectedKeys` / epoll 封装 | Part6：Selector + epoll 原理 |

> ⚠️ **阅读建议**：IO 相关源码建议在完成 NIODemo Part1~Part23 之后再读，印证效果最好。

---

### 🔧 JVM 相关

| 类 | 阅读重点 | 核心收益 |
|----|---------|---------|
| `String` | `char[]` / `byte[]` 存储 / `intern()` | 字符串常量池的本质；JDK9 后 Latin1 压缩优化 |
| `Integer` | `IntegerCache` / `valueOf` | 缓存 -128~127 的实现；为什么 `==` 比较会有坑 |
| `WeakReference` / `SoftReference` | `get` / GC 回收时机 | ThreadLocal 为什么用弱引用 key？内存泄漏根因 |

---

## 与已学知识的关联

| JDK 源码 | 已学模块 |
|---------|---------|
| `HashMap` 红黑树 | `datastructures/`（树） |
| `ConcurrentHashMap` CAS | `java_base_test/thread_aync/` |
| `AQS` 状态机 | `java_base_test/thread_aync/`（线程池） |
| `ThreadLocal` 弱引用 | `java_base_test/thread_aync/`（ThreadLocal内存泄漏） |
| `FileChannel.transferTo` | `java_base_test/io/nio/` Part9 |
| `BufferedInputStream` 装饰器 | `designmodel/`（装饰器模式） |

