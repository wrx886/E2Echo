package com.github.wrx886.e2echo.client.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wrx886.e2echo.client.model.entity.GroupUser;
import com.github.wrx886.e2echo.client.model.entity.User;

public interface GroupUserMapper extends BaseMapper<GroupUser> {

    // 根据群聊 ID 查询所有群成员
    List<User> listUserByGroupId(Long groupId);

}
