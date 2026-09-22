package com.github.wrx886.e2echo.client.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 会话实体，对应数据库 {@code conversation} 表。
 *
 * <p>记录当前用户的会话：会话对方、别名与最新一条消息。继承 {@link BaseEntity} 获得主键与审计字段，
 * 数据按登入用户隔离；同一用户的同一个会话对方只允许一条记录（唯一约束）。会话对方、别名、是否群聊
 * 都不可为空。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "conversation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conversation_owner_peer", columnNames = {"owner", "peer"})
})
public class Conversation extends BaseEntity {

    /**
     * 会话对方，私聊时为对方公钥、群聊时为群聊标识。
     */
    @Column(nullable = false)
    private String peer;

    /**
     * 会话别名，群聊时同时作为成员别名。
     */
    @Column(nullable = false)
    private String alias;

    /**
     * 是否群聊会话：{@code true} 群聊、{@code false} 私聊。
     */
    @Column(name = "group_", nullable = false)
    private Boolean group;

    /**
     * 会话是否启用。
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * 最新一条消息，用于会话列表展示；会话还没有消息时为空。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Message latestMessage;

}
