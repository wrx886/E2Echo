package com.github.wrx886.e2echo.server.util;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
    @Value("${jwt-utils.secret-key}")
    private String secretKey;

    @Value("${jwt-utils.expiration-day}")
    private Integer expirationDay;

    // 创建 token
    public String createLoginUserToken(Long userId) {
        return Jwts.builder()
                // 设置 1 天后过期
                .setExpiration(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * expirationDay)))
                .setSubject("LoginUser")
                .claim("userId", userId)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    // 解析 token，并将解析结果存放到 LoginUserHolder 中
    public Claims parseLoginUserToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_TOKEN_EXPIRED);
        }
    }
}
