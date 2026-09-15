package com.github.wrx886.e2echo.client.api;

import java.time.Duration;
import java.util.List;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.util.ApiUtil;
import com.github.wrx886.e2echo.client.util.IdUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 通知接口。
 *
 * <p>对应服务端的 {@code /notice} 接口：建立 SSE 长连接并声明订阅的目标，此后目标发生变化时
 * 服务端通过该连接推送通知。推送内容是固定的 {@link #NOTICE_REFLASH}，只表示“数据已经变化，
 * 该重新拉取了”，真正的数据需要用消息或文件接口查询。</p>
 */
@Component
@RequiredArgsConstructor
public class NoticeApi {

    /**
     * 服务端推送的内容：订阅的目标发生变化，需要重新拉取数据。
     */
    public static final String NOTICE_REFLASH = "reflash";

    /**
     * 重连的初始间隔，失败次数越多间隔越长。
     */
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(1);

    /**
     * 重连间隔的上限。
     */
    private static final Duration RECONNECT_MAX_DELAY = Duration.ofMinutes(1);

    /**
     * 连接保持时间达到该值即视为稳定连接，下一次重连的间隔重新从 {@link #RECONNECT_DELAY} 开始。
     *
     * <p>保持时间从发起连接算起，到这次连接结束（正常结束或出错）为止，重连前的等待时间不计入。
     * 这么算是因为服务端在第一条通知之前不会下发响应：客户端在连接空闲期间收不到任何数据，无法用
     * “收到响应”判断连接是否建立，只能用保持时长区分“连不住”和“连上了并且长时间存活”。</p>
     *
     * <p>短于该值说明连接建不起来、或者刚连上就被断开，属于连续失败，间隔继续按次数增长；服务端会
     * 主动关闭超过 30 分钟的连接，因此正常情况下的重连都是“稳定连接后的重连”，间隔都从 1 秒开始。</p>
     */
    private static final Duration RECONNECT_HEALTHY_DURATION = Duration.ofSeconds(30);

    /**
     * WebClient，其 baseUrl 为登入时填写的服务器地址。
     */
    private final WebClient webClient;

    /**
     * 建立通知连接。
     *
     * <p>返回的是冷流：订阅时才真正发起请求，连接建立后每收到一条通知就发出一个元素。连接结束或
     * 出错时本方法会自动重连，订阅方不需要自己处理：服务端会主动关闭超过 30 分钟的连接，此时流
     * 正常结束；连接出错（网络中断、服务端未启动、服务端返回失败状态等）时同样直接重连，不做是否
     * 可恢复的判断。两种情况的重连间隔一致：起始 1 秒，每次翻倍，最长 1 分钟；但上一次连接若是
     * 稳定运行过的（保持时间达到 30 秒），重连间隔会重新从 1 秒开始，避免长期运行时退避时间一路
     * 涨到上限。</p>
     *
     * <p>重连是无限次的，不需要时用订阅得到的 {@code Disposable} 结束即可。</p>
     *
     * <p>客户端 ID 由本方法用 {@link IdUtil#newId()} 生成，并且每次重连都会换一个新的：服务端要求
     * 同一个实例上的客户端 ID 不能重复，而连接断开后服务端不一定立刻清理掉旧连接，换新 ID 才能保证
     * 重连不被拒绝。为此请求构造放在 {@code defer} 里，重连时重新订阅会重新生成 ID。</p>
     *
     * <p>异常处理交给 {@link ApiUtil#apiFlux(Flux)}：传输异常与服务端失败状态会以
     * {@link E2EchoException} 的形式出现在流的错误信号里，不会在调用本方法时抛出。</p>
     *
     * @param tos 需要订阅的目标列表，不能为空
     * @return 通知内容流，元素为服务端推送的内容（通常等于 {@link #NOTICE_REFLASH}）
     */
    public Flux<String> connect(List<String> tos) {

        // 外层 defer：本次订阅的重复计数放在订阅时创建，避免多次订阅之间互相影响
        return ApiUtil.apiFlux(Flux.defer(() -> {

            // 上一次连接的建立时间与连续重连次数，供重连时计算等待时间
            AtomicLong connectedAt = new AtomicLong();
            AtomicLong reconnectCount = new AtomicLong();

            // 内层 defer 位于重连操作符的上游：重连时会重新订阅它，
            // 从而重新构造请求、重新生成客户端 ID
            Flux<String> notices = Flux.defer(() -> {
                connectedAt.set(System.currentTimeMillis());
                return webClient.post()
                        .uri("notice/connect/{client}", IdUtil.newId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(tos)
                        .retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                        })
                        .mapNotNull(ServerSentEvent::data);
            });

            return notices
                    // 出错一律重连，不做是否可恢复的判断
                    .retryWhen(Retry.from(signals -> signals.concatMap(
                            signal -> Mono.delay(nextReconnectDelay(connectedAt, reconnectCount)))))
                    // 正常结束重连：服务端会关闭超过 30 分钟的连接，退避规则与出错重连一致；
                    // repeatWhen 的伴生流每次发出的都是 1，所以重复次数由 nextReconnectDelay 自己数
                    .repeatWhen(companion -> companion.concatMap(
                            signal -> Mono.delay(nextReconnectDelay(connectedAt, reconnectCount))));
        }));
    }

    /**
     * 计算下一次重连的等待时间。
     *
     * <p>上一次连接保持时间达到 {@link #RECONNECT_HEALTHY_DURATION} 时视为稳定连接，重连次数清零，
     * 等待时间回到 {@link #RECONNECT_DELAY}；否则等待时间按次数翻倍，最长 {@link #RECONNECT_MAX_DELAY}。</p>
     *
     * @param connectedAt    上一次连接的建立时间（毫秒）
     * @param reconnectCount 连续重连次数
     * @return 下一次重连的等待时间
     */
    private static Duration nextReconnectDelay(AtomicLong connectedAt, AtomicLong reconnectCount) {

        if (System.currentTimeMillis() - connectedAt.get() >= RECONNECT_HEALTHY_DURATION.toMillis()) {
            // 上一次连接稳定运行过，退避重新开始
            reconnectCount.set(0);
        }
        return reconnectDelay(reconnectCount.incrementAndGet());
    }

    /**
     * 计算第 {@code attempt} 次连续重连的等待时间。
     *
     * <p>起始 {@link #RECONNECT_DELAY}，每次翻倍，最长 {@link #RECONNECT_MAX_DELAY}。</p>
     *
     * @param attempt 重连次数，从 1 开始
     * @return 本次重连的等待时间
     */
    private static Duration reconnectDelay(long attempt) {

        // 移位上限取 20 是为了避免溢出：2^20 个起始间隔已经远大于最长等待时间
        long exponent = Math.min(attempt - 1, 20);
        long millis = RECONNECT_DELAY.toMillis() << exponent;
        return Duration.ofMillis(Math.min(millis, RECONNECT_MAX_DELAY.toMillis()));
    }

}
