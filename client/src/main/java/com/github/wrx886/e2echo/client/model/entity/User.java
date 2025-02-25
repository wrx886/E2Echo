package com.github.wrx886.e2echo.client.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wrx886.e2echo.client.model.enums.UserType;

import lombok.Data;

// 用户实体
@Data
@TableName("users")
public class User {

    // 自增主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 所属登入用户
    @TableField(value = "owner_id")
    private Long ownerId;

    // 用户名
    @TableField(value = "name")
    private String name;

    // 用户类型（个人/群聊）
    @TableField(value = "type")
    private UserType type;

    // 公钥（如果是群聊，则为群主公钥）
    @TableField(value = "public_key")
    private String publicKey;

    // 群聊 uuid
    @TableField(value = "group_uuid")
    private String groupUuid;

}
