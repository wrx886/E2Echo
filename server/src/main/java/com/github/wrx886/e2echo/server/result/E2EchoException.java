package com.github.wrx886.e2echo.server.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class E2EchoException extends RuntimeException {
    private Integer code;

    public E2EchoException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public E2EchoException(ResultCodeEnum resultCodeEnum) {
        this(resultCodeEnum.getCode(), resultCodeEnum.getMessage());
    }
}
