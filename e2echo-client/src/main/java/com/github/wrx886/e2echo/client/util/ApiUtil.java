package com.github.wrx886.e2echo.client.util;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;

import java.util.concurrent.Callable;

/**
 * 接口调用工具类。
 *
 * <p>把 WebClient 的传输异常与统一响应 {@link Result} 的状态码统一转换成业务异常
 * {@link E2EchoException}，使各个 API 类只关注请求参数与响应类型。</p>
 */
@Slf4j
public final class ApiUtil {

    /**
     * 私有构造方法，防止外部实例化工具类。
     */
    private ApiUtil() {
    }

    /**
     * 执行返回统一响应的请求，并返回响应中的数据。
     *
     * @param callable 执行请求并返回统一响应的动作
     * @param <R>      业务数据类型
     * @return 响应中的数据，可能为 {@code null}
     * @throws E2EchoException 请求失败，或服务端返回失败状态
     */
    public static <R> R apiGet(Callable<Result<R>> callable) {

        Result<R> result = apiCall(callable);
        // 响应体为空（例如响应不是约定的 JSON 结构）时不能直接取 message，避免空指针
        if (result == null) {
            throw new E2EchoException("服务器状态异常！");
        }
        if (!ResultCodeEnum.OK.getCode().equals(result.code())) {
            throw new E2EchoException(result.message() == null ? "服务器状态异常！" : result.message());
        }
        return result.data();
    }

    /**
     * 执行请求，把传输异常转换成业务异常。
     *
     * <p>响应不由统一响应包装时（例如下载文件）可以直接使用本方法，由调用方自行处理返回值。</p>
     *
     * @param callable 执行请求并返回结果的动作
     * @param <R>      结果类型
     * @return 请求结果
     * @throws E2EchoException 请求失败
     */
    public static <R> R apiCall(Callable<R> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw toBusinessException(e);
        }
    }

    /**
     * 处理返回流的请求，把流中的异常转换成业务异常。
     *
     * <p>用于 SSE 这类长连接：异常不会在调用时抛出，而是作为流的错误信号传递。</p>
     *
     * @param flux 请求的响应流
     * @param <R>  流中元素的类型
     * @return 转换异常后的响应流
     */
    public static <R> Flux<R> apiFlux(Flux<R> flux) {
        return flux.onErrorMap(ApiUtil::toBusinessException);
    }

    /**
     * 把接口调用中的异常转换成业务异常。
     *
     * @param throwable 原始异常
     * @return 业务异常；传入的本来就是业务异常时原样返回
     */
    private static E2EchoException toBusinessException(Throwable throwable) {
        if (throwable instanceof E2EchoException e) {
            return e;
        }
        if (throwable instanceof WebClientRequestException) {
            // 地址不可达、服务未启动等情况：请求根本没有发到服务端
            return new E2EchoException("无法连接服务器！");
        }
        if (throwable instanceof WebClientException) {
            // 服务端返回了非 2xx 状态码（正常业务失败约定为 200 + code）
            return new E2EchoException("服务器状态异常！");
        }
        log.error("接口调用异常", throwable);
        return new E2EchoException("服务异常！");
    }

}
