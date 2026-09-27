package com.github.wrx886.e2echo.client.handler;

import com.github.wrx886.e2echo.client.common.MessageHandler;
import com.github.wrx886.e2echo.client.entity.Message;
import com.github.wrx886.e2echo.client.enums.ChannelEnum;
import com.github.wrx886.e2echo.client.enums.MessageTypeEnum;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.vo.ChatTextMessageVo;
import com.github.wrx886.e2echo.ecc.Ecc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 文字聊天消息处理器。
 *
 * <p>负责文字聊天消息的收发：发送时把正文序列化成 {@link ChatTextMessageVo} 的 JSON 交给业务层，
 * 接收时不需要额外处理（正文入库即可）。</p>
 */
@Component
@RequiredArgsConstructor
public class ChatTextMessageHandler implements MessageHandler {

    /**
     * 消息业务逻辑对象。
     */
    private final MessageService messageService;

    /**
     * JSON 对象映射器，用于消息正文的序列化。
     */
    private final ObjectMapper objectMapper;

    /**
     * 接收消息：文字聊天消息入库即可，不需要额外处理。
     *
     * @param message 收到的消息
     */
    @Override
    public void receive(Message message) {
    }

    /**
     * 消息正文的类型。
     *
     * @return {@link ChatTextMessageVo}
     */
    @Override
    public Class<ChatTextMessageVo> getMessageType() {
        return ChatTextMessageVo.class;
    }

    /**
     * 发送文字聊天消息。
     *
     * @param to    接收者，私聊时为对方公钥、群聊时为群聊标识
     * @param group 是否群聊
     * @param text  消息正文
     */
    public void send(String to, boolean group, String text) {
        Message message = new Message();
        message.setFrom(Ecc.getPublicKey());
        message.setTo(to);
        message.setMessage(objectMapper.writeValueAsString(new ChatTextMessageVo(text)));
        message.setType(MessageTypeEnum.CHAT_TEXT.name());
        message.setChannel(group ? ChannelEnum.CHAT_GROUP_AES.name() : ChannelEnum.CHAT_PRIVATE_ECC.name());
        // info 不可为空：服务端会校验，空白值会被直接拒绝
        message.setInfo("{}");
        // 私聊要先存本地：私聊拉取只按“接收者是自己”过滤，自己发的消息不存就永远补不回来；
        // 群聊不存，等服务端通知后连自己发的消息一起拉回来
        messageService.send(message, !group);
    }

}
