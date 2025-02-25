package com.github.wrx886.e2echo.client.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

// 登入用户
@Data
@TableName("login_users")
public class LoginUser {

    // 自增主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 公钥
    @TableField(value = "public_key")
    private String publicKey;

}
