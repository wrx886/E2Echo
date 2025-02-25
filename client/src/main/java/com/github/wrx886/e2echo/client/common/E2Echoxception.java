package com.github.wrx886.e2echo.client.common;

import lombok.Getter;

// 项目异常
public final class E2Echoxception extends RuntimeException {

    // 错误代码
    @Getter
    private final Integer code;

    public E2Echoxception(Integer code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public E2Echoxception(CodeEnum codeEnum) {
        this(codeEnum.getCode(), codeEnum.getMessage());
    }

}
