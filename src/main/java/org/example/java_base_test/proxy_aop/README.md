# 代理模式 & AOP 专题

从静态代理出发，逐步理解 JDK 动态代理、CGLIB 代理的底层原理，再到 Spring AOP 的工程实践。

---

## 学习顺序

```
ProxyFullDemo → AopDemo → AopAdviceTypesDemo → SpringAopScenariosDemo
```

---

## 文件说明

| 文件 | 核心内容 |
|------|---------|
| `ProxyFullDemo.java` | 代理模式完整演进：静态代理 → JDK动态代理（`InvocationHandler`）→ CGLIB动态代理 → Spring AOP，理解三者的底层差异与适用场景 |
| `AopDemo.java` | AOP 核心术语（切面/连接点/切点/通知/织入）详解，手写模拟 Spring AOP 多切面叠加的执行顺序 |
| `AopAdviceTypesDemo.java` | 五种通知类型完整对比：`@Before` / `@After` / `@Around` / `@AfterReturning` / `@AfterThrowing`，含数据脱敏实战案例 |
| `SpringAopScenariosDemo.java` | 五大真实生产场景：链路日志追踪 / `@Transactional` 事务 / 接口限流 / 幂等性校验 / 敏感数据脱敏 |

---

## 重点提示

- JDK 动态代理只能代理**接口**，CGLIB 通过继承可以代理**普通类** — 这是高频面试题
- Spring AOP 默认优先 JDK 代理（有接口时），`@EnableAspectJAutoProxy(proxyTargetClass=true)` 强制 CGLIB
- `@Around` 是最强大的通知类型，可以完全控制目标方法的执行，也最容易出 bug（忘记调用 `proceed()`）

