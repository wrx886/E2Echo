package com.github.wrx886.e2echo.client.common;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Bean 提供者。
 *
 * <p>实现 {@link ApplicationContextAware} 把 Spring 应用上下文保存为静态字段，使不由 Spring
 * 管理的对象（例如 JavaFX 控制器）也能通过 {@link #getBean(Class)} 获取容器中的 Bean。</p>
 */
@Component
public final class BeanProvider implements ApplicationContextAware {

    /**
     * Spring 应用上下文，容器启动时注入。
     */
    @Getter
    private static ApplicationContext applicationContext;

    /**
     * 按类型获取容器中的 Bean。
     *
     * @param clazz Bean 的类型
     * @param <T>   Bean 类型
     * @return 该类型对应的 Bean 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 由 Spring 容器回调注入应用上下文。
     *
     * @param applicationContext 容器创建的应用上下文
     * @throws BeansException 上下文注入失败
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        BeanProvider.applicationContext = applicationContext;
    }
}
