# RPC 框架源码研读

从「一次远程调用」出发，理解 Dubbo 与 gRPC 如何在 Netty 基础上构建完整的 RPC 框架，掌握微服务通信层的工业级实现。

> **前置建议**：先完成 NIODemo Part17~Part19（Netty 框架）和 Java 代理模式（`proxy_aop/`），再读本模块源码，印证效果最好。

---

## 推荐阅读路线

```
【Dubbo】
ServiceConfig.export()（服务导出，理解服务注册全链路）
    ↓
ReferenceConfig.get()（服务引用，理解动态代理生成 Stub）
    ↓
NettyServer / NettyClient（网络层，Netty 封装）
    ↓
DubboCodec（协议编解码，自定义二进制协议）
    ↓
LoadBalance SPI（负载均衡，SPI 扩展机制）
    ↓
Filter Chain（过滤器链，责任链模式）

【gRPC】
ManagedChannelBuilder.forTarget()（Channel 创建）
    ↓
ClientCall / ServerCall（调用抽象层）
    ↓
NettyClientTransport / NettyServerTransport（HTTP/2 传输层）
    ↓
Marshaller / ProtoLiteUtils（Protobuf 序列化）
    ↓
NameResolver / LoadBalancer（服务发现与负载均衡）
```

---

## Dubbo 核心模块详解

### 🚀 服务导出（最先读）

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ServiceConfig` | `export()` / `doExport()` / `doExportUrls()` | 一个 `@DubboService` 注解最终是如何变成 Netty Server 监听端口的？ |
| `RegistryProtocol` | `export()` → 注册到 Zookeeper/Nacos | 服务注册和服务暴露是同一个动作吗？ |
| `DubboProtocol` | `export()` / `openServer()` → `NettyServer` | 为什么同一个 JVM 内多个服务只启动一个 Netty Server？ |
| `Invoker` | `invoke(Invocation)` / `AbstractInvoker` | Invoker 是什么？为什么 Dubbo 一切皆 Invoker？ |

**入口**：`ServiceConfig.export()` 打断点，跟着调用栈走一遍，基本能看清服务导出的完整树状结构。

---

### 📞 服务引用

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ReferenceConfig` | `get()` / `createProxy()` | 客户端 `@DubboReference` 注入的对象究竟是什么？ |
| `InvokerInvocationHandler` | `invoke()` → `MockClusterInvoker` → `DubboInvoker` | 一次方法调用如何穿越代理层、集群层、协议层？ |
| `JdkProxyFactory` | `getProxy()` / `getInvoker()` | Dubbo 为什么默认用 JDK 动态代理，何时切换 Javassist？ |

**对照已学**：`proxy_aop/` JDK 动态代理专题——Dubbo 的 Stub 就是动态代理的工业级应用。

---

### 🌐 网络层

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `NettyServer` | Boss/Worker 线程模型 / `NettyServerHandler` | Dubbo 的 IO 线程与业务线程如何分离？ |
| `NettyChannel` | `send` / `receive` / 属性绑定 | Dubbo Channel 与 Netty Channel 是什么关系？ |
| `DubboCodec` | `encodeRequest` / `decodeBody` / 协议头16字节 | Dubbo 自定义协议的魔数、序列化类型、数据长度各在哪几位？ |
| `AllChannelHandler` | 将 IO 事件分发到业务线程池 | 为什么需要 AllChannelHandler？直接在 IO 线程处理有什么问题？ |

**对照 NIODemo**：Part17~Part19 Netty Pipeline、编解码、粘包拆包。

---

### 🔌 SPI 与扩展机制

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ExtensionLoader` | `getExtension` / `getAdaptiveExtension` / AOP 包装 | Dubbo SPI 与 JDK SPI 的核心区别是什么？ |
| `LoadBalance` | `RandomLoadBalance` / `RoundRobinLoadBalance` / `ConsistentHashLoadBalance` | 一致性哈希负载均衡如何实现虚拟节点？ |
| `Filter` | 责任链 / `MonitorFilter` / `TimeoutFilter` | Dubbo Filter 链与 Netty Pipeline 有什么异同？ |

**对照已学**：`designmodel/`（责任链模式、策略模式）——Filter 链 = 责任链，LoadBalance = 策略模式。

---

## gRPC 核心模块详解

### 📡 调用全链路

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ManagedChannel` | `newCall` / `Channel` 抽象 | Channel 是连接池还是单连接？ |
| `ClientCallImpl` | `start` / `sendMessage` / `halfClose` | 一次 Unary RPC 调用经历了哪些状态流转？ |
| `NettyClientTransport` | HTTP/2 `HEADERS` + `DATA` 帧 / Stream 多路复用 | gRPC 如何在一条 TCP 连接上并发多个 RPC 请求？ |
| `ServerCallHandler` | 请求分发 / `MethodDescriptor` 路由 | 服务端如何根据方法名路由到对应 Handler？ |

---

### 📦 序列化

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `ProtoLiteUtils` | `marshaller()` / `MessageLite.toByteArray()` | Protobuf 为什么比 JSON 快？varint 编码的原理？ |
| `MethodDescriptor` | `Marshaller<ReqT>` / `Marshaller<RespT>` | 请求和响应各自独立序列化，如何保证类型安全？ |

---

### 🔍 服务发现与负载均衡

| 类 | 阅读重点 | 关键问题 |
|----|---------|---------|
| `NameResolver` | `resolve()` / 推送地址列表 | gRPC 客户端如何感知服务地址变更？ |
| `LoadBalancer` | `RoundRobinLoadBalancer` / `SubChannel` 状态机 | 为什么 gRPC 的负载均衡是客户端侧的？ |

---

## Dubbo vs gRPC 横向对比

| 维度 | Dubbo | gRPC |
|------|-------|------|
| 传输协议 | 自定义 Dubbo 协议 / Triple（HTTP/2） | HTTP/2 |
| 序列化 | Hessian2 / JSON / Protobuf 可选 | Protobuf（强制） |
| 服务发现 | Zookeeper / Nacos / Redis 等（SPI） | DNS / xDS / 自定义 NameResolver |
| 负载均衡 | 客户端侧（SPI 可扩展） | 客户端侧（插件化） |
| 语言支持 | 主要是 Java | 多语言（Java/Go/Python/C++ 等） |
| 生态 | 阿里系，与 Spring Cloud 深度整合 | Google 开源，云原生 / gRPC-Web |

---

## 与已学知识的关联

| RPC 源码 | 已学模块 |
|---------|---------|
| Dubbo 动态代理 Stub | `proxy_aop/` JDK 动态代理 |
| `DubboCodec` 编解码 | NIODemo Part18 Netty FrameDecoder |
| `NettyServer` / `NettyClient` | NIODemo Part17~Part19 Netty 架构 |
| `Filter` 责任链 | `designmodel/` 责任链模式 |
| `LoadBalance` 策略 | `designmodel/` 策略模式 |
| gRPC HTTP/2 多路复用 | `compute_network/`（传输层 / HTTP/2） |

