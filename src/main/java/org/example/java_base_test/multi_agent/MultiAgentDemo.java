package org.example.java_base_test.multi_agent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * 多 Agent 协作系统演示（Orchestrator-Worker 模式）
 * ============================================================
 *
 * 本文件演示如何用 Java 构建一个多 Agent 学习辅助系统：
 *
 * 系统架构：
 *
 *                  ┌─────────────────────────┐
 *                  │   OrchestratorAgent      │  ← 主Agent：接收任务，分析知识点，
 *                  │   （任务调度中枢）         │    拆分成子任务，分发给Worker
 *                  └────────────┬────────────┘
 *                               │ 并行分发
 *              ┌────────────────┼────────────────┐
 *              ▼                ▼                ▼
 *   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
 *   │ ExampleAgent │  │  QuizAgent   │  │ SummaryAgent │
 *   │（生成代码示例）│  │（生成测验题）  │  │（生成总结）   │
 *   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
 *           │                │                  │
 *           └────────────────┴──────────────────┘
 *                               │ 结果汇聚
 *                  ┌────────────▼────────────┐
 *                  │      结果聚合 & 输出      │
 *                  └─────────────────────────┘
 *
 * 核心设计模式：
 *  1. Agent 接口（统一契约）：所有 Agent 实现同一接口
 *  2. 消息驱动（AgentMessage）：Agent 之间通过消息对象通信，解耦
 *  3. 并发执行（ExecutorService）：Worker Agent 并行工作，提升效率
 *  4. Future 汇聚（CompletableFuture）：主 Agent 等待所有子 Agent 完成后汇总
 *
 * 生活类比：
 *  就像一个教学团队备课：
 *  - 教研主任（Orchestrator）分析这节课的知识点，然后安排：
 *  - 讲师A（ExampleAgent）    去准备「代码示例」
 *  - 讲师B（QuizAgent）       去准备「课后测验题」
 *  - 讲师C（SummaryAgent）    去准备「知识总结卡片」
 *  三人同时工作（并行），最后汇总成一份完整的教学材料。
 *
 * ============================================================
 */
public class MultiAgentDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          多 Agent 学习辅助系统演示（Orchestrator-Worker）       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // ── 演示1：单个知识点的多 Agent 协作 ────────────────────────────
        System.out.println("【演示1】单个知识点 → 三个 Worker Agent 并行处理");
        System.out.println("─────────────────────────────────────────────────────────────");
        runSingleTopicDemo();
        System.out.println();

        // ── 演示2：多知识点批量处理（主 Agent 任务队列 + 子 Agent 池）────
        System.out.println("【演示2】多个知识点 → Orchestrator 任务队列 + Agent 池并行处理");
        System.out.println("─────────────────────────────────────────────────────────────");
        runBatchTopicsDemo();
        System.out.println();

        // ── 演示3：带优先级的任务调度 ───────────────────────────────────
        System.out.println("【演示3】带优先级的任务调度（高优先级知识点优先处理）");
        System.out.println("─────────────────────────────────────────────────────────────");
        runPrioritySchedulingDemo();
        System.out.println();

        // ── 演示4：Agent 失败重试与容错 ─────────────────────────────────
        System.out.println("【演示4】Agent 失败处理与自动重试机制");
        System.out.println("─────────────────────────────────────────────────────────────");
        runFaultToleranceDemo();

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    全部演示完毕                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    // ────────────────────────────────────────────────────────────────────
    // 演示1：单个知识点，三 Agent 并行
    // ────────────────────────────────────────────────────────────────────
    static void runSingleTopicDemo() throws Exception {
        // 1. 构建知识点任务
        LearningTask task = new LearningTask(
            "NIO-Buffer",
            "Java NIO Buffer 三指针状态机（position/limit/capacity）及 flip/clear/compact 操作",
            LearningTask.Priority.HIGH
        );
        System.out.println("📚 知识点：" + task.getTopic());
        System.out.println("   描述：" + task.getDescription());
        System.out.println();

        // 2. 启动 Orchestrator，它内部会并行调度三个 Worker
        OrchestratorAgent orchestrator = new OrchestratorAgent("主教研员-1");
        LearningResult result = orchestrator.process(task);

        // 3. 输出汇总结果
        System.out.println(result.toFormattedString());
    }

    // ────────────────────────────────────────────────────────────────────
    // 演示2：多个知识点批量处理
    // ────────────────────────────────────────────────────────────────────
    static void runBatchTopicsDemo() throws Exception {
        List<LearningTask> tasks = Arrays.asList(
            new LearningTask("NIO-Selector",
                "Selector + epoll 工作原理：一个线程监控 N 个连接",
                LearningTask.Priority.HIGH),
            new LearningTask("NIO-ZeroCopy",
                "零拷贝：transferTo 从 Java 到内核的调用链，DMA 传输原理",
                LearningTask.Priority.MEDIUM),
            new LearningTask("Netty-Pipeline",
                "Netty ChannelPipeline：Handler 链式处理，入站/出站事件传播",
                LearningTask.Priority.MEDIUM)
        );

        System.out.println("📚 批量处理 " + tasks.size() + " 个知识点（并行）：");
        for (LearningTask t : tasks) {
            System.out.println("   · [" + t.getPriority() + "] " + t.getTopic());
        }
        System.out.println();

        // 使用 AgentPool 并行处理所有任务
        AgentPool pool = new AgentPool(3); // 3个 Orchestrator 并行
        List<LearningResult> results = pool.processAll(tasks);
        pool.shutdown();

        System.out.println("✅ 批量处理完成，共生成 " + results.size() + " 份学习材料：");
        for (LearningResult r : results) {
            System.out.println("   · 知识点「" + r.getTopic() + "」→ 耗时 " + r.getElapsedMs() + "ms");
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // 演示3：带优先级的任务调度
    // ────────────────────────────────────────────────────────────────────
    static void runPrioritySchedulingDemo() throws Exception {
        PriorityOrchestrator priorityOrch = new PriorityOrchestrator("优先级调度器");

        // 提交不同优先级的任务（故意乱序）
        priorityOrch.submit(new LearningTask("Kafka-SendFile",   "Kafka sendfile 零拷贝消费", LearningTask.Priority.LOW));
        priorityOrch.submit(new LearningTask("NIO-SocketChannel","SocketChannel 非阻塞连接建立", LearningTask.Priority.HIGH));
        priorityOrch.submit(new LearningTask("BIO-ThreadPool",   "BIO 线程池模式改进方案",    LearningTask.Priority.MEDIUM));
        priorityOrch.submit(new LearningTask("Netty-ByteBuf",    "Netty ByteBuf 零拷贝切片",  LearningTask.Priority.HIGH));

        System.out.println("📋 提交 4 个任务（乱序提交），按优先级处理：");
        List<LearningResult> results = priorityOrch.processAll();
        priorityOrch.shutdown();

        System.out.println("\n处理顺序（按优先级排序）：");
        for (int i = 0; i < results.size(); i++) {
            LearningResult r = results.get(i);
            System.out.println("  第" + (i + 1) + "个完成：[" + r.getPriority() + "] " + r.getTopic());
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // 演示4：故障容错与重试
    // ────────────────────────────────────────────────────────────────────
    static void runFaultToleranceDemo() throws Exception {
        // 创建一个会随机失败的"不稳定" Agent（模拟真实 AI Agent 偶尔超时）
        UnstableWorkerAgent unstableAgent = new UnstableWorkerAgent("不稳定-ExampleAgent", 0.6); // 60%概率失败
        ResilientOrchestrator resilientOrch = new ResilientOrchestrator("容错调度器", unstableAgent, 3);

        LearningTask task = new LearningTask("MappedByteBuffer",
            "MappedByteBuffer 内存映射文件原理及 RocketMQ CommitLog 实践",
            LearningTask.Priority.HIGH);

        System.out.println("📚 使用容错 Orchestrator 处理知识点：" + task.getTopic());
        System.out.println("   Worker Agent 故障率：60%（会自动重试，最多3次）");
        System.out.println();

        LearningResult result = resilientOrch.processWithRetry(task);
        System.out.println("✅ 最终结果（经过 " + result.getRetryCount() + " 次重试）：");
        System.out.println("   " + result.getSummary());
    }
}


// ====================================================================
// 一、消息协议层：Agent 之间的通信数据结构
// ====================================================================

/**
 * 学习任务（输入消息）
 *
 * 类比：教研主任发给讲师的「备课任务单」
 */
class LearningTask {

    enum Priority { HIGH, MEDIUM, LOW }

    private final String topic;           // 知识点名称（如 "NIO-Buffer"）
    private final String description;     // 知识点详细描述
    private final Priority priority;      // 优先级
    private final long createdAt;         // 创建时间

    LearningTask(String topic, String description, Priority priority) {
        this.topic = topic;
        this.description = description;
        this.priority = priority;
        this.createdAt = System.currentTimeMillis();
    }

    String getTopic()       { return topic; }
    String getDescription() { return description; }
    Priority getPriority()  { return priority; }
    long getCreatedAt()     { return createdAt; }

    @Override
    public String toString() {
        return "[" + priority + "] " + topic;
    }
}

/**
 * 子任务（Orchestrator 拆分后分发给 Worker 的任务）
 *
 * 类比：教研主任告诉某位讲师「你负责准备XX知识点的YY内容」
 */
class SubTask {

    enum Type { EXAMPLE, QUIZ, SUMMARY }

    private final String parentTopic;     // 来自哪个知识点
    private final String description;     // 知识点描述
    private final Type type;              // 子任务类型
    private final int taskId;             // 唯一编号

    private static final AtomicInteger ID_GEN = new AtomicInteger(1000);

    SubTask(String parentTopic, String description, Type type) {
        this.parentTopic = parentTopic;
        this.description = description;
        this.type = type;
        this.taskId = ID_GEN.getAndIncrement();
    }

    String getParentTopic() { return parentTopic; }
    String getDescription() { return description; }
    Type getType()          { return type; }
    int getTaskId()         { return taskId; }
}

/**
 * 子任务结果（Worker 返回给 Orchestrator 的产出）
 *
 * 类比：讲师完成备课后提交的「材料文档」
 */
class SubTaskResult {

    private final int taskId;
    private final SubTask.Type type;
    private final String content;         // 生成的内容（示例/题目/总结）
    private final String agentName;       // 是哪个 Agent 生成的
    private final long elapsedMs;         // 耗时

    SubTaskResult(int taskId, SubTask.Type type, String content, String agentName, long elapsedMs) {
        this.taskId = taskId;
        this.type = type;
        this.content = content;
        this.agentName = agentName;
        this.elapsedMs = elapsedMs;
    }

    int getTaskId()       { return taskId; }
    SubTask.Type getType(){ return type; }
    String getContent()   { return content; }
    String getAgentName() { return agentName; }
    long getElapsedMs()   { return elapsedMs; }
}

/**
 * 最终学习结果（Orchestrator 汇总后的完整输出）
 */
class LearningResult {

    private final String topic;
    private final String example;
    private final String quiz;
    private final String summary;
    private final long elapsedMs;
    private final LearningTask.Priority priority;
    private int retryCount = 0;

    LearningResult(String topic, String example, String quiz, String summary,
                   long elapsedMs, LearningTask.Priority priority) {
        this.topic = topic;
        this.example = example;
        this.quiz = quiz;
        this.summary = summary;
        this.elapsedMs = elapsedMs;
        this.priority = priority;
    }

    String getTopic()    { return topic; }
    String getExample()  { return example; }
    String getQuiz()     { return quiz; }
    String getSummary()  { return summary; }
    long getElapsedMs()  { return elapsedMs; }
    LearningTask.Priority getPriority() { return priority; }
    int getRetryCount()  { return retryCount; }
    void setRetryCount(int c) { this.retryCount = c; }

    String toFormattedString() {
        return "┌── 📖 知识点：" + topic + " ──────────────────────────────────\n" +
               "│\n" +
               "│  【💻 代码示例 · ExampleAgent 生成】\n" +
               formatBlock(example) +
               "│\n" +
               "│  【📝 测验题目 · QuizAgent 生成】\n" +
               formatBlock(quiz) +
               "│\n" +
               "│  【📌 知识总结 · SummaryAgent 生成】\n" +
               formatBlock(summary) +
               "│\n" +
               "└── ⏱ 总耗时（三 Agent 并行）：" + elapsedMs + "ms ──────────────\n";
    }

    private String formatBlock(String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            sb.append("│    ").append(line).append("\n");
        }
        return sb.toString();
    }
}


// ====================================================================
// 二、Agent 接口层：统一契约
// ====================================================================

/**
 * Agent 基础接口 —— 所有 Agent 必须实现
 *
 * 类比：所有讲师都有统一的「职责契约」：
 *   - 有名字（getName）
 *   - 能接受任务（接受 SubTask）
 *   - 能汇报状态（getStatus）
 */
interface Agent {
    String getName();
    AgentStatus getStatus();
}

/**
 * Worker Agent 接口 —— 负责执行具体子任务
 */
interface WorkerAgent extends Agent {
    SubTaskResult execute(SubTask subTask) throws Exception;
}

/**
 * Orchestrator Agent 接口 —— 负责分解、调度、汇聚
 */
interface OrchestratorAgentInterface extends Agent {
    LearningResult process(LearningTask task) throws Exception;
}

/** Agent 运行状态枚举 */
enum AgentStatus {
    IDLE("空闲"),
    WORKING("工作中"),
    COMPLETED("已完成"),
    FAILED("失败");

    private final String label;
    AgentStatus(String label) { this.label = label; }

    @Override public String toString() { return label; }
}


// ====================================================================
// 三、主 Agent：OrchestratorAgent（任务调度中枢）
// ====================================================================

/**
 * OrchestratorAgent —— 多 Agent 系统的「大脑」
 *
 * 职责：
 *  1. 接收一个 LearningTask（知识点）
 *  2. 拆分成三个 SubTask（示例/测验/总结）
 *  3. 并行分发给三个 WorkerAgent
 *  4. 等待全部完成后，汇聚结果 → LearningResult
 *
 * 类比：教研主任 —— 不自己写材料，而是安排讲师们并行备课，最后汇总
 */
class OrchestratorAgent implements OrchestratorAgentInterface {

    private final String name;
    private volatile AgentStatus status = AgentStatus.IDLE;

    // 三个 Worker Agent（可替换为真实 AI 调用）
    private final WorkerAgent exampleAgent;
    private final WorkerAgent quizAgent;
    private final WorkerAgent summaryAgent;

    // 线程池：并行执行 3 个 Worker
    private final ExecutorService executor;

    OrchestratorAgent(String name) {
        this.name = name;
        this.exampleAgent = new ExampleAgent("示例讲师-" + name);
        this.quizAgent    = new QuizAgent("测验讲师-" + name);
        this.summaryAgent = new SummaryAgent("总结讲师-" + name);
        // 固定3线程池，对应3个 Worker Agent
        this.executor = Executors.newFixedThreadPool(3,
            r -> new Thread(r, "agent-worker-" + name));
    }

    @Override
    public String getName() { return name; }

    @Override
    public AgentStatus getStatus() { return status; }

    @Override
    public LearningResult process(LearningTask task) throws Exception {
        status = AgentStatus.WORKING;
        long start = System.currentTimeMillis();

        System.out.println("  🎯 [" + name + "] 收到任务：" + task.getTopic());
        System.out.println("  🔀 拆分为 3 个子任务，并行分发给 Worker Agent...");

        // ── 第一步：拆分子任务 ──────────────────────────────────
        SubTask exampleSubTask = new SubTask(task.getTopic(), task.getDescription(), SubTask.Type.EXAMPLE);
        SubTask quizSubTask    = new SubTask(task.getTopic(), task.getDescription(), SubTask.Type.QUIZ);
        SubTask summarySubTask = new SubTask(task.getTopic(), task.getDescription(), SubTask.Type.SUMMARY);

        // ── 第二步：并行提交给 Worker Agent（核心！） ──────────────
        //
        //  CompletableFuture 让三个 Worker 同时工作，
        //  就像三位讲师同时开始备课，而不是依次等待
        //
        CompletableFuture<SubTaskResult> exampleFuture = CompletableFuture.supplyAsync(
            () -> safeExecute(exampleAgent, exampleSubTask), executor);

        CompletableFuture<SubTaskResult> quizFuture = CompletableFuture.supplyAsync(
            () -> safeExecute(quizAgent, quizSubTask), executor);

        CompletableFuture<SubTaskResult> summaryFuture = CompletableFuture.supplyAsync(
            () -> safeExecute(summaryAgent, summarySubTask), executor);

        System.out.println("  ⚡ 三个 Worker Agent 并行工作中...");
        System.out.println("     · " + exampleAgent.getName() + " [" + AgentStatus.WORKING + "] → 生成代码示例");
        System.out.println("     · " + quizAgent.getName()    + " [" + AgentStatus.WORKING + "] → 生成测验题");
        System.out.println("     · " + summaryAgent.getName() + " [" + AgentStatus.WORKING + "] → 生成总结");

        // ── 第三步：等待所有 Worker 完成（汇聚点） ────────────────
        CompletableFuture.allOf(exampleFuture, quizFuture, summaryFuture).join();

        SubTaskResult exampleResult = exampleFuture.get();
        SubTaskResult quizResult    = quizFuture.get();
        SubTaskResult summaryResult = summaryFuture.get();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  ✅ 所有 Worker 完成，汇聚结果（总耗时 " + elapsed + "ms）");
        System.out.println("     · " + exampleResult.getAgentName() + " 耗时 " + exampleResult.getElapsedMs() + "ms");
        System.out.println("     · " + quizResult.getAgentName()    + " 耗时 " + quizResult.getElapsedMs() + "ms");
        System.out.println("     · " + summaryResult.getAgentName() + " 耗时 " + summaryResult.getElapsedMs() + "ms");

        status = AgentStatus.COMPLETED;

        // ── 第四步：汇聚成最终结果 ────────────────────────────────
        return new LearningResult(
            task.getTopic(),
            exampleResult.getContent(),
            quizResult.getContent(),
            summaryResult.getContent(),
            elapsed,
            task.getPriority()
        );
    }

    /** 安全执行（捕获异常，返回错误结果） */
    private SubTaskResult safeExecute(WorkerAgent agent, SubTask subTask) {
        try {
            return agent.execute(subTask);
        } catch (Exception e) {
            return new SubTaskResult(subTask.getTaskId(), subTask.getType(),
                "[生成失败: " + e.getMessage() + "]", agent.getName(), 0);
        }
    }

    void shutdown() {
        executor.shutdown();
    }
}


// ====================================================================
// 四、Worker Agent：三个专职子 Agent
// ====================================================================

/**
 * ExampleAgent —— 负责生成代码示例
 *
 * 类比：专门负责「写代码演示」的讲师
 * 真实场景：这里调用 GPT/Claude API，让 AI 根据知识点描述生成代码
 */
class ExampleAgent implements WorkerAgent {

    private final String name;
    private volatile AgentStatus status = AgentStatus.IDLE;

    ExampleAgent(String name) { this.name = name; }

    @Override
    public String getName()   { return name; }

    @Override
    public AgentStatus getStatus() { return status; }

    @Override
    public SubTaskResult execute(SubTask subTask) throws Exception {
        status = AgentStatus.WORKING;
        long start = System.currentTimeMillis();

        // 模拟 AI 生成代码示例（真实场景替换为 HTTP 调用 AI API）
        simulateAIThinking(80, 150); // 模拟 80~150ms 的 AI 响应时间

        String code = generateCodeExample(subTask.getParentTopic(), subTask.getDescription());

        long elapsed = System.currentTimeMillis() - start;
        status = AgentStatus.COMPLETED;
        return new SubTaskResult(subTask.getTaskId(), SubTask.Type.EXAMPLE, code, name, elapsed);
    }

    /**
     * 根据知识点生成对应代码示例
     * 真实场景：此处发送 prompt 给 LLM，获取代码
     */
    private String generateCodeExample(String topic, String description) {
        // 用预置模板模拟 AI 生成（演示用）
        Map<String, String> codeExamples = new HashMap<>();

        codeExamples.put("NIO-Buffer",
            "// Buffer 三指针状态机演示\n" +
            "ByteBuffer buf = ByteBuffer.allocate(10); // capacity=10\n" +
            "buf.put(\"Hello\".getBytes());              // position=5, limit=10\n" +
            "buf.flip();                                // position=0, limit=5  ← 写→读切换\n" +
            "byte[] data = new byte[buf.limit()];\n" +
            "buf.get(data);                             // position=5, limit=5\n" +
            "buf.clear();                               // position=0, limit=10 ← 重置");

        codeExamples.put("NIO-Selector",
            "// Selector 多路复用演示\n" +
            "Selector selector = Selector.open();\n" +
            "ServerSocketChannel server = ServerSocketChannel.open();\n" +
            "server.configureBlocking(false);           // 必须非阻塞\n" +
            "server.register(selector, SelectionKey.OP_ACCEPT);\n" +
            "while (true) {\n" +
            "    selector.select();                     // 阻塞直到有事件\n" +
            "    Set<SelectionKey> keys = selector.selectedKeys();\n" +
            "    for (SelectionKey key : keys) {\n" +
            "        if (key.isAcceptable()) { /* 处理新连接 */ }\n" +
            "        if (key.isReadable())   { /* 处理读事件 */ }\n" +
            "    }\n" +
            "}");

        codeExamples.put("NIO-ZeroCopy",
            "// transferTo 零拷贝演示\n" +
            "FileChannel src = FileChannel.open(Path.of(\"data.bin\"));\n" +
            "SocketChannel dest = SocketChannel.open(serverAddress);\n" +
            "long position = 0;\n" +
            "long size = src.size();\n" +
            "// 循环确保全部传输（内核拥塞时可能一次传不完）\n" +
            "while (position < size) {\n" +
            "    long transferred = src.transferTo(position, size - position, dest);\n" +
            "    position += transferred; // 仅2次DMA拷贝，无需用户态中转\n" +
            "}");

        codeExamples.put("Netty-Pipeline",
            "// Netty Pipeline 链式处理\n" +
            "ServerBootstrap b = new ServerBootstrap();\n" +
            "b.childHandler(new ChannelInitializer<SocketChannel>() {\n" +
            "    protected void initChannel(SocketChannel ch) {\n" +
            "        ch.pipeline()\n" +
            "          .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,4)) // 拆包\n" +
            "          .addLast(new StringDecoder(CharsetUtil.UTF_8))            // 解码\n" +
            "          .addLast(new BusinessHandler());                          // 业务\n" +
            "    }\n" +
            "});");

        codeExamples.put("MappedByteBuffer",
            "// MappedByteBuffer 内存映射文件\n" +
            "FileChannel fc = FileChannel.open(Path.of(\"commitlog\"),\n" +
            "    StandardOpenOption.READ, StandardOpenOption.WRITE);\n" +
            "// 将文件前 1GB 映射到内存（RocketMQ CommitLog 就是这样）\n" +
            "MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 1024*1024*1024);\n" +
            "mbb.put(messageBytes);  // 直接写内存，OS 异步刷盘，无系统调用开销\n" +
            "mbb.force();            // 显式刷盘（对应 RocketMQ 的 flush 操作）");

        // 如果没有预置，生成通用模板
        return codeExamples.getOrDefault(topic,
            "// " + topic + " 代码示例\n" +
            "// 知识点：" + description + "\n" +
            "// （真实场景：ExampleAgent 调用 LLM API 生成此处代码）\n" +
            "public class " + topic.replace("-","") + "Example {\n" +
            "    // ... 生成的示例代码 ...\n" +
            "}");
    }

    private void simulateAIThinking(int minMs, int maxMs) throws InterruptedException {
        long delay = minMs + (long)(Math.random() * (maxMs - minMs));
        Thread.sleep(delay);
    }
}

/**
 * QuizAgent —— 负责生成测验题目
 *
 * 类比：专门负责「出考试题」的讲师
 * 真实场景：调用 AI API，prompt 中要求输出选择题/问答题格式
 */
class QuizAgent implements WorkerAgent {

    private final String name;
    private volatile AgentStatus status = AgentStatus.IDLE;

    QuizAgent(String name) { this.name = name; }

    @Override
    public String getName()   { return name; }

    @Override
    public AgentStatus getStatus() { return status; }

    @Override
    public SubTaskResult execute(SubTask subTask) throws Exception {
        status = AgentStatus.WORKING;
        long start = System.currentTimeMillis();

        simulateAIThinking(60, 120);

        String quiz = generateQuiz(subTask.getParentTopic());

        long elapsed = System.currentTimeMillis() - start;
        status = AgentStatus.COMPLETED;
        return new SubTaskResult(subTask.getTaskId(), SubTask.Type.QUIZ, quiz, name, elapsed);
    }

    private String generateQuiz(String topic) {
        Map<String, String> quizMap = new HashMap<>();

        quizMap.put("NIO-Buffer",
            "Q1. ByteBuffer.flip() 后，position 和 limit 分别变为？\n" +
            "   A) position=0, limit=capacity\n" +
            "   B) position=0, limit=原来的position  ✓\n" +
            "   C) position=limit, limit=capacity\n" +
            "\n" +
            "Q2. compact() 和 clear() 的区别是什么？\n" +
            "   答：compact() 保留未读数据并压缩到开头；clear() 直接重置所有指针（数据不清除但会被覆盖）\n" +
            "\n" +
            "Q3. ByteBuffer.allocateDirect() 和 allocate() 有何区别？（面试高频）\n" +
            "   答：Direct Buffer 在堆外内存，省去一次堆内→堆外的拷贝，适合 IO 密集场景");

        quizMap.put("NIO-Selector",
            "Q1. Selector.select() 和 selectNow() 的区别？\n" +
            "   答：select() 阻塞直到有事件；selectNow() 立即返回，没有事件返回0\n" +
            "\n" +
            "Q2. 为什么注册 OP_WRITE 会导致 CPU 100% 空转？\n" +
            "   答：只要写缓冲区未满，OP_WRITE 就一直就绪，应只在写不完时才注册，写完立即取消注册\n" +
            "\n" +
            "Q3. epoll 为什么比 select/poll 性能好？（操作系统原理）\n" +
            "   答：select/poll 每次都要遍历全部 fd；epoll 用事件驱动，就绪列表只含活跃 fd，O(1) 复杂度");

        quizMap.put("NIO-ZeroCopy",
            "Q1. transferTo 相比传统读写少了几次拷贝？\n" +
            "   答：传统4次（磁盘→内核→用户→Socket缓冲区→网卡），零拷贝2次（磁盘→内核→网卡），少2次\n" +
            "\n" +
            "Q2. transferTo 一次调用能保证把文件全部发送出去吗？\n" +
            "   答：不能。内核发送缓冲区满或网络拥塞时会传不完，需要循环调用直到 position==size");

        quizMap.put("Netty-Pipeline",
            "Q1. Netty 的 InboundHandler 和 OutboundHandler 传播方向？\n" +
            "   答：Inbound 从头到尾（head→tail），Outbound 从尾到头（tail→head）\n" +
            "\n" +
            "Q2. ctx.writeAndFlush() 和 channel.writeAndFlush() 的区别？\n" +
            "   答：ctx 从当前 Handler 开始向前传播；channel 从 tail 开始，经过所有 OutboundHandler");

        quizMap.put("MappedByteBuffer",
            "Q1. mmap 为什么能减少系统调用次数？\n" +
            "   答：文件映射到进程虚拟地址空间后，读写就像操作内存，Page Fault 时内核自动加载数据\n" +
            "\n" +
            "Q2. RocketMQ 使用 MappedByteBuffer 的好处是什么？\n" +
            "   答：顺序写内存（极快）+ OS 异步刷盘 + Page Cache 加速读取，实现极高的消息吞吐量\n" +
            "\n" +
            "Q3. 使用 MappedByteBuffer 需要注意什么风险？\n" +
            "   答：内存映射文件不受 GC 管理，需要手动调用 Cleaner 释放，否则会堆外内存泄漏");

        return quizMap.getOrDefault(topic,
            "Q1. " + topic + " 的核心概念是什么？\n" +
            "   答：（真实场景：QuizAgent 调用 LLM API 生成此题答案）\n" +
            "\n" +
            "Q2. 在实际项目中如何应用 " + topic + "？\n" +
            "   答：（真实场景：QuizAgent 根据知识点描述生成实战题）");
    }

    private void simulateAIThinking(int minMs, int maxMs) throws InterruptedException {
        long delay = minMs + (long)(Math.random() * (maxMs - minMs));
        Thread.sleep(delay);
    }
}

/**
 * SummaryAgent —— 负责生成知识总结
 *
 * 类比：专门负责「整理知识卡片」的讲师
 * 真实场景：调用 AI API，要求输出要点列表 + 关键词 + 一句话总结
 */
class SummaryAgent implements WorkerAgent {

    private final String name;
    private volatile AgentStatus status = AgentStatus.IDLE;

    SummaryAgent(String name) { this.name = name; }

    @Override
    public String getName()   { return name; }

    @Override
    public AgentStatus getStatus() { return status; }

    @Override
    public SubTaskResult execute(SubTask subTask) throws Exception {
        status = AgentStatus.WORKING;
        long start = System.currentTimeMillis();

        simulateAIThinking(50, 100);

        String summary = generateSummary(subTask.getParentTopic(), subTask.getDescription());

        long elapsed = System.currentTimeMillis() - start;
        status = AgentStatus.COMPLETED;
        return new SubTaskResult(subTask.getTaskId(), SubTask.Type.SUMMARY, summary, name, elapsed);
    }

    private String generateSummary(String topic, String description) {
        Map<String, String> summaryMap = new HashMap<>();

        summaryMap.put("NIO-Buffer",
            "一句话：Buffer 是 NIO 的「有刻度水杯」，三指针精确控制读写位置。\n" +
            "核心要点：\n" +
            "  · capacity：杯子总容量，创建后不变\n" +
            "  · position：当前读/写指针，操作后自动移动\n" +
            "  · limit：   有效边界，写模式=capacity，读模式=上次写到的位置\n" +
            "  · flip()   = 写→读切换（position→0，limit→原position）\n" +
            "  · clear()  = 重置为写模式（丢弃未读数据，快速重用）\n" +
            "  · compact()= 保留未读 → 继续追加写（适合半包场景）\n" +
            "面试金句：「flip 不是清空，是切换模式；compact 不是清空，是保留未读」");

        summaryMap.put("NIO-Selector",
            "一句话：Selector 是 NIO 的「前台振动手环」，一个线程管理 N 个连接。\n" +
            "核心要点：\n" +
            "  · select()    阻塞等待就绪事件，返回就绪 fd 数量\n" +
            "  · selectedKeys() 获取就绪事件集合，处理后必须手动 remove\n" +
            "  · epoll vs select：epoll O(1) 事件通知，select O(n) 轮询\n" +
            "  · OP_WRITE 陷阱：只在写缓冲满时注册，写完立即取消\n" +
            "生产实践：Netty、Tomcat NIO Connector 都基于此模型");

        summaryMap.put("NIO-ZeroCopy",
            "一句话：零拷贝 = 让数据直接走内核，绕过用户态，减少 CPU 参与。\n" +
            "核心要点：\n" +
            "  · 传统拷贝：4次拷贝（2次DMA + 2次CPU），4次上下文切换\n" +
            "  · sendfile（transferTo）：2次DMA拷贝，2次上下文切换\n" +
            "  · 适用场景：大文件传输（Kafka消费、静态文件服务）\n" +
            "  · 注意：小文件零拷贝可能反而更慢（系统调用开销 > 拷贝开销）\n" +
            "使用框架：Kafka sendfile、Nginx sendfile、Netty FileRegion");

        summaryMap.put("Netty-Pipeline",
            "一句话：Pipeline 是 Netty 的「流水线」，每个 Handler 处理一道工序。\n" +
            "核心要点：\n" +
            "  · ChannelPipeline = 双向链表，head ↔ handler1 ↔ handler2 ↔ tail\n" +
            "  · Inbound 事件：从 head 流向 tail（解码→业务处理）\n" +
            "  · Outbound 事件：从 tail 流向 head（编码→发送）\n" +
            "  · ctx.fireXxx() 传递给下一个 Handler，不调用则截断\n" +
            "设计亮点：责任链模式，各 Handler 单一职责，可自由组合");

        summaryMap.put("MappedByteBuffer",
            "一句话：mmap 把文件「贴」到内存地址空间，读写文件像读写内存一样快。\n" +
            "核心要点：\n" +
            "  · 原理：建立虚拟地址→文件的映射，缺页时 OS 自动加载\n" +
            "  · 优点：减少系统调用 + 利用 Page Cache + 支持大文件随机访问\n" +
            "  · 缺点：内存不受 GC 管理，释放需显式调用 Cleaner\n" +
            "  · RocketMQ：每个 CommitLog（1GB）对应一个 MappedByteBuffer\n" +
            "面试金句：「mmap 是 RocketMQ 高吞吐的核心秘密之一」");

        return summaryMap.getOrDefault(topic,
            "一句话：" + topic + " 是 Java IO 体系中的重要知识点。\n" +
            "核心要点：\n" +
            "  · 知识点：" + description + "\n" +
            "  · （真实场景：SummaryAgent 调用 LLM API 提炼要点）\n" +
            "面试提示：理解原理 > 记忆 API");
    }

    private void simulateAIThinking(int minMs, int maxMs) throws InterruptedException {
        long delay = minMs + (long)(Math.random() * (maxMs - minMs));
        Thread.sleep(delay);
    }
}


// ====================================================================
// 五、AgentPool：多个 Orchestrator 并行处理多个任务
// ====================================================================

/**
 * AgentPool —— 管理一组 Orchestrator，并行处理多个知识点
 *
 * 类比：多个「备课小组」并行工作，每组独立处理一个知识点
 *
 * 这是多 Agent 系统的第二层扩展：
 *   演示1/2：1个Orchestrator → 3个Worker（1:3）
 *   AgentPool：N个Orchestrator → 各自3个Worker（N:3N）
 */
class AgentPool {

    private final int poolSize;
    private final ExecutorService pool;

    AgentPool(int poolSize) {
        this.poolSize = poolSize;
        this.pool = Executors.newFixedThreadPool(poolSize,
            r -> new Thread(r, "orchestrator-pool-" + System.currentTimeMillis()));
    }

    /**
     * 并行处理所有任务，返回结果列表（顺序与输入一致）
     */
    List<LearningResult> processAll(List<LearningTask> tasks) throws Exception {
        List<CompletableFuture<LearningResult>> futures = new ArrayList<>();

        for (LearningTask task : tasks) {
            OrchestratorAgent orch = new OrchestratorAgent("Pool-Orch-" + task.getTopic());
            CompletableFuture<LearningResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return orch.process(task);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    orch.shutdown();
                }
            }, pool);
            futures.add(future);
        }

        // 等待所有 Orchestrator 完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<LearningResult> results = new ArrayList<>();
        for (CompletableFuture<LearningResult> f : futures) {
            results.add(f.get());
        }
        return results;
    }

    void shutdown() {
        pool.shutdown();
    }
}


// ====================================================================
// 六、PriorityOrchestrator：带优先级队列的任务调度
// ====================================================================

/**
 * PriorityOrchestrator —— 按优先级顺序处理任务
 *
 * 使用 PriorityBlockingQueue，HIGH 优先级任务最先被处理
 *
 * 类比：急诊分诊台 —— 危重病人（HIGH）先处理，普通病人（LOW）排队等
 */
class PriorityOrchestrator {

    private final String name;

    // 优先级队列：HIGH > MEDIUM > LOW
    private final PriorityBlockingQueue<LearningTask> taskQueue;
    private final ExecutorService executor;
    private final List<LearningResult> results = Collections.synchronizedList(new ArrayList<>());

    PriorityOrchestrator(String name) {
        this.name = name;
        // 按优先级排序的阻塞队列（HIGH=0 最小，排最前）
        this.taskQueue = new PriorityBlockingQueue<>(16, (a, b) -> {
            int pa = priorityValue(a.getPriority());
            int pb = priorityValue(b.getPriority());
            return Integer.compare(pa, pb);
        });
        this.executor = Executors.newFixedThreadPool(2); // 限制并发，体现优先级效果
    }

    void submit(LearningTask task) {
        taskQueue.offer(task);
        System.out.println("  ➕ 提交任务：" + task);
    }

    List<LearningResult> processAll() throws Exception {
        int total = taskQueue.size();
        List<CompletableFuture<LearningResult>> futures = new ArrayList<>();

        System.out.println("\n  🔢 开始按优先级处理（队列长度=" + total + "）：");

        for (int i = 0; i < total; i++) {
            LearningTask task = taskQueue.poll(); // 按优先级取出
            if (task == null) break;
            System.out.println("  📌 [第" + (i+1) + "个出队] " + task);

            OrchestratorAgent orch = new OrchestratorAgent("PriorityOrch-" + i);
            CompletableFuture<LearningResult> f = CompletableFuture.supplyAsync(() -> {
                try {
                    LearningResult r = orch.process(task);
                    results.add(r);
                    return r;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    orch.shutdown();
                }
            }, executor);
            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<LearningResult> ordered = new ArrayList<>();
        for (CompletableFuture<LearningResult> f : futures) {
            ordered.add(f.get());
        }
        return ordered;
    }

    void shutdown() {
        executor.shutdown();
    }

    private int priorityValue(LearningTask.Priority p) {
        switch (p) {
            case HIGH:   return 0;
            case MEDIUM: return 1;
            case LOW:    return 2;
            default:     return 3;
        }
    }
}


// ====================================================================
// 七、UnstableWorkerAgent + ResilientOrchestrator：容错机制
// ====================================================================

/**
 * UnstableWorkerAgent —— 模拟不稳定的 AI Agent（偶尔超时/报错）
 *
 * 真实场景：调用 OpenAI/Claude API 时，偶尔会：
 *   - 网络超时
 *   - Rate Limit (429)
 *   - 模型崩溃 (500)
 * 这时需要自动重试机制
 */
class UnstableWorkerAgent implements WorkerAgent {

    private final String name;
    private final double failureRate; // 失败概率（0.0 ~ 1.0）
    private volatile AgentStatus status = AgentStatus.IDLE;
    private int callCount = 0;

    UnstableWorkerAgent(String name, double failureRate) {
        this.name = name;
        this.failureRate = failureRate;
    }

    @Override
    public String getName() { return name; }

    @Override
    public AgentStatus getStatus() { return status; }

    @Override
    public SubTaskResult execute(SubTask subTask) throws Exception {
        callCount++;
        status = AgentStatus.WORKING;

        // 模拟随机失败
        if (Math.random() < failureRate) {
            status = AgentStatus.FAILED;
            System.out.println("    ❌ [" + name + "] 第" + callCount + "次调用失败（模拟网络超时/Rate Limit）");
            throw new RuntimeException("AI API 调用失败（模拟故障，调用次数=" + callCount + "）");
        }

        Thread.sleep(100);
        status = AgentStatus.COMPLETED;
        System.out.println("    ✓  [" + name + "] 第" + callCount + "次调用成功");
        return new SubTaskResult(subTask.getTaskId(), SubTask.Type.EXAMPLE,
            "// " + subTask.getParentTopic() + " 代码示例（经 " + callCount + " 次尝试后生成）\n" +
            "// 此处为 AI 重试成功后返回的内容", name, 100);
    }
}

/**
 * ResilientOrchestrator —— 带自动重试的容错 Orchestrator
 *
 * 重试策略：指数退避（Exponential Backoff）
 *   第1次失败 → 等50ms重试
 *   第2次失败 → 等100ms重试
 *   第3次失败 → 等200ms重试
 *   超过最大重试次数 → 返回降级结果（Fallback）
 *
 * 类比：外卖骑手接单失败 → 系统自动重新派单给其他骑手（或等待后重派）
 */
class ResilientOrchestrator {

    private final String name;
    private final WorkerAgent primaryWorker;   // 主 Worker
    private final int maxRetries;

    ResilientOrchestrator(String name, WorkerAgent primaryWorker, int maxRetries) {
        this.name = name;
        this.primaryWorker = primaryWorker;
        this.maxRetries = maxRetries;
    }

    LearningResult processWithRetry(LearningTask task) throws Exception {
        SubTask subTask = new SubTask(task.getTopic(), task.getDescription(), SubTask.Type.EXAMPLE);

        SubTaskResult result = null;
        int retryCount = 0;
        Exception lastException = null;

        System.out.println("  🔄 [" + name + "] 开始执行，最大重试次数=" + maxRetries);

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                System.out.println("  🔁 第 " + attempt + " 次尝试...");
                result = primaryWorker.execute(subTask);
                System.out.println("  ✅ 第 " + attempt + " 次成功！");
                retryCount = attempt - 1;
                break;
            } catch (Exception e) {
                lastException = e;
                retryCount = attempt;

                if (attempt <= maxRetries) {
                    // 指数退避
                    long backoffMs = 50L * (1L << (attempt - 1)); // 50, 100, 200, ...
                    System.out.println("  ⏳ 等待 " + backoffMs + "ms 后重试（指数退避）...");
                    Thread.sleep(backoffMs);
                }
            }
        }

        if (result == null) {
            // 超过最大重试次数，返回降级结果（Fallback）
            System.out.println("  ⚠️  超过最大重试次数，使用降级内容（Fallback）");
            String fallback = "// [降级内容] " + task.getTopic() + "\n" +
                              "// AI 服务暂时不可用，显示预置的基础示例\n" +
                              "// 建议稍后重试或查看官方文档";
            result = new SubTaskResult(subTask.getTaskId(), SubTask.Type.EXAMPLE,
                fallback, "FallbackAgent", 0);
        }

        LearningResult learningResult = new LearningResult(
            task.getTopic(),
            result.getContent(),
            "（仅演示重试机制，未调用 QuizAgent）",
            "（仅演示重试机制，未调用 SummaryAgent）",
            0,
            task.getPriority()
        );
        learningResult.setRetryCount(retryCount);
        return learningResult;
    }
}

