package com.github.wrx886.e2echo.client.common.exception;

import lombok.Getter;

// 项目异常类
@Getter
public final class E2EchoException extends RuntimeException {

    private E2EchoExceptionCodeEnum resultCodeEnum;

    public E2EchoException(E2EchoExceptionCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.resultCodeEnum = resultCodeEnum;
    }

}
