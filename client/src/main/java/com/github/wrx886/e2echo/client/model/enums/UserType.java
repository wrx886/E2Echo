package com.github.wrx886.e2echo.client.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

@Getter
public enum UserType implements IEnum<Integer> {
    // 个人
    PERSON(1),
    // 群聊
    GROUP(2);

    // 枚举值
    private Integer value;

    // 构造函数
    private UserType(Integer value) {
        this.value = value;
    }
}
