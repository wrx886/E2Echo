package com.github.wrx886.e2echo.server.result;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 响应状态码枚举。
 *
 * <p>定义统一的业务状态码及其默认提示信息，供 {@link Result} 使用。</p>
 */
@Getter
@ToString
public enum ResultCodeEnum {

    /**
     * 成功。
     */
    OK("0", "OK"),

    /**
     * 失败。
     */
    FAIL("-1", "FAIL");

    /**
     * 状态码。
     */
    private final String code;

    /**
     * 默认提示信息。
     */
    private final String message;

    /**
     * 使用状态码与默认提示信息构造枚举项。
     *
     * @param code    状态码
     * @param message 默认提示信息
     */
    ResultCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
