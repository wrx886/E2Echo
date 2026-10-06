package com.github.wrx886.e2echo.client.dto;

import com.github.wrx886.e2echo.client.entity.Conversation;
import com.github.wrx886.e2echo.client.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 会话数据传输对象。
 *
 * <p>在会话实体与接口之间传递数据：最新消息直接用实体带出，供会话列表展示。保存时
 * {@code id}、{@code peer}、{@code alias}、{@code group}、{@code enabled} 由调用方给出，
 * 其余字段由持久化层填充。</p>
 *
 * @param id            主键 ID，为空表示新建
 * @param owner         数据所有者，即登入用户的公钥
 * @param createTime    创建时间
 * @param updateTime    修改时间
 * @param peer          会话对方，私聊时为对方公钥、群聊时为群聊标识
 * @param alias         会话别名
 * @param group         是否群聊会话
 * @param enabled       会话是否启用
 * @param latestMessage 最新一条消息，还没有消息时为空
 * @param unread        未读消息数（保存时忽略，由收发消息与查看会话维护）
 */
public record ConversationDto(
        String id,
        String owner,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        @NotBlank String peer,
        @NotBlank String alias,
        @NotNull Boolean group,
        @NotNull Boolean enabled,
        Message latestMessage,
        Integer unread
) {

    /**
     * 由会话实体生成 DTO。
     *
     * @param conversation 会话实体
     * @return 会话 DTO
     */
    public static ConversationDto fromEntity(Conversation conversation) {
        return new ConversationDto(
                conversation.getId(),
                conversation.getOwner(),
                conversation.getCreateTime(),
                conversation.getUpdateTime(),
                conversation.getPeer(),
                conversation.getAlias(),
                conversation.getGroup(),
                conversation.getEnabled(),
                conversation.getLatestMessage(),
                conversation.getUnread()
        );
    }

}
