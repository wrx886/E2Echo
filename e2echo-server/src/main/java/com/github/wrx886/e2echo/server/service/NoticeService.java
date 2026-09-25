package com.github.wrx886.e2echo.server.service;

import com.github.wrx886.e2echo.server.common.RedisPrefix;
import com.github.wrx886.e2echo.server.exception.E2EchoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知业务逻辑层。
 *
 * <p>为客户端提供 SSE 长连接，并借助 Redis 的发布订阅在多个实例之间转发通知，使客户端无论
 * 连接在哪个实例上都能收到自己订阅目标的通知。</p>
 *
 * <p>连接信息保存在本实例内存的映射中，键为“实例 ID + 客户端原始 ID”，加上实例前缀是为
 * 了避免不同实例上的同名客户端相互冲突。订阅关系保存在 Redis 的 Set 中：
 * {@code client -> to}（{@link RedisPrefix#CLIENT2TOS}）用于连接断开时找到需要清理的订阅，
 * {@code to -> client}（{@link RedisPrefix#TO2CLIENTS}）用于通知时找到需要通知的连接。</p>
 *
 * <p>通知流程：{@link #notice(String)} 在 {@code to -> client} 中查出订阅者，按客户端 ID 中
 * 记录的实例 ID 把消息发布到对应实例的频道；持有连接的实例收到频道消息后，通过
 * {@link #onMessage(Message, byte[])} 找到本实例的 SSE 连接并推送。消息保存成功后，
 * {@link MessageService} 会调用 {@link #notice(String)} 触发通知，该通知异步执行，不会阻塞
 * 保存消息的调用方。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService implements MessageListener {

    /**
     * 本实例 ID，用于隔离不同实例上的同名客户端以及各自的订阅频道。
     */
    private final String instanceId = UUID.randomUUID().toString().replace("-", "");

    /**
     * Redis 操作对象，用于维护订阅关系与发布频道消息。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 本实例持有的 SSE 连接，键为“实例 ID + 客户端原始 ID”。
     */
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 建立 SSE 连接并订阅目标。
     *
     * <p>客户端 ID 会加上本实例 ID 前缀后作为键，注册成功即开始接收订阅目标的通知；注册时
     * 同时把订阅关系写入 Redis，并登记连接关闭、出错、超时三种回调用于后续清理。注册完成后还会
     * 立即向该连接推送一条通知，让客户端确认订阅已经生效并拉取一次数据。</p>
     *
     * @param clientOriginal 客户端原始 ID
     * @param tos            需要订阅的目标列表
     * @return SSE 连接对象，由 Spring MVC 负责保持与关闭
     * @throws E2EchoException 参数 {@code tos} 为空，或同一实例上该客户端 ID 已被占用
     */
    public SseEmitter connect(String clientOriginal, List<String> tos) {
        // 以 当前实例ID + 原始客户端ID 作为 client
        String client = instanceId + clientOriginal;

        if (tos == null || tos.isEmpty()) {
            throw new E2EchoException("参数 tos 不能为空！");
        }

        // 生成并注册
        SseEmitter sseEmitter = new SseEmitter(30 * 60 * 1000L); // 超时时间 30 分钟
        emitters.compute(client, (k, v) -> {
            if (v != null) throw new E2EchoException("客户端ID已被占用！");

            // 注册
            // client -> tos
            stringRedisTemplate.opsForSet().add(
                    RedisPrefix.CLIENT2TOS + client,
                    tos.toArray(new String[0])
            );
            // to -> clients
            for (String to : tos) {
                stringRedisTemplate.opsForSet().add(
                        RedisPrefix.TO2CLIENTS + to,
                        client
                );
            }

            // 注册关闭回调
            sseEmitter.onCompletion(() -> onEmitterClose(client, null));
            sseEmitter.onError((e) -> onEmitterClose(client, e));
            sseEmitter.onTimeout(() -> onEmitterClose(client, null));

            // 返回即完成注册
            return sseEmitter;
        });

        // 注册完成后立即推一条通知（内容与数据变化的通知相同）：SSE 的响应头要等第一次写出才
        // 提交，客户端在此之前收不到任何数据、无法确认订阅已经生效；这条通知既让它确认连接可用，
        // 也会让它按通知语义立刻拉取一次数据
        //
        // 这时 emitter 还没交给 Spring MVC，send 只把数据缓冲下来，等框架接管后统一写出，写出
        // 失败会被框架转成 onError 回调，订阅关系由回调清理，这里不需要另外处理
        try {
            sseEmitter.send("reflash");
        } catch (IOException e) {
            // send 声明的是受检异常，包装成非受检异常交给全局异常处理
            throw new RuntimeException(e);
        }

        return sseEmitter;
    }

    /**
     * 处理 SSE 连接关闭：清理 Redis 中的订阅关系，并把连接从本实例移除。
     *
     * <p>先按 {@code client -> to} 找到该客户端订阅过的目标，逐个把客户端从
     * {@code to -> client} 中移除，再删除 {@code client -> to} 本身；最后返回 {@code null}
     * 使映射中不再保留该连接。</p>
     *
     * @param client    带实例前缀的客户端 ID
     * @param throwable 关闭原因，正常关闭或超时为 {@code null}，出错时为对应异常
     */
    private void onEmitterClose(String client, Throwable throwable) {
        emitters.computeIfPresent(client, (k, v) -> {
            // 输出日至
            if (throwable == null) {
                log.info("SSE connection close for client: {}", client.substring(instanceId.length()));
            } else {
                log.error("SSE connection error for client: {}", client.substring(instanceId.length()), throwable);
            }

            // 这里写关闭回调
            // to -> clients
            try (Cursor<String> cursor = stringRedisTemplate.opsForSet().scan(RedisPrefix.CLIENT2TOS + client, ScanOptions.scanOptions().build())) {
                while (cursor.hasNext()) {
                    String to = cursor.next();
                    stringRedisTemplate.opsForSet().remove(RedisPrefix.TO2CLIENTS + to, client);
                }
            }
            // client -> tos
            stringRedisTemplate.delete(RedisPrefix.CLIENT2TOS + client);

            // 这里直接清空即可
            return null;
        });
    }

    /**
     * 通知订阅了某个目标的所有客户端。
     *
     * <p>从 {@code to -> client} 中取出订阅者，按客户端 ID 中的实例 ID 把消息发布到该实例
     * 的频道；消息体为带实例前缀的客户端 ID，由持有连接的实例据此找到 SSE 连接。</p>
     *
     * <p>该方法异步执行，调用方不会等待通知发出；通知过程中的异常只记录日志，不影响消息保存
     * 的结果。</p>
     *
     * @param to 通知目标，与客户端订阅时传入的目标一致
     */
    @Async
    public void notice(String to) {
        try (Cursor<String> cursor = stringRedisTemplate.opsForSet().scan(RedisPrefix.TO2CLIENTS + to, ScanOptions.scanOptions().build())) {
            while (cursor.hasNext()) {
                String client = cursor.next();
                String channel = RedisPrefix.CHANNEL + client.substring(0, instanceId.length());
                stringRedisTemplate.convertAndSend(channel, client);
            }
        }
    }

    /**
     * 处理订阅频道收到的消息：把通知推送给本实例上对应的 SSE 连接。
     *
     * <p>推送失败时以该异常结束连接，避免后续通知继续投向已经不可用的连接。</p>
     *
     * @param message 频道消息，消息体为带实例前缀的客户端 ID
     * @param pattern 订阅模式，精确频道订阅时为 {@code null}
     */
    @Override
    public void onMessage(@NonNull Message message, byte @Nullable [] pattern) {
        // 获取消息的基本信息
        String client = new String(message.getBody(), StandardCharsets.UTF_8);

        // 处理消息：用 computeIfPresent 让推送与连接关闭时的移除、清理互斥，避免向已经关闭、
        // 已从映射中移除的连接推送（complete 之后再 send 会抛 IllegalStateException）；同一个
        // emitter 上的并发写入由 SseEmitter 内部的写锁串行化
        emitters.computeIfPresent(client, (k, emitter) -> {
            try {
                emitter.send("reflash");
            } catch (Exception e) {
                log.error("SSE send error for id: {}", client.substring(instanceId.length()), e);
                emitter.completeWithError(e);
            }
            return emitter;
        });
    }

    /**
     * 获取本实例的订阅频道。
     *
     * @return 由频道前缀与本实例 ID 拼成的频道名
     */
    public String getChannel() {
        return RedisPrefix.CHANNEL + instanceId;
    }

    /**
     * 应用关闭时结束本实例持有的全部 SSE 连接，并等待关闭回调完成清理。
     *
     * <p>监听 {@link ContextClosedEvent} 而不是用 {@code @PreDestroy}：关闭事件在 Spring 停止
     * 生命周期组件之前发布，此时 Web 容器与 Redis 连接都还可以使用，{@link SseEmitter#complete()}
     * 发出的关闭信号能真正送达客户端并触发关闭回调，兜底清理中的 Redis 调用也能正常执行。若放在
     * {@code @PreDestroy} 里，两者都已被停止，连接结束不了、Redis 调用还会抛
     * {@code IllegalStateException}。</p>
     *
     * <p>{@link SseEmitter#complete()} 只是发出关闭信号，真正的关闭过程由容器异步完成，连接
     * 关闭回调 {@link #onEmitterClose(String, Throwable)} 会把连接从映射中移除并清理 Redis 中的
     * 订阅关系。这里以指数退避的方式等待，直到映射清空或等待超时；等待结束后再对仍然留在映射
     * 中的连接同步清理一次，保证 Redis 中的订阅关系不残留。</p>
     */
    @SuppressWarnings("BusyWait")
    @EventListener(ContextClosedEvent.class)
    public void cleanUp() {
        log.info("Shutting down, {} SSE connection(s) to close.", emitters.size());

        emitters.forEach((client, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 单个连接结束失败不能影响其它连接与后续清理
                log.warn("Failed to complete SSE connection for client: {}",
                        client.substring(instanceId.length()), e);
            }
        });

        // 指数退避等待：休眠 1、2、4 …… 8192 毫秒，累计最长约 16 秒
        long waited = 0;
        boolean interrupted = false;
        try {
            long sleep = 1;
            while (sleep < 10 * 1000L && !emitters.isEmpty()) {
                log.info("Waiting {} ms for {} SSE connection(s) to close.", sleep, emitters.size());
                Thread.sleep(sleep);
                waited += sleep;
                sleep *= 2;
            }
        } catch (InterruptedException e) {
            // 这里只记下被中断过，不抛出、也不立即恢复中断标志：标志一旦恢复，后面的 Redis 调用
            log.warn("Interrupted while waiting for SSE connections to close.", e);
            interrupted = true;
        }

        // 等待结束后，仍留在映射里的连接直接同步清理，保证 Redis 不残留
        if (!emitters.isEmpty()) {
            log.warn("{} SSE connection(s) did not close within timeout, cleaning up directly.",
                    emitters.size());
        }
        for (String client : List.copyOf(emitters.keySet())) {
            onEmitterClose(client, null);
        }

        log.info("SSE connections closed, waited {} ms.", waited);

        // 清理完成后再恢复中断标志，交由调用方处理
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

}
