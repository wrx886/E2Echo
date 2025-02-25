package com.github.wrx886.e2echo.client.model.api;

import lombok.Data;

// 登入参数
@Data

public class LoginApiVo {

    // 手机号
    private String phone;

    // 手机验证码
    private String phoneCode;

    // 签名码的 key
    private String signCodeKey;

    // 签名码的 value
    private String signCodeValue;

    // 公钥
    private String publicKey;

}
