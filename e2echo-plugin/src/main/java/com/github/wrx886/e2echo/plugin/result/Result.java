package com.github.wrx886.e2echo.plugin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "全局统一返回结果类")
public class Result<T> {

    @Schema(description = "返回码")
    private String code;

    @Schema(description = "返回消息")
    private String message;

    @Schema(description = "返回数据")
    private T data;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> build(ResultCodeEnum resultCodeEnum, T data) {
        return new Result<>(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), data);
    }

    public static Result<Void> build(ResultCodeEnum resultCodeEnum) {
        return build(resultCodeEnum, null);
    }

    public static <T> Result<T> ok(T data) {
        return build(ResultCodeEnum.OK, data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(T data) {
        return build(ResultCodeEnum.FAIL, data);
    }

    public static <T> Result<T> fail() {
        return fail(null);
    }

}
