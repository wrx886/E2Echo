package com.github.wrx886.e2echo.client.api;

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
 * <p>调用服务端 {@code /timestamp} 接口获取服务端当前时间，用于校准本地时间、判断客户端与服务端
 * 的连通性以及检查两端的时间偏差。</p>
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

}
