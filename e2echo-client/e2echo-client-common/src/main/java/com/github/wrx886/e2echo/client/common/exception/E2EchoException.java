package com.github.wrx886.e2echo.client.common.exception;

import lombok.Getter;

// 项目异常类
@Getter
public final class E2EchoException extends RuntimeException {

    private E2EchoExceptionCodeEnum e2EchoExceptionCodeEnum;

    public E2EchoException(E2EchoExceptionCodeEnum e2EchoExceptionCodeEnum) {
        super(e2EchoExceptionCodeEnum.getMessage());
        this.e2EchoExceptionCodeEnum = e2EchoExceptionCodeEnum;
    }

    public E2EchoException(String message) {
        super(message);
        this.e2EchoExceptionCodeEnum = E2EchoExceptionCodeEnum.CUSTOM;
    }

}
