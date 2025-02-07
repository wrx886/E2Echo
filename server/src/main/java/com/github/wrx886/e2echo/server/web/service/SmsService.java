package com.github.wrx886.e2echo.server.web.service;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    // 发送手机验证码
    public void sendCode(String phone, String code, String use) {
        System.out.println(Map.of(
                "phone", phone,
                "code", code,
                "use", use));
    }

}
