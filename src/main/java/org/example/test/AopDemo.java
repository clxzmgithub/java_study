package org.example.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * AOP（面向切面编程）深度解析
 *
 * 本文件覆盖：
 * 1. AOP 核心概念和术语
 * 2. 为什么需要 AOP（没有 AOP 的痛点）
 * 3. 静态代理（理解代理思想）
 * 4. JDK 动态代理（核心）—— 基于接口
 * 5. 模拟 Spring AOP 多个切面叠加
 */
public class AopDemo {

    public static void main(String[] args) {
        System.out.println("========== AOP 面向切面编程 深度解析 ==========\n");

        System.out.println("【1. AOP 核心术语解释】");
        explainTerms();

        System.out.println("\n【2. 没有 AOP 的痛点（为什么需要 AOP）】");
        demoProblemWithoutAop();

        System.out.println("\n【3. 静态代理：理解代理的思想】");
        demoStaticProxy();

        System.out.println("\n【4. JDK 动态代理：AOP 的核心实现】");
        demoJdkDynamicProxy();

        System.out.println("\n【5. 动态代理原理：运行时生成的代理类长什么样？】");
        explainProxyInternals();

        System.out.println("\n【6. 模拟 Spring AOP：多个切面叠加（切面链）】");
        demoMultipleAspects();
    }

    // ================================================================
    // 【1. AOP 核心术语解释】
    // ================================================================
    private static void explainTerms() {
        System.out.println("AOP = Aspect-Oriented Programming = 面向切面编程");
        System.out.println("核心思想：把'横切关注点'从业务逻辑中分离出来\n");

        System.out.println("  ┌────────────────────────────────────────────────────┐");
        System.out.println("  │              业务方法调用流程                       │");
        System.out.println("  │                                                    │");
        System.out.println("  │  调用方          ┌──────────────────┐             │");
        System.out.println("  │  ────────────►  │  目标方法（Target）│            │");
        System.out.println("  │                 │  orderService     │             │");
        System.out.println("  │                 │  .createOrder()   │             │");
        System.out.println("  │                 └──────────────────┘             │");
        System.out.println("  │                                                    │");
        System.out.println("  │  AOP 在调用链上插入'切面'：                         │");
        System.out.println("  │                                                    │");
        System.out.println("  │         ┌──────────┐  ┌──────────┐  ┌─────────┐  │");
        System.out.println("  │  ──────►│ 日志切面  │─►│ 事务切面  │─►│ 目标方法│  │");
        System.out.println("  │         └──────────┘  └──────────┘  └─────────┘  │");
        System.out.println("  │              ↑              ↑            ↑         │");
        System.out.println("  │           Aspect         Aspect        业务代码    │");
        System.out.println("  └────────────────────────────────────────────────────┘\n");

        System.out.println("术语对照表：");
        System.out.println("  Aspect（切面）    = 横切关注点的模块化，如'日志切面'、'事务切面'");
        System.out.println("  Advice（通知）    = 切面在特定时机执行的动作");
        System.out.println("                      • @Before    方法执行前");
        System.out.println("                      • @After     方法执行后（无论是否异常）");
        System.out.println("                      • @AfterReturning 方法正常返回后");
        System.out.println("                      • @AfterThrowing  方法抛异常后");
        System.out.println("                      • @Around    环绕（最强，可控制是否执行目标方法）");
        System.out.println("  Pointcut（切点）  = 定义'在哪些方法上'应用切面（表达式）");
        System.out.println("  JoinPoint（连接点）= 程序执行中可以插入切面的点（方法调用、异常等）");
        System.out.println("  Target（目标对象）= 被代理的真实对象");
        System.out.println("  Proxy（代理对象） = AOP 生成的代理，包裹了 Target");
        System.out.println("  Weaving（织入）   = 将切面应用到目标对象的过程");
    }

    // ================================================================
    // 【2. 没有 AOP 的痛点】
    // ================================================================
    private static void demoProblemWithoutAop() {
        System.out.println("场景：订单服务、用户服务、支付服务都需要：日志 + 事务 + 权限检查\n");

        System.out.println("没有 AOP 时，每个方法都要写重复代码：\n");
        System.out.println("  class OrderService {");
        System.out.println("      void createOrder() {");
        System.out.println("          log.info(\"开始执行\");          // ← 重复");
        System.out.println("          checkPermission();             // ← 重复");
        System.out.println("          transaction.begin();           // ← 重复");
        System.out.println("          // === 真正的业务代码 ===");
        System.out.println("          db.insert(order);");
        System.out.println("          // ===================");
        System.out.println("          transaction.commit();          // ← 重复");
        System.out.println("          log.info(\"执行完成\");          // ← 重复");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  class UserService {");
        System.out.println("      void createUser() {");
        System.out.println("          log.info(\"开始执行\");          // ← 重复（100 个方法写 100 次）");
        System.out.println("          checkPermission();             // ← 重复");
        System.out.println("          transaction.begin();           // ← 重复");
        System.out.println("          // === 真正的业务代码 ===");
        System.out.println("          db.insert(user);");
        System.out.println("          // ===================");
        System.out.println("          transaction.commit();          // ← 重复");
        System.out.println("          log.info(\"执行完成\");          // ← 重复");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("问题：");
        System.out.println("  ❌ 代码冗余：100 个方法 × 5 行重复代码 = 500 行废话");
        System.out.println("  ❌ 修改困难：日志格式改了，要改 100 个地方");
        System.out.println("  ❌ 关注点混乱：业务代码里掺杂大量非业务逻辑");
        System.out.println();
        System.out.println("AOP 的解决方案：把这些'横切关注点'抽取成切面");
        System.out.println("  只写一次，自动应用到所有匹配的方法 ✅");
    }

    // ================================================================
    // 【3. 静态代理：理解代理的思想】
    // ================================================================
    private static void demoStaticProxy() {
        System.out.println("静态代理 = 手写一个代理类，在里面增强目标对象的方法\n");

        System.out.println("目标接口：OrderService");
        System.out.println("目标实现：RealOrderService（只负责业务逻辑）");
        System.out.println("代理类：  LoggingOrderServiceProxy（负责日志）\n");

        // 使用静态代理
        OrderService realService = new RealOrderService();
        OrderService proxy = new LoggingOrderServiceProxy(realService);

        System.out.println("通过静态代理对象调用方法：");
        proxy.createOrder("iPhone 15", 2);
        System.out.println();
        proxy.cancelOrder("ORDER-001");

        System.out.println("\n静态代理的缺点：");
        System.out.println("  ❌ 每个接口都要手写一个代理类（OrderService → OrderServiceProxy）");
        System.out.println("                               （UserService → UserServiceProxy）");
        System.out.println("                               （PayService  → PayServiceProxy）");
        System.out.println("  ❌ 接口加了一个新方法，代理类也要同步修改");
        System.out.println("  ❌ 代码量大，维护困难");
        System.out.println();
        System.out.println("  → 解决方案：动态代理（运行时自动生成代理类）");
    }

    // ================================================================
    // 【4. JDK 动态代理：核心】
    // ================================================================
    private static void demoJdkDynamicProxy() {
        System.out.println("JDK 动态代理的三要素：");
        System.out.println("  1. 目标对象（Target）：实现了接口的真实对象");
        System.out.println("  2. InvocationHandler：定义'增强逻辑'的接口");
        System.out.println("  3. Proxy.newProxyInstance()：运行时生成代理对象\n");

        // 创建目标对象
        OrderService target = new RealOrderService();

        System.out.println("-- 4.1 只加日志的动态代理 --\n");
        // 创建日志 InvocationHandler
        InvocationHandler loggingHandler = new LoggingInvocationHandler(target);
        // 生成代理对象（运行时动态生成，不需要手写代理类）
        OrderService loggingProxy = (OrderService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),   // 类加载器
                target.getClass().getInterfaces(),    // 目标类实现的接口
                loggingHandler                        // 增强逻辑
        );
        System.out.println("代理对象的类名：" + loggingProxy.getClass().getName());
        System.out.println("代理对象是 OrderService 的实例：" + (loggingProxy instanceof OrderService));
        System.out.println();
        loggingProxy.createOrder("MacBook Pro", 1);
        System.out.println();
        loggingProxy.cancelOrder("ORDER-002");

        System.out.println("\n-- 4.2 只加事务的动态代理 --\n");
        InvocationHandler transactionHandler = new TransactionInvocationHandler(target);
        OrderService transactionProxy = (OrderService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                transactionHandler
        );
        transactionProxy.createOrder("iPad", 3);

        System.out.println("\n-- 4.3 同时加日志 + 事务（代理的代理）--\n");
        // 先用事务代理包裹目标
        OrderService txProxy = (OrderService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new TransactionInvocationHandler(target)
        );
        // 再用日志代理包裹事务代理（代理链）
        OrderService logTxProxy = (OrderService) Proxy.newProxyInstance(
                txProxy.getClass().getClassLoader(),
                new Class[]{OrderService.class},
                new LoggingInvocationHandler(txProxy)
        );
        System.out.println("调用 logTxProxy.createOrder()：");
        logTxProxy.createOrder("AirPods", 2);
    }

    // ================================================================
    // 【5. 动态代理原理：代理类长什么样】
    // ================================================================
    private static void explainProxyInternals() {
        System.out.println("Proxy.newProxyInstance() 在运行时生成的代理类，伪代码如下：\n");
        System.out.println("  // JVM 在内存中动态生成，类名类似 $Proxy0");
        System.out.println("  public final class $Proxy0 extends Proxy implements OrderService {");
        System.out.println();
        System.out.println("      // 构造器接收 InvocationHandler");
        System.out.println("      public $Proxy0(InvocationHandler h) { super(h); }");
        System.out.println();
        System.out.println("      // 实现接口的每个方法");
        System.out.println("      @Override");
        System.out.println("      public void createOrder(String product, int qty) {");
        System.out.println("          // 核心：把方法调用转发给 InvocationHandler");
        System.out.println("          Method m = OrderService.class.getMethod(\"createOrder\", ...);");
        System.out.println("          h.invoke(this, m, new Object[]{product, qty});");
        System.out.println("          //  ↑                              ↑");
        System.out.println("          //  你的 InvocationHandler       方法参数");
        System.out.println("      }");
        System.out.println();
        System.out.println("      @Override");
        System.out.println("      public void cancelOrder(String orderId) {");
        System.out.println("          Method m = OrderService.class.getMethod(\"cancelOrder\", ...);");
        System.out.println("          h.invoke(this, m, new Object[]{orderId});");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("调用链分析：");
        System.out.println("  调用方: logTxProxy.createOrder(\"AirPods\", 2)");
        System.out.println("      ↓");
        System.out.println("  $Proxy0.createOrder() → h.invoke(proxy, method, args)");
        System.out.println("      ↓");
        System.out.println("  LoggingInvocationHandler.invoke()");
        System.out.println("      → 打印'方法开始'日志");
        System.out.println("      → method.invoke(target, args)  // target 是事务代理");
        System.out.println("          ↓");
        System.out.println("          TransactionInvocationHandler.invoke()");
        System.out.println("              → 开启事务");
        System.out.println("              → method.invoke(target, args)  // target 是真实对象");
        System.out.println("                  ↓");
        System.out.println("                  RealOrderService.createOrder()  // 真正的业务");
        System.out.println("              → 提交事务");
        System.out.println("      → 打印'方法结束'日志");
        System.out.println();
        System.out.println("JDK 动态代理的限制：");
        System.out.println("  ❌ 目标类必须实现接口（$Proxy0 extends Proxy，Java 不支持多继承）");
        System.out.println("  ✅ 如果目标类没有接口 → 用 CGLIB（通过继承子类实现代理）");
        System.out.println("  Spring AOP：有接口 → JDK 动态代理；无接口 → CGLIB");
    }

    // ================================================================
    // 【6. 模拟 Spring AOP：多个切面叠加】
    // ================================================================
    private static void demoMultipleAspects() {
        System.out.println("模拟 Spring @Around 切面（最强大的通知类型）\n");

        OrderService target = new RealOrderService();

        // 构建切面链：日志 → 性能监控 → 事务 → 目标方法
        // 从内到外包裹（越内层越先被目标方法'看到'）
        OrderService withTransaction = createProxy(target, new TransactionInvocationHandler(target));
        OrderService withPerf        = createProxy(withTransaction,
                                                   new PerformanceInvocationHandler(withTransaction));
        OrderService withAll         = createProxy(withPerf,
                                                   new LoggingInvocationHandler(withPerf));

        System.out.println("最终调用（日志 + 性能监控 + 事务）：");
        System.out.println("──────────────────────────────────────────────────");
        withAll.createOrder("Spring AOP 演示商品", 1);
        System.out.println("──────────────────────────────────────────────────");
        System.out.println("\n切面执行顺序（洋葱模型）：");
        System.out.println("  日志-前 → 性能-前 → 事务开启 → 业务方法 → 事务提交 → 性能-后 → 日志-后");
        System.out.println("  ← 外层切面   ← 中层切面   ← 内层切面      目标       内层-后   中层-后   外层-后 →");
    }

    private static OrderService createProxy(OrderService target, InvocationHandler handler) {
        return (OrderService) Proxy.newProxyInstance(
                OrderService.class.getClassLoader(),
                new Class[]{OrderService.class},
                handler
        );
    }
}

// ================================================================
// 目标接口
// ================================================================
interface OrderService {
    void createOrder(String product, int quantity);
    void cancelOrder(String orderId);
}

// ================================================================
// 真实目标对象（只有业务逻辑，不含日志/事务）
// ================================================================
class RealOrderService implements OrderService {
    @Override
    public void createOrder(String product, int quantity) {
        System.out.println("  [业务] 创建订单：商品=" + product + "，数量=" + quantity);
    }

    @Override
    public void cancelOrder(String orderId) {
        System.out.println("  [业务] 取消订单：" + orderId);
    }
}

// ================================================================
// 静态代理（方式 3 使用）
// ================================================================
class LoggingOrderServiceProxy implements OrderService {
    private final OrderService target; // 持有真实对象引用

    public LoggingOrderServiceProxy(OrderService target) {
        this.target = target;
    }

    @Override
    public void createOrder(String product, int quantity) {
        System.out.println("  [静态代理-日志] 方法开始：createOrder");
        target.createOrder(product, quantity);          // 调用真实方法
        System.out.println("  [静态代理-日志] 方法结束：createOrder");
    }

    @Override
    public void cancelOrder(String orderId) {
        System.out.println("  [静态代理-日志] 方法开始：cancelOrder");
        target.cancelOrder(orderId);
        System.out.println("  [静态代理-日志] 方法结束：cancelOrder");
    }
    // ❌ 接口新增方法，这里也要加，很麻烦
}

// ================================================================
// JDK 动态代理：日志 InvocationHandler
// ================================================================
class LoggingInvocationHandler implements InvocationHandler {
    private final Object target; // 被代理的真实对象（或下一层代理）

    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }

    /**
     * 每次通过代理调用方法时，都会执行这个 invoke
     *
     * @param proxy  代理对象本身（$Proxy0）
     * @param method 被调用的方法（反射 Method 对象）
     * @param args   方法的实际参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // === Before 逻辑（@Before）===
        System.out.println("  [日志切面-前] 方法开始：" + method.getName()
                + "，参数：" + formatArgs(args));
        long start = System.currentTimeMillis();

        Object result = null;
        try {
            // === 调用目标方法（或下一层代理）===
            result = method.invoke(target, args);

            // === AfterReturning 逻辑 ===
            System.out.println("  [日志切面-后] 方法正常结束：" + method.getName()
                    + "，耗时：" + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            // === AfterThrowing 逻辑 ===
            System.out.println("  [日志切面-异常] 方法抛出异常：" + e.getCause().getMessage());
            throw e;
        }
        return result;
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i]);
            if (i < args.length - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}

// ================================================================
// JDK 动态代理：事务 InvocationHandler
// ================================================================
class TransactionInvocationHandler implements InvocationHandler {
    private final Object target;

    public TransactionInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("  [事务切面] 开启事务 BEGIN");
        Object result;
        try {
            result = method.invoke(target, args);
            System.out.println("  [事务切面] 提交事务 COMMIT");
        } catch (Exception e) {
            System.out.println("  [事务切面] 回滚事务 ROLLBACK");
            throw e;
        }
        return result;
    }
}

// ================================================================
// JDK 动态代理：性能监控 InvocationHandler
// ================================================================
class PerformanceInvocationHandler implements InvocationHandler {
    private final Object target;

    public PerformanceInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        System.out.println("  [性能切面] 开始计时：" + method.getName());
        Object result = method.invoke(target, args);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        System.out.println("  [性能切面] 耗时：" + elapsed + "ms，方法：" + method.getName());
        return result;
    }
}

