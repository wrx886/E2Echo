package com.github.wrx886.e2echo.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 前端通知业务逻辑层。
 *
 * <p>给前端提供一条 SSE 通道：前端连上来后注册到 {@link #emitters}，客户端拉到新消息时调用
 * {@link #notice()} 推一条 {@code reflash}，前端收到后重新拉取数据。推送内容只表示“数据可能变了”，
 * 具体数据仍由消息、会话接口查询。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    /**
     * 已经连上来的前端连接：键是 SSE 连接本身（按对象身份区分），值用 {@code Optional} 占位，
     * 因为 {@code ConcurrentHashMap} 不允许存 {@code null}。
     */
    private final ConcurrentHashMap<SseEmitter, Optional<Void>> emitters = new ConcurrentHashMap<>();

    /**
     * 建立前端通知连接。
     *
     * <p>超时时间 30 分钟，与连接服务端 notice 通道的做法一致：到点由容器断开，前端重连即可。注册
     * 完成后再立即推一条 {@code reflash}——SSE 的响应头要等第一次写出才提交，这条既让前端确认连接
     * 已经生效，也会让它先拉一次数据。</p>
     *
     * @return SSE 连接对象，交给 Spring MVC 接管
     */
    public SseEmitter connect() {
        // 生成并注册
        SseEmitter sseEmitter = new SseEmitter(30 * 60 * 1000L); // 超时时间 30 分钟
        emitters.compute(sseEmitter, (k, v) -> {
            // 注册关闭回调
            sseEmitter.onCompletion(() -> onEmitterClose(sseEmitter, null));
            sseEmitter.onError((e) -> onEmitterClose(sseEmitter, e));
            sseEmitter.onTimeout(() -> onEmitterClose(sseEmitter, null));

            // 返回即完成注册
            return Optional.empty();
        });

        try {
            sseEmitter.send("reflash");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return sseEmitter;
    }

    /**
     * 连接关闭（正常结束、出错或超时）后的清理：记日志并从映射中移除。
     *
     * @param sseEmitter 关闭的连接
     * @param throwable  出错时的异常，正常结束或超时为 {@code null}
     */
    private void onEmitterClose(SseEmitter sseEmitter, Throwable throwable) {
        // 输出日志
        if (throwable == null) {
            log.info("SSE connection close: {}", sseEmitter);
        } else {
            log.error("SSE connection error: {}", sseEmitter, throwable);
        }
        emitters.remove(sseEmitter);
    }

    /**
     * 通知所有连上来的前端：数据可能变了，重新拉取。
     *
     * <p>标了 {@code @Async}，需要客户端容器开启 {@code @EnableAsync} 才会真正异步执行，否则就在
     * 调用线程上同步推送（本方法只是往各连接写一小段数据，同步执行也没有阻塞风险）。单个连接推送
     * 失败只记日志，连接由超时或关闭回调清理。</p>
     */
    @Async
    public void notice() {
        emitters.forEach((emitter, v) -> {
            try {
                emitter.send("reflash");
            } catch (Exception e) {
                log.error("通知失败", e);
            }
        });
    }

    /**
     * 应用关闭时结束本实例持有的全部 SSE 连接，并等待关闭回调完成清理。
     */
    @SuppressWarnings("BusyWait")
    @EventListener(ContextClosedEvent.class)
    public void cleanUp() {
        log.info("Shutting down, {} SSE connection(s) to close.", emitters.size());

        emitters.forEach((emitter, v) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 单个连接结束失败不能影响其它连接与后续清理
                log.warn("Failed to complete SSE connection for client: {}",
                        emitter, e);
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
        log.info("SSE connections closed, waited {} ms.", waited);

        // 清理完成后再恢复中断标志，交由调用方处理
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

}
