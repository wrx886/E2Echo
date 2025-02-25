package com.github.wrx886.e2echo.client.common;

import lombok.Getter;

// 后端返回的状态码 + 前端自定义的状态码
@Getter
public enum CodeEnum {
    // 通用的返回
    OK(200, "OK"),
    FAIL(201, "Fail"),

    // 登入业务
    LOGIN_AUTH(501, "未登陆"),
    LOGIN_TOKEN_EXPIRED(502, "Token 已过期"),
    LOGIN_PHONE_EMPTY(503, "手机号码为空"),
    LOGIN_PHONE_CODE_EMPTY(504, "手机验证码为空"),
    LOGIN_PHONE_CODE_SEND_TOO_OFTEN(505, "手机验证码发送过于频繁"),
    LOGIN_PHONE_CODE_EXPIRED(506, "手机验证码已过期"),
    LOGIN_PHONE_CODE_ERROR(507, "手机验证码错误"),
    LOGIN_PUBLIC_KEY_EMPTY(508, "公钥为空"),
    LOGIN_CAPTCHA_EMPTY(509, "图形验证码为空"),
    LOGIN_CAPTCHA_ERROR(510, "图形验证码错误"),
    LOGIN_CAPTCHA_EXPIRED(511, "图形验证码已过期"),
    LOGIN_SIGN_CODE_EMPTY(512, "签名码为空"),
    LOGIN_SIGN_CODE_ERROR(513, "签名码错误"),
    LOGIN_SIGN_CODE_EXPIRED(514, "签名码已过期"),
    LOGIN_USER_INFO_NOT_MATCH(515, "用户信息不匹配"),
    LOGIN_USER_DELETED(516, "登入用户已被删除"),

    // 消息收发业务
    MESSAGE_SENDER_INFO_NOT_MATCH(301, "发送者信息不匹配"),
    MESSAGE_RECEIVER_NOT_EXIST(302, "接收者不存在"),
    MESSAGE_SEND_EXPIRED(303, "消息发送超时"),
    MESSAGE_DATA_TO_LONG(304, "消息数据过长"),

    // 用户管理业务
    USER_PHONE_EMPTY(401, "手机号码为空"),
    USER_PHONE_CODE_EMPTY(402, "手机验证码为空"),
    USER_PHONE_CODE_SEND_TOO_OFTEN(403, "手机验证码发送过于频繁"),
    USER_PHONE_CODE_EXPIRED(404, "手机验证码已过期"),
    USER_PHONE_CODE_ERROR(405, "手机验证码错误"),
    USER_SIGN_CODE_EMPTY(406, "签名码为空"),
    USER_SIGN_CODE_ERROR(407, "签名码错误"),
    USER_SIGN_CODE_EXPIRED(408, "签名码过期"),

    // WebSocket
    WEB_SOCKET_REQUEST_FARMAT_ERROR(601, "请求格式错误"),
    WEB_SOCKET_REQUEST_NOT_FOUND(602, "请求命令不存在"),
    WEB_SOCKET_REQUEST_PARAM_ERROR(603, "请求参数错误"),
    WEB_SOCKET_REQUEST_ID_EMPTY(604, "请求 ID 为空"),
    WEB_SOCKET_REQUEST_COMMAND_EMPTY(605, "请求命令为空"),

    // ECC 加密
    ECC_PUBLIC_KEY_FORMAT_ERROR(701, "公钥格式错误"),
    ECC_PRIVATE_KEY_FORMAT_ERROR(702, "私钥格式错误")
    ;

    // 状态码
    private final Integer code;

    // 状态描述
    private final String message;

    private CodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
