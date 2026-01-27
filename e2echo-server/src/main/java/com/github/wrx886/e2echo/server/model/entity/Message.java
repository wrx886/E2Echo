package com.github.wrx886.e2echo.server.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("message")
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseEntity {

    @TableField(value = "uuid")
    @Schema(description = "UUID")
    private String uuid;

    @TableField(value = "timestamp")
    @Schema(description = "时间戳(int64)，表示自1970年1月1日00:00:00 UTC以来的毫秒数")
    private Date timestamp;

    @TableField(value = "from_public_key_hex")
    @Schema(description = "发送者公钥")
    private String fromPublicKeyHex;

    @TableField(value = "to_public_key_hex")
    @Schema(description = "接收者公钥")
    private String toPublicKeyHex;

    @TableField(value = "data")
    @Schema(description = "明文或密文")
    private String data;

    @TableField(value = "signature")
    @Schema(description = "签名")
    private String signature;

    // 如果为群聊消息，toPublicKeyHex 改为群聊的 UUID
    @TableField(value = "group_")
    @Schema(description = "是否是群聊消息")
    private Boolean group;

}
