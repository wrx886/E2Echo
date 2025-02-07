package com.github.wrx886.e2echo.server.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wrx886.e2echo.server.model.enums.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息实体")
@TableName(value = "message")
public class Message extends BaseEntity {

    @Schema(description = "发送者公钥")
    @TableField(value = "from_public_key")
    private String fromPublicKey;

    @Schema(description = "接收者公钥")
    @TableField(value = "to_public_key")
    private String toPublicKey;

    @Schema(description = "发送者用户id（根据公钥自动填充）")
    @TableField(value = "from_user_id")
    private Long fromUserId;

    @Schema(description = "接收者用户id（根据公钥自动填充）")
    @TableField(value = "to_user_id")
    private Long toUserId;

    @Schema(description = "数据的签名（HEX 格式）")
    @TableField(value = "sign")
    private String sign;

    @Schema(description = "数据（使用接收者的公钥加密，HEX 格式）")
    @TableField(value = "data")
    private String data;

    @Schema(description = "发送时间")
    @TableField(value = "send_time")
    private Date sendTime;

    @Schema(description = "消息类型（1：用户消息；2：群聊消息）")
    @TableField(value = "message_type")
    private MessageType messageType;

}
