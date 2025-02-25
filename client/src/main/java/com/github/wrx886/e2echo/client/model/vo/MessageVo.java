package com.github.wrx886.e2echo.client.model.vo;

import com.github.wrx886.e2echo.client.model.entity.Message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MessageVo extends Message {

    // 发送者名称
    private String fromName;

}
