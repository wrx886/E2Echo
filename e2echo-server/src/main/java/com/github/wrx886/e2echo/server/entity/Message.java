package com.github.wrx886.e2echo.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息实体，对应数据库 {@code message} 表。
 *
 * <p>字段与 {@link com.github.wrx886.e2echo.ecc.EccMessage} 基本一致，并额外保存
 * {@code timestamp}（由消息 ID 前 16 位十六进制时间戳解析而来），用于按时间查询。</p>
 *
 * <p>主键与消息 ID 是两回事：继承 {@link BaseEntity} 得到的 {@code id} 是数据库主键，由服务端
 * 生成；{@code messageId} 是客户端生成的消息 ID，单独保存且全局唯一，查询与排序都以它为准。
 * 消息只追加、不修改，因此不需要实现 {@code Persistable}：主键生成前为空，保存时天然按新建处理。</p>
 *
 * <p>{@code from}、{@code to} 是数据库保留字，因此对应列名改为 {@code from_}、{@code to_}。</p>
 *
 * <p>为 {@code MessageService.list} 的过滤条件建立索引：{@code from_}、{@code to_}、
 * {@code channel}、{@code timestamp}；消息 ID 的唯一索引同时用于排序与增量拉取。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_message_from", columnList = "from_"),
        @Index(name = "idx_message_to", columnList = "to_"),
        @Index(name = "idx_message_channel", columnList = "channel"),
        @Index(name = "idx_message_timestamp", columnList = "timestamp")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_message_id", columnNames = "message_id")
})
public class Message extends BaseEntity {

    /**
     * 消息 ID，由客户端生成（“16 位十六进制时间戳 + 32 位无连字符 UUID”，共 48 位），
     * 全局唯一，同一消息重复提交会因唯一约束被拒绝。
     */
    @Column(name = "message_id", length = 48, nullable = false, updatable = false)
    private String messageId;

    /**
     * 发送者身份，即发送者的 secp256k1 公钥（RAW HEX 格式）。
     */
    @Column(name = "from_")
    private String from;

    /**
     * 接收者信息，具体含义由通道决定，如私聊时为接收者公钥、群聊时为群聊标识。
     */
    @Column(name = "to_")
    private String to;

    /**
     * 消息正文：加密方式由 {@code channel} 决定，可为密文或明文。
     */
    @Column(columnDefinition = "text")
    private String message;

    /**
     * 消息类型，由业务方自行定义。
     */
    private String type;

    /**
     * 消息通道（如私聊、群聊），由业务方自行定义。
     */
    private String channel;

    /**
     * 消息时间戳，保存为系统当前毫秒数（long），用于按时间查询。
     */
    @Column(nullable = false)
    private Long timestamp;

    /**
     * 消息附加信息，由业务方自行定义。
     */
    @Column(columnDefinition = "text")
    private String info;

    /**
     * 消息签名，基于不含签名的消息原文使用 SHA256withECDSA 计算。
     */
    private String sign;

}
