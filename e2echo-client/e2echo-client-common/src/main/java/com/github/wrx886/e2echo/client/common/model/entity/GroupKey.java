package com.github.wrx886.e2echo.client.common.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("group_key")
@Schema(description = "群密钥管理")
@EqualsAndHashCode(callSuper = true)
public class GroupKey extends BaseEntity {

    @TableField(value = "group_uuid")
    @Schema(description = "群聊UUID")
    private String groupUuid;

    @TableField(value = "timestamp_")
    @Schema(description = "时间戳(int64)")
    private Long timestamp;

    @TableField(value = "aes_key")
    @Schema(description = "AES密钥")
    private String aesKey;

}
