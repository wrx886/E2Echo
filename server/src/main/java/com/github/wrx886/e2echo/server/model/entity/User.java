package com.github.wrx886.e2echo.server.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户实体")
@TableName(value = "user")
public class User extends BaseEntity {

    @Schema(description = "用户的手机号")
    @TableField(value = "phone")
    private String phone;

    @Schema(description = "ECC HEX 公钥")
    @TableField(value = "public_key")
    private String publicKey;

}
