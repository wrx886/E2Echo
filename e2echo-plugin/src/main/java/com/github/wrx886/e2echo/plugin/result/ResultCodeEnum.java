package com.github.wrx886.e2echo.plugin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "返回状态码枚举")
public enum ResultCodeEnum {
    OK("0000", "OK"),
    FAIL("0001", "FAIL"),

    // ECC 相关
    ECC_PUBLIC_KEY_IS_EMPTY("AA01", "ECC 公钥为空"),
    ECC_PUBLIC_KEY_NOT_MATCH("AA02", "ECC 公钥不匹配"),
    ECC_ENCRYPT_FAILED("AA03", "ECC 加密失败"),
    ECC_SIGN_FAILED("AA04", "ECC 签名失败"),
    ECC_NOT_LOGIN("AA05", "ECC 用户未登录"),
    ECC_SIGNATURE_NOT_MATCH("AA06", "ECC 签名不匹配"),
    ECC_SIGNATURE_VERIFY_FAILED("AA07", "ECC 签名验证失败"),
    ECC_DECRYPT_FAILED("AA08", "ECC 解密失败"),
    ECC_KEY_PAIR_INVALID("AA09", "ECC 公钥和私钥不匹配或不合法"),
    ECC_KEY_PAIR_GENERATION_FAILED("AA10", "ECC 公钥和私钥生成失败"),
    ECC_DATA_IS_EMPTY("AA11", "ECC 数据为空"),

    // GUI 相关
    GUI_SAVE_JSON_FILE_FAILED("AB01", "GUI 保存 JSON 文件失败"),
    GUI_READ_JSON_FILE_FAILED("AB02", "GUI 读取 JSON 文件失败");

    @Schema(description = "状态码")
    private final String code;

    @Schema(description = "状态描述")
    private final String message;

    private ResultCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
