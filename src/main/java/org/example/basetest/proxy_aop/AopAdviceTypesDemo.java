package org.example.basetest.proxy_aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 问题 1：AOP 通知类型完整对比（@Before/@After/@Around/@AfterReturning/@AfterThrowing）
 * 问题 2：数据脱敏 —— 用 AOP @AfterReturning 实现（而非动态代理手写）
 *
 * 本文件完整展示：
 * ① 5 种通知类型的执行时机、使用场景、代码写法
 * ② 通知类型之间的执行顺序（正常 vs 异常两条链路）
 * ③ 数据脱敏的 Spring AOP 完整写法（@AfterReturning + 反射）
 * ④ 动态代理 vs Spring AOP 写法对比
 */
public class AopAdviceTypesDemo {

    public static void main(String[] args) {
        System.out.println("========== AOP 通知类型 & 数据脱敏 完整解析 ==========\n");

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 第一部分：5 种通知类型详解");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        AdviceTypesSection.run();

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 第二部分：执行顺序验证（正常 & 异常两条路径）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        ExecutionOrderSection.run();

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" 第三部分：数据脱敏 —— Spring AOP @AfterReturning 完整实现");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        DataMaskingAopSection.run();
    }
}

// ====================================================================
// 第一部分：5 种通知类型详解
// ====================================================================
class AdviceTypesSection {

    static void run() {
        System.out.println("【总览：5 种通知类型 vs 执行时机】\n");
        System.out.println("  目标方法执行时间线：");
        System.out.println("  ─────────────────────────────────────────────────────────────");
        System.out.println("  @Before        │ 方法调用之前（入参已确定，方法还没执行）");
        System.out.println("  ─────────────── ─────────────────────────────────────────────");
        System.out.println("                 │ ◄── 目标方法开始执行");
        System.out.println("                 │     ...");
        System.out.println("                 │     正常返回？───► @AfterReturning  （只在正常时触发）");
        System.out.println("                 │     抛出异常？───► @AfterThrowing   （只在异常时触发）");
        System.out.println("                 │ ◄── 目标方法结束（无论是否异常）");
        System.out.println("  ─────────────── ─────────────────────────────────────────────");
        System.out.println("  @After         │ 方法结束之后（无论正常还是异常，类似 finally）");
        System.out.println("  ─────────────── ─────────────────────────────────────────────");
        System.out.println("  @Around        │ 完全包裹方法，可以在执行前后各插逻辑，也可以阻止执行");
        System.out.println("  ─────────────────────────────────────────────────────────────\n");

        System.out.println("【1. @Before —— 方法执行前】");
        System.out.println("  触发时机：目标方法调用之前，入参已确定，方法尚未执行");
        System.out.println("  能做什么：可以读取入参，但不能修改入参，不能阻止方法执行（@Around 才能）");
        System.out.println("  常用场景：");
        System.out.println("    • 权限校验：校验当前用户是否有权限调用该方法");
        System.out.println("    • 参数校验：检查入参是否合法（非空、格式等）");
        System.out.println("    • 操作记录：记录'谁在什么时候准备做什么'");
        System.out.println("  Spring AOP 写法：");
        System.out.println("    @Before(\"execution(* com.example.service.OrderService.deleteOrder(..))\")");
        System.out.println("    public void checkPermission(JoinPoint jp) {");
        System.out.println("        String userId = SecurityContext.getCurrentUser();");
        System.out.println("        Object[] args = jp.getArgs(); // 可以获取入参");
        System.out.println("        if (!hasPermission(userId, \"DELETE_ORDER\")) {");
        System.out.println("            throw new ForbiddenException(\"无权删除订单\");");
        System.out.println("        }");
        System.out.println("        // 注意：@Before 抛异常才能阻止方法执行，本身无法阻止");
        System.out.println("    }");
        System.out.println();

        System.out.println("【2. @After —— 方法结束后（类似 finally）】");
        System.out.println("  触发时机：目标方法结束后，无论正常返回还是抛出异常都会触发");
        System.out.println("  能做什么：不能获取返回值（用 @AfterReturning），不能获取异常（用 @AfterThrowing）");
        System.out.println("  常用场景：");
        System.out.println("    • 资源清理：方法执行后必须清理的资源（类似 finally 块）");
        System.out.println("    • 释放锁：无论成功失败都要释放分布式锁");
        System.out.println("    • 上下文清理：清除 ThreadLocal 中的数据");
        System.out.println("  Spring AOP 写法：");
        System.out.println("    @After(\"@annotation(com.example.annotation.DistributedLock)\")");
        System.out.println("    public void releaseLock(JoinPoint jp) {");
        System.out.println("        // 无论方法成功失败，都释放分布式锁");
        System.out.println("        String lockKey = getLockKey(jp);");
        System.out.println("        redisLock.unlock(lockKey);");
        System.out.println("        log.info(\"释放锁：{}\", lockKey);");
        System.out.println("    }");
        System.out.println();

        System.out.println("【3. @AfterReturning —— 方法正常返回后】");
        System.out.println("  触发时机：目标方法正常执行并返回后（有异常则不触发）");
        System.out.println("  能做什么：可以获取并修改返回值（returning 参数绑定）");
        System.out.println("  常用场景：");
        System.out.println("    • 数据脱敏：拦截返回的用户信息，自动脱敏手机号/身份证");
        System.out.println("    • 结果缓存：把方法的返回值存入缓存（@Cacheable 底层类似）");
        System.out.println("    • 发送通知：方法成功后发 MQ 消息或邮件");
        System.out.println("    • 操作审计：记录'谁成功做了什么，结果是什么'");
        System.out.println("  Spring AOP 写法：");
        System.out.println("    @AfterReturning(");
        System.out.println("        pointcut = \"@annotation(com.example.annotation.DataMasking)\",");
        System.out.println("        returning = \"result\")  // 把返回值绑定到 result 参数");
        System.out.println("    public void maskSensitiveData(JoinPoint jp, Object result) {");
        System.out.println("        // result 就是目标方法的返回值，可以读取和修改");
        System.out.println("        if (result instanceof UserDTO) {");
        System.out.println("            UserDTO dto = (UserDTO) result;");
        System.out.println("            dto.setPhone(maskPhone(dto.getPhone()));   // 修改返回值");
        System.out.println("            dto.setIdCard(maskIdCard(dto.getIdCard()));");
        System.out.println("        }");
        System.out.println("        // 注意：@AfterReturning 不能替换返回值（@Around 才能）");
        System.out.println("        //       只能修改返回对象的内部属性");
        System.out.println("    }");
        System.out.println();

        System.out.println("【4. @AfterThrowing —— 方法抛出异常后】");
        System.out.println("  触发时机：目标方法抛出异常后（正常返回则不触发）");
        System.out.println("  能做什么：可以获取异常对象，但不能阻止异常传播（@Around 才能 try-catch）");
        System.out.println("  常用场景：");
        System.out.println("    • 统一异常告警：方法出错时自动发钉钉/邮件告警");
        System.out.println("    • 异常监控上报：把异常信息上报到监控系统（Sentry/哨兵）");
        System.out.println("    • 异常日志：统一记录异常的方法名、入参、异常信息");
        System.out.println("  Spring AOP 写法：");
        System.out.println("    @AfterThrowing(");
        System.out.println("        pointcut = \"execution(* com.example.service..*.*(..))\",");
        System.out.println("        throwing = \"ex\")   // 把异常对象绑定到 ex 参数");
        System.out.println("    public void handleException(JoinPoint jp, Throwable ex) {");
        System.out.println("        String method = jp.getSignature().toShortString();");
        System.out.println("        // 告警通知（钉钉/邮件）");
        System.out.println("        alertService.send(\"方法异常: \" + method + \", 原因: \" + ex.getMessage());");
        System.out.println("        // 注意：异常依然会继续往上抛，@AfterThrowing 拦截不住");
        System.out.println("        //       要拦截异常需要用 @Around + try-catch");
        System.out.println("    }");
        System.out.println();

        System.out.println("【5. @Around —— 环绕通知（最强大）】");
        System.out.println("  触发时机：完全包裹目标方法，可在执行前后各插逻辑");
        System.out.println("  能做什么：");
        System.out.println("    • 可以修改入参（在 proceed 之前替换 args）");
        System.out.println("    • 可以阻止方法执行（不调用 pjp.proceed()）");
        System.out.println("    • 可以修改/替换返回值（proceed 的返回值可以不直接返回）");
        System.out.println("    • 可以捕获并处理异常（try-catch 包裹 proceed）");
        System.out.println("  常用场景：日志、事务、限流、幂等、性能监控（覆盖最广的场景）");
        System.out.println("  Spring AOP 写法：");
        System.out.println("    @Around(\"execution(* com.example.service..*.*(..))\")");
        System.out.println("    public Object around(ProceedingJoinPoint pjp) throws Throwable {");
        System.out.println("        // === 前置逻辑（等同于 @Before）===");
        System.out.println("        log.info(\"方法开始: {}\", pjp.getSignature().getName());");
        System.out.println("        long start = System.currentTimeMillis();");
        System.out.println();
        System.out.println("        Object result;");
        System.out.println("        try {");
        System.out.println("            // === 调用真实方法（如果不调用，方法就被拦截了）===");
        System.out.println("            result = pjp.proceed();       // 可传入修改后的参数: pjp.proceed(newArgs)");
        System.out.println();
        System.out.println("            // === 正常返回后逻辑（等同于 @AfterReturning）===");
        System.out.println("            log.info(\"方法正常返回: {}, 耗时: {}ms\",");
        System.out.println("                     pjp.getSignature().getName(),");
        System.out.println("                     System.currentTimeMillis() - start);");
        System.out.println("        } catch (Throwable e) {");
        System.out.println("            // === 异常处理（等同于 @AfterThrowing，但能阻止异常传播）===");
        System.out.println("            log.error(\"方法异常: {}\", e.getMessage());");
        System.out.println("            throw e; // 或者转换为业务异常: throw new BizException(e)");
        System.out.println("        } finally {");
        System.out.println("            // === 清理逻辑（等同于 @After）===");
        System.out.println("            MDC.clear();");
        System.out.println("        }");
        System.out.println("        return result; // 可以替换成任意值: return \"mock result\";");
        System.out.println("    }");
        System.out.println();

        System.out.println("【一句话记忆】");
        System.out.println("  @Around = @Before + @AfterReturning + @AfterThrowing + @After 的超集");
        System.out.println("  实际项目中 @Around 用得最多，一个注解搞定所有场景");
        System.out.println("  其他4种是语义更明确的'精简版'，让代码意图更清晰");
    }
}

// ====================================================================
// 第二部分：执行顺序验证
// ====================================================================
class ExecutionOrderSection {

    static void run() {
        System.out.println("【正常执行时的完整顺序】\n");
        IOrderSvc normalTarget = new OrderSvcImpl(false); // 不抛异常
        IOrderSvc normalProxy = buildAllAdviceProxy(normalTarget, false);

        System.out.println("  调用 normalProxy.placeOrder(\"iPhone\"):\n");
        normalProxy.placeOrder("iPhone");

        System.out.println("\n  正常执行顺序总结：");
        System.out.println("  @Around(前) → @Before → 目标方法 → @AfterReturning → @After → @Around(后)");
        System.out.println("  注意：@After 在 @AfterReturning 之后，但在 @Around 返回之前");

        System.out.println("\n【异常执行时的完整顺序】\n");
        IOrderSvc exceptTarget = new OrderSvcImpl(true); // 抛异常
        IOrderSvc exceptProxy = buildAllAdviceProxy(exceptTarget, true);

        System.out.println("  调用 exceptProxy.placeOrder(\"BadItem\")（会抛出异常）:\n");
        exceptProxy.placeOrder("BadItem");

        System.out.println("\n  异常执行顺序总结：");
        System.out.println("  @Around(前) → @Before → 目标方法(抛异常) → @AfterThrowing → @After → @Around(catch)");
        System.out.println("  注意：异常时 @AfterReturning 不触发，@AfterThrowing 触发");

        System.out.println("\n【执行顺序图】");
        System.out.println("  ┌─────────────────────────────────────────────────────┐");
        System.out.println("  │  @Around 开始                                       │");
        System.out.println("  │    ↓                                                │");
        System.out.println("  │  @Before                                            │");
        System.out.println("  │    ↓                                                │");
        System.out.println("  │  目标方法执行                                        │");
        System.out.println("  │    ↓                           ↓                   │");
        System.out.println("  │  正常返回                     抛出异常               │");
        System.out.println("  │    ↓                           ↓                   │");
        System.out.println("  │  @AfterReturning          @AfterThrowing           │");
        System.out.println("  │    ↓                           ↓                   │");
        System.out.println("  │  @After ←─────────────────────┘                   │");
        System.out.println("  │    ↓                                                │");
        System.out.println("  │  @Around 结束（finally块或catch块）                  │");
        System.out.println("  └─────────────────────────────────────────────────────┘");

        System.out.println("\n【5 种通知的能力对比表】");
        System.out.println("  ┌─────────────────┬───────┬───────┬────────────────┬──────────────┬────────┐");
        System.out.println("  │ 能力             │@Before│@After │@AfterReturning │@AfterThrowing│@Around │");
        System.out.println("  ├─────────────────┼───────┼───────┼────────────────┼──────────────┼────────┤");
        System.out.println("  │ 读取入参         │  ✅   │  ✅   │      ✅        │      ✅      │   ✅   │");
        System.out.println("  │ 修改入参         │  ❌   │  ❌   │      ❌        │      ❌      │   ✅   │");
        System.out.println("  │ 阻止方法执行     │  ❌*  │  ❌   │      ❌        │      ❌      │   ✅   │");
        System.out.println("  │ 读取返回值       │  ❌   │  ❌   │      ✅        │      ❌      │   ✅   │");
        System.out.println("  │ 修改/替换返回值  │  ❌   │  ❌   │      ❌*       │      ❌      │   ✅   │");
        System.out.println("  │ 获取异常对象     │  ❌   │  ❌   │      ❌        │      ✅      │   ✅   │");
        System.out.println("  │ 阻止异常传播     │  ❌   │  ❌   │      ❌        │      ❌      │   ✅   │");
        System.out.println("  │ 正常时触发       │  ✅   │  ✅   │      ✅        │      ❌      │   ✅   │");
        System.out.println("  │ 异常时触发       │  ✅   │  ✅   │      ❌        │      ✅      │   ✅   │");
        System.out.println("  └─────────────────┴───────┴───────┴────────────────┴──────────────┴────────┘");
        System.out.println("  * @Before 只能通过抛异常来阻止方法执行");
        System.out.println("  * @AfterReturning 可以修改返回对象的属性，但不能替换返回值本身");
    }

    // 用动态代理模拟 5 种通知的执行顺序
    private static IOrderSvc buildAllAdviceProxy(IOrderSvc target, boolean hasException) {
        return (IOrderSvc) Proxy.newProxyInstance(
                IOrderSvc.class.getClassLoader(),
                new Class[]{IOrderSvc.class},
                new AllAdviceHandler(target)
        );
    }
}

// ====================================================================
// 第三部分：数据脱敏 —— Spring AOP @AfterReturning 实现
// ====================================================================
class DataMaskingAopSection {

    static void run() {
        System.out.println("【问题澄清：前面的例子是用动态代理模拟 AOP，下面展示真正的 Spring AOP 写法】\n");

        System.out.println("【动态代理 vs Spring AOP 写法对比】\n");

        System.out.println("  方式 A：手写动态代理（之前的例子）");
        System.out.println("  ─────────────────────────────────────────");
        System.out.println("  // 需要手写 Handler，实现 InvocationHandler");
        System.out.println("  class DataMaskingHandler implements InvocationHandler {");
        System.out.println("      @Override");
        System.out.println("      public Object invoke(Object proxy, Method method, Object[] args) {");
        System.out.println("          Object result = method.invoke(target, args); // 调用真实方法");
        System.out.println("          if (result instanceof UserInfo) {");
        System.out.println("              // 手动脱敏逻辑");
        System.out.println("              ((UserInfo)result).setPhone(maskPhone(...));");
        System.out.println("          }");
        System.out.println("          return result;");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println("  // 创建代理（必须手动创建）");
        System.out.println("  IUserInfoService proxy = (IUserInfoService) Proxy.newProxyInstance(");
        System.out.println("      IUserInfoService.class.getClassLoader(),");
        System.out.println("      new Class[]{IUserInfoService.class},");
        System.out.println("      new DataMaskingHandler(realService)");
        System.out.println("  );");
        System.out.println();

        System.out.println("  方式 B：Spring AOP（真实项目写法）");
        System.out.println("  ─────────────────────────────────────────");
        System.out.println("  // 步骤 1：定义脱敏注解（标注在接口方法上）");
        System.out.println("  @Target(ElementType.METHOD)");
        System.out.println("  @Retention(RetentionPolicy.RUNTIME)");
        System.out.println("  public @interface DataMasking {}");
        System.out.println();
        System.out.println("  // 步骤 2：定义字段级脱敏注解（标注在 DTO 字段上）");
        System.out.println("  @Target(ElementType.FIELD)");
        System.out.println("  @Retention(RetentionPolicy.RUNTIME)");
        System.out.println("  public @interface Desensitize {");
        System.out.println("      DesensitizeType type();  // PHONE / ID_CARD / BANK_CARD / NAME");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // 步骤 3：在 DTO 字段上标注脱敏类型");
        System.out.println("  public class UserDTO {");
        System.out.println("      private String name;");
        System.out.println("      @Desensitize(type = DesensitizeType.PHONE)");
        System.out.println("      private String phone;    // 自动脱敏为 138****8888");
        System.out.println("      @Desensitize(type = DesensitizeType.ID_CARD)");
        System.out.println("      private String idCard;   // 自动脱敏为 110***...1234");
        System.out.println("      @Desensitize(type = DesensitizeType.BANK_CARD)");
        System.out.println("      private String bankCard; // 自动脱敏为 *****1234");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // 步骤 4：写切面（@AfterReturning 拦截返回值）");
        System.out.println("  @Aspect");
        System.out.println("  @Component");
        System.out.println("  public class DataMaskingAspect {");
        System.out.println();
        System.out.println("      // 切点：标注了 @DataMasking 的方法");
        System.out.println("      @AfterReturning(");
        System.out.println("          pointcut = \"@annotation(com.example.annotation.DataMasking)\",");
        System.out.println("          returning = \"result\")   // ← 把返回值绑定到 result");
        System.out.println("      public void maskReturnValue(JoinPoint jp, Object result) {");
        System.out.println("          if (result == null) return;");
        System.out.println("          // 用反射扫描返回值的所有字段");
        System.out.println("          for (Field field : result.getClass().getDeclaredFields()) {");
        System.out.println("              Desensitize ann = field.getAnnotation(Desensitize.class);");
        System.out.println("              if (ann == null) continue;");
        System.out.println("              field.setAccessible(true);");
        System.out.println("              String val = (String) field.get(result);");
        System.out.println("              // 根据注解类型选择脱敏策略");
        System.out.println("              String masked = DesensitizeUtils.mask(ann.type(), val);");
        System.out.println("              field.set(result, masked); // 直接修改字段值");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // 步骤 5：业务代码只需加一个注解，极其简洁");
        System.out.println("  @Service");
        System.out.println("  public class UserServiceImpl implements IUserService {");
        System.out.println("      @DataMasking          // ← 只需这一行，其余全自动");
        System.out.println("      public UserDTO getUserById(String userId) {");
        System.out.println("          return userRepository.findById(userId); // 直接返回原始数据");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();

        System.out.println("【为什么数据脱敏用 @AfterReturning 而不是 @Around】");
        System.out.println("  @AfterReturning 语义更精确：");
        System.out.println("    • 明确表达'我只在方法正常返回时处理返回值'");
        System.out.println("    • 方法抛异常时不执行（异常时也没有返回值可以脱敏）");
        System.out.println("    • 代码意图更清晰，一看就知道是处理返回值的切面");
        System.out.println();
        System.out.println("  @Around 也可以实现，但写法更繁琐：");
        System.out.println("    Object result = pjp.proceed();");
        System.out.println("    maskFields(result);  // 手动脱敏");
        System.out.println("    return result;");
        System.out.println("  → 对于只需要处理返回值的场景，@AfterReturning 更简洁\n");

        System.out.println("【模拟 Spring AOP @AfterReturning 执行效果】\n");

        // 用动态代理模拟 Spring AOP @AfterReturning 的行为
        IUserQueryService realSvc = new UserQueryServiceImpl();
        IUserQueryService aopProxy = (IUserQueryService) Proxy.newProxyInstance(
                IUserQueryService.class.getClassLoader(),
                new Class[]{IUserQueryService.class},
                new AfterReturningMaskingHandler(realSvc)
        );

        System.out.println("  调用 getUserInfo(\"user_001\")（@DataMasking 方法）：");
        UserProfile p1 = aopProxy.getUserInfo("user_001");
        System.out.println("  最终返回给调用方：");
        printProfile(p1);

        System.out.println();
        System.out.println("  调用 getUserInfo(\"user_002\")：");
        UserProfile p2 = aopProxy.getUserInfo("user_002");
        System.out.println("  最终返回给调用方：");
        printProfile(p2);

        System.out.println();
        System.out.println("  调用 getRawUserInfo(\"user_001\")（无 @DataMasking 注解，不脱敏）：");
        UserProfile p3 = aopProxy.getRawUserInfo("user_001");
        System.out.println("  最终返回给调用方（原始数据，内部接口用）：");
        printProfile(p3);

        System.out.println("\n【@AfterReturning 的注意事项】");
        System.out.println("  ⚠️ 注意 1：@AfterReturning 只能修改返回对象的内部属性");
        System.out.println("             不能替换返回值本身（如 return null 或 return 其他对象）");
        System.out.println("             如果需要替换返回值，改用 @Around");
        System.out.println();
        System.out.println("  ⚠️ 注意 2：returning 参数类型要匹配");
        System.out.println("             public void mask(JoinPoint jp, Object result)  ← Object 匹配所有");
        System.out.println("             public void mask(JoinPoint jp, UserDTO result) ← 只匹配返回 UserDTO 的方法");
        System.out.println();
        System.out.println("  ⚠️ 注意 3：如果方法返回 List<UserDTO>，需要遍历 List 逐个脱敏");
        System.out.println("             if (result instanceof List) {");
        System.out.println("                 ((List<?>) result).forEach(item -> maskFields(item));");
        System.out.println("             }");
    }

    private static void printProfile(UserProfile p) {
        System.out.println("    姓名：" + p.getName()
                + "，手机：" + p.getPhone()
                + "，身份证：" + p.getIdCard()
                + "，银行卡：" + p.getBankCard());
    }
}

// ====================================================================
// 模拟所有 5 种通知类型的 InvocationHandler
// ====================================================================
class AllAdviceHandler implements InvocationHandler {
    private final Object target;

    AllAdviceHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // === @Around 前半段 ===
        System.out.println("  [@Around 前]  进入环绕通知（前置部分），方法=" + method.getName());

        // === @Before ===
        System.out.println("  [@Before]     方法执行前，入参=" + java.util.Arrays.toString(args));

        Object result = null;
        try {
            // === 调用目标方法 ===
            result = method.invoke(target, args);

            // === @AfterReturning（仅正常返回时）===
            System.out.println("  [@AfterReturning] 方法正常返回，返回值=" + result);

        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            // === @AfterThrowing（仅抛异常时）===
            System.out.println("  [@AfterThrowing]  方法抛出异常，异常=" + cause.getMessage());
            // 注意：@AfterThrowing 后异常依然会传播（除非用 @Around catch 住）
        } finally {
            // === @After（无论正常还是异常都执行）===
            System.out.println("  [@After]      方法结束（无论是否异常），类似 finally 块");
        }

        // === @Around 后半段 ===
        System.out.println("  [@Around 后]  环绕通知结束，可以修改返回值或处理异常");
        return result;
    }
}

// ====================================================================
// 模拟 @AfterReturning 数据脱敏
// ====================================================================
class AfterReturningMaskingHandler implements InvocationHandler {
    private final Object target;
    // 标记哪些方法需要脱敏（模拟 @DataMasking 注解）
    private static final java.util.Set<String> MASKING_METHODS =
            new java.util.HashSet<>(java.util.Arrays.asList("getUserInfo"));

    AfterReturningMaskingHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 调用真实方法（Service 返回原始数据）
        Object result = method.invoke(target, args);

        // 模拟 @AfterReturning：仅在方法正常返回且需要脱敏时处理
        if (result != null && MASKING_METHODS.contains(method.getName())) {
            System.out.println("  [AOP @AfterReturning] 切面拦截到返回值，开始脱敏...");
            maskFields(result);
            System.out.println("  [AOP @AfterReturning] 脱敏完成");
        }
        // 直接返回（已修改内部字段的对象）
        return result;
    }

    private void maskFields(Object obj) {
        if (!(obj instanceof UserProfile)) return;
        UserProfile profile = (UserProfile) obj;
        // 用反射+注解驱动脱敏（真实 Spring AOP 实现）
        for (java.lang.reflect.Field field : obj.getClass().getDeclaredFields()) {
            SensitiveField ann = field.getAnnotation(SensitiveField.class);
            if (ann == null) continue;
            try {
                field.setAccessible(true);
                String val = (String) field.get(obj);
                String masked = applyMask(ann.type(), val);
                field.set(obj, masked);   // ← 直接修改字段（@AfterReturning 能做的）
            } catch (Exception ignored) {}
        }
    }

    private String applyMask(SensitiveType type, String val) {
        if (val == null) return null;
        switch (type) {
            case PHONE:
                return val.length() >= 7
                        ? val.substring(0, 3) + "****" + val.substring(val.length() - 4)
                        : val;
            case ID_CARD:
                if (val.length() < 7) return val;
                StringBuilder sb = new StringBuilder(val.substring(0, 3));
                for (int i = 3; i < val.length() - 4; i++) sb.append('*');
                return sb.append(val.substring(val.length() - 4)).toString();
            case BANK_CARD:
                if (val.length() < 4) return val;
                StringBuilder sb2 = new StringBuilder();
                for (int i = 0; i < val.length() - 4; i++) sb2.append('*');
                return sb2.append(val.substring(val.length() - 4)).toString();
            default:
                return val;
        }
    }
}

// ====================================================================
// 支撑类：接口和 DTO
// ====================================================================
interface IOrderSvc {
    String placeOrder(String item);
}

class OrderSvcImpl implements IOrderSvc {
    private final boolean throwEx;
    OrderSvcImpl(boolean throwEx) { this.throwEx = throwEx; }

    @Override
    public String placeOrder(String item) {
        if (throwEx) {
            System.out.println("  [目标方法]    业务执行中，准备抛出异常...");
            throw new RuntimeException("库存不足，商品=" + item);
        }
        System.out.println("  [目标方法]    业务执行中，创建订单成功");
        return "ORD-" + System.currentTimeMillis();
    }
}

// 脱敏注解
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@interface SensitiveField {
    SensitiveType type();
}

enum SensitiveType { PHONE, ID_CARD, BANK_CARD }

// 用户档案 DTO（字段上标注脱敏注解）
class UserProfile {
    private String name;

    @SensitiveField(type = SensitiveType.PHONE)
    private String phone;

    @SensitiveField(type = SensitiveType.ID_CARD)
    private String idCard;

    @SensitiveField(type = SensitiveType.BANK_CARD)
    private String bankCard;

    // Constructor
    UserProfile(String name, String phone, String idCard, String bankCard) {
        this.name = name; this.phone = phone;
        this.idCard = idCard; this.bankCard = bankCard;
    }
    // Getters Setters
    public String getName()     { return name; }
    public String getPhone()    { return phone; }
    public String getIdCard()   { return idCard; }
    public String getBankCard() { return bankCard; }
    public void setPhone(String v)    { this.phone = v; }
    public void setIdCard(String v)   { this.idCard = v; }
    public void setBankCard(String v) { this.bankCard = v; }
}

interface IUserQueryService {
    UserProfile getUserInfo(String userId);       // 标注了 @DataMasking，需要脱敏
    UserProfile getRawUserInfo(String userId);    // 无注解，返回原始数据（供内部系统用）
}

class UserQueryServiceImpl implements IUserQueryService {
    @Override
    public UserProfile getUserInfo(String userId) {
        UserProfile p = buildProfile(userId);
        System.out.println("  [Service 业务] 从 DB 查询用户=" + userId
                + "，手机(原始)=" + p.getPhone()
                + "，身份证(原始)=" + p.getIdCard());
        return p;
    }

    @Override
    public UserProfile getRawUserInfo(String userId) {
        UserProfile p = buildProfile(userId);
        System.out.println("  [Service 业务] 内部接口查询用户=" + userId
                + "，手机(原始)=" + p.getPhone());
        return p;
    }

    private UserProfile buildProfile(String userId) {
        if ("user_001".equals(userId))
            return new UserProfile("张三", "13812348888", "110101199001011234", "6222021301007564321");
        return new UserProfile("欧阳修远", "18688889999", "440101198505055678", "6225882001029876");
    }
}

