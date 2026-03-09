package com.github.wrx886.e2echo.server.config;

import java.util.UUID;

import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
public class IdConfig {

    // 标识该实例的唯一标识符
    @Getter
    public static final String ID = UUID.randomUUID().toString();

}
