package com.github.wrx886.e2echo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin("*")
@EnableDiscoveryClient // 开启服务发现
@SpringBootApplication
// @EnableAsync
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}