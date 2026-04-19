package org.example.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring AOP 5 大真实项目场景 深度展开
 *
 * 场景 1：统一接口日志（MDC 链路追踪）
 * 场景 2：@Transactional 事务管理（含 6 大失效场景）
 * 场景 3：接口限流（令牌桶算法）
 * 场景 4：接口幂等性（Redis Token 去重）
 * 场景 5：数据脱敏（返回值自动处理）
 */
public class SpringAopScenariosDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== Spring AOP 5 大真实项目场景 深度展开 ==========\n");

        // 场景 1
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 场景 1：统一接口日志（全链路追踪）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        Scenario1Logging.run();

        // 场景 2
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 场景 2：@Transactional 事务管理（含失效场景）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        Scenario2Transaction.run();

        // 场景 3
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 场景 3：接口限流（令牌桶算法）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        Scenario3RateLimit.run();

        // 场景 4
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 场景 4：接口幂等性（防止重复提交）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        Scenario4Idempotent.run();

        // 场景 5
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 场景 5：数据脱敏（返回值自动处理）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        Scenario5DataMasking.run();
    }
}

// =====================================================================
// 场景 1：统一接口日志（全链路追踪）
// =====================================================================
class Scenario1Logging {

    static void run() {
        System.out.println("【背景】");
        System.out.println("  线上故障排查时，最痛苦的是：不知道哪个请求出了问题，");
        System.out.println("  日志里一堆线程混在一起，不知道一次请求经历了哪些服务调用。");
        System.out.println("  解决方案：每个请求生成唯一 TraceId，贯穿整个调用链。\n");

        System.out.println("【Spring AOP 实现方式（伪代码）】");
        System.out.println("  @Around(\"execution(* com.example.controller..*.*(..)) ||\"");
        System.out.println("         \"execution(* com.example.service..*.*(..))\")");
        System.out.println("  public Object trace(ProceedingJoinPoint pjp) throws Throwable {");
        System.out.println("      // 1. 生成 TraceId（或从 HTTP Header 中读取上游传来的）");
        System.out.println("      String traceId = MDC.get(\"traceId\");");
        System.out.println("      if (traceId == null) {");
        System.out.println("          traceId = UUID.randomUUID().toString().replace(\"-\",\"\");");
        System.out.println("          MDC.put(\"traceId\", traceId);   // 存入 MDC（线程本地）");
        System.out.println("      }");
        System.out.println("      // 2. 记录请求信息");
        System.out.println("      String method = pjp.getSignature().toShortString();");
        System.out.println("      log.info(\"[{}] >>> {}, args={}\", traceId, method, pjp.getArgs());");
        System.out.println("      long start = System.currentTimeMillis();");
        System.out.println("      try {");
        System.out.println("          Object result = pjp.proceed();");
        System.out.println("          // 3. 记录响应信息");
        System.out.println("          log.info(\"[{}] <<< {}, cost={}ms, result={}\",");
        System.out.println("                   traceId, method, System.currentTimeMillis()-start, result);");
        System.out.println("          return result;");
        System.out.println("      } catch (Throwable t) {");
        System.out.println("          // 4. 记录异常信息");
        System.out.println("          log.error(\"[{}] !!! {}, cost={}ms, error={}\",");
        System.out.println("                    traceId, method, System.currentTimeMillis()-start, t.getMessage());");
        System.out.println("          throw t;");
        System.out.println("      } finally {");
        System.out.println("          MDC.clear(); // 请求结束清理，防止内存泄漏");
        System.out.println("      }");
        System.out.println("  }\n");

        System.out.println("【模拟执行效果】");

        // 用 JDK 动态代理模拟
        IEcommerceOrderService orderSvc = new EcommerceOrderServiceImpl();
        IEcommerceOrderService proxy = (IEcommerceOrderService) Proxy.newProxyInstance(
                IEcommerceOrderService.class.getClassLoader(),
                new Class[]{IEcommerceOrderService.class},
                new TraceLoggingHandler(orderSvc)
        );

        System.out.println("  请求 1（用户A 下单）：");
        proxy.createOrder("user_A", "iPhone 15 Pro", 2, 17998.0);

        System.out.println();
        System.out.println("  请求 2（用户B 下单，与请求1并发，TraceId 不同）：");
        proxy.createOrder("user_B", "MacBook Air", 1, 9999.0);

        System.out.println();
        System.out.println("  请求 3（查询订单，发生异常的情况）：");
        proxy.queryOrder("INVALID_ORDER_ID");

        System.out.println("\n【MDC 的核心价值】");
        System.out.println("  • 每个请求有唯一 TraceId，在 Kibana/ELK 中搜索 traceId=xxx");
        System.out.println("    就能看到这个请求的完整调用链，无论经过多少个 Service");
        System.out.println("  • 多线程并发时，各自的 TraceId 互不干扰（ThreadLocal 存储）");
        System.out.println("  • 配合 Feign/RestTemplate 把 TraceId 传递到下游微服务");
        System.out.println("    实现跨服务的全链路追踪（类似 Sleuth/SkyWalking 的原理）");

        System.out.println("\n【日志格式对比】");
        System.out.println("  没有 TraceId（难以排查）：");
        System.out.println("    13:00:01 INFO  createOrder args=[userA, iPhone, 2]");
        System.out.println("    13:00:01 INFO  createOrder args=[userB, Mac, 1]");
        System.out.println("    13:00:01 ERROR queryOrder error=Order not found");
        System.out.println("    ← 哪条日志属于哪个请求？完全看不出来！");
        System.out.println();
        System.out.println("  有 TraceId（一目了然）：");
        System.out.println("    13:00:01 [abc123] INFO  createOrder args=[userA, iPhone, 2]");
        System.out.println("    13:00:01 [def456] INFO  createOrder args=[userB, Mac, 1]");
        System.out.println("    13:00:01 [abc123] INFO  createOrder cost=12ms result=ORD-001");
        System.out.println("    13:00:01 [ghi789] ERROR queryOrder cost=1ms error=Order not found");
        System.out.println("    ← 搜索 [abc123] 就能看到 userA 请求的完整日志链");
    }
}

// =====================================================================
// 场景 2：@Transactional 事务管理
// =====================================================================
class Scenario2Transaction {

    static void run() {
        System.out.println("【背景】");
        System.out.println("  @Transactional 是 Spring 最核心的 AOP 应用，底层是 TransactionInterceptor。");
        System.out.println("  它就是一个 @Around 切面，方法开始 BEGIN，正常结束 COMMIT，异常 ROLLBACK。\n");

        System.out.println("【@Transactional 底层等价于】");
        System.out.println("  // Spring 自动生成的 Around 切面（TransactionInterceptor）");
        System.out.println("  public Object invoke(ProceedingJoinPoint pjp) throws Throwable {");
        System.out.println("      TransactionStatus tx = txManager.getTransaction(txAttr);");
        System.out.println("      try {");
        System.out.println("          Object result = pjp.proceed(); // 执行业务方法");
        System.out.println("          txManager.commit(tx);          // 正常：提交");
        System.out.println("          return result;");
        System.out.println("      } catch (RuntimeException | Error e) {");
        System.out.println("          txManager.rollback(tx);        // 异常：回滚");
        System.out.println("          throw e;");
        System.out.println("      }");
        System.out.println("  }\n");

        System.out.println("【演示：事务的正常流程 vs 异常回滚】");
        TransferService svc = new TransferServiceImpl();
        TransferService proxy = (TransferService) Proxy.newProxyInstance(
                TransferService.class.getClassLoader(),
                new Class[]{TransferService.class},
                new TransactionAspectHandler(svc)
        );

        System.out.println("  --- 正常转账（COMMIT）---");
        proxy.transfer("Alice", "Bob", 500.0);

        System.out.println();
        System.out.println("  --- 余额不足转账（ROLLBACK）---");
        proxy.transfer("Alice", "Bob", 999999.0);

        System.out.println("\n【@Transactional 的 6 大失效场景（面试高频）】\n");

        System.out.println("  ❌ 失效 1：同类内部方法调用（最常见！）");
        System.out.println("     @Service class OrderService {");
        System.out.println("         public void createOrder() {");
        System.out.println("             this.updateStock();  // ← this 是真实对象，不走代理！");
        System.out.println("         }");
        System.out.println("         @Transactional");
        System.out.println("         public void updateStock() { ... }  // 事务不生效！");
        System.out.println("     }");
        System.out.println("     原因：createOrder 直接调用 this.updateStock()，");
        System.out.println("           绕过了 Spring 的代理对象，事务切面未被触发");
        System.out.println("     解法：把 updateStock() 移到另一个 Service");
        System.out.println("           或注入自身：@Autowired OrderService self;");
        System.out.println("                        self.updateStock(); // 走代理 ✅");
        System.out.println();

        System.out.println("  ❌ 失效 2：方法是 private 的");
        System.out.println("     @Transactional");
        System.out.println("     private void saveOrder() { ... }  // 代理无法拦截 private 方法");
        System.out.println("     必须是 public 方法才能被代理增强");
        System.out.println();

        System.out.println("  ❌ 失效 3：异常被 catch 吞掉，没有重新抛出");
        System.out.println("     @Transactional");
        System.out.println("     public void createOrder() {");
        System.out.println("         try {");
        System.out.println("             db.save(order);");
        System.out.println("         } catch (Exception e) {");
        System.out.println("             log.error(e);  // ← 异常被吞，事务以为成功，不会回滚！");
        System.out.println("         }");
        System.out.println("     }");
        System.out.println("     解法：catch 之后 throw e; 或 throw new BusinessException(e);");
        System.out.println();

        System.out.println("  ❌ 失效 4：异常类型不匹配（默认只回滚 RuntimeException）");
        System.out.println("     @Transactional");
        System.out.println("     public void createOrder() throws Exception {");
        System.out.println("         throw new Exception(\"受检异常\");  // ← 不回滚！");
        System.out.println("     }");
        System.out.println("     解法：@Transactional(rollbackFor = Exception.class)");
        System.out.println("           显式指定受检异常也触发回滚");
        System.out.println();

        System.out.println("  ❌ 失效 5：数据库引擎不支持事务（如 MyISAM）");
        System.out.println("     MySQL 的 MyISAM 引擎不支持事务，@Transactional 无效");
        System.out.println("     必须使用 InnoDB 引擎");
        System.out.println();

        System.out.println("  ❌ 失效 6：多线程场景（子线程不继承父线程的事务）");
        System.out.println("     @Transactional");
        System.out.println("     public void batchProcess() {");
        System.out.println("         new Thread(() -> db.save(item)).start(); // ← 新线程，无事务！");
        System.out.println("     }");
        System.out.println("     原因：事务绑定在 ThreadLocal，子线程有自己的 ThreadLocal");

        System.out.println("\n【传播行为（Propagation）】");
        System.out.println("  REQUIRED（默认）：有事务就加入，没有就新建 —— 99% 的场景用这个");
        System.out.println("  REQUIRES_NEW：    挂起当前事务，新建独立事务 —— 记录操作日志（即使业务失败也要保留日志）");
        System.out.println("  NESTED：          嵌套事务，外层回滚内层也回滚，内层回滚外层不受影响");
        System.out.println("  NOT_SUPPORTED：   非事务方式执行，挂起当前事务 —— 读多写少的查询");
        System.out.println();
        System.out.println("  真实场景：下单扣库存（主事务）+ 记录操作日志（REQUIRES_NEW）");
        System.out.println("    即使下单失败回滚，操作日志也能独立提交，便于审计");
    }
}

// =====================================================================
// 场景 3：接口限流（令牌桶算法）
// =====================================================================
class Scenario3RateLimit {

    static void run() throws InterruptedException {
        System.out.println("【背景】");
        System.out.println("  电商大促（双11）时，秒杀接口可能瞬间涌入百万请求。");
        System.out.println("  如果没有限流，数据库直接被打垮，整个系统雪崩。");
        System.out.println("  解决方案：AOP 切面 + 令牌桶算法，每秒最多放行 N 个请求。\n");

        System.out.println("【令牌桶算法原理】");
        System.out.println("  • 桶里有固定容量的令牌（如 10 个）");
        System.out.println("  • 每秒向桶里补充固定数量的令牌（如每 100ms 补 1 个）");
        System.out.println("  • 每个请求消耗 1 个令牌，令牌不足则拒绝请求");
        System.out.println("  • 令牌桶可以应对突发流量（桶满时可以瞬间处理 10 个请求）\n");

        System.out.println("【Spring AOP 实现（伪代码）】");
        System.out.println("  // 自定义限流注解");
        System.out.println("  @Target(ElementType.METHOD)");
        System.out.println("  @Retention(RetentionPolicy.RUNTIME)");
        System.out.println("  public @interface RateLimit {");
        System.out.println("      int permitsPerSecond() default 10; // 每秒允许的请求数");
        System.out.println("      String key() default \"\";           // 限流维度 key");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // 限流切面");
        System.out.println("  @Aspect @Component");
        System.out.println("  public class RateLimitAspect {");
        System.out.println("      // 每个方法独立的限流器");
        System.out.println("      private Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();");
        System.out.println();
        System.out.println("      @Around(\"@annotation(rateLimit)\")");
        System.out.println("      public Object limit(ProceedingJoinPoint pjp,");
        System.out.println("                          RateLimit rateLimit) throws Throwable {");
        System.out.println("          String key = pjp.getSignature().toShortString();");
        System.out.println("          RateLimiter limiter = limiters.computeIfAbsent(key,");
        System.out.println("              k -> RateLimiter.create(rateLimit.permitsPerSecond()));");
        System.out.println("          if (!limiter.tryAcquire()) {   // 尝试获取令牌");
        System.out.println("              throw new TooManyRequestsException(\"请求过于频繁，请稍后重试\");");
        System.out.println("          }");
        System.out.println("          return pjp.proceed();");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // 业务接口只需加一个注解");
        System.out.println("  @RateLimit(permitsPerSecond = 5) // 每秒最多 5 个请求");
        System.out.println("  public String seckill(String userId, String itemId) { ... }");
        System.out.println();

        System.out.println("【模拟执行效果（令牌桶限流，容量=3，每秒补充2个令牌）】");

        ISeckillService svc = new SeckillServiceImpl();
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 2);
        ISeckillService proxy = (ISeckillService) Proxy.newProxyInstance(
                ISeckillService.class.getClassLoader(),
                new Class[]{ISeckillService.class},
                new RateLimitHandler(svc, limiter)
        );

        System.out.println("  瞬间发送 7 个请求（令牌桶容量=3）：\n");
        for (int i = 1; i <= 7; i++) {
            System.out.print("  请求 " + i + "：");
            proxy.seckill("user_" + i, "iPhone15");
        }

        System.out.println("\n  等待 1 秒（补充令牌）后再请求：");
        Thread.sleep(1100);
        System.out.print("  请求 8（等待补充后）：");
        proxy.seckill("user_8", "iPhone15");

        System.out.println("\n【进阶：基于用户维度的限流（防单用户刷接口）】");
        System.out.println("  @Around(\"@annotation(rateLimit)\")");
        System.out.println("  public Object limit(...) {");
        System.out.println("      // 从 ThreadLocal 取当前用户 ID，每个用户独立限流");
        System.out.println("      String userId = UserContext.getCurrentUserId();");
        System.out.println("      String key = methodName + \":\" + userId;  // 如 seckill:user_001");
        System.out.println("      // 用 Redis 实现分布式限流（单机令牌桶在多实例时不准）");
        System.out.println("      boolean allowed = redisLimiter.tryAcquire(key, 5, 1, TimeUnit.SECONDS);");
        System.out.println("      if (!allowed) throw new TooManyRequestsException();");
        System.out.println("      return pjp.proceed();");
        System.out.println("  }");
    }
}

// =====================================================================
// 场景 4：接口幂等性
// =====================================================================
class Scenario4Idempotent {

    static void run() {
        System.out.println("【背景】");
        System.out.println("  用户点击'支付'按钮，因网络慢，以为没成功，又点了一次。");
        System.out.println("  或者 MQ 消息重投导致订单被创建两次。");
        System.out.println("  如果没有幂等保护 → 扣款两次、创建两笔订单！\n");

        System.out.println("【幂等性定义】");
        System.out.println("  同一个操作执行多次，结果和执行一次完全相同。");
        System.out.println("  GET 天然幂等，POST/PUT 需要额外保证。\n");

        System.out.println("【AOP 实现方案（Token + Redis 去重）】");
        System.out.println("  流程：");
        System.out.println("  1. 前端请求接口前，先调 /api/token 获取唯一 token");
        System.out.println("  2. 提交请求时把 token 放入 Header");
        System.out.println("  3. 切面拦截，检查 Redis 中是否存在该 token");
        System.out.println("     • 存在 → 首次请求，删除 token，放行");
        System.out.println("     • 不存在 → 重复请求，直接返回上次结果");
        System.out.println();
        System.out.println("  // 幂等注解");
        System.out.println("  @Idempotent(expireSeconds = 60)  // token 有效期 60 秒");
        System.out.println("  public OrderResult createOrder(CreateOrderRequest req) { ... }");
        System.out.println();
        System.out.println("  // 幂等切面（Spring AOP 实现）");
        System.out.println("  @Around(\"@annotation(idempotent)\")");
        System.out.println("  public Object check(ProceedingJoinPoint pjp,");
        System.out.println("                      Idempotent idempotent) throws Throwable {");
        System.out.println("      String token = request.getHeader(\"Idempotency-Key\");");
        System.out.println("      if (token == null) throw new IllegalArgumentException(\"缺少幂等 key\");");
        System.out.println();
        System.out.println("      // Redis SET token result NX EX 60（原子操作，防并发重复）");
        System.out.println("      String existing = redis.get(\"idempotent:\" + token);");
        System.out.println("      if (existing != null) {");
        System.out.println("          // 重复请求：直接返回上次缓存的结果");
        System.out.println("          return JSON.parse(existing, returnType);");
        System.out.println("      }");
        System.out.println("      Object result = pjp.proceed();");
        System.out.println("      // 把结果存入 Redis，供后续重复请求复用");
        System.out.println("      redis.setex(\"idempotent:\" + token,");
        System.out.println("                  idempotent.expireSeconds(), JSON.toJSON(result));");
        System.out.println("      return result;");
        System.out.println("  }");
        System.out.println();

        System.out.println("【模拟执行效果】");

        IPaymentService svc = new PaymentServiceImpl();
        IdempotentStore store = new IdempotentStore(); // 模拟 Redis
        IPaymentService proxy = (IPaymentService) Proxy.newProxyInstance(
                IPaymentService.class.getClassLoader(),
                new Class[]{IPaymentService.class},
                new IdempotentHandler(svc, store)
        );

        String token1 = "TOKEN-" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("  生成幂等 Token：" + token1 + "\n");

        System.out.println("  --- 第 1 次调用（正常请求）---");
        String result1 = proxy.pay(token1, "user_001", 299.0);
        System.out.println("  返回结果：" + result1);

        System.out.println();
        System.out.println("  --- 第 2 次调用（网络重试，相同 token）---");
        String result2 = proxy.pay(token1, "user_001", 299.0);
        System.out.println("  返回结果：" + result2);
        System.out.println("  两次结果相同：" + result1.equals(result2) + "，扣款仅发生一次 ✅");

        System.out.println();
        System.out.println("  --- 第 3 次调用（全新请求，新 token）---");
        String token2 = "TOKEN-" + UUID.randomUUID().toString().substring(0, 8);
        String result3 = proxy.pay(token2, "user_001", 299.0);
        System.out.println("  返回结果：" + result3);

        System.out.println("\n【与数据库唯一索引方案的对比】");
        System.out.println("  唯一索引方案：在订单表加唯一约束（如 order_no 唯一）");
        System.out.println("    优点：简单，依赖数据库保证");
        System.out.println("    缺点：重复请求会打到数据库，产生 SQL 错误，需要捕获异常");
        System.out.println();
        System.out.println("  Token + Redis 方案（AOP 实现）：");
        System.out.println("    优点：在 AOP 层直接拦截，重复请求不落库");
        System.out.println("          返回值可以复用，对调用方透明（返回一样的结果）");
        System.out.println("    缺点：依赖 Redis，需要前端先获取 token");
    }
}

// =====================================================================
// 场景 5：数据脱敏
// =====================================================================
class Scenario5DataMasking {

    static void run() {
        System.out.println("【背景】");
        System.out.println("  用户信息接口返回手机号、身份证、银行卡号等敏感数据。");
        System.out.println("  合规要求：接口响应必须脱敏，如 138****8888、6222****1234。");
        System.out.println("  如果在每个 Service 方法里手动脱敏 → 遗漏风险极高！\n");

        System.out.println("【AOP 实现方案（@AfterReturning 拦截返回值）】");
        System.out.println("  // 脱敏注解（标记在需要脱敏的字段上）");
        System.out.println("  @Target(ElementType.FIELD)");
        System.out.println("  @Retention(RetentionPolicy.RUNTIME)");
        System.out.println("  public @interface Sensitive {");
        System.out.println("      SensitiveType type(); // PHONE / ID_CARD / BANK_CARD");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // 在 DTO 字段上标注");
        System.out.println("  public class UserDTO {");
        System.out.println("      private String name;");
        System.out.println("      @Sensitive(type = SensitiveType.PHONE)");
        System.out.println("      private String phone;       // 138****8888");
        System.out.println("      @Sensitive(type = SensitiveType.ID_CARD)");
        System.out.println("      private String idCard;      // 110***********1234");
        System.out.println("      @Sensitive(type = SensitiveType.BANK_CARD)");
        System.out.println("      private String bankCard;    // 6222****1234");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // 脱敏切面（用反射扫描返回值的字段，自动脱敏）");
        System.out.println("  @AfterReturning(pointcut = \"@annotation(DataMasking)\",");
        System.out.println("                  returning = \"result\")");
        System.out.println("  public void mask(Object result) {");
        System.out.println("      if (result == null) return;");
        System.out.println("      for (Field field : result.getClass().getDeclaredFields()) {");
        System.out.println("          Sensitive ann = field.getAnnotation(Sensitive.class);");
        System.out.println("          if (ann == null) continue;");
        System.out.println("          field.setAccessible(true);");
        System.out.println("          String value = (String) field.get(result);");
        System.out.println("          field.set(result, SensitiveUtils.mask(ann.type(), value));");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();

        System.out.println("【模拟执行效果】");

        IUserInfoService svc = new UserInfoServiceImpl();
        IUserInfoService proxy = (IUserInfoService) Proxy.newProxyInstance(
                IUserInfoService.class.getClassLoader(),
                new Class[]{IUserInfoService.class},
                new DataMaskingHandler(svc)
        );

        System.out.println("  查询用户信息（自动脱敏）：\n");
        UserInfo user = proxy.getUserInfo("user_001");
        System.out.println("  返回结果：");
        System.out.println("    姓名：" + user.getName());
        System.out.println("    手机：" + user.getPhone()    + "  （原始：" + user.getOriginalPhone() + "）");
        System.out.println("    身份证：" + user.getIdCard() + "  （原始：" + user.getOriginalIdCard() + "）");
        System.out.println("    银行卡：" + user.getBankCard()+ "  （原始：" + user.getOriginalBankCard() + "）");

        System.out.println("\n  查询另一个用户：\n");
        UserInfo user2 = proxy.getUserInfo("user_002");
        System.out.println("  返回结果：");
        System.out.println("    姓名：" + user2.getName());
        System.out.println("    手机：" + user2.getPhone());
        System.out.println("    身份证：" + user2.getIdCard());

        System.out.println("\n【各种脱敏规则】");
        System.out.println("  手机号：保留前3后4，中间*  → 138****8888");
        System.out.println("  身份证：保留前3后4，中间*  → 110***********1234");
        System.out.println("  银行卡：保留后4位，前面*   → ************1234");
        System.out.println("  姓名：  超过2字只留首尾    → 张*三 / 欧阳*修");
        System.out.println("  邮箱：  @ 前只保留前3位    → abc***@gmail.com");

        System.out.println("\n【为什么用 AOP 而不是在 Service 里手动脱敏】");
        System.out.println("  1. 业务代码干净：Service 直接返回原始数据，不关心脱敏");
        System.out.println("  2. 不会遗漏：只要接口加了 @DataMasking 注解，切面自动处理");
        System.out.println("  3. 灵活切换：内部接口（给数据分析用）不加注解，直接返回原始值");
        System.out.println("               外部接口（给前端）加注解，自动脱敏");
        System.out.println("  4. 集中修改：脱敏规则改了只改切面，不用改 N 个 Service");
    }
}

// =====================================================================
// 公共接口和实现类
// =====================================================================

// 场景1：电商订单服务接口
interface IEcommerceOrderService {
    String createOrder(String userId, String product, int qty, double price);
    String queryOrder(String orderId);
}

class EcommerceOrderServiceImpl implements IEcommerceOrderService {
    private static final AtomicInteger seq = new AtomicInteger(1000);

    @Override
    public String createOrder(String userId, String product, int qty, double price) {
        String orderId = "ORD-" + seq.incrementAndGet();
        System.out.println("      [业务] 订单入库：" + orderId + "，用户=" + userId
                + "，商品=" + product + "，金额=" + price * qty + "元");
        return orderId;
    }

    @Override
    public String queryOrder(String orderId) {
        if (orderId.startsWith("INVALID")) {
            throw new RuntimeException("订单不存在：" + orderId);
        }
        return "Order{id=" + orderId + ", status=COMPLETED}";
    }
}

// 场景1：TraceId 日志 Handler
class TraceLoggingHandler implements InvocationHandler {
    private final Object target;

    TraceLoggingHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String traceId = "TRACE-" + Integer.toHexString((int)(Math.random() * 0xFFFF)).toUpperCase();
        long start = System.currentTimeMillis();
        System.out.println("  [" + traceId + "] >>> " + method.getName()
                + "  入参=" + Arrays.toString(args));
        try {
            Object result = method.invoke(target, args);
            System.out.println("  [" + traceId + "] <<< " + method.getName()
                    + "  耗时=" + (System.currentTimeMillis() - start) + "ms"
                    + "  返回=" + result);
            return result;
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.println("  [" + traceId + "] !!! " + method.getName()
                    + "  耗时=" + (System.currentTimeMillis() - start) + "ms"
                    + "  异常=" + cause.getMessage());
            return null;
        }
    }
}

// 场景2：转账服务
interface TransferService {
    void transfer(String from, String to, double amount);
}

class TransferServiceImpl implements TransferService {
    @Override
    public void transfer(String from, String to, double amount) {
        if (amount > 10000) {
            throw new RuntimeException("余额不足，转账失败！金额=" + amount);
        }
        System.out.println("      [业务] 扣减 " + from + " 账户 " + amount + " 元");
        System.out.println("      [业务] 增加 " + to + " 账户 " + amount + " 元");
    }
}

class TransactionAspectHandler implements InvocationHandler {
    private final Object target;

    TransactionAspectHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("  [事务切面] BEGIN TRANSACTION（connection.setAutoCommit(false)）");
        try {
            Object result = method.invoke(target, args);
            System.out.println("  [事务切面] COMMIT（connection.commit()）");
            return result;
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.println("  [事务切面] ROLLBACK（connection.rollback()）原因：" + cause.getMessage());
            return null;
        }
    }
}

// 场景3：秒杀服务
interface ISeckillService {
    void seckill(String userId, String itemId);
}

class SeckillServiceImpl implements ISeckillService {
    @Override
    public void seckill(String userId, String itemId) {
        System.out.println("秒杀成功 → " + userId + " 抢到了 " + itemId);
    }
}

// 简单令牌桶实现
class TokenBucketRateLimiter {
    private final int capacity;          // 桶容量
    private final int refillPerSecond;   // 每秒补充量
    private volatile int tokens;         // 当前令牌数
    private volatile long lastRefillTime;

    TokenBucketRateLimiter(int capacity, int refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    synchronized boolean tryAcquire() {
        refill();
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        int toAdd = (int) (elapsed * refillPerSecond / 1000);
        if (toAdd > 0) {
            tokens = Math.min(capacity, tokens + toAdd);
            lastRefillTime = now;
        }
    }
}

class RateLimitHandler implements InvocationHandler {
    private final Object target;
    private final TokenBucketRateLimiter limiter;

    RateLimitHandler(Object target, TokenBucketRateLimiter limiter) {
        this.target = target;
        this.limiter = limiter;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!limiter.tryAcquire()) {
            System.out.println("被限流拒绝（令牌不足）→ 返回：请求过于频繁，请稍后重试");
            return null;
        }
        return method.invoke(target, args);
    }
}

// 场景4：支付服务
interface IPaymentService {
    String pay(String idempotentToken, String userId, double amount);
}

class PaymentServiceImpl implements IPaymentService {
    private static int payCount = 0;

    @Override
    public String pay(String idempotentToken, String userId, double amount) {
        payCount++;
        String txId = "TX-" + System.currentTimeMillis();
        System.out.println("      [业务] 实际扣款：用户=" + userId
                + "，金额=" + amount + "，流水号=" + txId
                + "  （第 " + payCount + " 次扣款）");
        return txId;
    }
}

class IdempotentStore {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    boolean exists(String token) { return store.containsKey(token); }
    void put(String token, String result) { store.put(token, result); }
    String get(String token) { return store.get(token); }
}

class IdempotentHandler implements InvocationHandler {
    private final Object target;
    private final IdempotentStore store;

    IdempotentHandler(Object target, IdempotentStore store) {
        this.target = target;
        this.store = store;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String token = (String) args[0]; // 第一个参数是幂等 token
        if (store.exists(token)) {
            String cached = store.get(token);
            System.out.println("  [幂等切面] 检测到重复请求！Token=" + token
                    + "，直接返回缓存结果（不重复扣款）：" + cached);
            return cached;
        }
        System.out.println("  [幂等切面] 首次请求，Token=" + token + "，放行");
        Object result = method.invoke(target, args);
        store.put(token, result.toString());
        System.out.println("  [幂等切面] 结果已缓存，Token=" + token);
        return result;
    }
}

// 场景5：用户信息服务
interface IUserInfoService {
    UserInfo getUserInfo(String userId);
}

class UserInfo {
    private String name;
    private String phone;        // 脱敏后
    private String idCard;       // 脱敏后
    private String bankCard;     // 脱敏后
    private String originalPhone;
    private String originalIdCard;
    private String originalBankCard;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getBankCard() { return bankCard; }
    public void setBankCard(String bankCard) { this.bankCard = bankCard; }
    public String getOriginalPhone() { return originalPhone; }
    public void setOriginalPhone(String p) { this.originalPhone = p; }
    public String getOriginalIdCard() { return originalIdCard; }
    public void setOriginalIdCard(String i) { this.originalIdCard = i; }
    public String getOriginalBankCard() { return originalBankCard; }
    public void setOriginalBankCard(String b) { this.originalBankCard = b; }
}

class UserInfoServiceImpl implements IUserInfoService {
    @Override
    public UserInfo getUserInfo(String userId) {
        UserInfo info = new UserInfo();
        if ("user_001".equals(userId)) {
            info.setName("张三");
            info.setPhone("13812348888");
            info.setIdCard("110101199001011234");
            info.setBankCard("6222021301007564321");
        } else {
            info.setName("欧阳修远");
            info.setPhone("18688889999");
            info.setIdCard("440101198505055678");
            info.setBankCard("6225882001029876");
        }
        System.out.println("      [业务] 从数据库查询用户：" + userId
                + "，手机=" + info.getPhone()
                + "，身份证=" + info.getIdCard());
        return info;
    }
}

class DataMaskingHandler implements InvocationHandler {
    private final Object target;

    DataMaskingHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(target, args);
        if (result instanceof UserInfo) {
            UserInfo info = (UserInfo) result;
            // 保存原始值用于演示
            info.setOriginalPhone(info.getPhone());
            info.setOriginalIdCard(info.getIdCard());
            info.setOriginalBankCard(info.getBankCard());
            // 脱敏处理
            info.setPhone(maskPhone(info.getPhone()));
            info.setIdCard(maskIdCard(info.getIdCard()));
            info.setBankCard(maskBankCard(info.getBankCard()));
            System.out.println("  [脱敏切面] 自动完成字段脱敏（手机/身份证/银行卡）");
        }
        return result;
    }

    // 手机号：保留前3后4，中间4位用*替换
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 身份证：保留前3后4，中间用*替换
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 7) return idCard;
        int maskLen = idCard.length() - 7;
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < maskLen; i++) stars.append('*');
        return idCard.substring(0, 3) + stars + idCard.substring(idCard.length() - 4);
    }

    // 银行卡：只保留后4位
    private String maskBankCard(String card) {
        if (card == null || card.length() < 4) return card;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < card.length() - 4; i++) sb.append('*');
        return sb.append(card.substring(card.length() - 4)).toString();
    }
}

