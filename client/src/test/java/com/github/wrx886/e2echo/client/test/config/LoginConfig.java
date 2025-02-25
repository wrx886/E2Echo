package com.github.wrx886.e2echo.client.test.config;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class LoginConfig {

    // 用户登入id
    private final Long ownerId = 1L;

    // 用户公钥
    private final String publicKey = "04162dd8a037f4910945f2faab09eb0beeb4a61c34d92c50888cc87ded010b6a3f8cda33d96b3ee99600a0bc1ace8e8b6c4c3e8f954460c8d8d5a3419f3cf7a830";

    // 用户私钥
    private final String privateKey = "71159bed96c6755bf11fb1411154fc5cec299167554c761d2df8f5a2c163e706";

    // 用户手机号
    private final String phone = "12345678910";

    // 有效手机验证码
    private final String phoneCode = "193997";

    // 服务器地址
    private final String baseUrl = "http://localhost:8080";

    // access-token
    private final String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjIwNTU4MTIzMTYsInN1YiI6IkxvZ2luVXNlciIsInVzZXJJZCI6MX0.gg0mBcwxeqWZBfMh53q5X-23bh0C-f5K2o0tnL9ICvU";

}
