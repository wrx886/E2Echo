package com.github.wrx886.e2echo.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 系统参数实体，对应数据库 {@code sys_param} 表。
 *
 * <p>保存按登入用户隔离的键值参数：继承 {@link BaseEntity} 因此带有 {@code owner}（登入用户的
 * 公钥），主键是基类的 {@code id}；{@code key} 是参数名、在同一用户下唯一，参数名与参数值都不可
 * 为空。</p>
 */
@Data
@Entity
@Table(name = "sys_param",
        uniqueConstraints = @UniqueConstraint(name = "uk_sys_param_owner_key",
                columnNames = {"owner", "key_"}))
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SysParam extends BaseEntity {

    /**
     * 参数名，同一用户下唯一。
     */
    @Column(name = "key_", length = 64, nullable = false, updatable = false)
    private String key;

    /**
     * 参数值，不可为空。
     */
    @Column(name = "value_", columnDefinition = "text", nullable = false)
    private String value;

}
