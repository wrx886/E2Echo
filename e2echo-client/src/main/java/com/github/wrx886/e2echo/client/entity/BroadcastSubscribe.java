package com.github.wrx886.e2echo.client.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 广播订阅实体，对应数据库 {@code broadcast_subscribe} 表。
 *
 * <p>记录当前用户订阅了谁：订阅关系按登入用户隔离，同一用户对同一个被订阅者只允许一条记录（唯一
 * 约束）。继承 {@link BaseEntity} 获得主键与审计字段。</p>
 *
 * <p>订阅只存在本地库：服务端并不知道谁订阅了谁，拉取订阅流时由调用方把订阅的作者列表当作
 * {@code from} 过滤条件传下去。</p>
 */
@Entity
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "broadcast_subscribe", indexes = {
        @Index(name = "idx_broadcast_subscribe_owner_public_key", columnList = "owner, public_key")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_broadcast_subscribe_owner_public_key", columnNames = {"owner", "public_key"})
})
public class BroadcastSubscribe extends BaseEntity {

    /**
     * 被订阅者的公钥。
     */
    @Column(nullable = false)
    private String publicKey;

    /**
     * 被订阅者的别名，只在本机展示用，可以为空。
     */
    private String alias;

}
