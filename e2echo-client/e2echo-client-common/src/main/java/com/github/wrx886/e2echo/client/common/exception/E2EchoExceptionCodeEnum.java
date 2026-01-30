package com.github.wrx886.e2echo.client.common.exception;

import lombok.Getter;

// 返回状态码枚举
@Getter
public enum E2EchoExceptionCodeEnum {
    OK("0000", "OK"),
    FAIL("0001", "FAIL"),
    CUSTOM("0003", "CUSTOM MESSAGE"),

    // ECC 相关
    ECC_PUBLIC_KEY_IS_EMPTY("AA01", "ECC：公钥为空"),
    ECC_PUBLIC_KEY_NOT_MATCH("AA02", "ECC：公钥不匹配"),
    ECC_ENCRYPT_FAILED("AA03", "ECC：加密失败"),
    ECC_SIGN_FAILED("AA04", "ECC：签名失败"),
    ECC_NOT_LOGIN("AA05", "ECC：用户未登录"),
    ECC_SIGNATURE_NOT_MATCH("AA06", "ECC：签名不匹配"),
    ECC_SIGNATURE_VERIFY_FAILED("AA07", "ECC：签名验证失败"),
    ECC_DECRYPT_FAILED("AA08", "ECC：解密失败"),
    ECC_KEY_PAIR_INVALID("AA09", "ECC：公钥和私钥不匹配或不合法"),
    ECC_KEY_PAIR_GENERATION_FAILED("AA10", "ECC：公钥和私钥生成失败"),
    ECC_DATA_IS_NULL("AA11", "ECC：数据为 null"),
    ECC_SAVE_JSON_FILE_FAILED("AA12", "ECC：保存 JSON 文件失败"),
    ECC_READ_JSON_FILE_FAILED("AA13", "ECC：读取 JSON 文件失败");

    // 状态码
    private final String code;

    // 状态描述
    private final String message;

    private E2EchoExceptionCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
