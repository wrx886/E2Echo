package com.github.wrx886.e2echo.client.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.mapper.UserMapper;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.model.enums.UserType;
import com.github.wrx886.e2echo.client.store.GuiStore;
import com.github.wrx886.e2echo.client.store.LoginUserStore;

// 用户服务
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private LoginUserStore loginUserStore;

    @Autowired
    private GuiStore guiStore;

    // 根据公钥获取（不存在则插入并返回）一个用户
    public User putPersonByPublicKey(Long ownerId, String publicKey) {
        // 查询用户
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getOwnerId, ownerId)
                .eq(User::getPublicKey, publicKey)
                .eq(User::getType, UserType.PERSON));
        if (user == null) {
            // 注册用户
            user = new User();
            user.setName("pserson-" + publicKey.substring(0, 16));
            user.setOwnerId(ownerId);
            user.setPublicKey(publicKey);
            user.setType(UserType.PERSON);
            save(user);
        }
        return user;
    }

    // 根据群聊 ID 获取（不存在则插入并返回）一个用户
    public User putGroupByGroupUuid(Long ownerId, String groupUuid, String publicKey) {
        // 查询用户
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getOwnerId, ownerId)
                .eq(User::getGroupUuid, groupUuid)
                .eq(User::getType, UserType.GROUP));
        if (user == null) {
            // 注册用户
            user = new User();
            user.setName("group-" + groupUuid.substring(0, 16));
            user.setOwnerId(ownerId);
            user.setPublicKey(publicKey);
            user.setType(UserType.GROUP);
            user.setGroupUuid(groupUuid);
            save(user);
        }
        return user;
    }

    // 更新群聊列表
    public void updateGroupList() {
        // 查询群聊列表
        List<User> groups = list(new LambdaQueryWrapper<User>()
                .eq(User::getOwnerId, loginUserStore.getId())
                .eq(User::getPublicKey, loginUserStore.getPublicKey())
                .eq(User::getType, UserType.GROUP));
        // 放入
        guiStore.getGroups().clear();
        guiStore.getGroups().addAll(groups);
    }

}
