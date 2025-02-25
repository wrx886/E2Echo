package com.github.wrx886.e2echo.client.model.api;

import lombok.Data;

// 用户换绑业务参数
@Data
public class UserChangeApiVo {

    // 旧手机验证码
    private String oldCode;

    // 新手机号
    private String newPhone;

    // 新手机验证码
    private String newCode;

    // 签名码的 key
    private String signCodeKey;

    // 签名码的 value
    private String signCodeValue;

}
