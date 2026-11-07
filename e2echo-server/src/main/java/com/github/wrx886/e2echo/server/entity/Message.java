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
 * <p>字段与 {@link com.github.wrx886.e2echo.ecc.EccMessage} 基本一致。消息 ID 的前 16 位本身就是
 * 十六进制毫秒时间戳，因此不再单独保存时间字段；按时间区间查询时由服务层转换为消息 ID 的范围。</p>
 *
 * <p>主键与消息 ID 是两回事：继承 {@link BaseEntity} 得到的 {@code id} 是数据库主键，由服务端
 * 生成；{@code messageId} 是客户端生成的消息 ID，单独保存且全局唯一，查询与排序都以它为准。
 * 消息只追加、不修改，因此不需要实现 {@code Persistable}：主键生成前为空，保存时天然按新建处理。</p>
 *
 * <p>各字段都不可为空，对应列均为非空约束，保存前由 {@code MessageService} 统一校验。</p>
 *
 * <p>{@code from}、{@code to} 是数据库保留字，因此对应列名改为 {@code from_}、{@code to_}。</p>
 *
 * <p>列表查询必须带 {@code channel}，{@code from_}、{@code to_} 可选，并始终按 {@code message_id}
 * 过滤游标或排序，因此为四种组合建立索引：{@code channel}、{@code channel+from_}、
 * {@code channel+to_}、{@code channel+from_+to_}，消息 ID 均放在最后。消息 ID 的唯一约束保证其
 * 全局唯一，并支持不带通道的按 ID 查询。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_message_channel_message_id", columnList = "channel, message_id"),
        @Index(name = "idx_message_channel_from_message_id", columnList = "channel, from_, message_id"),
        @Index(name = "idx_message_channel_to_message_id", columnList = "channel, to_, message_id"),
        @Index(name = "idx_message_channel_from_to_message_id", columnList = "channel, from_, to_, message_id"),
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
    @Column(name = "from_", nullable = false)
    private String from;

    /**
     * 接收者信息，具体含义由通道决定，如私聊时为接收者公钥、群聊时为群聊标识。
     */
    @Column(name = "to_", nullable = false)
    private String to;

    /**
     * 消息正文：加密方式由 {@code channel} 决定，可为密文或明文。
     */
    @Column(columnDefinition = "text", nullable = false)
    private String message;

    /**
     * 消息类型，由业务方自行定义。
     */
    @Column(nullable = false)
    private String type;

    /**
     * 消息通道（如私聊、群聊），由业务方自行定义。
     */
    @Column(nullable = false)
    private String channel;

    /**
     * 消息附加信息，由业务方自行定义。
     */
    @Column(columnDefinition = "text", nullable = false)
    private String info;

    /**
     * 消息签名，基于不含签名的消息原文使用 SHA256withECDSA 计算。
     */
    @Column(nullable = false)
    private String sign;

}
