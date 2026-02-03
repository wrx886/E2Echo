package com.github.wrx886.e2echo.client.srv.interceptor;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.srv.store.WebUrlStore;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DynamicUrlRequestInterceptor implements RequestInterceptor {

    private final WebUrlStore webUrlStore;

    @Override
    public void apply(RequestTemplate template) {
        // 替换 $Dynamic$ 为 Web URL
        String url = webUrlStore.getWebUrl();
        if (url == null || url.isBlank()) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEB_URL_IS_EMPTY);
        }
        // 去掉开头的 http 或 https
        url = url.replaceFirst("^https?://", "");
        template.target(
                template.feignTarget().url()
                        .replace("$Dynamic$", url));
    }

}
