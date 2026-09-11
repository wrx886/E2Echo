package com.github.wrx886.e2echo.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 审计配置。
 *
 * <p>开启 JPA 审计功能，使实体中的 {@code @CreatedDate}、{@code @LastModifiedDate}
 * 等审计字段能够自动填充。</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
