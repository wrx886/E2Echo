package com.github.wrx886.e2echo.client.srv.msg;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.srv.model.vo.GroupKeyVo;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;
import com.github.wrx886.e2echo.client.srv.service.GroupKeyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupKeyUpdateMessageHandler extends BaseMessageHandler<GroupKeyVo> {

    // 注册
    static {
        personMessageHandlerMap.put(MessageType.GROUP_KEY_UPDATE, GroupKeyUpdateMessageHandler.class);
    }

    private final ObjectMapper objectMapper;
    private final GroupKeyService groupKeyService;

    @Override
    public void receiveHandler(EccMessage eccMessage, SendMessageVo sendMessageVo) {
        // 消息类型必须是群聊密钥更新
        if (!MessageType.GROUP_KEY_UPDATE.equals(sendMessageVo.getType())) {
            throw new RuntimeException("消息类型错误");
        }

        // 提取消息
        GroupKeyVo groupKeyVo;
        try {
            groupKeyVo = objectMapper.readValue(sendMessageVo.getData(), GroupKeyVo.class);
        } catch (Exception e) {
            log.error("", e);
            return;
        }

        // 提取群主公钥
        String groupOwnerPublicKeyHex;
        try {
            groupOwnerPublicKeyHex = groupKeyVo.getGroupUuid().split(":")[0];
        } catch (Exception e) {
            log.error("", e);
            return;
        }

        // 群主不匹配
        if (!eccMessage.getFromPublicKeyHex().equals(groupOwnerPublicKeyHex)) {
            // 忽略
            return;
        }

        // 更新密钥
        groupKeyService.put(
                groupKeyVo.getGroupUuid(),
                Long.valueOf(groupKeyVo.getTimestamp()),
                groupKeyVo.getAesKey());
    }

}
