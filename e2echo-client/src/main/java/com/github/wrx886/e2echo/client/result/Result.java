package com.github.wrx886.e2echo.client.result;

/**
 * 统一响应结果。
 *
 * <p>所有接口均以该结构返回，包含状态码、提示信息与业务数据。</p>
 *
 * @param code    状态码，取值参见 {@link ResultCodeEnum}
 * @param message 提示信息
 * @param data    业务数据，可为 {@code null}
 * @param <T>     业务数据类型
 */
public record Result<T>(String code, String message, T data) {

    /**
     * 使用状态码、提示信息与数据构建结果。
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    业务数据
     * @param <T>     业务数据类型
     * @return 响应结果
     */
    public static <T> Result<T> build(String code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 使用状态码与提示信息构建不含数据的结果。
     *
     * @param code    状态码
     * @param message 提示信息
     * @return 响应结果
     */
    public static Result<Void> build(String code, String message) {
        return build(code, message, null);
    }

    /**
     * 使用状态码枚举与数据构建结果。
     *
     * @param resultCodeEnum 状态码枚举
     * @param data           业务数据
     * @param <T>            业务数据类型
     * @return 响应结果
     */
    public static <T> Result<T> build(ResultCodeEnum resultCodeEnum, T data) {
        return build(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), data);
    }

    /**
     * 使用状态码枚举构建不含数据的结果。
     *
     * @param resultCodeEnum 状态码枚举
     * @return 响应结果
     */
    public static Result<Void> build(ResultCodeEnum resultCodeEnum) {
        return build(resultCodeEnum, null);
    }

    /**
     * 构建成功结果。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功结果
     */
    public static <T> Result<T> ok(T data) {
        return build(ResultCodeEnum.OK, data);
    }

    /**
     * 构建不含数据的成功结果。
     *
     * @return 成功结果
     */
    public static Result<Void> ok() {
        return ok(null);
    }

    /**
     * 使用指定提示信息构建失败结果。
     *
     * @param message 提示信息
     * @return 失败结果
     */
    public static Result<Void> fail(String message) {
        return build(ResultCodeEnum.FAIL.getCode(), message);
    }

    /**
     * 使用默认提示信息构建失败结果。
     *
     * @return 失败结果
     */
    public static Result<Void> fail() {
        return fail(ResultCodeEnum.FAIL.getMessage());
    }

}
