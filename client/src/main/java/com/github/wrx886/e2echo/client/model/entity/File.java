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

    // 文件 AES KEY
    @TableField(value = "aes_key")
    private String aesKey;

    // 文件 SHA256
    @TableField(value = "sha256")
    private String sha256;

    // 文件网络路径
    @TableField(value = "url")
    private String url;

    // 文件本地路径
    @TableField(value = "path")
    private String path;

    // 文件名
    @TableField(value = "file_name")
    private String fileName;

}
