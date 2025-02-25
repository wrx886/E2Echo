package com.github.wrx886.e2echo.client.model.vo;

import com.github.wrx886.e2echo.client.model.entity.Message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// 要发送的消息
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SendMessageVo extends Message {

    // 发送者公钥
    private String fromPublicKey;

    // 群聊 uuid
    private String groupUuid;

}
