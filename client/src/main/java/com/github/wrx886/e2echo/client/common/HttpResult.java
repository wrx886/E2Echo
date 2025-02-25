package com.github.wrx886.e2echo.client.common;

import lombok.Data;

// HTTP 请求返回的 json 字符串所对应的对象
@Data
public class HttpResult<E> {

    // 状态码
    private Integer code;

    // 状态信息
    private String message;

    // 数据
    private E data;

}
