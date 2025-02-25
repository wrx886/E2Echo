package com.github.wrx886.e2echo.client.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.mapper.GroupUserMapper;
import com.github.wrx886.e2echo.client.model.entity.GroupUser;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.store.GuiStore;
import com.github.wrx886.e2echo.client.store.LoginUserStore;

@Service
public class GroupUserService extends ServiceImpl<GroupUserMapper, GroupUser> {

    @Autowired
    private UserService userService;

    @Autowired
    private GroupUserMapper groupUserMapper;

    @Autowired
    private GuiStore guiStore;

    @Autowired
    private LoginUserStore loginUserStore;

    // 根据群聊 ID 更新群成员列表
    public void updateGroupMenbers(Long groupId) {
        // 查询群成员
        List<User> members = groupUserMapper.listUserByGroupId(groupId);

        // 写入数据
        guiStore.getCurrentGroupMembers().clear();
        guiStore.getCurrentGroupMembers().addAll(members);
    }

    // 保存登入用户到群聊
    public void putToMember(Long groupId, String memberPublicKey) {
        // 查询当前用户信息
        User user = userService.putPersonByPublicKey(loginUserStore.getId(), memberPublicKey);
        // 查询关系是否存在
        boolean exist = count(new LambdaQueryWrapper<GroupUser>()
                .eq(GroupUser::getOwnerId, loginUserStore.getId())
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getMemberId, user.getId())) > 0;
        // 不存在则插入
        if (!exist) {
            GroupUser groupUser = new GroupUser();
            groupUser.setGroupId(groupId);
            groupUser.setMemberId(user.getId());
            groupUser.setOwnerId(loginUserStore.getId());
            save(groupUser);
        }
    }

    public void putLoginUserToMember(Long groupId) {
        putToMember(groupId, loginUserStore.getPublicKey());
    }
}
