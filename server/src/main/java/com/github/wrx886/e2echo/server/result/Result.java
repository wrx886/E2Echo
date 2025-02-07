package com.github.wrx886.e2echo.server.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "全局统一返回结果类")
public class Result<T> {
    @Schema(description = "返回码")
    private Integer code;

    @Schema(description = "返回消息")
    private String message;

    @Schema(description = "返回数据")
    private T data;

    private Result(Integer code, String message, T date) {
        this.code = code;
        this.message = message;
        this.data = date;
    }

    private Result(ResultCodeEnum resultEnum, T date) {
        this(resultEnum.getCode(), resultEnum.getMessage(), date);
    }

    public static Result<Void> build(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<T>(ResultCodeEnum.OK, data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(T data) {
        return new Result<T>(ResultCodeEnum.FAIL, data);
    }

    public static <T> Result<T> fail() {
        return fail(null);
    }
}
