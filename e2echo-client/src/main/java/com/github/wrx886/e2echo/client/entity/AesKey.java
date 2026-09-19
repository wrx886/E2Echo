package com.github.wrx886.e2echo.client.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Persistable;

/**
 * AES 密钥实体，对应数据库 {@code aes_key} 表。
 *
 * <p>保存群聊消息加解密用的 AES 密钥：每个加密对象（如群聊标识）可以有多个密钥，按签发时间区分，
 * 解密时取发送时间附近的密钥。继承 {@link BaseEntity} 获得主键与审计字段，数据按登入用户隔离；
 * 同一用户、同一对象、同一签发时间只允许一条记录（唯一约束）。</p>
 */
@Entity
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "aes_key", indexes = {
        @Index(name = "idx_aes_key_owner_obj_publish_time", columnList = "owner, obj, publish_time")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_aes_key_owner_obj_publish_time", columnNames = {"owner", "obj", "publish_time"})
})
public class AesKey extends BaseEntity implements Persistable<String> {

    /**
     * 加密对象，如群聊标识。
     */
    @Column(nullable = false)
    private String obj;

    /**
     * 密钥签发时间（毫秒）。
     */
    @Column(nullable = false)
    private long publishTime;

    /**
     * AES 密钥（HEX 格式）。
     */
    @Column(nullable = false)
    private String aesKey;

    /**
     * 判断实体是否为新建，恒为 {@code true}。
     *
     * <p>密钥只追加、不修改，恒按新建处理，使保存始终执行插入。</p>
     *
     * @return 恒为 {@code true}
     */
    @Override
    public boolean isNew() {
        return true;
    }
}
