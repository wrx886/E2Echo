package com.github.wrx886.e2echo.client.config;

import com.github.wrx886.e2echo.client.common.BaseUrlStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(BaseUrlStore.getBaseUrl())
                .build();
    }

}
