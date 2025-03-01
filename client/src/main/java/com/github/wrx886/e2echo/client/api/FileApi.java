package com.github.wrx886.e2echo.client.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.HttpResult;
import com.github.wrx886.e2echo.client.util.HttpUtil;
import com.github.wrx886.e2echo.client.util.JsonUtil;

// File API
@Component
public class FileApi {

    @Autowired
    private HttpUtil httpUtil;

    @Autowired
    private JsonUtil jsonUtil;

    // 上传文件，返回文件的 URL
    public String upload(String baseUrl, String accessToken, String filePath) {
        HttpResult<?> result = httpUtil.postFile(baseUrl, "/server/file/upload",
                Map.of("access-token", accessToken), "file", filePath);
        return jsonUtil.typeCast(result.getData(), String.class);
    }

}
