package com.github.wrx886.e2echo.client.common.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("group_member")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "群成员（仅群主存储）")
public class GroupMember extends BaseEntity {

    @TableField(value = "group_uuid")
    @Schema(description = "群聊UUID")
    private String groupUuid;

    @TableField(value = "public_key_hex")
    @Schema(description = "群成员公钥")
    private String publicKeyHex;

}
