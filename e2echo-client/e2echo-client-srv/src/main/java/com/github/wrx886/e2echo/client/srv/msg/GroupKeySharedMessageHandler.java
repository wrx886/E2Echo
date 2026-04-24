package com.github.wrx886.e2echo.client.srv.msg;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.srv.model.vo.GroupKeyVo;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;
import com.github.wrx886.e2echo.client.srv.service.GroupKeyService;
import com.github.wrx886.e2echo.client.srv.service.GroupKeySharedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupKeySharedMessageHandler extends BaseMessageHandler {

    private final ObjectMapper objectMapper;
    private final EccController eccController;
    private final GroupKeyService groupKeyService;

    // 注册
    static {
        personMessageHandlerMap.put(MessageType.GROUP_KEY_SHARED, GroupKeySharedMessageHandler.class);
    }

    @Override
    public void receiveHandler(EccMessage eccMessage, SendMessageVo sendMessageVo) {
        // 消息类型必须是群聊密钥更新
        if (!MessageType.GROUP_KEY_SHARED.equals(sendMessageVo.getType())) {
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

        // 检测规则是否存在
        GroupKeySharedService groupKeySharedService = BeanProvider.getBean(GroupKeySharedService.class);
        if (!groupKeySharedService.contains(groupKeyVo.getGroupUuid(), eccMessage.getFromPublicKeyHex(),
                eccController.getPublicKey())) {
            return;
        }

        // 更新密钥
        groupKeyService.put(
                groupKeyVo.getGroupUuid(),
                Long.valueOf(groupKeyVo.getTimestamp()),
                groupKeyVo.getAesKey());
    }

}
