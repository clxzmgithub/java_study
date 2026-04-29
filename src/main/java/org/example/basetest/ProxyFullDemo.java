package org.example.basetest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 代理模式完整演进路线：
 * 静态代理 → JDK 动态代理 → CGLIB 动态代理 → Spring AOP
 *
 * 贯穿始终的真实项目场景：电商平台的订单服务
 * 横切关注点：日志记录、性能监控、权限校验、事务管理
 */
public class ProxyFullDemo {

    public static void main(String[] args) {
        System.out.println("========== 代理模式完整演进：静态代理 → 动态代理 ==========\n");

        // ① 静态代理
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 第一阶段：静态代理");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        StaticProxySection.run();

        // ② JDK 动态代理
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 第二阶段：JDK 动态代理");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        JdkDynamicProxySection.run();

        // ③ CGLIB 动态代理（纯 Java 模拟，无需引入 cglib 依赖）
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 第三阶段：CGLIB 动态代理（原理模拟）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        CglibSection.run();

        // ④ Spring AOP 原理总结
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 第四阶段：Spring AOP（原理 + 使用方式 + 项目场景）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        SpringAopSection.run();
    }
}

// ====================================================================
// ① 静态代理
// ====================================================================
class StaticProxySection {

    static void run() {
        System.out.println("【是什么】");
        System.out.println("  静态代理 = 手动编写一个代理类，在调用真实方法前后插入增强逻辑。");
        System.out.println("  代理类和目标类实现同一个接口，调用方只感知接口，不感知真实对象。\n");

        System.out.println("【为什么需要】");
        System.out.println("  场景：电商平台订单服务上线，需要统计每个接口的执行时间。");
        System.out.println("  如果直接改 OrderServiceImpl，业务代码和监控代码混在一起：");
        System.out.println();
        System.out.println("    // 改之前（纯业务）");
        System.out.println("    public void createOrder(Order order) {");
        System.out.println("        db.save(order);  // 只有这一行是业务");
        System.out.println("    }");
        System.out.println();
        System.out.println("    // 改之后（业务+监控混杂）");
        System.out.println("    public void createOrder(Order order) {");
        System.out.println("        long start = System.currentTimeMillis(); // 监控");
        System.out.println("        db.save(order);                          // 业务");
        System.out.println("        log(System.currentTimeMillis() - start); // 监控");
        System.out.println("    }");
        System.out.println("  → 业务代码被污染，而且 100 个方法要改 100 次！\n");

        System.out.println("【解决方案：静态代理】");
        System.out.println("  新建 OrderServiceProxy，里面持有 OrderServiceImpl 的引用：\n");

        // 演示
        IOrderService realService = new OrderServiceImpl();
        IOrderService proxy = new OrderServiceStaticProxy(realService);

        System.out.println("  调用代理对象（调用方感知不到真实对象）：");
        proxy.createOrder("iPhone 15 Pro", 2, 17998.0);
        System.out.println();
        proxy.cancelOrder("ORD-20240101-001");
        System.out.println();
        proxy.queryOrder("ORD-20240101-001");

        System.out.println("\n【静态代理的优点】");
        System.out.println("  ✅ 业务代码（OrderServiceImpl）保持干净，只有业务逻辑");
        System.out.println("  ✅ 代理逻辑集中在 Proxy 类，修改一处即可");
        System.out.println("  ✅ 原理简单，易于理解\n");

        System.out.println("【静态代理的缺点】");
        System.out.println("  ❌ 问题 1：接口有多少方法，代理类就要写多少方法（机械重复）");
        System.out.println("     IOrderService   有 createOrder / cancelOrder / queryOrder");
        System.out.println("     IUserService    有 login / logout / updateProfile");
        System.out.println("     IPayService     有 pay / refund / queryBalance");
        System.out.println("     → 每个接口都要写一个对应的 Proxy 类，繁琐");
        System.out.println();
        System.out.println("  ❌ 问题 2：接口新增方法时，代理类也必须同步修改");
        System.out.println("     IOrderService 新增 modifyOrder() → OrderServiceProxy 也要加");
        System.out.println();
        System.out.println("  ❌ 问题 3：如果有 N 个接口，需要 N 个代理类，类爆炸");
        System.out.println();
        System.out.println("  → 引出动态代理：运行时自动生成代理类，无需手写！");
    }
}

// ====================================================================
// ② JDK 动态代理
// ====================================================================
class JdkDynamicProxySection {

    static void run() {
        System.out.println("【是什么】");
        System.out.println("  JDK 动态代理 = JVM 在运行时动态生成代理类（字节码）。");
        System.out.println("  不需要手写代理类，只需实现一个 InvocationHandler 接口。");
        System.out.println("  核心 API：java.lang.reflect.Proxy\n");

        System.out.println("【核心原理 — 三步走】");
        System.out.println("  步骤 1：你提供 InvocationHandler（增强逻辑）");
        System.out.println("  步骤 2：Proxy.newProxyInstance() 在内存中动态生成 $Proxy0 类");
        System.out.println("  步骤 3：$Proxy0 实现目标接口，每个方法都转发给 InvocationHandler");
        System.out.println();
        System.out.println("  生成的 $Proxy0 伪代码：");
        System.out.println("    public final class $Proxy0 extends Proxy implements IOrderService {");
        System.out.println("        public void createOrder(String product, int qty, double price) {");
        System.out.println("            // 把调用转发给你的 InvocationHandler");
        System.out.println("            Method m = IOrderService.class.getMethod(\"createOrder\",...);");
        System.out.println("            h.invoke(this, m, new Object[]{product, qty, price});");
        System.out.println("        }");
        System.out.println("        // 其余方法同理...");
        System.out.println("    }\n");

        System.out.println("【为什么需要】");
        System.out.println("  电商平台有几十个 Service，都需要：");
        System.out.println("  • 请求日志（入参、出参、耗时）");
        System.out.println("  • 权限校验（当前用户有没有权限）");
        System.out.println("  • 异常统一处理");
        System.out.println("  静态代理要写几十个 Proxy 类，动态代理一个 Handler 搞定所有！\n");

        System.out.println("【演示 1：日志 Handler（自动记录所有方法的入参出参耗时）】");
        IOrderService orderService = new OrderServiceImpl();
        IOrderService loggedOrderService = JdkProxyFactory.createLoggingProxy(orderService,
                IOrderService.class);

        System.out.println("  代理类名：" + loggedOrderService.getClass().getName());
        System.out.println("  代理类父类：" + loggedOrderService.getClass().getSuperclass().getName());
        System.out.println("  实现接口：" + loggedOrderService.getClass().getInterfaces()[0].getName());
        System.out.println();
        loggedOrderService.createOrder("MacBook Pro M3", 1, 19999.0);
        System.out.println();
        loggedOrderService.queryOrder("ORD-20240102-008");

        System.out.println("\n【演示 2：权限校验 Handler（检查当前用户是否有权限）】");
        // 模拟普通用户
        IOrderService permProxy = JdkProxyFactory.createPermissionProxy(orderService,
                IOrderService.class, "USER");
        System.out.println("  以普通用户身份调用 cancelOrder（需要 ADMIN 权限）：");
        permProxy.cancelOrder("ORD-20240102-008");

        System.out.println();
        // 模拟管理员
        IOrderService adminProxy = JdkProxyFactory.createPermissionProxy(orderService,
                IOrderService.class, "ADMIN");
        System.out.println("  以管理员身份调用 cancelOrder：");
        adminProxy.cancelOrder("ORD-20240102-008");

        System.out.println("\n【演示 3：Handler 叠加（日志 + 权限 + 事务）】");
        IUserService userService = new UserServiceImpl();
        // 对 UserService 也应用相同的日志代理，不需要新写代理类！
        IUserService loggedUserService = JdkProxyFactory.createLoggingProxy(userService,
                IUserService.class);
        System.out.println("  对 UserService 应用相同的日志代理（Handler 复用）：");
        loggedUserService.login("zhangsan", "123456");

        System.out.println("\n【JDK 动态代理的限制】");
        System.out.println("  ❌ 目标类必须实现接口");
        System.out.println("     原因：$Proxy0 已经 extends Proxy，Java 单继承");
        System.out.println("           只能通过实现接口来'假扮'成目标类型");
        System.out.println("  ❌ 如果目标类没有接口（如遗留代码中的 OrderManager 类）→ 用 CGLIB");
    }
}

// ====================================================================
// ③ CGLIB 动态代理
// ====================================================================
class CglibSection {

    static void run() {
        System.out.println("【是什么】");
        System.out.println("  CGLIB = Code Generation Library，代码生成库。");
        System.out.println("  原理：通过 ASM 字节码框架，在运行时生成目标类的'子类'来实现代理。");
        System.out.println("  不需要目标类实现接口！\n");

        System.out.println("【JDK 动态代理 vs CGLIB 对比】");
        System.out.println("  ┌──────────────┬──────────────────────┬──────────────────────┐");
        System.out.println("  │              │   JDK 动态代理        │   CGLIB              │");
        System.out.println("  ├──────────────┼──────────────────────┼──────────────────────┤");
        System.out.println("  │ 代理原理     │ 实现接口              │ 继承目标类（子类）    │");
        System.out.println("  │ 目标类要求   │ 必须实现接口          │ 不需要接口           │");
        System.out.println("  │ final 方法   │ 可以代理接口方法      │ 无法代理 final 方法  │");
        System.out.println("  │ final 类     │ 无影响（接口层面）    │ 无法代理 final 类    │");
        System.out.println("  │ 性能（旧）   │ 反射调用，稍慢        │ FastClass 直接调用   │");
        System.out.println("  │ 性能（新）   │ JDK 8+ 差距已很小    │ 差距很小             │");
        System.out.println("  │ 依赖         │ JDK 内置              │ 需要引入 cglib jar   │");
        System.out.println("  └──────────────┴──────────────────────┴──────────────────────┘\n");

        System.out.println("【CGLIB 代理的原理图】");
        System.out.println("  目标类（无接口）：");
        System.out.println("    class OrderManager {           // 没有实现任何接口");
        System.out.println("        public void placeOrder() { ... }");
        System.out.println("        public final void audit() { ... }  // final，CGLIB 无法代理");
        System.out.println("    }");
        System.out.println();
        System.out.println("  CGLIB 生成的子类（伪代码）：");
        System.out.println("    class OrderManager$$EnhancerByCGLIB extends OrderManager {");
        System.out.println("        @Override");
        System.out.println("        public void placeOrder() {");
        System.out.println("            // 调用 MethodInterceptor（类似 InvocationHandler）");
        System.out.println("            interceptor.intercept(this, method, args, proxy);");
        System.out.println("        }");
        System.out.println("        // final 方法 audit() 无法 @Override，所以无法代理！");
        System.out.println("    }\n");

        System.out.println("【用手动继承模拟 CGLIB 子类代理的思想】");
        // 用手动子类来演示 CGLIB 的核心思想（不引入实际 cglib 依赖）
        OrderManager target = new OrderManager();
        OrderManagerCglibProxy proxy = new OrderManagerCglibProxy(target);

        System.out.println("  目标类 OrderManager 没有实现任何接口：");
        System.out.println("  通过'子类代理'调用 placeOrder：");
        proxy.placeOrder("AirPods Pro", 5);
        System.out.println();
        System.out.println("  调用 final 方法 audit（子类无法覆盖，直接调用原方法，无法增强）：");
        proxy.audit("ORD-003");

        System.out.println("\n【Spring 如何选择 JDK 还是 CGLIB】");
        System.out.println("  Spring AOP 的选择策略：");
        System.out.println("  if (目标类实现了接口) {");
        System.out.println("      使用 JDK 动态代理;   // 默认");
        System.out.println("  } else {");
        System.out.println("      使用 CGLIB;          // 目标类没接口时自动切换");
        System.out.println("  }");
        System.out.println();
        System.out.println("  Spring Boot 2.x 以后默认使用 CGLIB（即使有接口也用 CGLIB）");
        System.out.println("  原因：避免使用接口代理时'同类内部调用不走 AOP'的问题");
        System.out.println();
        System.out.println("  也可以强制指定：");
        System.out.println("  @EnableAspectJAutoProxy(proxyTargetClass = true)  // 强制 CGLIB");
        System.out.println("  @EnableAspectJAutoProxy(proxyTargetClass = false) // JDK 动态代理");
    }
}

// ====================================================================
// ④ Spring AOP
// ====================================================================
class SpringAopSection {

    static void run() {
        System.out.println("【是什么】");
        System.out.println("  Spring AOP 是在 JDK 动态代理 / CGLIB 之上封装的高级框架。");
        System.out.println("  你不需要手写 InvocationHandler，只需：");
        System.out.println("  1. 用注解 @Aspect 定义切面");
        System.out.println("  2. 用 @Pointcut 表达式指定'在哪些方法上生效'");
        System.out.println("  3. 用 @Before/@After/@Around 等定义增强逻辑");
        System.out.println("  Spring 容器启动时自动生成代理，你的 Service 调用时自动走切面。\n");

        System.out.println("【Spring AOP 的五种通知类型】");
        System.out.println("  @Before        方法执行前  —— 权限校验、参数校验");
        System.out.println("  @After         方法执行后（无论是否异常）—— 资源清理");
        System.out.println("  @AfterReturning 方法正常返回后 —— 记录返回值、发通知");
        System.out.println("  @AfterThrowing 方法抛出异常后 —— 告警、记录异常");
        System.out.println("  @Around        环绕，最强大 —— 日志、事务、性能监控\n");

        System.out.println("【Spring AOP 的写法（伪代码，展示真实项目用法）】");
        printSpringAopCode();

        System.out.println("\n【模拟执行效果（用我们自己的动态代理模拟 Spring AOP）】");
        simulateSpringAop();

        System.out.println("\n【真实项目中 Spring AOP 的 5 大应用场景】");
        printRealWorldScenarios();

        System.out.println("\n【Spring AOP 的局限性（重要！）】");
        printLimitations();

        System.out.println("\n【总结：四个阶段的关系】");
        printSummary();
    }

    private static void printSpringAopCode() {
        System.out.println("  // ① 定义日志切面（真实项目代码）");
        System.out.println("  @Aspect");
        System.out.println("  @Component");
        System.out.println("  public class LoggingAspect {");
        System.out.println();
        System.out.println("      // 切点：service 包下所有类的所有 public 方法");
        System.out.println("      @Pointcut(\"execution(public * com.example.service..*.*(..))\")");
        System.out.println("      public void serviceLayer() {}");
        System.out.println();
        System.out.println("      // 环绕通知：最常用，可以控制是否执行目标方法");
        System.out.println("      @Around(\"serviceLayer()\")");
        System.out.println("      public Object around(ProceedingJoinPoint pjp) throws Throwable {");
        System.out.println("          String method = pjp.getSignature().getName();");
        System.out.println("          Object[] args = pjp.getArgs();");
        System.out.println("          long start = System.currentTimeMillis();");
        System.out.println();
        System.out.println("          log.info(\"[请求] 方法={}, 参数={}\", method, args);");
        System.out.println("          try {");
        System.out.println("              Object result = pjp.proceed(); // 调用真实方法");
        System.out.println("              log.info(\"[响应] 方法={}, 耗时={}ms\", method,");
        System.out.println("                        System.currentTimeMillis() - start);");
        System.out.println("              return result;");
        System.out.println("          } catch (Throwable e) {");
        System.out.println("              log.error(\"[异常] 方法={}, 异常={}\", method, e.getMessage());");
        System.out.println("              throw e;");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // ② 权限校验切面（基于自定义注解）");
        System.out.println("  @Aspect");
        System.out.println("  @Component");
        System.out.println("  public class PermissionAspect {");
        System.out.println();
        System.out.println("      // 切点：所有标注了 @RequireLogin 注解的方法");
        System.out.println("      @Before(\"@annotation(com.example.annotation.RequireLogin)\")");
        System.out.println("      public void checkLogin(JoinPoint jp) {");
        System.out.println("          String token = RequestContextHolder.getToken();");
        System.out.println("          if (!tokenService.isValid(token)) {");
        System.out.println("              throw new UnauthorizedException(\"请先登录\");");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // ③ 业务代码（极其干净，无任何横切逻辑）");
        System.out.println("  @Service");
        System.out.println("  public class OrderServiceImpl implements IOrderService {");
        System.out.println();
        System.out.println("      @RequireLogin              // 声明需要登录");
        System.out.println("      @Transactional             // 声明需要事务（Spring 内置 AOP）");
        System.out.println("      public void createOrder(Order order) {");
        System.out.println("          orderRepository.save(order);  // 只有这一行业务代码！");
        System.out.println("      }");
        System.out.println("  }");
    }

    private static void simulateSpringAop() {
        // 用 JDK 动态代理模拟 Spring AOP 的执行效果
        IOrderService real = new OrderServiceImpl();

        // 模拟 Spring 容器：自动叠加多个切面
        // 切面顺序（从外到内）：日志 → 权限 → 事务 → 目标方法
        InvocationHandler txHandler   = new TxInvocationHandler(real);
        IOrderService txProxy = (IOrderService) Proxy.newProxyInstance(
                IOrderService.class.getClassLoader(),
                new Class[]{IOrderService.class}, txHandler);

        InvocationHandler permHandler = new PermCheckInvocationHandler(txProxy, "ADMIN");
        IOrderService permProxy = (IOrderService) Proxy.newProxyInstance(
                IOrderService.class.getClassLoader(),
                new Class[]{IOrderService.class}, permHandler);

        InvocationHandler logHandler  = new FullLoggingInvocationHandler(permProxy);
        IOrderService finalProxy = (IOrderService) Proxy.newProxyInstance(
                IOrderService.class.getClassLoader(),
                new Class[]{IOrderService.class}, logHandler);

        System.out.println("  Spring 容器启动，自动为 OrderServiceImpl 创建代理，叠加三个切面：");
        System.out.println("  [日志切面] → [权限切面] → [事务切面] → 真实 OrderServiceImpl\n");
        System.out.println("  业务代码调用（调用方感知不到任何代理）：\n");
        finalProxy.createOrder("iPad Pro M4", 2, 9998.0);
        System.out.println();
        System.out.println("  调用结果：业务代码没有一行日志/权限/事务代码，但全部自动执行了！");
    }

    private static void printRealWorldScenarios() {
        System.out.println("  场景 1：统一接口日志（最常见）");
        System.out.println("    问题：线上问题排查时不知道入参是什么、哪步出错");
        System.out.println("    方案：@Around 切面自动记录所有 Controller/Service 的入参、出参、耗时");
        System.out.println("    效果：不改一行业务代码，全量接口日志自动采集");
        System.out.println();
        System.out.println("  场景 2：@Transactional 事务管理（Spring 内置 AOP）");
        System.out.println("    你写：@Transactional public void transferMoney(...) {}");
        System.out.println("    Spring 做：方法开始前 BEGIN，正常结束 COMMIT，异常 ROLLBACK");
        System.out.println("    底层：TransactionInterceptor 就是一个 AOP 的 Around 切面");
        System.out.println();
        System.out.println("  场景 3：接口限流（防止刷单）");
        System.out.println("    自定义注解：@RateLimit(permitsPerSecond = 10)");
        System.out.println("    @Before 切面：检查当前用户 1 秒内的请求次数，超过则拒绝");
        System.out.println("    效果：在注解上一标，接口自动限流，不改业务代码");
        System.out.println();
        System.out.println("  场景 4：接口幂等性（防止重复提交）");
        System.out.println("    自定义注解：@Idempotent");
        System.out.println("    @Around 切面：用请求唯一 ID 在 Redis 做去重，已处理的直接返回");
        System.out.println("    效果：支付/下单接口天然防重，一行注解搞定");
        System.out.println();
        System.out.println("  场景 5：数据脱敏（返回结果自动脱敏）");
        System.out.println("    @AfterReturning 切面：拦截返回值，自动把手机号/身份证号脱敏");
        System.out.println("    效果：业务代码直接返回原始数据，切面自动处理敏感字段");
    }

    private static void printLimitations() {
        System.out.println("  ❌ 限制 1：同类内部方法调用不走 AOP（最常见的坑！）");
        System.out.println("     @Service");
        System.out.println("     class OrderService {");
        System.out.println("         public void createOrder() {");
        System.out.println("             this.sendNotification(); // ❌ 内部调用，不走代理，AOP 不生效！");
        System.out.println("         }");
        System.out.println("         @RequireLogin");
        System.out.println("         public void sendNotification() { ... }");
        System.out.println("     }");
        System.out.println("     原因：this 指向真实对象，不是代理对象，切面绕过了");
        System.out.println("     解法：注入自身（@Autowired OrderService self; self.sendNotification()）");
        System.out.println("           或者把 sendNotification 提取到另一个 Service 中");
        System.out.println();
        System.out.println("  ❌ 限制 2：private 方法不走 AOP");
        System.out.println("     CGLIB 代理是子类，无法覆盖父类的 private 方法");
        System.out.println("     JDK 代理基于接口，接口只有 public 方法");
        System.out.println();
        System.out.println("  ❌ 限制 3：final 类 / final 方法不走 CGLIB 代理");
        System.out.println("     子类无法 override final 方法，所以无法增强");
    }

    private static void printSummary() {
        System.out.println("  ┌──────────────┬────────────┬──────────────┬──────────────────┐");
        System.out.println("  │              │ 静态代理    │ JDK 动态代理  │  CGLIB 动态代理  │");
        System.out.println("  ├──────────────┼────────────┼──────────────┼──────────────────┤");
        System.out.println("  │ 代理类来源   │ 手动编写   │ 运行时自动生成│ 运行时自动生成   │");
        System.out.println("  │ 目标类要求   │ 需实现接口  │ 需实现接口   │ 不需要接口       │");
        System.out.println("  │ 代码量       │ 多（每个接口│ 少（一个     │ 少（一个         │");
        System.out.println("  │              │ 一个代理类）│ Handler）    │ Interceptor）    │");
        System.out.println("  │ Spring 关系  │ 基础思想   │ Spring 底层  │ Spring 底层      │");
        System.out.println("  └──────────────┴────────────┴──────────────┴──────────────────┘");
        System.out.println();
        System.out.println("  演进路线：");
        System.out.println("  静态代理（理解思想）");
        System.out.println("      ↓ 解决类爆炸问题");
        System.out.println("  JDK 动态代理（基于接口，JDK 内置）");
        System.out.println("      ↓ 解决目标类无接口的问题");
        System.out.println("  CGLIB 动态代理（基于子类，无需接口）");
        System.out.println("      ↓ 在两者之上封装，提供声明式 AOP 编程体验");
        System.out.println("  Spring AOP（自动选择 JDK 或 CGLIB，注解驱动，极简使用）");
    }
}

// ====================================================================
// 公共接口定义
// ====================================================================
interface IOrderService {
    void createOrder(String product, int quantity, double price);
    void cancelOrder(String orderId);
    String queryOrder(String orderId);
}

interface IUserService {
    boolean login(String username, String password);
    void logout(String username);
}

// ====================================================================
// 真实业务实现类（只有业务逻辑，无任何横切代码）
// ====================================================================
class OrderServiceImpl implements IOrderService {
    @Override
    public void createOrder(String product, int quantity, double price) {
        System.out.println("    [业务] 创建订单：" + product
                + " × " + quantity + "，金额=" + price * quantity + "元");
    }

    @Override
    public void cancelOrder(String orderId) {
        System.out.println("    [业务] 取消订单：" + orderId);
    }

    @Override
    public String queryOrder(String orderId) {
        System.out.println("    [业务] 查询订单：" + orderId);
        return "Order{id=" + orderId + ", status=COMPLETED}";
    }
}

class UserServiceImpl implements IUserService {
    @Override
    public boolean login(String username, String password) {
        System.out.println("    [业务] 用户登录：" + username);
        return true;
    }

    @Override
    public void logout(String username) {
        System.out.println("    [业务] 用户登出：" + username);
    }
}

// ====================================================================
// 没有接口的目标类（CGLIB 适用场景）
// ====================================================================
class OrderManager {
    public void placeOrder(String product, int qty) {
        System.out.println("    [业务] 下单：" + product + " × " + qty);
    }

    // final 方法，CGLIB 无法代理
    public final void audit(String orderId) {
        System.out.println("    [业务] 审计订单：" + orderId + "（final方法，无法被代理增强）");
    }
}

// ====================================================================
// 静态代理：手写的代理类
// ====================================================================
class OrderServiceStaticProxy implements IOrderService {
    private final IOrderService target;

    public OrderServiceStaticProxy(IOrderService target) {
        this.target = target;
    }

    @Override
    public void createOrder(String product, int quantity, double price) {
        long start = System.currentTimeMillis();
        System.out.println("  [静态代理-日志] createOrder 开始，参数：" + product + "," + quantity + "," + price);
        target.createOrder(product, quantity, price);
        System.out.println("  [静态代理-日志] createOrder 结束，耗时："
                + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void cancelOrder(String orderId) {
        System.out.println("  [静态代理-日志] cancelOrder 开始");
        target.cancelOrder(orderId);
        System.out.println("  [静态代理-日志] cancelOrder 结束");
    }

    @Override
    public String queryOrder(String orderId) {
        System.out.println("  [静态代理-日志] queryOrder 开始");
        String result = target.queryOrder(orderId);
        System.out.println("  [静态代理-日志] queryOrder 结束，返回：" + result);
        return result;
    }
    // ❌ IOrderService 每新增一个方法，这里就要新增一个对应的代理方法
}

// ====================================================================
// JDK 动态代理工厂
// ====================================================================
class JdkProxyFactory {

    /** 创建日志代理 —— 适用于任意接口 */
    @SuppressWarnings("unchecked")
    static <T> T createLoggingProxy(T target, Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new FullLoggingInvocationHandler(target)
        );
    }

    /** 创建权限校验代理 */
    @SuppressWarnings("unchecked")
    static <T> T createPermissionProxy(T target, Class<T> interfaceClass, String role) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new PermCheckInvocationHandler(target, role)
        );
    }
}

// ====================================================================
// JDK 动态代理：完整日志 Handler（入参 + 出参 + 耗时）
// ====================================================================
class FullLoggingInvocationHandler implements InvocationHandler {
    private final Object target;

    FullLoggingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        System.out.println("  [日志切面] >>> 方法：" + method.getName()
                + "，入参：" + argsToString(args));
        Object result;
        try {
            result = method.invoke(target, args);
            System.out.println("  [日志切面] <<< 方法：" + method.getName()
                    + "，耗时：" + (System.currentTimeMillis() - start) + "ms"
                    + (result != null ? "，返回：" + result : ""));
        } catch (Exception e) {
            System.out.println("  [日志切面] !!! 方法：" + method.getName()
                    + " 抛出异常：" + e.getCause().getMessage());
            throw e;
        }
        return result;
    }

    private String argsToString(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i]);
            if (i < args.length - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}

// ====================================================================
// JDK 动态代理：权限校验 Handler
// ====================================================================
class PermCheckInvocationHandler implements InvocationHandler {
    private final Object target;
    private final String currentRole;

    // 需要 ADMIN 权限的方法集合
    private static final java.util.Set<String> ADMIN_METHODS =
            new java.util.HashSet<>(java.util.Arrays.asList("cancelOrder"));

    PermCheckInvocationHandler(Object target, String currentRole) {
        this.target = target;
        this.currentRole = currentRole;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ADMIN_METHODS.contains(method.getName()) && !"ADMIN".equals(currentRole)) {
            System.out.println("  [权限切面] 拒绝访问：方法 " + method.getName()
                    + " 需要 ADMIN 权限，当前角色：" + currentRole);
            return null;
        }
        System.out.println("  [权限切面] 权限校验通过，角色：" + currentRole);
        return method.invoke(target, args);
    }
}

// ====================================================================
// JDK 动态代理：事务 Handler
// ====================================================================
class TxInvocationHandler implements InvocationHandler {
    private final Object target;

    TxInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("  [事务切面] BEGIN TRANSACTION");
        Object result;
        try {
            result = method.invoke(target, args);
            System.out.println("  [事务切面] COMMIT");
        } catch (Exception e) {
            System.out.println("  [事务切面] ROLLBACK");
            throw e;
        }
        return result;
    }
}

// ====================================================================
// 模拟 CGLIB 子类代理（手动实现思想，真实 CGLIB 由字节码引擎生成）
// ====================================================================
class OrderManagerCglibProxy extends OrderManager {
    private final OrderManager target;

    OrderManagerCglibProxy(OrderManager target) {
        this.target = target;
    }

    // CGLIB 通过 @Override 子类方法来插入增强逻辑
    @Override
    public void placeOrder(String product, int qty) {
        long start = System.currentTimeMillis();
        System.out.println("  [CGLIB代理-日志] placeOrder 开始（目标类无接口，通过继承代理）");
        target.placeOrder(product, qty);
        System.out.println("  [CGLIB代理-日志] placeOrder 结束，耗时："
                + (System.currentTimeMillis() - start) + "ms");
    }

    // ❌ final 方法无法 @Override，CGLIB 无法增强
    // audit() 方法直接调用父类，切面代码无处插入
}

