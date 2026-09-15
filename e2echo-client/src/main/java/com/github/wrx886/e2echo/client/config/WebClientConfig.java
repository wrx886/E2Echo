package com.github.wrx886.e2echo.client.config;

import com.github.wrx886.e2echo.client.common.BaseUrlStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置。
 *
 * <p>把容器中的 {@link WebClient} 指向登入时填写的服务器：baseUrl 取自 {@link BaseUrlStore}，
 * 容器启动时该地址已由登入界面写入，因此这里的 Bean 只需要读取一次。</p>
 */
@Configuration
public class WebClientConfig {

    /**
     * 创建以登入时填写的服务器地址为 baseUrl 的 WebClient。
     *
     * @param builder Spring Boot 自动配置的 WebClient 构造器
     * @return 指向 E2Echo 服务端的 WebClient
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(BaseUrlStore.getBaseUrl())
                .build();
    }

}
