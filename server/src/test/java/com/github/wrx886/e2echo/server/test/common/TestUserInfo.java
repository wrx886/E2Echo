package com.github.wrx886.e2echo.server.test.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class TestUserInfo {

    @Value("${test-user.phone}")
    private String phone;

    @Value("${test-user.public-key}")
    private String publicKey;

    @Value("${test-user.private-key}")
    private String privateKey;

    @Value("${test-user.access-token}")
    private String accessToken;

}
