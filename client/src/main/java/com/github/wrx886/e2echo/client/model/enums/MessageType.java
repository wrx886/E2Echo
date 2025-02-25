package com.github.wrx886.e2echo.client.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

@Getter
public enum MessageType implements IEnum<Integer> {
    // 文本
    TEXT(1),
    // 图片
    PICTURE(2),
    // 音频
    AUDIO(3),
    // 视频
    VIDEO(4),
    // 文件
    FILE(5);

    // 枚举码
    private final Integer value;

    // 构造函数
    private MessageType(Integer value) {
        this.value = value;
    }

}
