package com.github.wrx886.e2echo.server.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {

    @Bean
    public Executor executor() {
        int coreNumber = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                coreNumber,
                coreNumber * 2,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(300),
                new ThreadPoolExecutor.AbortPolicy());
    }

}
