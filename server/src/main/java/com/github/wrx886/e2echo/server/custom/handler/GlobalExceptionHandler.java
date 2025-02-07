package com.github.wrx886.e2echo.server.custom.handler;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.Result;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 捕获所有异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        // 自定义处理异常
        e.printStackTrace();
        return Result.fail(e.getMessage());
    }

    // 处理公寓异常
    @ExceptionHandler(E2EchoException.class)
    public Result<Void> handleE2EchoException(E2EchoException e) {
        return Result.build(e.getCode(), e.getMessage());
    }
}
