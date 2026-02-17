package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.entity.GroupMember;
import com.github.wrx886.e2echo.client.srv.mapper.GroupMemberMapper;
import com.github.wrx886.e2echo.client.srv.service.GroupManageService;
import com.github.wrx886.e2echo.client.srv.service.GroupMemberService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements GroupMemberService {

    private final EccController eccController;
    private final GuiController guiController;

    /**
     * 获取群成员
     * 
     * @param groupUuid 群聊UUID
     * @return 群成员的公钥列表
     */
    @Override
    public List<String> listMember(String groupUuid) {
        verifyGroupOwner(groupUuid);
        return list(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupMember::getGroupUuid, groupUuid))
                .stream()
                .map(groupMember -> groupMember.getPublicKeyHex())
                .toList();
    }

    @Override
    public void addMember(String groupUuid, String publicKeyHex) {
        verifyGroupOwner(groupUuid);
        // 查询群成员是否存在
        long count = count(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupMember::getGroupUuid, groupUuid)
                .eq(GroupMember::getPublicKeyHex, publicKeyHex));
        if (count > 0) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_GROUP_MEMBER_EXIST);
        }

        // 添加群成员
        GroupMember groupMember = new GroupMember();
        groupMember.setOwnerPublicKeyHex(eccController.getPublicKey());
        groupMember.setGroupUuid(groupUuid);
        groupMember.setPublicKeyHex(publicKeyHex);
        this.save(groupMember);

        // 定向发送密钥
        GroupManageService groupManageService = BeanProvider.getBean(GroupManageService.class);
        groupManageService.sendKey(groupUuid, publicKeyHex);

        // 刷新主界面
        guiController.flushAsync();
    }

    @Override
    public void removeMember(String groupUuid, String publicKeyHex) {
        verifyGroupOwner(groupUuid);
        // 查询群成员是否存在
        GroupMember groupMember = getOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupMember::getGroupUuid, groupUuid)
                .eq(GroupMember::getPublicKeyHex, publicKeyHex));
        if (groupMember == null) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_GROUP_MEMBER_NOT_EXIST);
        }

        // 不能删除群主
        String groupOwner = groupUuid.split(":")[0];
        if (groupOwner.equals(publicKeyHex)) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_GROUP_MEMBER_DELETE_OWNER_NOT_ALLOWED);
        }

        // 删除群成员
        removeById(groupMember.getId());

        // 刷新密钥
        GroupManageService groupManageService = BeanProvider.getBean(GroupManageService.class);
        groupManageService.reflushKey(groupUuid);

        // 刷新主界面
        guiController.flushAsync();
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
