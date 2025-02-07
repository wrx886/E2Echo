package com.github.wrx886.e2echo.server.custom.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.server.model.enums.BaseEnum;

@Component
public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {
    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return new Converter<String, T>() {
            @Override
            public T convert(String code) {
                // 通过 Class 获取枚举类型的所有实例
                T[] ts = targetType.getEnumConstants();
                // 遍历寻找
                for (T t : ts) {
                    if (t.getCode().equals(Integer.valueOf(code))) {
                        return t;
                    }
                }
                // 没有对应的类型
                throw new IllegalArgumentException("code: " + code + " illegal");
            }
        };
    }
}
