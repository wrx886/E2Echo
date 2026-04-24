package com.github.wrx886.e2echo.client.srv.msg;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TextMessageHandler extends BaseMessageHandler {

    // 注册
    static {
        personMessageHandlerMap.put(MessageType.TEXT, TextMessageHandler.class);
        groupMessageHandlerMap.put(MessageType.TEXT, TextMessageHandler.class);
    }

    /**
     * 接收消息处理
     *
     * @param sendMessageVo 消息
     */
    @Override
    public void receiveHandler(EccMessage eccMessage, SendMessageVo sendMessageVo) {
        // 不需要处理
    }

}
