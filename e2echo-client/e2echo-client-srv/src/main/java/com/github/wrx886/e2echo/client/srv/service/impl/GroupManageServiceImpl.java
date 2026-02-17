package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
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

    private final SessionService sessionService;
    private final EccController eccController;
    private final MessageService messageService;
    private final GroupMemberService groupMemberService;
    private final GroupKeyService groupKeyService;

    /**
     * 创建群聊
     * 
     * @return 群聊UUID
     */
    @Override
    public void create() {
        sessionService.create(eccController.getPublicKey() + ":" + UUID.randomUUID().toString(), true);
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

        // 获取群成员
        List<String> members = groupMemberService.listMember(groupUuid);

        // 将群密钥发送给群成员
        for (String member : members) {
            messageService.sendOne(member, aesKey, MessageType.GROUP_KEY_UPDATE);
        }

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
        String aesKey = groupKeyService.getById(sessionService.getSession(groupUuid).getGroupKeyId()).getAesKey();

        // 获取群成员
        List<String> members = groupMemberService.listMember(groupUuid);

        // 将群密钥发送给群成员
        for (String member : members) {
            messageService.sendOne(member, aesKey, MessageType.GROUP_KEY_UPDATE);
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

}
