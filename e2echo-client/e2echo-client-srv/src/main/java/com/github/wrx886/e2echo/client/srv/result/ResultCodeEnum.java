package com.github.wrx886.e2echo.client.srv.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "返回状态码枚举")
public enum ResultCodeEnum {
    OK("0000", "OK"),
    FAIL("0001", "FAIL");

    @Schema(description = "状态码")
    private final String code;

    @Schema(description = "状态描述")
    private final String message;

    private ResultCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
