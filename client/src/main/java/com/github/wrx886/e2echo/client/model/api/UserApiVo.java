package com.github.wrx886.e2echo.client.model.api;

import lombok.Data;

// 用户信息
@Data
public class UserApiVo {

    // 用户的手机号
    private String phone;

    // 用户的公钥
    private String publicKey;

}
