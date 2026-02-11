package com.github.wrx886.e2echo.client.common.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "基础实体")
public class BaseEntity {

    @Schema(description = "自增主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属登入用户")
    @TableField(value = "owner_public_key_hex")
    private String ownerPublicKeyHex;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT, select = false)
    @JsonIgnore
    private Date createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE, select = false)
    @JsonIgnore
    private Date updateTime;

    @Schema(description = "逻辑删除（0：未删除；1：已删除）")
    @TableField(value = "is_deleted", select = false)
    @TableLogic
    @JsonIgnore
    private Byte isDeleted;

    @Version
    @TableField(value = "version")
    @Schema(description = "版本号")
    private Long version;

}
