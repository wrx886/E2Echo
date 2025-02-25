package com.github.wrx886.e2echo.client.store;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.model.entity.LoginUser;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// 存储登入用户信息
@Component
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LoginUserStore extends LoginUser {

    // 服务器地址
    private String baseUrl;

    // 用户私钥
    private String privateKey;

    // access-token
    private String accessToken;

}
