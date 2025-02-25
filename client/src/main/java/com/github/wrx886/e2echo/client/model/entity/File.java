package com.github.wrx886.e2echo.client.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

// 文件实体
@Data
@TableName("files")
public class File {

    // 自增主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 所属登入用户
    @TableField(value = "owner_id")
    private Long ownerId;

    // 对应的消息id
    @TableField(value = "message_id")
    private Long messageId;

    // 文件本地路径
    @TableField(value = "path")
    private String path;

}
