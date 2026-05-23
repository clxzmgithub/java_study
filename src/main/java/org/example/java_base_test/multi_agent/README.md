# 多 Agent 系统专题

用 Java 代码演示 AI 多 Agent 协作架构，理解 Orchestrator-Worker 模式的设计思想与工程实现。

---

## 文件说明

| 文件 | 内容 |
|------|------|
| `MultiAgentDemo.java` | 多 Agent 学习辅助系统完整演示，含 4 个场景 |

---

## 4 个演示场景

| 场景 | 内容 |
|------|------|
| 演示1 | 单知识点 → 三个 Worker Agent 并行处理（ExampleAgent / QuizAgent / SummaryAgent） |
| 演示2 | 多知识点 → AgentPool 批量并行，多个 Orchestrator 同时工作 |
| 演示3 | 带优先级的任务调度（`PriorityBlockingQueue`，HIGH 优先） |
| 演示4 | Agent 故障容错与指数退避自动重试（60% 故障率模拟） |

---

## 架构设计

```
OrchestratorAgent（主 Agent，不干活只调度）
    ├── ExampleAgent  → 生成代码示例
    ├── QuizAgent     → 生成测验题目
    └── SummaryAgent  → 生成知识总结

三个 Worker 通过 CompletableFuture 并行执行，allOf().join() 汇聚结果
```

核心设计模式：**Agent 接口统一契约 + 消息驱动解耦 + CompletableFuture 并发 + 优先级队列调度 + 指数退避容错**

---

## 扩展思路

真实 AI 多 Agent 场景中，只需将 `ExampleAgent.execute()` 等方法内的模拟逻辑替换为对 LLM API（如 OpenAI / Claude）的 HTTP 调用，整体架构无需改变。

