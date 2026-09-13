package com.github.wrx886.e2echo.client.config;

import com.github.wrx886.e2echo.client.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p>注册登录校验拦截器并设置拦截范围：除用于换取登录状态的 {@code /auth/**} 之外，其余请求
 * （含静态资源）都必须来自已认证的会话。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /**
     * 登录校验拦截器。
     */
    private final AuthInterceptor authInterceptor;

    /**
     * 注册登录校验拦截器。
     *
     * @param registry 拦截器注册表，由 Spring MVC 传入
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**");
    }

}
