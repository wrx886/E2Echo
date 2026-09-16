package com.github.wrx886.e2echo.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Persistable;

/**
 * 消息实体，对应数据库 {@code message} 表。
 *
 * <p>字段与 {@link com.github.wrx886.e2echo.ecc.EccMessage} 基本一致，并额外保存
 * {@code timestamp}（由消息 ID 前 16 位十六进制时间戳解析而来），用于按时间查询。
 * 继承 {@link BaseEntity} 获得主键与审计字段，其中主键沿用 EccMessage 的
 * “16 位十六进制时间戳 + 32 位无连字符 UUID”形式。</p>
 *
 * <p>{@code from}、{@code to} 是数据库保留字，因此对应列名使用双引号包裹。</p>
 *
 * <p>为 {@code MessageService.list} 的过滤条件建立索引：{@code from}、{@code to}、
 * {@code channel}、{@code timestamp}；主键 {@code id} 已有主键索引，并用于排序。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_message_from", columnList = "\"from\""),
        @Index(name = "idx_message_to", columnList = "\"to\""),
        @Index(name = "idx_message_channel", columnList = "channel"),
        @Index(name = "idx_message_timestamp", columnList = "timestamp")
})
public class Message extends BaseEntity implements Persistable<String> {

    /**
     * 发送者身份，即发送者的 secp256k1 公钥（RAW HEX 格式）。
     */
    @Column(name = "\"from\"")
    private String from;

    /**
     * 接收者信息，具体含义由通道决定，如私聊时为接收者公钥、群聊时为群聊标识。
     */
    @Column(name = "\"to\"")
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


    /**
     * 判断实体是否为新建，恒为 {@code true}。
     *
     * <p>消息 ID 由客户端生成，保存前主键已非空，按默认规则会被当作已存在的实体，
     * 从而走 merge 分支（先查询再更新）。消息只追加、不作修改，这里恒返回
     * {@code true}，使 {@code save} 始终执行 persist 插入，既省去一次主键查询，
     * 也避免误更新已有消息。</p>
     *
     * @return 恒为 {@code true}，表示实体始终按新建处理
     * @see org.springframework.data.domain.Persistable#isNew()
     */
    @Override
    public boolean isNew() {
        // 仅插入
        return true;
    }

}
