# IO / NIO 专题

Java IO 体系全面学习，从最基础的 BIO 阻塞模型出发，逐层深入到 NIO、零拷贝、Netty 框架、Kafka/RocketMQ 开源实践，直到 JDK21 虚拟线程。

---

## 学习顺序

```
bio/  →  nio/NIODemo.java（Part1~Part23）  →  zerocopy/
```

### bio/ — BIO 阻塞模型
理解 BIO 的根本缺陷是学习 NIO 的前提。

| 文件 | 内容 |
|------|------|
| `BIOServer_NoThread.java` | 单线程阻塞服务端，演示一个连接如何卡死后续所有请求 |
| `BIOServer_WithThread.java` | 每连接一线程模式，解决阻塞问题但引入线程爆炸风险 |
| `BIOClient.java` | 配套客户端 |
| `BIODemo.java` | 运行引导说明 |

**运行方式：** 先启动 Server，再运行 Client，对比两个 Server 的行为差异。

### nio/ — NIO 全体系（核心）
按 **Part1 → Part23** 七层递进，覆盖 NIO 全部核心知识点。

| 层级 | Part | 核心内容 |
|------|------|---------|
| 第一层 | Part1 ~ Part3 | 五种IO模型 / BIO问题 / BufferedInputStream提速 |
| 第二层 | Part4 ~ Part5 | Buffer三指针状态机 / Channel类型体系 |
| 第三层 | Part6 ~ Part13 | Selector+epoll / SelectionKey / 零拷贝 / 选型指南 |
| 第四层 | Part14 ~ Part16 | FileChannel / MappedByteBuffer(mmap) / NIO.2 |
| 第五层 | Part17 ~ Part19 | Netty架构 / 粘包拆包 / 实战 |
| 第六层 | Part20 ~ Part21 | Kafka IO体系 / 四大框架横向对比 |
| 第七层 | Part22 ~ Part23 | NIO.2进阶 / 虚拟线程(JDK21) |

两个版本：
- `NIODemo.java` — 单文件完整版，适合全局阅读
- `show_multi_agent/NIODemoMain.java` — 多Agent重构版，每个Part独立文件，适合按知识点单独查阅

### zerocopy/ — 零拷贝专题
`ZeroCopyDemo.java`：传统拷贝 vs `transferTo` vs `MappedByteBuffer` 三种方式的性能对比与适用场景分析。建议在学完 Part9~Part15 后再看。

