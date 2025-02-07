package com.github.wrx886.e2echo.server.model.vo.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登入参数")
public class LoginVo {

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "手机验证码")
    private String phoneCode;

    @Schema(description = "签名码的key")
    private String signCodeKey;

    @Schema(description = "使用私钥加密后的签名码的值")
    private String signCodeValue;

    @Schema(description = "公钥")
    private String publicKey;

}
