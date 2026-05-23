package org.example.java_base_test.io.nio.show_multi_agent;

class Part23_ModernJdkIOFeatures {

    static void demonstrate() throws Exception {
        System.out.println("【第二十三部分：JDK 9~21 NIO 相关新特性】");
        System.out.println();
        System.out.println("═══ 🚀 生活场景：手机系统的持续进化 理解JDK IO新特性 ═══");
        System.out.println();
        System.out.println("  把 JDK 的版本升级想象成手机系统（iOS/Android）的持续迭代：");
        System.out.println("  基础功能早就有了，每个新版本都在某些地方让「用户体验更好」：");
        System.out.println();
        System.out.println("  JDK9 = 系统更新：「分享」功能从三步简化成一步");
        System.out.println("    InputStream.transferTo()：以前要写10行的「读到内存再写出去」，");
        System.out.println("    现在一行：in.transferTo(out)  就像分享一张图片变成了直接「一键分享」");
        System.out.println();
        System.out.println("  JDK11 = 系统更新：「便签」应用终于支持「直接保存到文件」了");
        System.out.println("    Files.writeString/readString：以前写文件要转 byte[]，很繁琐");
        System.out.println("    现在：Files.writeString(path, \"内容\") 直接写，Files.readString(path) 直接读");
        System.out.println("    内置 HttpClient：以前要用第三方库（OkHttp），现在系统自带，支持 HTTP/2");
        System.out.println();
        System.out.println("  JDK15 = 系统更新：「备忘录」终于支持多行格式，不再丢失换行");
        System.out.println("    Text Block（文本块）：写 JSON/SQL 不再需要一堆 \\n 和引号转义");
        System.out.println("    直接用三引号包裹，所见即所得");
        System.out.println();
        System.out.println("  JDK21 = 重大系统升级：「多任务处理」彻底重构");
        System.out.println("    虚拟线程（Virtual Threads）：让 IO 阻塞不再是性能瓶颈");
        System.out.println("    就像手机从「单核」升级到「多核」，同样的写法性能大幅提升");
        System.out.println("    结构化并发：并发任务管理从「危险的传真」变成「安全的项目管理系统」");
        System.out.println();
        System.out.println("  这些改进的共同主题：让代码更简单、更安全、更高效");
        System.out.println("  每个新特性都有「以前的痛点」→「新的解法」两步理解法");
        System.out.println();
        System.out.println("═══ 以下是各版本新特性技术细节 ═══");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第一节：JDK9 - InputStream.transferTo()
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 1. JDK9：InputStream.transferTo()（BIO 也有零拷贝语义了）━━━");
        System.out.println();
        System.out.println("  JDK9 之前，从 InputStream 读数据到 OutputStream 要写循环：");
        System.out.println();
        System.out.println("  // JDK8 写法（繁琐）");
        System.out.println("  byte[] buf = new byte[8192];");
        System.out.println("  int n;");
        System.out.println("  while ((n = in.read(buf)) != -1) {");
        System.out.println("      out.write(buf, 0, n);");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // JDK9 写法（一行搞定）");
        System.out.println("  in.transferTo(out);  // ← InputStream 新增方法");
        System.out.println();
        System.out.println("  实际演示：");

        // 用字节数组模拟 InputStream
        byte[] data = "Hello transferTo! This is JDK9 new feature.".getBytes();
        java.io.InputStream inStream = new java.io.ByteArrayInputStream(data);
        java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream();
        // JDK9+ transferTo，JDK8 用循环兜底
        long transferred;
        int jv9 = getJavaVersion();
        if (jv9 >= 9) {
            try {
                java.lang.reflect.Method m = java.io.InputStream.class
                    .getMethod("transferTo", java.io.OutputStream.class);
                transferred = (Long) m.invoke(inStream, outStream);
            } catch (Exception e) { transferred = doCopy(inStream, outStream); }
        } else {
            transferred = doCopy(inStream, outStream);
        }
        System.out.println("    transferTo() 传输了 " + transferred + " 字节");
        System.out.println("    输出内容: \"" + outStream.toString() + "\"");
        System.out.println();
        System.out.println("  注意：InputStream.transferTo() ≠ FileChannel.transferTo()");
        System.out.println("    InputStream.transferTo()：简化写法，底层还是 read/write 循环");
        System.out.println("    FileChannel.transferTo()：底层调 sendfile()，真正零拷贝");
        System.out.println("    前者是编程便利，后者是 IO 性能优化，两者不一样！");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第二节：JDK11 - Files.writeString/readString + InputStream增强
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 2. JDK11：Files.writeString / readString（文件 IO 更简洁）━━━");
        System.out.println();
        System.out.println("  JDK8 写字符串到文件：");
        System.out.println("    Files.write(path, content.getBytes(StandardCharsets.UTF_8));");
        System.out.println("  JDK11：");
        System.out.println("    Files.writeString(path, content);  // 默认 UTF-8");
        System.out.println("    Files.writeString(path, content, Charset.forName(\"GBK\")); // 指定编码");
        System.out.println();
        System.out.println("  JDK8 读文件到字符串：");
        System.out.println("    new String(Files.readAllBytes(path), StandardCharsets.UTF_8)");
        System.out.println("  JDK11：");
        System.out.println("    Files.readString(path);  // 默认 UTF-8，一行搞定");
        System.out.println();

        // 运行时检测 JDK 版本（JDK8 兼容的写法用反射调用）
        int javaVersion = getJavaVersion();
        System.out.println("  当前 JDK 版本: " + javaVersion);
        java.nio.file.Path jdk11TestFile = java.nio.file.Files.createTempFile("jdk11_", ".txt");
        try {
            if (javaVersion >= 11) {
                // 使用反射调用 JDK11 API，避免 JDK8 编译失败
                // 实际项目直接写：Files.writeString(path, "content")
                java.lang.reflect.Method writeString =
                    java.nio.file.Files.class.getMethod("writeString",
                        java.nio.file.Path.class, CharSequence.class,
                        java.nio.file.OpenOption[].class);
                writeString.invoke(null, jdk11TestFile, "JDK11 writeString test",
                    new java.nio.file.OpenOption[0]);

                java.lang.reflect.Method readString =
                    java.nio.file.Files.class.getMethod("readString", java.nio.file.Path.class);
                String read = (String) readString.invoke(null, jdk11TestFile);
                System.out.println("    Files.writeString + Files.readString 演示: \"" + read + "\"");
            } else {
                // JDK8 回退方案
                java.nio.file.Files.write(jdk11TestFile,
                    "JDK8 fallback write".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String read = new String(
                    java.nio.file.Files.readAllBytes(jdk11TestFile),
                    java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("    JDK8 写法（JDK11 Files.writeString/readString 不可用）: \""
                    + read + "\"");
            }
        } finally {
            java.nio.file.Files.deleteIfExists(jdk11TestFile);
        }
        System.out.println();

        System.out.println("  JDK11 InputStream 新方法：");
        System.out.println();
        System.out.println("  readNBytes(int len)：精确读取 N 字节（不像 read() 可能读少）");
        System.out.println("    // JDK8：");
        System.out.println("    byte[] buf = new byte[len];");
        System.out.println("    int totalRead = 0;");
        System.out.println("    while (totalRead < len) {");
        System.out.println("        int n = in.read(buf, totalRead, len - totalRead);");
        System.out.println("        if (n == -1) break;");
        System.out.println("        totalRead += n;");
        System.out.println("    }");
        System.out.println("    // JDK11：");
        System.out.println("    byte[] buf = in.readNBytes(len);  // 一行搞定！");
        System.out.println();
        System.out.println("  readAllBytes()：读全部（JDK9 已有，但 JDK11 性能改进）");
        System.out.println("  skipNBytes(long n)：精确跳过 N 字节（JDK12+，不会少跳）");
        System.out.println();

        // 演示 readNBytes（JDK11+，JDK8 用循环兜底）
        byte[] src = "0123456789ABCDEF".getBytes();
        java.io.InputStream is = new java.io.ByteArrayInputStream(src);
        byte[] first5 = readNBytesCompat(is, 5);
        System.out.println("  readNBytes(5) 演示: \"" + new String(first5) + "\"  (精确读5字节)");
        byte[] rest = readAllBytesCompat(is);
        System.out.println("  readAllBytes() 读剩余: \"" + new String(rest) + "\"");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第三节：JDK12~16 - Stream 增强 + 文本块 + Record 对 IO 代码的影响
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 3. JDK12~16：代码层面改进（影响 IO 代码写法）━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  这些版本没有重大 IO API 新特性，但以下改进影响 IO 代码写法：");
        System.out.println();
        System.out.println("  ① JDK13~15 文本块（Text Block）——多行字符串不再痛苦");
        System.out.println("     写 JSON/SQL/协议体时不再需要大量转义：");
        System.out.println();
        System.out.println("  // JDK8（痛苦）");
        System.out.println("  String json = \"{\\\"name\\\": \\\"Alice\\\",\" +");
        System.out.println("                \"\\\"age\\\": 30}\";");
        System.out.println();
        System.out.println("  // JDK15（文本块）");
        System.out.println("  String json = \"\"\"");
        System.out.println("      {");
        System.out.println("          \\\"name\\\": \\\"Alice\\\",");
        System.out.println("          \\\"age\\\": 30");
        System.out.println("      }");
        System.out.println("      \"\"\";");
        System.out.println("  // 直接发给 HttpClient 或写到文件，不再需要字符串拼接");
        System.out.println();
        System.out.println("  ② JDK14~16 Record（不可变数据类）——网络消息对象更简洁");
        System.out.println("     协议消息对象用 record 定义，替代 Builder 模式：");
        System.out.println();
        System.out.println("  // JDK8 写法（要写 getter/equals/hashCode/toString）");
        System.out.println("  public class LoginRequest {");
        System.out.println("      private final String username;");
        System.out.println("      private final String password;");
        System.out.println("      // 几十行 getter/constructor/toString...");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // JDK16 Record 写法（一行）");
        System.out.println("  record LoginRequest(String username, String password) {}");
        System.out.println("  // 自动生成 getter/equals/hashCode/toString");
        System.out.println("  // 非常适合作为网络消息 DTO（Data Transfer Object）");
        System.out.println();
        System.out.println("  ③ JDK14~21 switch 表达式 / 模式匹配 instanceof");
        System.out.println("     处理不同 IO 类型时代码更简洁：");
        System.out.println();
        System.out.println("  // JDK8（繁琐的 instanceof + cast）");
        System.out.println("  Object channel = getChannel();");
        System.out.println("  if (channel instanceof FileChannel) {");
        System.out.println("      FileChannel fc = (FileChannel) channel;");
        System.out.println("      fc.transferTo(...);");
        System.out.println("  }");
        System.out.println();
        System.out.println("  // JDK16+（模式匹配 instanceof）");
        System.out.println("  if (channel instanceof FileChannel fc) {");
        System.out.println("      fc.transferTo(...);  // fc 自动绑定，不需要强转");
        System.out.println("  }");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第四节：JDK19~21 虚拟线程（Project Loom）——IO 编程模型革命
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 4. JDK19~21：虚拟线程（Virtual Threads）—— IO 编程的革命 ━━━");
        System.out.println();
        System.out.println("  虚拟线程是 JDK21 最重大的特性（Project Loom），");
        System.out.println("  它彻底改变了 Java IO 编程模型的选择逻辑。");
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        System.out.println("  │               平台线程 vs 虚拟线程                           │");
        System.out.println("  ├───────────────────────────┬──────────────────────────────────┤");
        System.out.println("  │ 平台线程（OS Thread）     │ 虚拟线程（Virtual Thread）      │");
        System.out.println("  ├───────────────────────────┼──────────────────────────────────┤");
        System.out.println("  │ 1个JVM线程 = 1个OS线程    │ N个虚拟线程 = M个OS线程(N>>M)   │");
        System.out.println("  │ 栈内存：~1MB/线程         │ 栈内存：~几KB/虚拟线程（动态）  │");
        System.out.println("  │ 10万线程 → 100GB内存 崩溃 │ 100万虚拟线程 → 几GB 正常运行  │");
        System.out.println("  │ BIO阻塞 → OS线程挂起浪费  │ BIO阻塞 → 虚拟线程挂起，OS继续 │");
        System.out.println("  └───────────────────────────┴──────────────────────────────────┘");
        System.out.println();
        System.out.println("  虚拟线程的核心原理：");
        System.out.println("    每个虚拟线程运行在「载体线程」（平台线程）上");
        System.out.println("    当虚拟线程遇到阻塞 IO → JVM 自动把它从载体线程卸载");
        System.out.println("    载体线程继续运行其他虚拟线程（不阻塞！）");
        System.out.println("    IO 完成后 → JVM 把虚拟线程重新挂载到某个载体线程继续执行");
        System.out.println();
        System.out.println("  ═══ 🏭 生活场景：工厂流水线 理解虚拟线程 ═══");
        System.out.println();
        System.out.println("  假设你开了一家工厂，有 1000 个订单要处理：");
        System.out.println();
        System.out.println("  旧方式（平台线程 BIO，一个订单一个专属工人）：");
        System.out.println("    雇 1000 个工人，每个工人负责一个订单从头到尾");
        System.out.println("    订单处理中需要「等待供货商发货」（IO阻塞），工人就坐在那傻等");
        System.out.println("    1000 个工人大部分时间都在傻等供货商……");
        System.out.println("    问题：1000个工人工资（内存）、管理成本（线程切换）极高");
        System.out.println("          工人数量还有上限（OS线程数有限），超过就崩溃");
        System.out.println();
        System.out.println("  更好的方式（NIO，用 Selector 一个工人管多个订单）：");
        System.out.println("    只雇 8 个多面手工人（CPU核心数），");
        System.out.println("    工人不傻等，有哪个订单「可以继续做了」才去做哪个");
        System.out.println("    但问题是：写「哪个订单可以继续做了」的逻辑很复杂（NIO编程难）");
        System.out.println("    代码里全是 callback、CompletableFuture，难以理解和调试");
        System.out.println();
        System.out.println("  虚拟线程方式（JDK21，完美结合两者优点）：");
        System.out.println("    雇 8 个实际工人（平台线程，载体线程）");
        System.out.println("    为每个订单创建一张「任务便条」（虚拟线程），100万张便条内存够用");
        System.out.println("    工人拿起便条 A 开始干，遇到「等待供货」（IO阻塞）：");
        System.out.println("      JVM：「订单A在等货，先把便条 A 放一边（挂起）」");
        System.out.println("      JVM：「工人别闲着，去处理便条 B」");
        System.out.println("    货到了（IO完成）：");
        System.out.println("      JVM：「便条 A 的货到了，哪个工人空了去处理它」");
        System.out.println("    每张便条记录了「当前做到哪步了」（虚拟线程的调用栈/上下文）");
        System.out.println();
        System.out.println("  核心优势：");
        System.out.println("    ✅ 代码像 BIO 一样简单直观（一个虚拟线程处理一个请求，可以傻等）");
        System.out.println("    ✅ 性能像 NIO 一样高效（底层工人不傻等，IO阻塞自动切换）");
        System.out.println("    ✅ 便条（虚拟线程）可以有 100 万个，内存只占几 KB 每个");
        System.out.println("    ✅ 不需要 callback，代码从上往下顺序读懂，不再是「回调地狱」");
        System.out.println();
        System.out.println("  对比图：");
        System.out.println("    BIO + 平台线程：1000个请求 → 需要1000个OS线程 → 内存爆炸");
        System.out.println("    NIO + Selector：1000个请求 → 8个线程够用，但代码极复杂");
        System.out.println("    虚拟线程（JDK21）：1000个请求 → 1000个虚拟线程 → 8个工人，代码简单！");
        System.out.println();
        System.out.println("  类比：");
        System.out.println("    平台线程 = 真实员工（贵，10万人工资撑不住）");
        System.out.println("    虚拟线程 = 任务清单上的条目（便宜，100万条都行）");
        System.out.println("    载体线程 = 实际干活的工人（固定几个，CPU核数倍）");
        System.out.println("    阻塞 = 这个任务在等快递（IO），工人先去干别的任务");
        System.out.println("    IO 完成 = 快递到了，工人回来继续这个任务");
        System.out.println();
        System.out.println("  虚拟线程的使用方式：");
        System.out.println();
        System.out.println("  // 方式1：Thread.ofVirtual（JDK21）");
        System.out.println("  Thread vt = Thread.ofVirtual().start(() -> {");
        System.out.println("      // 这里可以写阻塞 BIO！");
        System.out.println("      String result = httpClient.get(\"https://api.example.com\"); // 阻塞");
        System.out.println("      System.out.println(result);");
        System.out.println("  });");
        System.out.println();
        System.out.println("  // 方式2：虚拟线程线程池（处理并发请求）");
        System.out.println("  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();");
        System.out.println("  // 每个任务都在独立虚拟线程中运行");
        System.out.println("  executor.submit(() -> {");
        System.out.println("      // BIO 数据库查询（可以阻塞，不浪费平台线程）");
        System.out.println("      User user = jdbc.query(\"select * from users where id=?\", 1);");
        System.out.println("      return user;");
        System.out.println("  });");
        System.out.println();

        // 实际创建虚拟线程演示（用反射兼容 JDK8 编译）
        System.out.println("  实际演示：");
        if (javaVersion >= 21) {
            try {
                // Thread.ofVirtual().start(Runnable) —— 通过反射调用
                java.lang.reflect.Method ofVirtual = Thread.class.getMethod("ofVirtual");
                Object builder = ofVirtual.invoke(null);
                java.lang.reflect.Method start = builder.getClass()
                    .getMethod("start", Runnable.class);
                Thread vt = (Thread) start.invoke(builder, (Runnable) () -> {
                    try {
                        boolean isVirtual = (Boolean) Thread.class
                            .getMethod("isVirtual").invoke(Thread.currentThread());
                        System.out.println("    虚拟线程执行中，isVirtual=" + isVirtual
                            + "  name=" + Thread.currentThread().getName());
                    } catch (Exception ex) {
                        System.out.println("    虚拟线程执行中，name=" + Thread.currentThread().getName());
                    }
                });
                vt.join();
            } catch (Exception e) {
                System.out.println("    反射调用虚拟线程失败: " + e.getMessage());
            }
        } else if (javaVersion >= 19) {
            System.out.println("    JDK19/20: 虚拟线程是 preview 特性，需要 --enable-preview 参数");
        } else {
            System.out.println("    当前 JDK" + javaVersion + " 不支持虚拟线程（需要 JDK21+）");
            System.out.println("    模拟普通线程: Thread.ofPlatform().start(() -> {...})");
        }
        System.out.println();

        System.out.println("  ★ 虚拟线程对 IO 编程模型的革命性影响：");
        System.out.println();
        System.out.println("  在虚拟线程之前（JDK8~17）：");
        System.out.println("    处理高并发 IO，必须用 NIO/Netty 的异步模型");
        System.out.println("    原因：BIO 阻塞 → 平台线程挂起 → 10万并发需要10万线程 → OOM");
        System.out.println("    代价：异步代码（Callback/CompletableFuture）极难调试");
        System.out.println();
        System.out.println("  有了虚拟线程（JDK21）：");
        System.out.println("    可以用 BIO 的写法处理高并发！");
        System.out.println("    BIO 阻塞 → 虚拟线程挂起（轻量级，不浪费平台线程）");
        System.out.println("    可以开 100万个虚拟线程，每个线程用同步阻塞 BIO");
        System.out.println("    代码简单如 BIO，性能接近 NIO");
        System.out.println();
        System.out.println("  对各框架的影响：");
        System.out.println("    Spring Boot 3.2+：默认支持虚拟线程（Tomcat 每个请求一个虚拟线程）");
        System.out.println("    Netty：仍然有价值（极致低延迟场景，Netty 比虚拟线程 BIO 快）");
        System.out.println("    JDBC：虚拟线程 + BIO JDBC 可以替代复杂的异步数据库客户端");
        System.out.println("    R2DBC（响应式DB驱动）：虚拟线程兴起后，R2DBC 的优势下降");
        System.out.println();
        System.out.println("  什么时候还需要 NIO/Netty（即使有了虚拟线程）：");
        System.out.println("    ① 极致低延迟：Netty 的 EventLoop 无上下文切换，比虚拟线程快");
        System.out.println("    ② 精确控制协议：自定义二进制协议、WebSocket 等");
        System.out.println("    ③ 大量短连接：每个请求一个虚拟线程，还是有切换开销");
        System.out.println("    ④ 现有 Netty 代码库：迁移成本高，不值得为了虚拟线程重写");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第五节：JDK21 结构化并发 + Scoped Values
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 5. JDK21：结构化并发（Structured Concurrency）━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  结构化并发解决的问题：");
        System.out.println("    一个 HTTP 请求需要并发调用两个微服务（用户服务 + 订单服务）");
        System.out.println("    用 CompletableFuture 写法复杂，还要处理：");
        System.out.println("      - 其中一个失败要取消另一个");
        System.out.println("      - 所有子任务完成才能结束父任务");
        System.out.println("      - 子任务的日志/追踪要关联到父任务");
        System.out.println();
        System.out.println("  ═══ 👷 生活场景：工程项目管理 理解结构化并发 ═══");
        System.out.println();
        System.out.println("  想象你是一个项目经理（主任务），负责完成一个装修项目：");
        System.out.println("  你派了两个施工队同时干活（两个子任务）：");
        System.out.println("    施工队A：负责刷墙（查用户信息）");
        System.out.println("    施工队B：负责铺地板（查订单信息）");
        System.out.println();
        System.out.println("  旧方式（CompletableFuture，就像传真机时代的管理）：");
        System.out.println("    你发了两份传真（submit 两个Future），然后你继续干别的");
        System.out.println("    问题来了：施工队A出事故了（任务失败），你根本不知道");
        System.out.println("    施工队B还在继续铺地板（白干了，浪费资源）");
        System.out.println("    你要手动写代码：「A失败了，取消B」——极其繁琐");
        System.out.println("    更糟的是：如果项目经理（主任务）提前退出，施工队还在后台乱跑！");
        System.out.println("    这就是「线程泄漏」——子任务脱离了父任务的控制");
        System.out.println();
        System.out.println("  结构化并发（StructuredTaskScope，就像现代项目管理系统）：");
        System.out.println("    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {");
        System.out.println("    // 「开工！」——创建一个「工程围栏」");
        System.out.println("    // 派出两个施工队，他们在「围栏」内工作");
        System.out.println("        Subtask<User> A = scope.fork(() -> 刷墙())");
        System.out.println("        Subtask<Order> B = scope.fork(() -> 铺地板())");
        System.out.println("        scope.join()  // 「等两队都完工」");
        System.out.println("        scope.throwIfFailed()  // 「任一失败，全停工（另一队自动收到停工令）」");
        System.out.println("        return 装修完成(A.get(), B.get())");
        System.out.println("    } // 「围栏关闭」——保证离开前所有施工队都已撤场，不会有队伍游荡在外");
        System.out.println();
        System.out.println("  核心保证（围栏的作用）：");
        System.out.println("    ✅ 任一失败 → 另一个自动停止（不浪费资源）");
        System.out.println("    ✅ 离开 try 块 → 所有子任务必然已完成/取消（不会线程泄漏）");
        System.out.println("    ✅ 子任务的异常/日志自动关联到父任务（追踪方便）");
        System.out.println("    ✅ 代码结构就像「进去→并发干活→等结束→出来」，逻辑清晰");
        System.out.println();
        System.out.println("  ShutdownOnFailure vs ShutdownOnSuccess：");
        System.out.println("    ShutdownOnFailure：任一失败，全部停（装修任一队出事，整个项目暂停）");
        System.out.println("    ShutdownOnSuccess：任一成功，全部停（多个备用服务竞速，谁快用谁）");
        System.out.println("    例子：同时请求3个城市的天气API，哪个先回来就用哪个");
        System.out.println();
        System.out.println("  JDK8 写法（CompletableFuture，复杂）：");
        System.out.println("    CompletableFuture<User> userFuture = CompletableFuture");
        System.out.println("        .supplyAsync(() -> userService.getUser(userId));");
        System.out.println("    CompletableFuture<Order> orderFuture = CompletableFuture");
        System.out.println("        .supplyAsync(() -> orderService.getOrders(userId));");
        System.out.println("    CompletableFuture.allOf(userFuture, orderFuture).thenRun(() -> {");
        System.out.println("        // 两个完成了再合并，但异常处理和取消逻辑很复杂...");
        System.out.println("    });");
        System.out.println();
        System.out.println("  JDK21 结构化并发写法（简洁）：");
        System.out.println("    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {");
        System.out.println("        // 并发提交两个子任务（都在虚拟线程里执行）");
        System.out.println("        Subtask<User> userTask  = scope.fork(() -> userService.getUser(id));");
        System.out.println("        Subtask<Order> orderTask = scope.fork(() -> orderService.getOrders(id));");
        System.out.println();
        System.out.println("        scope.join();           // 等两个都完成");
        System.out.println("        scope.throwIfFailed();  // 任一失败就抛出（另一个自动取消）");
        System.out.println();
        System.out.println("        // 两个都成功，安全获取结果");
        System.out.println("        return new Response(userTask.get(), orderTask.get());");
        System.out.println("    }  // 自动关闭 scope，确保子任务不泄漏");
        System.out.println();
        System.out.println("  ShutdownOnFailure（任一失败，全部取消）vs");
        System.out.println("  ShutdownOnSuccess（任一成功，全部取消——适合竞速场景）");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第六节：各 JDK 版本 IO 相关改进汇总表
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 6. JDK 版本 IO 相关改进汇总表 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  ┌────────┬──────────────────────────────────────────────────────┐");
        System.out.println("  │ 版本   │ IO / 并发相关新特性                                  │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK7   │ NIO.2（Path/Files/WatchService/AIO）                 │");
        System.out.println("  │        │ try-with-resources（AutoCloseable，IO资源自动关闭）   │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK8   │ Files.lines() Stream（懒加载大文件，避免OOM）         │");
        System.out.println("  │        │ CompletableFuture（异步编程）                        │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK9   │ InputStream.transferTo(OutputStream)                 │");
        System.out.println("  │        │ 模块系统（module-info.java，影响类加载和反射）        │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK11  │ Files.writeString/readString/isSameFile                │");
        System.out.println("  │        │ InputStream.readNBytes()/readAllBytes() 改进            │");
        System.out.println("  │        │ HttpClient（标准化，支持 HTTP/2 和 WebSocket）          │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK12+ │ InputStream.skipNBytes(long)（精确跳过N字节）          │");
        System.out.println("  │        │ switch 表达式（简化 IO 事件处理分支）                  │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK14+ │ Record（简化网络消息 DTO）                             │");
        System.out.println("  │        │ 模式匹配 instanceof（简化 Channel 类型判断）           │");
        System.out.println("  │        │ Text Block（简化 JSON/XML 协议体构建）                 │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK19  │ 虚拟线程 Preview（--enable-preview）                  │");
        System.out.println("  │        │ Structured Concurrency Preview                        │");
        System.out.println("  ├────────┼──────────────────────────────────────────────────────┤");
        System.out.println("  │ JDK21  │ ★ 虚拟线程 GA（Thread.ofVirtual()，无需 preview）    │");
        System.out.println("  │        │ ★ 结构化并发 Preview（StructuredTaskScope）           │");
        System.out.println("  │        │ Scoped Values Preview（替代 ThreadLocal 的轻量方案）   │");
        System.out.println("  │        │ Sequenced Collections（有序集合 API，影响 IO 缓冲区）  │");
        System.out.println("  └────────┴──────────────────────────────────────────────────────┘");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第七节：JDK11 HttpClient（标准化 HTTP 客户端）
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 7. JDK11：HttpClient（内置 HTTP/2 + 异步，替代 HttpURLConnection）");
        System.out.println();
        System.out.println("  HttpURLConnection（JDK 老 API）的问题：");
        System.out.println("    ① 不支持 HTTP/2（只有 HTTP/1.1）");
        System.out.println("    ② 只有同步阻塞 API");
        System.out.println("    ③ 连接复用（keep-alive）行为不透明");
        System.out.println("    ④ 代码极繁琐（开/关流/设头部/读响应...）");
        System.out.println();
        System.out.println("  JDK11 HttpClient（一步到位）：");
        System.out.println();
        System.out.println("  // 同步 GET（最简单）");
        System.out.println("  HttpClient client = HttpClient.newHttpClient();");
        System.out.println("  HttpRequest request = HttpRequest.newBuilder()");
        System.out.println("      .uri(URI.create(\"https://httpbin.org/get\"))");
        System.out.println("      .header(\"Accept\", \"application/json\")");
        System.out.println("      .GET()");
        System.out.println("      .build();");
        System.out.println("  HttpResponse<String> response =");
        System.out.println("      client.send(request, HttpResponse.BodyHandlers.ofString());");
        System.out.println("  System.out.println(response.statusCode()); // 200");
        System.out.println("  System.out.println(response.body());       // JSON 响应体");
        System.out.println();
        System.out.println("  // 异步 GET（非阻塞，返回 CompletableFuture）");
        System.out.println("  CompletableFuture<HttpResponse<String>> future =");
        System.out.println("      client.sendAsync(request, HttpResponse.BodyHandlers.ofString());");
        System.out.println("  future.thenApply(HttpResponse::body)");
        System.out.println("        .thenAccept(System.out::println);");
        System.out.println();
        System.out.println("  // POST JSON");
        System.out.println("  HttpRequest postReq = HttpRequest.newBuilder()");
        System.out.println("      .uri(URI.create(\"https://api.example.com/users\"))");
        System.out.println("      .header(\"Content-Type\", \"application/json\")");
        System.out.println("      .POST(HttpRequest.BodyPublishers.ofString(\"{\\\"name\\\":\\\"Alice\\\"}\"))");
        System.out.println("      .build();");
        System.out.println();
        System.out.println("  // 下载大文件（流式，不 OOM）");
        System.out.println("  HttpResponse<Path> fileResp = client.send(");
        System.out.println("      HttpRequest.newBuilder().uri(bigFileUri).build(),");
        System.out.println("      HttpResponse.BodyHandlers.ofFile(Paths.get(\"/tmp/download.zip\")));");
        System.out.println("  // 数据直接写入文件，不会把整个文件读进内存");
        System.out.println();
        System.out.println("  HttpClient 底层 IO：");
        System.out.println("    基于 NIO（java.nio.channels.SocketChannel）");
        System.out.println("    内置连接池，自动复用 keep-alive 连接");
        System.out.println("    支持 HTTP/2 多路复用（一个 TCP 连接并发多个请求）");
        System.out.println("    HTTP/2 底层：TLS + ALPN 协商协议，一个 SocketChannel 复用");
        System.out.println();

        // ════════════════════════════════════════════════════════════════
        // 第八节：虚拟线程 + Netty：选哪个？
        // ════════════════════════════════════════════════════════════════
        System.out.println("━━━ 8. 虚拟线程 + Netty 选型决策树（JDK21 时代）━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  场景1：普通微服务（Spring Boot + HTTP API + 数据库）");
        System.out.println("    → ✅ 虚拟线程（Spring Boot 3.2+）");
        System.out.println("    原因：业务 IO 等待（DB/HTTP）是瓶颈，虚拟线程解决了等待浪费问题");
        System.out.println("    代码：和 JDK8 BIO 一样简单，自动享受高并发");
        System.out.println();
        System.out.println("  场景2：高性能 RPC 框架 / 网关 / 消息中间件");
        System.out.println("    → ✅ Netty（NIO + EventLoop）");
        System.out.println("    原因：需要极致低延迟、自定义二进制协议、背压控制");
        System.out.println("    虚拟线程每次切换都有 ~μs 级开销，Netty EventLoop 是纳秒级");
        System.out.println();
        System.out.println("  场景3：文件批处理 / 日志处理（大量本地文件 IO）");
        System.out.println("    → ✅ 虚拟线程 + BIO（或 NIO）");
        System.out.println("    虚拟线程遇到文件读写阻塞时挂起，不浪费平台线程");
        System.out.println("    简洁的同步代码即可处理大量并发文件 IO");
        System.out.println();
        System.out.println("  场景4：大文件传输（网盘/CDN/下载服务）");
        System.out.println("    → ✅ FileChannel.transferTo()（零拷贝）+ NIO 或 Netty");
        System.out.println("    虚拟线程在这里没有优势（bottleneck 是磁盘/网络带宽，不是线程等待）");
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │ 总结：虚拟线程不是 Netty 的替代品，而是互补关系            │");
        System.out.println("  │       虚拟线程解决「线程数限制」问题                        │");
        System.out.println("  │       Netty 解决「极致性能 + 自定义协议」问题               │");
        System.out.println("  │       两者各有适用场景，未来可能会共存很长时间              │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        System.out.println();
        NIODemo.printSeparator();
    }

    /** 获取当前 JDK 主版本号（JDK8=8, JDK11=11, JDK21=21...） */
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            // JDK8 及之前：1.8.x -> 8
            return Integer.parseInt(version.substring(2, 3));
        } else {
            // JDK9+：11.0.2 -> 11, 21.0.1 -> 21
            int dot = version.indexOf('.');
            return Integer.parseInt(dot > 0 ? version.substring(0, dot) : version);
        }
    }

    /** 兼容 JDK8 的 InputStream 拷贝（替代 JDK9 transferTo） */
    private static long doCopy(java.io.InputStream in, java.io.OutputStream out)
            throws java.io.IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            total += n;
        }
        return total;
    }

    /** 兼容 JDK8 的 readNBytes（精确读N字节） */
    private static byte[] readNBytesCompat(java.io.InputStream in, int len)
            throws java.io.IOException {
        try {
            java.lang.reflect.Method m = java.io.InputStream.class
                .getMethod("readNBytes", int.class);
            return (byte[]) m.invoke(in, len);
        } catch (Exception e) {
            // JDK8 兜底：手动读 N 字节
            byte[] buf = new byte[len];
            int total = 0;
            while (total < len) {
                int n = in.read(buf, total, len - total);
                if (n == -1) break;
                total += n;
            }
            if (total < len) {
                byte[] trimmed = new byte[total];
                System.arraycopy(buf, 0, trimmed, 0, total);
                return trimmed;
            }
            return buf;
        }
    }

    /** 兼容 JDK8 的 readAllBytes */
    private static byte[] readAllBytesCompat(java.io.InputStream in)
            throws java.io.IOException {
        try {
            java.lang.reflect.Method m = java.io.InputStream.class.getMethod("readAllBytes");
            return (byte[]) m.invoke(in);
        } catch (Exception e) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            doCopy(in, bos);
            return bos.toByteArray();
        }
    }
}

