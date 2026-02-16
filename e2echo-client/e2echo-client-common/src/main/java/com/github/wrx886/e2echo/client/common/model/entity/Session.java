package com.github.wrx886.e2echo.client.common.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(description = "会话")
@TableName("session")
@EqualsAndHashCode(callSuper = true)
public class Session extends BaseEntity {

    @TableField(value = "public_key_hex")
    @Schema(description = "对方公钥或群聊UUID")
    private String publicKeyHex;

    @TableField(value = "message_id")
    @Schema(description = "最后一条消息ID（可以为null）")
    private Long messageId;

    @TableField(value = "timestamp_")
    @Schema(description = "时间戳(int64)，表示自1970年1月1日00:00:00 UTC以来的毫秒数")
    private Long timestamp;

    @TableField(value = "group_")
    @Schema(description = "是否是群聊")
    private Boolean group;

    @TableField(value = "group_key_id")
    @Schema(description = "群密钥ID")
    private Long groupKeyId;

}
