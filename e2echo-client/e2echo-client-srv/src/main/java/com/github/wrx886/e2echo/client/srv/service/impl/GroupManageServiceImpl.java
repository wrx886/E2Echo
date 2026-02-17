package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKey;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.srv.model.vo.GroupKeyVo;
import com.github.wrx886.e2echo.client.srv.service.GroupKeyService;
import com.github.wrx886.e2echo.client.srv.service.GroupManageService;
import com.github.wrx886.e2echo.client.srv.service.GroupMemberService;
import com.github.wrx886.e2echo.client.srv.service.MessageService;
import com.github.wrx886.e2echo.client.srv.service.SessionService;
import com.github.wrx886.e2echo.client.srv.util.AesUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GroupManageServiceImpl implements GroupManageService {

    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final EccController eccController;
    private final MessageService messageService;
    private final GroupMemberService groupMemberService;
    private final GroupKeyService groupKeyService;
    private final GuiController guiController;

    /**
     * 创建群聊
     * 
     * @return 群聊UUID
     */
    @Override
    public void create() {
        String groupUuid = eccController.getPublicKey() + ":" + UUID.randomUUID().toString();
        sessionService.create(groupUuid, true);
        reflushKey(groupUuid); // 刷新群密钥
        groupMemberService.addMember(groupUuid, eccController.getPublicKey());
        messageService.subscribeGroup(groupUuid); // 订阅群聊
        guiController.flushAsync();
    }

    /**
     * 刷新群密钥
     * 
     * @param groupUuid 群聊UUID
     */
    @Override
    public void reflushKey(String groupUuid) {
        verifyGroupOwner(groupUuid);

        // 生成新的群密钥
        String aesKey = AesUtil.generateKeyAsHex();
        Long timestamp = System.currentTimeMillis();

        // 更新群密钥
        groupKeyService.put(groupUuid, timestamp, aesKey);

        // 分发密钥
        redistributeKey(groupUuid);
    }

    /**
     * 重新分发原有群密钥
     * 
     * @param groupUuid
     */
    @Override
    public void redistributeKey(String groupUuid) {
        verifyGroupOwner(groupUuid);

        // 获取现有密钥
        GroupKey groupKey = groupKeyService.getById(sessionService.getSession(groupUuid).getGroupKeyId());

        // 获取群成员
        List<String> members = groupMemberService.listMember(groupUuid);

        // 封装密钥
        GroupKeyVo groupKeyVo = new GroupKeyVo();
        groupKeyVo.setGroupUuid(groupKey.getGroupUuid());
        groupKeyVo.setTimestamp(Long.toString(groupKey.getTimestamp()));
        groupKeyVo.setAesKey(groupKey.getAesKey());
        String groupKeyVoString;
        try {
            groupKeyVoString = objectMapper.writeValueAsString(groupKeyVo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 将群密钥发送给群成员
        for (String member : members) {
            messageService.sendOne(member, groupKeyVoString, MessageType.GROUP_KEY_UPDATE);
        }

    }

    /**
     * 判断是否是群主
     * 
     * @param groupUuid 群聊UUID
     * @return 是否是群主
     */
    private void verifyGroupOwner(String groupUuid) {
        try {
            if (!eccController.getPublicKey().equals(groupUuid.split(":")[0])) {
                throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_GROUP_IS_NOT_GROUP_OWNER);
            }
        } catch (Exception e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_GROUP_IS_NOT_GROUP_OWNER);
        }
    }

    /**
     * 列出所有群聊
     * 
     * @return 群聊列表
     */
    @Override
    public List<String> listGroup() {
        return sessionService
                .listSession()
                .stream()
                .filter(Session::getGroup)
                .filter(session -> {
                    try {
                        if (!eccController.getPublicKey().equals(session.getPublicKeyHex().split(":")[0])) {
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                    return true;
                })
                .map(Session::getPublicKeyHex)
                .toList();
    }

    /**
     * 发送群密钥
     * 
     * @param groupUuid    群聊UUID
     * @param publicKeyHex 接收者公钥
     */
    @Override
    public void sendKey(String groupUuid, String publicKeyHex) {
        verifyGroupOwner(groupUuid);

        // 获取现有密钥
        GroupKey groupKey = groupKeyService.getById(sessionService.getSession(groupUuid).getGroupKeyId());

        // 封装密钥
        GroupKeyVo groupKeyVo = new GroupKeyVo();
        groupKeyVo.setGroupUuid(groupKey.getGroupUuid());
        groupKeyVo.setTimestamp(Long.toString(groupKey.getTimestamp()));
        groupKeyVo.setAesKey(groupKey.getAesKey());
        String groupKeyVoString;
        try {
            groupKeyVoString = objectMapper.writeValueAsString(groupKeyVo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 将群密钥发送给群成员
        messageService.sendOne(publicKeyHex, groupKeyVoString, MessageType.GROUP_KEY_UPDATE);
    }

}
