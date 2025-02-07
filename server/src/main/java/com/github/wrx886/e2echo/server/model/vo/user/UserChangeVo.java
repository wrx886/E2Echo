package com.github.wrx886.e2echo.server.model.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "换绑业务参数")
public class UserChangeVo {

    @Schema(description = "旧手机号的验证码")
    private String oldCode;

    @Schema(description = "新手机号")
    private String newPhone;

    @Schema(description = "新手机号的验证码")
    private String newCode;

    @Schema(description = "签名码的 key")
    private String signCodeKey;

    @Schema(description = "使用私钥加密后的签名码的值")
    private String signCodeValue;
}
