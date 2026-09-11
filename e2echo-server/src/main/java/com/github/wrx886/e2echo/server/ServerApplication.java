package com.github.wrx886.e2echo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 服务端启动类。
 *
 * <p>负责启动 Spring Boot 应用，并开启异步执行能力。</p>
 */
@EnableAsync // 异步
@SpringBootApplication
public class ServerApplication {

    /**
     * 应用入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}
