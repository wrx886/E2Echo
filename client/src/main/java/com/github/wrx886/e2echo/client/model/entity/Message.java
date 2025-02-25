package com.github.wrx886.e2echo.client.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wrx886.e2echo.client.model.enums.MessageType;

import lombok.Data;

// 消息实体
@Data
@TableName("messages")
public class Message {

    // 自增主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 所属登入用户
    @TableField(value = "owner_id")
    private Long ownerId;

    // 发送者 id
    @TableField(value = "from_id")
    private Long fromId;

    // 会话 id
    @TableField(value = "session_id")
    private Long sessionId;

    // 消息数据
    @TableField(value = "data")
    private String data;

    // 消息类型
    @TableField(value = "type")
    private MessageType type;

    // 消息 uuid
    @TableField(value = "uuid")
    private String uuid;

    // 发送时间
    @TableField(value = "send_time")
    private Date sendTime;

}
