package com.github.wrx886.e2echo.client.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

// 群聊实体
@Data
@TableName("group_users")
public class GroupUser {

    // 自增主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 所属登入用户
    @TableField(value = "owner_id")
    private Long ownerId;

    // 群聊 id
    @TableField(value = "group_id")
    private Long groupId;

    // 成员 id
    @TableField(value = "member_id")
    private Long memberId;

}
