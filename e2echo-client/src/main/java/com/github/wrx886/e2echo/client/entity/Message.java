package com.github.wrx886.e2echo.client.entity;

import com.github.wrx886.e2echo.ecc.EccMessage;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Persistable;

/**
 * 消息实体，对应数据库 {@code message} 表。
 *
 * <p>保存本地解密后的消息，字段与 {@link EccMessage} 基本一致，区别有两点：{@code message}
 * 存的是解密后的明文（服务端存的是密文）；不保存 {@code sign}——签名针对的是原始报文，密钥与
 * 正文都不同了，留着也无法再验签。主键沿用消息 ID（“16 位十六进制时间戳 + 32 位无连字符
 * UUID”）。</p>
 *
 * <p>消息只插入、不更新（见 {@link #isNew()}），所以重复拉取同一条消息时不会覆盖已有数据，而是
 * 因主键冲突失败，拉取方需要先判断该消息是否已经存在。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_message_owner_from_to_seq", columnList = "owner, from_, to_, seq"),
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_owner_seq", columnNames = {"owner", "seq"})
})
public class Message extends BaseEntity implements Persistable<String> {

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
     * 全局序号，由客户端赋值、与会话及用户都无关，用户内唯一，用于本地排序与增量拉取。
     */
    @Column(nullable = false)
    private Long seq;

    /**
     * 消息正文，保存解密后的明文。
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
     * 消息附加信息，由业务方自行定义。
     */
    @Column(columnDefinition = "text")
    private String info;

    /**
     * 判断实体是否为新建，恒为 {@code true}。
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
