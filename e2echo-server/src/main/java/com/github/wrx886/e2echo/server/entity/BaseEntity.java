package com.github.wrx886.e2echo.server.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;

/**
 * 实体基类，包含主键与审计字段。
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 主键，由“16 位十六进制时间戳 + 32 位无连字符 UUID”组成，共 48 位。
     */
    @Id
    @Column(length = 48, nullable = false, updatable = false)
    private String id;

    /**
     * 创建时间，由 JPA 审计自动注入，创建后不可修改。
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 修改时间，由 JPA 审计自动注入。
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updateTime;

    /**
     * 持久化前生成主键：16 位十六进制时间戳 + 32 位无连字符 UUID。
     */
    @PrePersist
    protected void prePersist() {
        if (id == null) {
            id = String.format("%016x", System.currentTimeMillis())
                    + UUID.randomUUID().toString().replace("-", "");
        }
    }

}
