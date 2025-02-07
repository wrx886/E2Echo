package com.github.wrx886.e2echo.server.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public interface BaseEnum extends IEnum<Integer> {

    @Override
    default Integer getValue() {
        return getCode();
    }

    Integer getCode();

    String getName();
}
