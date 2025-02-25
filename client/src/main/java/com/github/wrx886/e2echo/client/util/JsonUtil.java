package com.github.wrx886.e2echo.client.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.E2Echoxception;

@Component
public class JsonUtil {

    @Autowired
    private ObjectMapper objectMapper;

    // 修改数据的类型
    public <E, T> T typeCast(E e, Class<T> clazz) {
        // 空返回
        if (e == null) {
            return null;
        }

        // 如果输入类型已经是输出类型，则直接返回
        if (clazz.isInstance(e)) {
            return clazz.cast(e);
        }

        // 通过将数据序列化和反序列化转换类型
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(e), clazz);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new E2Echoxception(null, exception.getMessage());
        }
    }

}
