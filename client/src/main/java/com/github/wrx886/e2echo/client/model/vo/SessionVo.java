package com.github.wrx886.e2echo.client.model.vo;

import java.util.Date;

import com.github.wrx886.e2echo.client.model.enums.MessageType;

import lombok.Data;

// 会话
@Data
public class SessionVo {

    // 会话 ID
    private Long sessionId;

    // 会话名称
    private String sessionName;

    // 发送者 id
    private Long fromId;

    // 发送者名称
    private String fromName;

    // 最新消息
    private String message;

    // 最新消息类型
    private MessageType messageType;

    // 最新消息时间
    private Date lastTime;

}
