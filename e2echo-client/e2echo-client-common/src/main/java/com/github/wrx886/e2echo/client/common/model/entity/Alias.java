package com.github.wrx886.e2echo.client.common.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(description = "别名")
@TableName("alias")
@EqualsAndHashCode(callSuper = true)
public class Alias extends BaseEntity {

    @TableField(value = "public_key_hex")
    @Schema(description = "对方公钥或群聊UUID")
    private String publicKeyHex;

    @TableField(value = "alias")
    @Schema(description = "别名")
    private String alias;

}
