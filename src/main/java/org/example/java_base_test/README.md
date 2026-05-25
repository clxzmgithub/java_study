# Java 基础核心专题

Java 语言核心机制的深度演示，每个知识点配有完整代码示例和生活场景类比。

---

## 子模块

| 子模块 | 内容概要 |
|--------|---------|
| `io/` | BIO / NIO / 零拷贝 / Netty / Kafka，七层递进 |
| `thread_aync/` | 线程池 / ThreadLocal / CompletableFuture / 跨线程传递 |
| `proxy_aop/` | 静态代理 / JDK动态代理 / CGLIB / Spring AOP |
| `multi_agent/` | Orchestrator-Worker 多Agent架构演示 |

各子模块详细说明见其目录下的 README.md。

---

## 基础知识文件

| 文件 | 主题 |
|------|------|
| `FanxingDemo.java` / `GenericList.java` / `GenericMethod.java` | 泛型：泛型类/接口/方法/上界/类型擦除 |
| `ComparableVsComparatorDemo.java` | 排序：内置排序 vs 外部比较器，多维度排序实战 |
| `EqualsAndHashCodeDemo.java` / `HashCollisionExplained.java` | equals/hashCode 契约，HashMap 原理，哈希碰撞 |
| `PrimitiveVsWrapper.java` | 基本类型 vs 包装类型，自动装箱陷阱，Integer 缓存 |
| `FloatingPointPrecision.java` | 浮点数精度问题，BigDecimal 正确用法 |
| `TryWithResourcesDemo.java` / `ResourceManagementDemo.java` | 资源管理，try-with-resources，Suppressed 异常 |
| `InnerClassDemo.java` | 四种内部类：静态/成员/局部/匿名 |
| `StaticVsNonStatic.java` | 静态成员 vs 实例成员，内存模型角度分析 |
| `RuntimeClassObjectExplained.java` / `GetClassMethodDemo.java` | Class 对象，运行时类型，反射基础 |
| `NullBestPracticeExamples.java` / `DatabaseNullAnalysis.java` | Null 最佳实践，MySQL NULL 的三值逻辑陷阱 |
| `Container.java` / `NumberBox.java` / `Box.java` | 泛型边界 extends/super，泛型容器设计 |
| `BaseDao.java` / `AbstractBaseDao.java` / `UserDao.java` | 泛型在 DAO 层的应用，抽象模板模式 |
| `StaticVsNonStatic.java` | 静态成员 vs 实例成员，内存模型角度分析 |
| `ResourceManagementDemo.java` | 资源获取即初始化（RAII），与 try-with-resources 对照 |
| `HashCollisionExplained.java` | 哈希冲突产生原因与链表/红黑树解决方案 |

---

## 学习建议

先把基础知识文件过一遍，重点关注 `equals/hashCode`、`泛型`、`基本类型 vs 包装类型`，这三块是后续学集合框架和并发的基础。
然后按 `io/` → `thread_aync/` → `proxy_aop/` 的顺序学习各子模块。

