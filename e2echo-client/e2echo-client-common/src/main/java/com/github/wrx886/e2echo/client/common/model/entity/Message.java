package com.github.wrx886.e2echo.client.common.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseEntity {

    @Schema(description = "UUID")
    @TableField(value = "uuid")
    private String uuid;

    @TableField(value = "timestamp_")
    @Schema(description = "时间戳(int64)，表示自1970年1月1日00:00:00 UTC以来的毫秒数")
    private Long timestamp;

    @TableField(value = "from_public_key_hex")
    @Schema(description = "发送者公钥")
    private String fromPublicKeyHex;

    @TableField(value = "to_public_key_hex")
    @Schema(description = "接收者公钥/群聊ID")
    private String toPublicKeyHex;

    @Schema(description = "数据")
    @TableField(value = "data")
    private String data;

    @Schema(description = "消息类型")
    @TableField(value = "type")
    private MessageType type;

    // 如果为群聊消息，toPublicKeyHex 改为群聊的 UUID（（格式：{群主公钥}:{群聊UUID}））
    @TableField(value = "group_")
    @Schema(description = "是否是群聊消息")
    private Boolean group;

}
