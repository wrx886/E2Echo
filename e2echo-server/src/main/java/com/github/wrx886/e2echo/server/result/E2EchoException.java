package com.github.wrx886.e2echo.server.result;

import lombok.Getter;

// 项目异常类
@Getter
public class E2EchoException extends RuntimeException {

    private ResultCodeEnum resultCodeEnum;

    public E2EchoException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.resultCodeEnum = resultCodeEnum;
    }

}
