package org.example.java_base_test.io.nio.show_multi_agent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

class Part8_NIOServerExplained {

    static void explain() {
        System.out.println("【第八部分：完整 NIO Server（Reactor 单线程模式）】");
        System.out.println();
        System.out.println("═══ ✈️ 生活场景：机场塔台调度 理解 Reactor 模式 ═══");
        System.out.println();
        System.out.println("  把整个 NIO Server 想象成一座繁忙机场的调度系统：");
        System.out.println();
        System.out.println("  【机场结构对应关系】");
        System.out.println("  塔台（1个调度员）    = 1个 Selector 线程（Reactor 核心）");
        System.out.println("  机场大门（一个）      = ServerSocketChannel（监听新连接）");
        System.out.println("  每架飞机             = 每个 SocketChannel（客户端连接）");
        System.out.println("  飞机呼叫塔台         = 数据就绪事件（OP_READ/OP_WRITE）");
        System.out.println("  塔台给飞机许可       = 处理完毕，继续监听");
        System.out.println();
        System.out.println("  【整个运作流程】");
        System.out.println("  1. 塔台开门营业（selector = Selector.open()）");
        System.out.println("  2. 大门注册「有新飞机要进来就通知我」（register OP_ACCEPT）");
        System.out.println("  3. 塔台调度员开始值班等消息（while(true) { selector.select() }）");
        System.out.println("     → 调度员在休息室睡觉（线程休眠），有事才醒");
        System.out.println("  4. 有新飞机进港（OP_ACCEPT）：");
        System.out.println("     登记这架飞机（accept() 得到 SocketChannel）");
        System.out.println("     给它分配频道（register OP_READ），以后听它说话");
        System.out.println("  5. 飞机有呼叫（OP_READ）：read() 读取数据，处理，回复");
        System.out.println("  6. 处理完回到步骤3等下一个事件");
        System.out.println();
        System.out.println("  【1个调度员为什么能管100架飞机？】");
        System.out.println("  因为大部分时间飞机不说话（连接空闲），");
        System.out.println("  调度员只处理「正在说话」的飞机，其余时间休息。");
        System.out.println("  这就是单线程能处理高并发的核心原理！");
        System.out.println();
        System.out.println("  【iter.remove() 为什么必须有？】");
        System.out.println("  机场记录本（selectedKeys）记录了「今天响应的飞机」");
        System.out.println("  处理完一架不 remove()，下次还会误以为它在呼叫！");
        System.out.println("  → 重复处理同一事件 = 飞机被叫了两次，大乱！");
        System.out.println();
        System.out.println("═══ 以下是完整 NIO Server 代码讲解 ═══");
        System.out.println();
        System.out.println("完整代码（逐行注释版）：");
        System.out.println();
        System.out.println("  // ① 创建 Selector（底层 epoll_create1）");
        System.out.println("  Selector selector = Selector.open();");
        System.out.println();
        System.out.println("  // ② 创建 ServerSocketChannel，绑定端口");
        System.out.println("  ServerSocketChannel server = ServerSocketChannel.open();");
        System.out.println("  server.bind(new InetSocketAddress(8080));");
        System.out.println("  server.configureBlocking(false); // ★ 必须设非阻塞");
        System.out.println();
        System.out.println("  // ③ 注册 OP_ACCEPT（底层 epoll_ctl ADD）");
        System.out.println("  server.register(selector, SelectionKey.OP_ACCEPT);");
        System.out.println();
        System.out.println("  // ④ 事件循环（Reactor 核心）");
        System.out.println("  while (true) {");
        System.out.println("      selector.select(); // 底层 epoll_wait，休眠等待事件");
        System.out.println("      Iterator<SelectionKey> iter = selector.selectedKeys().iterator();");
        System.out.println("      while (iter.hasNext()) {");
        System.out.println("          SelectionKey key = iter.next();");
        System.out.println("          iter.remove(); // ★ 必须手动移除！");
        System.out.println();
        System.out.println("          if (key.isAcceptable()) {");
        System.out.println("              // 新连接");
        System.out.println("              SocketChannel client = server.accept();");
        System.out.println("              client.configureBlocking(false); // ★ 也要非阻塞");
        System.out.println("              // 附加 ByteBuffer 作为这个连接的读缓冲");
        System.out.println("              client.register(selector, SelectionKey.OP_READ,");
        System.out.println("                  ByteBuffer.allocate(1024));");
        System.out.println();
        System.out.println("          } else if (key.isReadable()) {");
        System.out.println("              SocketChannel client = (SocketChannel) key.channel();");
        System.out.println("              ByteBuffer buf = (ByteBuffer) key.attachment();");
        System.out.println("              int n = client.read(buf);");
        System.out.println("              if (n == -1) {");
        System.out.println("                  key.cancel(); client.close(); // 对端关闭");
        System.out.println("              } else if (n > 0) {");
        System.out.println("                  buf.flip();");
        System.out.println("                  ByteBuffer response = ByteBuffer.wrap(处理(buf));");
        System.out.println("                  int written = client.write(response);");
        System.out.println("                  if (response.hasRemaining()) {");
        System.out.println("                      // 没写完，注册 OP_WRITE 等待通知");
        System.out.println("                      key.attach(response);");
        System.out.println("                      key.interestOps(OP_READ | OP_WRITE);");
        System.out.println("                  }");
        System.out.println("                  buf.clear();");
        System.out.println("              }");
        System.out.println();
        System.out.println("          } else if (key.isWritable()) {");
        System.out.println("              SocketChannel client = (SocketChannel) key.channel();");
        System.out.println("              ByteBuffer buf = (ByteBuffer) key.attachment();");
        System.out.println("              client.write(buf);");
        System.out.println("              if (!buf.hasRemaining()) {");
        System.out.println("                  // 写完，取消 OP_WRITE！否则 CPU 100%");
        System.out.println("                  key.interestOps(SelectionKey.OP_READ);");
        System.out.println("                  key.attach(ByteBuffer.allocate(1024));");
        System.out.println("              }");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();
        System.out.println("这就是 Reactor 单线程模式：1个线程 + 1个 Selector 管理所有连接");
        System.out.println();
        System.out.println("Netty 是对这个模式的工业级封装：");
        System.out.println("  Boss EventLoopGroup（1线程）  → 只负责 accept");
        System.out.println("  Worker EventLoopGroup（CPU×2）→ 负责 read/write/业务");
        System.out.println("  每个 Channel 绑定一个 EventLoop，整个生命周期不换");
        System.out.println("  → 无锁化：同一 Channel 的事件总在同一线程处理");
        System.out.println();
        System.out.println("Netty 解决的原生 NIO 七宗罪：");
        System.out.println("  ① Buffer flip/clear 容易搞错  → ByteBuf 双指针，无需 flip");
        System.out.println("     原生NIO需手动切换读写模式，易遗漏；ByteBuf自动管理readerIndex/writerIndex");
        System.out.println("  ② 粘包/拆包需自己处理         → 内置多种 FrameDecoder");
        System.out.println("     TCP流式协议无消息边界；Netty提供长度/分隔符等解码器自动处理");
        System.out.println("  ③ JDK Selector epoll 空轮询 Bug→ 检测重建 Selector");
        System.out.println("     Linux下select()可能立即返回导致CPU 100%；Netty检测后自动重建");
        System.out.println("  ④ 异常处理繁琐                → Pipeline 统一处理");
        System.out.println("     原生NIO到处try-catch；Netty通过责任链在末尾统一捕获异常");
        System.out.println("  ⑤ 无连接池                    → Channel 对象池");
        System.out.println("     频繁创建销毁Channel开销大；Netty提供ChannelPool复用连接");
        System.out.println("  ⑥ 无编解码                    → 内置 HTTP/WebSocket/自定义");
        System.out.println("     原生NIO只传字节；Netty内置HTTP/Protobuf/JSON等编解码器");
        System.out.println("  ⑦ 无心跳机制                  → IdleStateHandler");
        System.out.println("     需手动实现心跳检测；Netty可配置空闲超时自动触发事件");
        System.out.println();
        NIODemo.printSeparator();
    }

    /** 可选：后台启动一个真实的 NIO Echo Server */
    static Thread startAsync(int port) {
        Thread t = new Thread(() -> {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel server = ServerSocketChannel.open();
                server.bind(new InetSocketAddress(port));
                server.configureBlocking(false);
                server.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("  [NIO EchoServer] 启动在端口 " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    if (selector.select(500) == 0) continue;
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        try {
                            if (key.isAcceptable()) {
                                SocketChannel client = server.accept();
                                if (client != null) {
                                    client.configureBlocking(false);
                                    client.register(selector, SelectionKey.OP_READ,
                                            ByteBuffer.allocate(1024));
                                }
                            } else if (key.isReadable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                ByteBuffer buf = (ByteBuffer) key.attachment();
                                int n = client.read(buf);
                                if (n == -1) { key.cancel(); client.close(); }
                                else if (n > 0) { buf.flip(); client.write(buf); buf.clear(); }
                            }
                        } catch (IOException e) { key.cancel(); }
                    }
                }
                server.close(); selector.close();
            } catch (IOException e) {
                System.err.println("  [NIO EchoServer] 启动失败：" + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}

