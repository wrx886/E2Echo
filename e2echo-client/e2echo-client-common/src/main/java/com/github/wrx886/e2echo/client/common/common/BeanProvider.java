package com.github.wrx886.e2echo.client.common.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// Bean 提供者工具类，为 JavaFX 提供 SpringBoot 上下文和 Bean 获取功能
@Component
public final class BeanProvider implements ApplicationContextAware {
    // 上下文
    private static ApplicationContext applicationContext;

    // 获取 Bean
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    // 获取上下文
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BeanProvider.applicationContext = applicationContext;
    }
}
