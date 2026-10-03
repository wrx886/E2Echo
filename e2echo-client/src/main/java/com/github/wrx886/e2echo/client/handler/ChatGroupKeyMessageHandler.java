package com.github.wrx886.e2echo.client.handler;

import com.github.wrx886.e2echo.client.common.MessageHandler;
import com.github.wrx886.e2echo.client.dto.AesKeyDto;
import com.github.wrx886.e2echo.client.dto.GroupMemberDto;
import com.github.wrx886.e2echo.client.entity.AesKey;
import com.github.wrx886.e2echo.client.entity.Message;
import com.github.wrx886.e2echo.client.enums.ChannelEnum;
import com.github.wrx886.e2echo.client.enums.MessageTypeEnum;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.service.AesKeyService;
import com.github.wrx886.e2echo.client.service.ConversationService;
import com.github.wrx886.e2echo.client.service.GroupMemberService;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.vo.message.ChatGroupKeyMessageVo;
import com.github.wrx886.e2echo.ecc.Ecc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * 群聊密钥消息处理器。
 *
 * <p>群密钥通过私聊消息分发：正文是 {@link ChatGroupKeyMessageVo} 的 JSON。发送由群主发起，接收时
 * 会校验来源确实是群主（群标识以群主公钥开头），通过后把密钥写入本地，之后收发群聊消息就能用它
 * 加解密；同时确认群聊会话存在，保证这个群会被拉取和订阅。</p>
 */
@Component
@RequiredArgsConstructor
public class ChatGroupKeyMessageHandler implements MessageHandler {

    /**
     * 会话业务对象，用于收到群密钥后确认群聊会话存在。
     */
    private final ConversationService conversationService;

    /**
     * 群成员业务对象，用于取密钥分发的目标成员。
     */
    private final GroupMemberService groupMemberService;

    /**
     * AES 密钥业务对象，用于写入收到的密钥、取要分发的密钥。
     */
    private final AesKeyService aesKeyService;

    /**
     * 消息业务对象，用于把密钥消息发给成员。
     */
    private final MessageService messageService;

    /**
     * JSON 对象映射器，用于密钥消息正文的序列化与反序列化。
     */
    private final ObjectMapper objectMapper;

    /**
     * 接收群密钥消息：校验发送者是群主后，把密钥写入本地，并确认对应的群聊会话存在。
     *
     * <p>确认会话存在这步不能省：群聊只有在本地有会话记录（且启用）时才会被拉取与订阅，否则密钥
     * 到手了也收不到群里的消息。</p>
     *
     * @param message 收到的消息，正文是 {@link ChatGroupKeyMessageVo}
     * @throws E2EchoException 不是私聊通道，或来源不是群主
     */
    @Override
    public void receive(Message message) {
        // 必须以私聊渠道分发
        if (!message.getChannel().equals(ChannelEnum.CHAT_PRIVATE_ECC.name())) {
            throw new E2EchoException("群聊密钥必须以私聊渠道分发");
        }

        // 读取密钥
        ChatGroupKeyMessageVo chatGroupKeyMessageVo = objectMapper.readValue(message.getMessage(), ChatGroupKeyMessageVo.class);

        // 验证群主身份
        if (!Objects.equals(
                message.getFrom().substring(0, currentOwner().length()),
                chatGroupKeyMessageVo.group().substring(0, currentOwner().length())
        )) {
            throw new E2EchoException("群聊密钥来源非群主");
        }

        // 写入消息密钥
        AesKey aesKey = new AesKey();
        aesKey.setObj(chatGroupKeyMessageVo.group());
        aesKey.setPublishTime(chatGroupKeyMessageVo.publishTime());
        aesKey.setAesKey(chatGroupKeyMessageVo.aesKey());
        aesKeyService.save(aesKey);

        // 确定群聊会话存在
        conversationService.ensureExist(chatGroupKeyMessageVo.group(), true);
    }

    /**
     * 消息正文的类型。
     *
     * @return {@link ChatGroupKeyMessageVo}
     */
    @Override
    public Class<?> getMessageType() {
        return ChatGroupKeyMessageVo.class;
    }

    /**
     * 把最新的群密钥分发给群里的所有成员。
     *
     * @param group 群标识
     * @throws E2EchoException 自己不是群主，或该群还没有密钥
     */
    public void send(String group) {
        // 判断是否为群主
        if (!currentOwner().equals(group.substring(0, currentOwner().length()))) {
            throw new E2EchoException("非群主，禁止管理！");
        }

        List<GroupMemberDto> groupMembers = groupMemberService.listByGroup(group);
        AesKeyDto last = aesKeyService.getLast(group);
        if (last == null) throw new E2EchoException("群聊不存在密钥！");
        for (GroupMemberDto groupMember : groupMembers) {
            Message message = new Message();
            message.setFrom(Ecc.getPublicKey());
            message.setTo(groupMember.member());
            message.setMessage(objectMapper.writeValueAsString(new ChatGroupKeyMessageVo(last.obj(), last.publishTime(), last.aesKey())));
            message.setType(MessageTypeEnum.CHAT_GROUP_KEY.name());
            message.setChannel(ChannelEnum.CHAT_PRIVATE_ECC.name());
            message.setInfo("{}");
            messageService.send(message, true);
        }
    }

    /**
     * 把最新的群密钥单独发给一个成员，用于新成员加入。
     *
     * @param group  群标识
     * @param member 成员的公钥
     * @throws E2EchoException 该群还没有密钥
     */
    public void send(String group, String member) {
        // 获取群聊密钥
        AesKeyDto last = aesKeyService.getLast(group);
        if (last == null) throw new E2EchoException("群聊不存在密钥！");

        // 发送
        Message message = new Message();
        message.setFrom(Ecc.getPublicKey());
        message.setTo(member);
        message.setMessage(objectMapper.writeValueAsString(new ChatGroupKeyMessageVo(last.obj(), last.publishTime(), last.aesKey())));
        message.setType(MessageTypeEnum.CHAT_GROUP_KEY.name());
        message.setChannel(ChannelEnum.CHAT_PRIVATE_ECC.name());
        message.setInfo("{}");
        messageService.send(message, true);
    }

}
