package com.github.wrx886.e2echo.client.common.store;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class JsonStore {

    // 接收消息的起始时间
    private Long startTimestamp;

    // WebUrl
    private String webUrl;

}
