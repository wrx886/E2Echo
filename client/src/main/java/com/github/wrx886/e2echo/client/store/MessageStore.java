package com.github.wrx886.e2echo.client.store;

import java.util.Date;

import org.springframework.stereotype.Component;

import lombok.Data;

// 存储消息相关信息
@Component
@Data
public class MessageStore {

    // 上次刷新时间
    private Date lastUpdateTime = new Date(0);

}
