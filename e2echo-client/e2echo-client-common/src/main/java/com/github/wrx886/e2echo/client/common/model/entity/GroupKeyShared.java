package com.github.wrx886.e2echo.client.common.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("group_key_shared")
@Schema(description = "群密钥共享")
@EqualsAndHashCode(callSuper = true)
public class GroupKeyShared extends BaseEntity {

    @TableField(value = "group_uuid")
    @Schema(description = "群聊UUID")
    private String groupUuid;

    @TableField(value = "from_")
    @Schema(description = "分享者")
    private String from;

    @TableField(value = "to_")
    @Schema(description = "接收者")
    private String to;

}
