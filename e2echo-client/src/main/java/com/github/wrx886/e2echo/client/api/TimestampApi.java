package com.github.wrx886.e2echo.client.api;

import com.github.wrx886.e2echo.client.common.Const;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.util.ApiUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 时间戳接口。
 *
 * <p>调用服务端 {@code /timestamp} 接口获取服务端当前时间：其他接口在发请求前用它校验两端的时间
 * 偏差，也可以用来判断与服务端的连通性。</p>
 */
@Component
@RequiredArgsConstructor
public class TimestampApi {

    /**
     * WebClient，其 baseUrl 为登入时填写的服务器地址。
     */
    private final WebClient webClient;

    /**
     * 获取服务端当前时间戳。
     *
     * @return 服务端当前时间的毫秒数（十进制字符串）
     * @throws E2EchoException 无法连接服务器，或服务端返回失败状态
     */
    public String timestamp() {
        return ApiUtil.apiGet(() -> webClient
                .get()
                .uri("timestamp")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {
                })
                .block());
    }

    /**
     * 校验本机与服务器的时间偏差。
     *
     * <p>偏差超过 5 秒时抛异常：服务端会用时间戳校验消息，偏差过大时消息发不出去。</p>
     *
     * @throws E2EchoException 时间偏差过大，或时间戳接口调用失败
     */
    public void checkTime() {
        long timestamp = Long.parseLong(timestamp());
        if (Math.abs(timestamp - System.currentTimeMillis()) > Const.MAX_TIME_DIFF_MS) {
            throw new E2EchoException("本机与服务器时间差距过大!");
        }
    }

}
