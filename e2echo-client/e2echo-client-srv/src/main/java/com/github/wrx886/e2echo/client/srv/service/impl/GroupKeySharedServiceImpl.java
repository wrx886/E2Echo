package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKey;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKeyShared;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.srv.mapper.GroupKeySharedMapper;
import com.github.wrx886.e2echo.client.srv.model.vo.GroupKeyVo;
import com.github.wrx886.e2echo.client.srv.service.GroupKeyService;
import com.github.wrx886.e2echo.client.srv.service.GroupKeySharedService;
import com.github.wrx886.e2echo.client.srv.service.MessageService;
import com.github.wrx886.e2echo.client.srv.service.SessionService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GroupKeySharedServiceImpl extends ServiceImpl<GroupKeySharedMapper, GroupKeyShared>
        implements GroupKeySharedService {

    private final ObjectMapper objectMapper;
    private final GroupKeyService groupKeyService;
    private final EccController eccController;
    private final SessionService sessionService;
    private final MessageService messageService;
    private final GuiController guiController;

    /**
     * 添加
     * 
     * @param groupUuid 群聊UUID
     * @param from      分享者公钥
     * @param to        接收者公钥
     */
    @Override
    public void add(String groupUuid, String from, String to) {
        GroupKeyShared groupKeyShared = new GroupKeyShared();
        groupKeyShared.setOwnerPublicKeyHex(eccController.getPublicKey());
        groupKeyShared.setGroupUuid(groupUuid);
        groupKeyShared.setFrom(from);
        groupKeyShared.setTo(to);

        try {
            this.save(groupKeyShared);
        } catch (DuplicateKeyException e) {
            log.error("", e);
        }

        // 执行一次规则
        if (eccController.getPublicKey().equals(from)) {
            sharedTo(groupUuid, to);
        }

        // 刷新
        guiController.flushAsync();
    }

    /**
     * 删除
     * 
     * @param groupUuid 群聊UUID
     * @param from      分享者公钥
     * @param to        接收者公钥
     */
    @Override
    public void remove(String groupUuid, String from, String to) {
        remove(new LambdaQueryWrapper<GroupKeyShared>()
                .eq(GroupKeyShared::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupKeyShared::getGroupUuid, groupUuid)
                .eq(GroupKeyShared::getFrom, from)
                .eq(GroupKeyShared::getTo, to));
        guiController.flushAsync();
    }

    /**
     * 获取群密钥共享列表
     * 
     * @return 群密钥共享列表
     */
    @Override
    public List<GroupKeyShared> listGroupKeyShared() {
        return list(new LambdaQueryWrapper<GroupKeyShared>()
                .eq(GroupKeyShared::getOwnerPublicKeyHex, eccController.getPublicKey()));
    }

    /**
     * 群密钥更新监听
     * 
     * @param groupUuid 群聊UUID
     */
    @Override
    public void groupKeyUpdateListener(String groupUuid) {
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

        // 查询需要发送的共享者
        List<GroupKeyShared> groupKeyShareds = list(new LambdaQueryWrapper<GroupKeyShared>()
                .eq(GroupKeyShared::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupKeyShared::getGroupUuid, groupUuid)
                .eq(GroupKeyShared::getFrom, eccController.getPublicKey()));

        // 将群密钥发送给群成员
        for (GroupKeyShared groupKeyShared : groupKeyShareds) {
            messageService.sendOne(groupKeyShared.getTo(), groupKeyVoString, MessageType.GROUP_KEY_SHARED);
        }

    }

    /**
     * 规则是否存在
     * 
     * @param groupUuid 群聊UUID
     * @param from      发送者公钥
     * @param to        接收者公钥
     * @return true：存在，false：不存在
     */
    @Override
    public boolean contains(String groupUuid, String from, String to) {
        return count(new LambdaQueryWrapper<GroupKeyShared>()
                .eq(GroupKeyShared::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupKeyShared::getGroupUuid, groupUuid)
                .eq(GroupKeyShared::getFrom, from)
                .eq(GroupKeyShared::getTo, to)) > 0;
    }

    private void sharedTo(String groupUuid, String to) {
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

        // 发送
        messageService.sendOne(to, groupKeyVoString, MessageType.GROUP_KEY_SHARED);

    }

    /**
     * 重新分发一次所有规则（仅作为共享者）
     */
    @Override
    public void resendAll() {
        listGroupKeyShared().forEach((groupKeyShared) -> {
            if (eccController.getPublicKey().equals(groupKeyShared.getFrom())) {
                sharedTo(groupKeyShared.getGroupUuid(), groupKeyShared.getTo());
            }
        });
    }

}
