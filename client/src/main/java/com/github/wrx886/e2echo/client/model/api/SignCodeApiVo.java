package com.github.wrx886.e2echo.client.model.api;

import lombok.Data;

// 签名码格式
@Data
public class SignCodeApiVo {

    // 签名码的 key
    private String key;

    // 签名码的 value
    private String value;

}
