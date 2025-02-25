package com.github.wrx886.e2echo.client.model.api;

import java.util.Date;

import com.github.wrx886.e2echo.client.model.enums.MessageApiType;

import lombok.Data;

// 消息接收参数
@Data
public class ReceiveMessageApiVo {

    // 起始时间
    private Date startTime;

    // 消息类型枚举（同 MessageApiVo 的 messageType）
    private MessageApiType messageType;

}
