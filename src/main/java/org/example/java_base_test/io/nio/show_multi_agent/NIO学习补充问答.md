# NIO 学习补充问答

> 记录阅读 BIODemo / NIODemo / show_multi_agent 系列过程中，单独提问的技术问题及解答。
> 日期：2026-05-24

---

## Q0：五种 IO 模型原理与交互图

> Unix/Linux 网络 IO 的五种模型（来自 POSIX 规范），以"读一个 Socket 数据"为例。
> 整个过程分 **两个阶段**：
> - **阶段①**：等待数据就绪（网卡收到数据，写入内核缓冲区）
> - **阶段②**：数据拷贝（内核缓冲区 → 用户进程 buffer）

---

### 模型① 阻塞 IO（BIO）

```
用户进程                    内核                        硬件(网卡)
   │                          │                              │
   │  read() 系统调用          │                              │
   ├─────────────────────────►│                              │
   │                          │  数据还没到，线程挂起         │
   │  ⏸️ 阻塞等待              │◄ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│ 等待网卡
   │  (线程被 OS 挂起)         │                              │
   │                          │         网卡收到数据包        │
   │                          │◄─────────────────────────────┤ DMA 搬到内核buf
   │                          │  ← 数据就绪，唤醒线程         │
   │                          │                              │
   │                          │  内核 buf → 用户 buf          │
   │◄─────────────────────────┤  (CPU 参与拷贝)              │
   │  read() 返回              │                              │
   │  处理数据 ✅               │                              │

阶段① ████████████████████ 阻塞
阶段② ████ 阻塞
```

**特点**：两个阶段全程阻塞，线程什么都不能干，一对一（一线程 = 一连接）。
Java 对应：`java.io.InputStream.read()`

---

### 模型② 非阻塞 IO（Non-blocking IO）

```
用户进程                    内核                        硬件(网卡)
   │                          │                              │
   │  read() ─────────────►  │  数据未就绪                   │
   │◄─ 立刻返回 EAGAIN        │                              │
   │                          │                              │
   │  （等一会儿）             │                              │
   │  read() ─────────────►  │  数据未就绪                   │
   │◄─ 立刻返回 EAGAIN        │                              │
   │                          │         网卡收到数据包        │
   │  （等一会儿）             │◄─────────────────────────────┤ DMA 搬到内核buf
   │  read() ─────────────►  │  数据就绪！                   │
   │                          │  内核 buf → 用户 buf          │
   │◄─────────────────────────┤  (CPU 参与拷贝)              │
   │  read() 返回数据          │                              │
   │  处理数据 ✅               │                              │

阶段① ░░░░░░░░░░░░░░░░░░░░ 不阻塞（但 CPU 一直轮询，忙等浪费）
阶段② ████ 阻塞
```

**特点**：阶段① 不阻塞，但进程要不停地轮询询问内核，大量 CPU 浪费在"问了又没数据"上。
Java 对应：`channel.configureBlocking(false)` + 手动 while 循环

---

### 模型③ IO 多路复用（IO Multiplexing / epoll）

```
用户进程                    内核                        硬件(网卡)
   │                          │                              │
   │  epoll_ctl(ADD, fd1)      │                              │
   │  epoll_ctl(ADD, fd2) ───►│  注册 fd1、fd2 到红黑树       │
   │  epoll_ctl(ADD, fd3)      │                              │
   │                          │                              │
   │  epoll_wait() ──────────►│  没有 fd 就绪                 │
   │  ⏸️ 阻塞（线程挂起）      │  线程进入等待队列             │
   │                          │                              │
   │                          │      fd2 收到数据包           │
   │                          │◄─────────────────────────────┤ DMA→内核buf
   │                          │  硬件中断触发                 │
   │                          │  回调把 fd2 加入就绪链表       │
   │                          │  唤醒等待的线程               │
   │◄─────────────────────────┤                              │
   │  epoll_wait() 返回        │  返回就绪列表：[fd2]          │
   │                          │                              │
   │  read(fd2) ─────────────►│  内核 buf → 用户 buf          │
   │◄─────────────────────────┤  (CPU 参与拷贝)              │
   │  处理 fd2 的数据 ✅        │                              │

阶段① ████████████████████ 阻塞（但 1个线程可同时等 N 个 fd！）
阶段② ████ 阻塞

关键优势：
  1000个连接只需 1 个线程监控
  epoll_wait 只返回就绪的 fd，不遍历所有
```

**特点**：阶段① 仍阻塞，但 **1个线程监控N个连接**，有事件才唤醒处理，是 Netty/Redis/Nginx 的底层。
Java 对应：`java.nio.channels.Selector`

---

### 模型④ 信号驱动 IO（Signal-driven IO / SIGIO）

```
用户进程                    内核                        硬件(网卡)
   │                          │                              │
   │  sigaction(SIGIO, handler)│                              │
   │  fcntl(fd, F_SETOWN)  ──►│  注册信号处理函数             │
   │                          │  记录：fd 就绪时发 SIGIO       │
   │                          │                              │
   │  ✅ 继续干别的事           │                              │
   │  （完全不阻塞！）          │                              │
   │                          │         网卡收到数据包        │
   │  干别的...                │◄─────────────────────────────┤ DMA→内核buf
   │                          │  数据就绪！                   │
   │     ← ─ SIGIO 信号打断 ─ ─┤  向进程发送 SIGIO 信号       │
   │  signal_handler() 被调用  │                              │
   │                          │                              │
   │  read(fd) ──────────────►│  内核 buf → 用户 buf          │
   │◄─────────────────────────┤  (CPU 参与拷贝)              │
   │  处理数据 ✅               │                              │

阶段① ░░░░░░░░░░░░░░░░░░░░ 不阻塞（注册后立刻返回，被信号打断）
阶段② ████ 阻塞

问题：
  - 每个 fd 独立信号，高并发 → 信号风暴，信号可能丢失
  - 信号处理函数限制极多（只能调 async-signal-safe 函数）
  - JVM 自身大量使用 UNIX 信号，会冲突 → Java 无此 API
```

**特点**：阶段① 真正不阻塞，被信号**打断**来处理，但高并发下不可靠，实际工程几乎不用。
Java：**无对应 API**

---

### 模型⑤ 异步 IO（AIO / Asynchronous IO）

```
用户进程                    内核                        硬件(网卡)
   │                          │                              │
   │  aio_read(fd, buf,        │                              │
   │           callback) ────►│  注册异步请求 + 回调函数      │
   │◄─ 立刻返回                │                              │
   │                          │                              │
   │  ✅ 继续干别的事           │                              │
   │  （完全不阻塞！）          │                              │
   │                          │         网卡收到数据包        │
   │  干别的...                │◄─────────────────────────────┤ DMA→内核buf
   │                          │  数据就绪！                   │
   │  干别的...                │  内核 buf ──────────────────►│
   │                          │  ↓ 内核直接拷贝到用户 buf      │
   │                          │  （无需进程参与！）            │
   │                          │                              │
   │     ← ─ callback 回调 ─ ─┤  拷贝完成，触发回调          │
   │  处理数据 ✅               │  此时数据已经在用户 buf 里了  │

阶段① ░░░░░░░░░░░░░░░░░░░░ 不阻塞
阶段② ░░░░░░░░░░░░░░░░░░░░ 不阻塞（内核替你做！）

收到回调时：数据已经在你的 buffer 里，不需要再 read()
```

**特点**：两个阶段全程非阻塞，内核替你完成数据拷贝后才通知你，是理论上最完美的模型。
Java 对应：`java.nio.channels.AsynchronousSocketChannel`（有 API 但实际少用，Linux 假异步）

---

### 五种模型横向对比

```
                     阶段①（等就绪）      阶段②（数据拷贝）    谁拷贝    实际应用
                     ────────────────    ────────────────    ──────    ────────
① 阻塞 IO             ████ 阻塞           ████ 阻塞           进程      java.io BIO
② 非阻塞 IO           ░░░░ 轮询忙等       ████ 阻塞           进程      极少直接用
③ IO 多路复用         ████ 阻塞           ████ 阻塞           进程      ✅ 主流 epoll
   （但1线程管N连接）                                                  Netty/Redis/Nginx
④ 信号驱动 IO         ░░░░ 不阻塞(信号)   ████ 阻塞           进程      ❌ 几乎不用
⑤ 异步 IO（AIO）      ░░░░ 不阻塞         ░░░░ 不阻塞         内核      ⚠️ 有API少用
                                                                      Linux假异步

████ = 进程/线程阻塞等待
░░░░ = 进程/线程不阻塞，可以干其他事
```

### 关键区分点一句话总结

| 问题 | 答案 |
|------|------|
| ①②③④ 和 ⑤ 的本质区别？ | ①②③④ 阶段② 都要进程自己拷贝，⑤ 是内核替你拷贝完再通知 |
| ③ 和 ① 的区别？ | ③ 一个线程可以同时等 N 个 fd，① 一个线程只能等一个 |
| ③ 和 ④ 的区别？ | ③ 阶段① 仍阻塞（线程挂起等），④ 阶段① 不阻塞（注册完去干别的） |
| ② 和 ③ 的区别？ | ② 进程主动反复问（忙等），③ 进程睡觉等内核叫（事件驱动） |

---

## Q1：BIO 演示时，客户端2能 connect() 成功，但发消息收不到回复，这是为什么？

**关键：TCP 连接建立分两个阶段，两个阶段由不同层负责。**

### 阶段一：OS 内核层（Java 代码完全不参与）

```
客户端2                   OS 内核                    服务端 Java 进程
   │                        │                               │
   │── SYN ─────────────────►│                               │
   │◄─ SYN-ACK ─────────────│  内核自动回复（不需要Java）    │
   │── ACK ─────────────────►│                               │
   │                        │← TCP握手完成，连接入 backlog 队列
   │ connect() 返回成功      │  Java 的 accept() 还没调用   │
```

### 阶段二：Java 应用层

```
服务端主线程：accept() → 取出客户端1 → readLine() 阻塞 ← 卡死在这里
                                                  ↑
                                    accept() 不再被调用！
                                    客户端2 排在队列里，没人取它
```

**结果：**
- 客户端2 `connect()` 成功 ✅（OS 层完成握手）
- 客户端2 发消息到达服务端内核缓冲区 ✅
- 客户端2 `readLine()` 等待 Echo → 永远阻塞 ❌（服务端没有线程处理它）

### 为什么设 backlog=1 也没用？

macOS (BSD 内核) 会自动将 backlog 扩大（通常 ×1.5，最小保证 8），所以设成 1 实际当 8 用，客户端的 `connect()` 依然成功。backlog 队列真正满的场景是：服务端假死 + 大量客户端并发（比如同时 100 个连接，队列只有 50），第 51 个才会超时失败。

**BIO 真正的问题不是「连不进来」，而是「连进来了，但服务端没有线程去处理它」。**

---

## Q2：模型③ 和模型④ 本质有什么区别？看起来都是收到通知然后去搬运数据

**关键差别在阶段1是否阻塞：**

| 维度 | 模型③ IO多路复用 | 模型④ 信号驱动IO |
|------|----------------|----------------|
| 阶段1（等待就绪） | **阻塞**（select/epoll 挂起线程） | **不阻塞**（注册后直接去干别的） |
| 阶段2（数据拷贝） | 阻塞，自己 read | 阻塞，自己 read |
| 通知方式 | 内核唤醒挂起的线程 | 内核发 SIGIO 信号打断线程 |
| 等待期间线程状态 | 线程挂起 | 线程可以干其他事 |

**类比：**
- 模型③：你去前台坐着等，哪个外卖到了前台叫你，你再自己下楼取
- 模型④：你把手机号留给餐厅就走了，手机一响再去取

**一句话：** 模型③ 阶段1 仍然阻塞，模型④ 阶段1 真正不阻塞。

模型④ 实际几乎不用：高并发信号会丢失；信号处理函数限制极多；JVM 自身使用 UNIX 信号会冲突。

---

## Q3：模型③ 和模型④ 在 Java 里分别对应什么 API？

| IO模型 | Java API | 实际使用 |
|--------|----------|---------|
| ① 阻塞IO | `java.io`（InputStream/OutputStream） | BIO，简单场景 |
| ② 非阻塞IO | `channel.configureBlocking(false)` + 手动轮询 | 很少直接用 |
| ③ IO多路复用 | `java.nio.channels.Selector` | ✅ 主流，Netty 底层 |
| ④ 信号驱动IO | **无** | ❌ Java 标准库不支持 |
| ⑤ 异步IO | `java.nio.channels.AsynchronousSocketChannel` | 有 API 但实际也少用 |

**模型④ Java 没有 API 的原因：**
- 信号驱动 IO 是 Linux SIGIO 机制，JVM 自身大量使用 UNIX 信号（SIGTERM/SIGQUIT），空间冲突
- 信号处理函数有严格限制（async-signal-safe），JVM 内几乎无法安全实现

---

## Q4：模型③ 和模型⑤ 的区别？AIO 为什么没成为主流？

**两阶段对比：**

| IO模型 | 阶段1 | 阶段2 | 谁来拷贝 |
|--------|-------|-------|---------|
| ③ 多路复用 | 阻塞（epoll_wait） | 阻塞 | 你自己 |
| ⑤ 异步IO | 不阻塞 | 不阻塞 | 内核替你 |

- 模型③：前台叫你，你自己下楼搬（阶段2还是你做）
- 模型⑤：快递直接送到桌上才通知你，收到通知时数据已在 buffer 里

**一句话：** ③ 告诉你"可以去取了"，⑤ 告诉你"已经给你放好了"。

**AIO 没成为主流的 4 个原因：**

1. **Linux 实现是假异步（最核心）**
   Linux POSIX aio_read 底层是线程池模拟，并非真正内核异步，本质还是 epoll + 线程，没有性能优势。真正的 Linux 内核异步 IO（io_uring）要到 2019 年才出现，那时 Netty/epoll 已经统治多年。

2. **编程模型复杂，收益不明显**
   NIO 就绪后直接 `channel.read(buffer)` 处理即可；AIO 需要嵌套 CompletionHandler 回调，异常传播、超时控制、回调线程归属都难以控制。

3. **Netty + NIO 已经足够好**
   Netty 用 epoll + Reactor 线程池封装了 NIO 的复杂性，单机百万连接、代码清晰、生产验证多年（Kafka/RocketMQ/gRPC 全用它），AIO 在 Linux 上几乎没有额外收益。

4. **虚拟线程（Java 21）从另一个方向解决了问题**
   AIO 的动机是"BIO 一连接一线程太贵"，虚拟线程直接打破这个假设：阻塞的是虚拟线程（几 KB），平台线程不被占用，用同步代码写出接近 NIO 的性能，代码比 AIO 简单 100 倍。
   AIO 被两面夹击：性能侧被 NIO 持平，简洁侧被虚拟线程替代。

---

## Q5：多 Agent 和单 Agent 怎么选？决策规则是什么？

**核心判断两个问题：**
1. 子任务之间能不能同时做？（可并行性）
2. 子任务之间有没有共享资源冲突？（安全性）

| 情况 | 模式 | 原因 |
|------|------|------|
| 任务简单，一步搞定 | 单 Agent | 拆了反而慢 |
| 任务要按顺序做（B 依赖 A 的结果） | 单 Agent | 依赖关系必须串行 |
| 多个独立任务 + 写同一个文件 | 单 Agent | 文件冲突，必须串行写 |
| 多个独立任务 + 只读不写 | **多 Agent** | 并行探索，安全且快 |
| 多个独立任务 + 写不同文件 | **多 Agent** | 无冲突，可以并行 |

**一句话记住：只读可并行，写同一文件必须串行；独立任务用多 Agent，有依赖关系用单 Agent。**

实际案例中常见的混合模式：
- 探索/定位阶段（读文件找行号）→ 多 Agent 并行
- 写入阶段（修改同一文件）→ 单 Agent 串行

---

> 以下为 2026-05-25 追加

---

## Q6：epoll 的红黑树和就绪链表是什么数据结构？各自的作用是什么？

### 内核 eventpoll 结构体（简化）

```c
struct eventpoll {
    struct rb_root   rbr;       // 红黑树根节点，存"我关心哪些fd"
    struct list_head rdllist;   // 就绪链表，存"当前哪些fd有事件"
    wait_queue_head_t wq;       // 等待队列，存"谁在 epoll_wait 挂着"
};

// 红黑树每个节点 = 一个 epitem（fd档案）
struct epitem {
    struct rb_node   rbn;       // 红黑树节点（按 fd 大小排序）
    struct list_head rdllink;   // 就绪链表钩子
    struct epoll_filefd ffd;    // 对应的 fd
    __poll_t         event;     // 关心的事件（EPOLLIN/EPOLLOUT...）
    struct eventpoll *ep;       // 属于哪个 epoll 实例
};
```

### 红黑树（rb_root rbr）

- **作用**：记录所有"注册进来的 fd"（我关心谁）
- **操作**：
  - `epoll_ctl(ADD, fd)`  → O(log n) 插入节点
  - `epoll_ctl(DEL, fd)`  → O(log n) 删除节点
  - `epoll_ctl(MOD, fd)`  → O(log n) 修改节点
- **按 fd 值大小排序**，相同 fd 唯一
- **为什么用红黑树**：O(log n) 增删查，1000 个连接找某个 fd 只要 10 次比较

### 就绪链表（rdllist）

- **作用**：记录"当前有事件的 fd"（谁准备好了）
- **由谁写入**：`ep_poll_callback` 回调函数（硬件中断路径上调用）
- **由谁读取**：`epoll_wait` 返回时把 rdllist 内容复制到 events[] 数组
- **O(1) 操作**：链表头插/遍历，因为只需要遍历有事件的，不遍历全量

### 数据流向图

```
epoll_ctl(ADD, fd=6)
   → 创建 epitem（fd=6 的档案）
   → 插入红黑树（按 fd 排序）              红黑树
   → 在 fd=6 的 socket 上挂 ep_poll_callback   [fd=3]
                                                 /   \
                                              [fd=6] [fd=9]
                                               ...

--- 某时刻，网卡收到数据 ---

网卡DMA → 内核接收缓冲区 → 硬件中断
   → ep_poll_callback(fd=6)
   → 把 fd=6 的 epitem 加入就绪链表         就绪链表
   → 唤醒 epoll_wait 挂起的线程          [fd=6的epitem] → null

epoll_wait 返回
   → 把 rdllist 里的 epitem 转成 events[] 数组
   → 清空 rdllist（LT模式下保留未读完的）
   → 返回就绪数量 n
```

---

## Q7：epoll_ctl 注册 1000 个连接是什么意思？内核内部发生了什么？

### 代码对应

```java
// 每来一个新连接，调一次 epoll_ctl
SocketChannel clientCh = serverChannel.accept();     // fd=6
clientCh.register(selector, SelectionKey.OP_READ);   // epoll_ctl(ADD, fd=6, EPOLLIN)

// 1000 个连接就调了 1000 次 epoll_ctl(ADD)
```

### 内核内部每次 epoll_ctl(ADD, fd) 做的三件事

```
① 创建 epitem（fd=6 的档案）
   epitem.ffd  = fd=6
   epitem.event = EPOLLIN
   epitem.ep   = epfd=5（属于哪个 epoll 实例）

② 把 epitem 按 fd 值插入红黑树（O(log n)）
   红黑树变化：[fd=3] → [fd=3, fd=6] → [fd=3, fd=6, fd=7] → ...

③ 在 fd=6 对应的 socket 等待队列上挂一个钩子：
   fd=6 的 socket.wq → [ep_poll_callback, epfd=5]
   ↑ 这个钩子的作用：将来 fd=6 有数据时，
     内核自动调 ep_poll_callback，把 fd=6 的 epitem 加入就绪链表
```

### 注册 1000 个连接后的内核状态

```
epoll 实例（epfd=5）
红黑树（1000个节点，按 fd 值有序）：
  [fd=4] [fd=5] [fd=6] [fd=7] ... [fd=1003]

每个 socket（fd=4 ~ fd=1003）的等待队列：
  fd=4.wq → [ep_poll_callback]
  fd=5.wq → [ep_poll_callback]
  ...
  fd=1003.wq → [ep_poll_callback]

就绪链表：（空，没有数据到来时）
等待队列：[main线程]（main 线程在 epoll_wait 挂着）
```

**关键：注册后 main 线程只挂起一次（epoll_wait），等 1000 个 fd 中任何一个有事件都会唤醒它，而 select 是每次 epoll_wait 前要把 1000 个 fd 全部从用户态传到内核态，epoll 只传一次。**

---

## Q8：硬件中断是什么？CPU 怎么感知到数据写好了？

### 完整链路：从网线到唤醒线程

```
① 网卡从网线收到数据帧
   （电信号/光信号 → 网卡芯片解码 → 数据帧）

② 网卡 DMA 控制器把数据写入内核内存
   不需要 CPU 参与，硬件自动写入
   DMA 完成后，网卡产生一个硬件中断信号

③ CPU 的中断控制器（8259A/APIC）收到中断信号
   查 IDT（中断描述符表），找到对应的中断处理程序

④ CPU 打断当前正在执行的代码（哪怕在运行其他线程）
   保存当前线程的寄存器（上下文）
   跳转执行网卡的中断处理程序

⑤ 中断处理程序执行：
   - 处理网卡状态
   - 调用 TCP/IP 协议栈
   - 数据包进入 socket 的接收缓冲区（sk_rcvbuf）
   - 调用 ep_poll_callback（如果该 socket 注册进了 epoll）

⑥ ep_poll_callback 执行：
   - 把该 fd 的 epitem 加入就绪链表（rdllist）
   - 从 epoll 的等待队列（wq）找到挂起的线程（main 线程）
   - 调用 wake_up()，把 main 线程加回调度队列

⑦ OS 调度器在适当时机重新调度 main 线程
   main 线程在 epoll_wait 的挂起点继续执行
   epoll_wait 返回就绪数量

⑧ main 线程调用 read(fd) → 内核接收缓冲区 → ByteBuffer
```

### CPU 为什么能感知中断？

```
CPU 内部有一个引脚叫 INTR（Interrupt Request）
                                ┌───────────────────┐
  网卡 IRQ 信号 ──────────────►│  中断控制器 APIC   │
                                │  （决定发给哪个CPU）│
                                └─────────┬─────────┘
                                          │ 拉高 INTR 引脚
                                          ▼
                                ┌───────────────────┐
                                │       CPU         │
                                │  每条指令执行后    │
                                │  检查 INTR 引脚   │──► 有信号 → 查 IDT → 跳中断处理程序
                                └───────────────────┘
```

**CPU 每执行完一条指令就检查 INTR 引脚**，所以"感知"几乎是实时的（纳秒级）。

---

## Q9：epoll 三大系统调用各由哪个线程调用？结合 Java NIO 代码说明

### 对应关系总览

```
Java NIO 代码                     底层系统调用            执行线程      线程状态
──────────────────────────────────────────────────────────────────────────────
Selector.open()               →  epoll_create()          main 线程    RUNNABLE
serverChannel.register(OP_ACCEPT) → epoll_ctl(ADD,fd=3)  main 线程    RUNNABLE
clientCh.register(OP_READ)    →  epoll_ctl(ADD,fd=6)     main 线程    RUNNABLE（accept事件处理中）
key.cancel()+close()          →  epoll_ctl(DEL,fd)+close main 线程    RUNNABLE（read事件处理中）
selector.select()             →  epoll_wait(...)         main 线程    WAITING ★（挂起，0%CPU）
serverChannel.accept()        →  accept(fd=3)            main 线程    RUNNABLE（已被唤醒）
channel.read(buf)             →  read(fd,buf)            main 线程    RUNNABLE（已被唤醒）
channel.write(buf)            →  write(fd,buf)           main 线程    RUNNABLE（已被唤醒）
```

**关键结论：所有系统调用都由 main-IO 线程发起。没有"内核线程"来帮你调。**

### 容易混淆的点：ep_poll_callback 不是"你的线程"调的

```
ep_poll_callback 执行路径：
  网卡DMA完成 → 硬件中断 → 中断处理程序（内核代码）
  → ep_poll_callback（内核代码，借用当前CPU，不属于任何用户线程）
  → 把 fd 加入就绪链表
  → 唤醒 main 线程（只是把 main 线程加回调度队列）

main 线程并没有参与这一步，它在 WAITING 状态挂着。
```

### 线程状态时间轴（以 OrderQueryServer 为例）

```
时间轴                main 线程状态         发生了什么
────────────────────────────────────────────────────────────
T=0ms   RUNNABLE      socket/bind/listen/epoll_create/epoll_ctl(ADD,fd=3)
T=1ms   WAITING       epoll_wait(-1)：main 线程挂起，0% CPU
                                      （等待 epoll 就绪链表非空）

T=500ms             网卡收到客户端A连接请求，内核完成三次握手
                    ep_poll_callback → fd=3 加入就绪链表 → 唤醒 main

T=500ms RUNNABLE      epoll_wait 返回，readyCount=1
                      accept(fd=3) → 新 fd=6
                      epoll_ctl(ADD, fd=6, EPOLLIN)
T=501ms WAITING       epoll_wait(-1)：再次挂起

T=502ms             客户端A发来 HTTP 请求数据
                    DMA 写内核缓冲区 → 硬件中断 → ep_poll_callback
                    → fd=6 加入就绪链表 → 唤醒 main

T=502ms RUNNABLE      epoll_wait 返回，readyCount=1
                      read(fd=6, buf)：内核缓冲区 → ByteBuffer（内存拷贝）
                      业务处理（解析 HTTP，查询订单）
                      write(fd=6, resp)：ByteBuffer → 内核发送缓冲区
                      epoll_ctl(DEL, fd=6) + close(fd=6)
T=505ms WAITING       epoll_wait(-1)：再次挂起，等下一个连接
```

---

## Q10：为什么 accept() 和 read() 在 NIO 里不阻塞？

**一句话核心原理：epoll_wait 返回是一个前提承诺，承诺数据已经就绪。**

### 详细解释

```
selector.select() 返回
   = epoll_wait 确认：就绪链表非空
   = 已知：至少一个 fd 的事件满足条件

OP_ACCEPT 就绪 ← 意味着：
  fd=3（监听socket）的 Accept 队列非空
  → accept(fd=3) 直接从 Accept 队列取连接，队列里有就不等，立刻返回

OP_READ 就绪 ← 意味着：
  fd=6（客户端连接）的内核接收缓冲区有数据（DMA 已写完）
  → read(fd=6, buf) 直接 memcpy（内核buf → buf），有数据就不等，立刻返回
```

### 对比：BIO 的 read() 为什么阻塞？

```
BIO：
  read(fd=6, buf)  // 不管有没有数据，直接调
  → 内核检查接收缓冲区：没数据！
  → 把当前线程加入 fd=6 的 socket 等待队列
  → 调 schedule()，线程挂起
  → 等数据来了再唤醒

NIO + epoll：
  epoll_wait()  // 先等事件
  → fd=6 有数据了，唤醒线程
  read(fd=6, buf)  // 此时已确认有数据
  → 内核检查接收缓冲区：有数据！
  → 直接 memcpy，不挂起，立刻返回
```

**本质：NIO 把"等待"和"读取"分离了。等待在 epoll_wait，读取时只做搬运。**

---

## Q11：ByteBuffer.allocate 和 allocateDirect 的区别？read() 时各有几次拷贝？

| 方法 | 内存位置 | read() 拷贝路径 | 拷贝次数 |
|------|---------|----------------|---------|
| `allocate(n)` | JVM 堆内（GC管理） | 内核buf → 堆外临时buf → 堆内ByteBuffer | 2次 |
| `allocateDirect(n)` | 堆外（native memory，不被GC移动） | 内核buf → 堆外ByteBuffer | 1次 |

**为什么堆内需要2次：**
JVM GC 会移动堆内对象的地址，内核做 DMA 时地址必须固定，所以 JDK 内部会先把数据拷贝到地址固定的堆外临时缓冲区，再 memcpy 到堆内 ByteBuffer。

**Netty 用 `allocateDirect`（PooledDirectByteBuf）** 减少一次拷贝，这也是 Netty 性能优于直接用 JDK NIO 的原因之一。

