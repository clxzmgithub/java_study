# 计算机网络专题

网络协议栈核心知识，理解 Java 网络编程（Socket / NIO / HTTP）的底层基础。

---

## 核心知识点

| 层次 | 重点内容 |
|------|---------|
| **应用层** | HTTP/1.1 vs HTTP/2 vs HTTP/3，WebSocket，DNS |
| **传输层** | TCP 三次握手/四次挥手，流量控制，拥塞控制，UDP |
| **网络层** | IP 路由，NAT，子网划分 |
| **链路层** | MAC 地址，ARP 协议 |

---

## 与 Java 的关联

- TCP 粘包/拆包 → Netty FrameDecoder 的设计背景（详见 `io/nio/` Part18）
- TCP 流量控制/拥塞控制 → `transferTo` 为什么有时传不完（详见 `io/nio/` Part10）
- HTTP 协议 → JDK11+ `HttpClient` 用法（详见 `io/nio/` Part23）
- 长连接 / 心跳机制 → Netty IdleStateHandler（详见 `io/nio/` Part18）

建议在学完 `io/nio/` 第三层（NIO 网络IO）后，结合本专题加深理解。

