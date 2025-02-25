package com.github.wrx886.e2echo.client.model.api;

import java.util.Date;

import com.github.wrx886.e2echo.client.model.enums.MessageApiType;

import lombok.Data;

// 消息收发 API 的参数和数据
@Data
public class MessageApiVo {

    // 发送者公钥
    private String fromPublicKey;

    // 接收者公钥
    private String toPublicKey;

    // 数据签名（HEX 格式）
    private String sign;

    // 数据（HEX 格式）
    private String data;

    // 发送时间
    private Date sendTime;

    // 消息类型
    private MessageApiType messageType;

}
