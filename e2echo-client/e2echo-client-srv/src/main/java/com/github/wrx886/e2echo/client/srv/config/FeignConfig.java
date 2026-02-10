package com.github.wrx886.e2echo.client.srv.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.github.wrx886.e2echo.client.srv.feign")
public class FeignConfig {

}
