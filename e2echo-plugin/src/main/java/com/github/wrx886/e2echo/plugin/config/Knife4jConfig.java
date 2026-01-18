package com.github.wrx886.e2echo.plugin.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("E2Echo 加解密插件 API")
                .version("1.0")
                .description("E2Echo 加解密插件 API"));
    }

    @Bean
    public GroupedOpenApi eccAPI() {
        return GroupedOpenApi.builder()
                .group("ECC 加解密")
                .pathsToMatch("/ecc/**")
                .build();
    }
}
