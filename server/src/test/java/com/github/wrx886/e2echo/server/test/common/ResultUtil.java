package com.github.wrx886.e2echo.server.test.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.result.Result;

@Component
public class ResultUtil {

    @Autowired
    private ObjectMapper objectMapper;

    // 将响应体体转为转为一个 Result 对象
    public <E> Result<E> jsonString2Result(String jsonString, Class<E> clazz) throws Exception {
        @SuppressWarnings("unchecked")
        Result<E> result = objectMapper.readValue(jsonString, Result.class);
        if (result.getData() != null) {
            result.setData(to(result.getData(), clazz));
        }
        return result;
    }

    // 从响应中提取 Result 对象
    public <E> Result<E> getResultFromMockResponse(MockHttpServletResponse mockHttpServletResponse, Class<E> clazz)
            throws Exception {
        return jsonString2Result(mockHttpServletResponse.getContentAsString(), clazz);
    }

    // 数据类型转换
    public <E> E to(Object src, Class<E> clazz) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsString(src), clazz);
    }

}
