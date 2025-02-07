package com.github.wrx886.e2echo.server.test.util;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.server.util.JwtUtils;

import io.jsonwebtoken.Claims;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class JwtUtilsTest {

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    public void test() {
        Long userId = new Random().nextLong((long) 1e9);
        System.out.println("userId: " + userId);

        String token = jwtUtils.createLoginUserToken(userId);
        System.out.println("token: " + token);

        Claims claims = jwtUtils.parseLoginUserToken(token);
        System.out.println("claims: " + claims);

        assert claims.get("userId", Long.class).equals(userId);
    }

}
