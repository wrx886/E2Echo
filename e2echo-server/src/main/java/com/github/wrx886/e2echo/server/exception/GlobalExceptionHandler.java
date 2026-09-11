package com.github.wrx886.e2echo.server.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wrx886.e2echo.server.result.Result;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器，统一将异常转换为 {@link Result} 返回。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，返回异常携带的具体信息。
     *
     * @param e 业务异常
     * @return 失败结果，message 为异常信息
     */
    @ExceptionHandler(E2EchoException.class)
    public Result<Void> handleE2EchoException(E2EchoException e) {
        return Result.fail(e.getMessage());
    }

    /**
     * 处理其他未捕获的异常，返回统一的失败结果。
     *
     * @param e 未捕获的异常
     * @return 失败结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail();
    }

}
