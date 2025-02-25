package com.github.wrx886.e2echo.client.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.HttpResult;
import com.github.wrx886.e2echo.client.model.api.MessageApiVo;
import com.github.wrx886.e2echo.client.model.api.ReceiveMessageApiVo;
import com.github.wrx886.e2echo.client.util.HttpUtil;
import com.github.wrx886.e2echo.client.util.JsonUtil;

// Message API
@Component
public class MessageApi {

    @Autowired
    private JsonUtil jsonUtil;

    @Autowired
    private HttpUtil httpUtil;

    // 发送消息
    public void send(String baseUrl, String accessToken, MessageApiVo messageApiVo) {
        httpUtil.post(baseUrl, "/server/message/send",
                Map.of("access-token", accessToken), messageApiVo);
    }

    // 接收消息
    public HashSet<MessageApiVo> receive(String baseUrl, String accessToken,
            ReceiveMessageApiVo ReceiveMessageApiVo) {
        // 接收消息
        HttpResult<?> httpResult = httpUtil.post(baseUrl, "/server/message/receive",
                Map.of("access-token", accessToken), ReceiveMessageApiVo);

        // 转为列表
        ArrayList<?> resultData = jsonUtil.typeCast(httpResult.getData(), ArrayList.class);

        // 转为 MessageApiVo 集合
        HashSet<MessageApiVo> messageApiVos = new HashSet<>();
        for (Object data : resultData) {
            messageApiVos.add(jsonUtil.typeCast(data, MessageApiVo.class));
        }

        // 返回
        return messageApiVos;
    }

}
