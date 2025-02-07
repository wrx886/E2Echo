package com.github.wrx886.e2echo.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI().info(
                new Info()
                        .title("E2Echo 后端 API")
                        .version("1.0-SNAPSHOT")
                        .description("E2Echo 后端 API"));
    }

    @Bean
    public GroupedOpenApi loginAPI() {
        return GroupedOpenApi.builder().group("登录管理").pathsToMatch(
                "/server/login/**").build();
    }

    @Bean
    public GroupedOpenApi messageAPI() {
        return GroupedOpenApi.builder().group("消息管理").pathsToMatch(
                "/server/message/**").build();
    }

    @Bean
    public GroupedOpenApi fileAPI() {
        return GroupedOpenApi.builder().group("文件管理").pathsToMatch(
                "/server/file/**").build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder().group("用户管理").pathsToMatch(
                "/server/user/**").build();
    }

}