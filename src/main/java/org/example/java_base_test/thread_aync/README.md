# 多线程 & 异步编程专题

Java 并发编程核心知识，从线程池原理到 CompletableFuture 异步编排，再到 ThreadLocal 的内存泄漏细节。

---

## 学习顺序

```
ThreadPoolDemo → ThreadLocalDemo → FutureDemo → CrossThreadTransmitDemo
```

---

## 文件说明

| 文件 | 核心内容 |
|------|---------|
| `ThreadPoolDemo.java` | 线程池七大核心参数、五种生命周期状态、四种拒绝策略、常见线程池类型对比与生产最佳实践 |
| `ThreadLocalDemo.java` | ThreadLocal 底层原理（ThreadLocalMap）、内存泄漏根因分析（弱引用 key + 强引用 value）、正确使用规范 |
| `FutureDemo.java` | Callable/Future/FutureTask 基础用法，CompletableFuture 链式编排、异常处理、多任务组合（allOf/anyOf） |
| `CrossThreadTransmitDemo.java` | 父子线程 ThreadLocal 传递问题，InheritableThreadLocal 的局限性，TransmittableThreadLocal（TTL）原理 |

---

## 重点提示

- **ThreadLocal 内存泄漏**是高频面试题，`ThreadLocalDemo.java` 有详细分析，务必理解
- **线程池参数**（核心线程数怎么设、队列用哪种）在 `ThreadPoolDemo.java` 里有完整的选型指南
- `CompletableFuture` 是现代 Java 异步编程的主力，掌握后可以替代大部分手动 `Thread` + `Future` 的场景

