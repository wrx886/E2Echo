package com.github.wrx886.e2echo.client.api;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.result.ResultCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

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

        Result<String> result;
        try {
            result = webClient
                    .get()
                    .uri("timestamp")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<String>>() {
                    })
                    .block();
        } catch (WebClientRequestException e) {
            // 地址不可达、服务未启动等情况：请求根本没有发到服务端
            throw new E2EchoException("无法连接服务器！");
        } catch (WebClientException e) {
            // 服务端返回了非 2xx 状态码（正常业务失败约定为 200 + code）
            throw new E2EchoException("服务器状态异常！");
        }

        // 响应体为空（例如响应不是约定的 JSON 结构）时不能直接取 message，避免空指针
        if (result == null) {
            throw new E2EchoException("服务器状态异常！");
        }
        if (!ResultCodeEnum.OK.getCode().equals(result.code())) {
            throw new E2EchoException(result.message() == null ? "服务器状态异常！" : result.message());
        }
        return result.data();
    }

}
