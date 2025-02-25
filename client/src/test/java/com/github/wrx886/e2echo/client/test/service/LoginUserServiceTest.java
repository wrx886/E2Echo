package com.github.wrx886.e2echo.client.test.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.wrx886.e2echo.client.service.LoginUserService;
import com.github.wrx886.e2echo.client.store.LoginUserStore;
import com.github.wrx886.e2echo.client.test.config.LoginConfig;

@SpringBootTest
public class LoginUserServiceTest {

    @Autowired
    private LoginUserService loginUserService;

    @Autowired
    private LoginConfig loginConfig;

    @Autowired
    private LoginUserStore loginUserStore;

    // 获取手机验证码
    @Test
    public void getPhoneCode() {
        loginUserService.getPhoneCode(loginConfig.getBaseUrl(), loginConfig.getPhone());
    }

    // 登入
    @Test
    public void login() {
        loginUserService.login(loginConfig.getBaseUrl(),
                loginConfig.getPhone(),
                loginConfig.getPhoneCode(),
                loginConfig.getPublicKey(),
                loginConfig.getPrivateKey());
        Assertions.assertNotNull(loginUserStore.getAccessToken());
        Assertions.assertNotNull(loginUserStore.getBaseUrl());
        Assertions.assertNotNull(loginUserStore.getId());
        Assertions.assertNotNull(loginUserStore.getPrivateKey());
        Assertions.assertNotNull(loginUserStore.getPublicKey());
        System.out.println(loginUserStore);
    }

}
