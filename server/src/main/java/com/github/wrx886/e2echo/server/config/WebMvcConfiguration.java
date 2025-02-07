package com.github.wrx886.e2echo.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.wrx886.e2echo.server.custom.converter.StringToBaseEnumConverterFactory;
import com.github.wrx886.e2echo.server.custom.interceptor.AuthenticationInterceptor;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Autowired
    private StringToBaseEnumConverterFactory stringToBaseEnumConverterFactory;

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(stringToBaseEnumConverterFactory);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 这里设置 admin 项目的所有请求都要登入，登入请求除外
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/server/**")
                .excludePathPatterns("/server/login/**");
    }
}
