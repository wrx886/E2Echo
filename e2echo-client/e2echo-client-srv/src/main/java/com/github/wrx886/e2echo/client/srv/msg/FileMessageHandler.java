package com.github.wrx886.e2echo.client.srv.msg;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.model.vo.FileVo;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;
import com.github.wrx886.e2echo.client.srv.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileMessageHandler extends BaseMessageHandler {

    // 注册
    static {
        BaseMessageHandler.personMessageHandlerMap.put(MessageType.AUDIO, FileMessageHandler.class);
        BaseMessageHandler.personMessageHandlerMap.put(MessageType.PICTURE, FileMessageHandler.class);
        BaseMessageHandler.personMessageHandlerMap.put(MessageType.FILE, FileMessageHandler.class);
        BaseMessageHandler.personMessageHandlerMap.put(MessageType.VIDEO, FileMessageHandler.class);

        BaseMessageHandler.groupMessageHandlerMap.put(MessageType.AUDIO, FileMessageHandler.class);
        BaseMessageHandler.groupMessageHandlerMap.put(MessageType.PICTURE, FileMessageHandler.class);
        BaseMessageHandler.groupMessageHandlerMap.put(MessageType.FILE, FileMessageHandler.class);
        BaseMessageHandler.groupMessageHandlerMap.put(MessageType.VIDEO, FileMessageHandler.class);
    }

    private final ObjectMapper objectMapper;
    private final FileService fileService;

    @Override
    public void receiveHandler(EccMessage eccMessage, SendMessageVo sendMessageVo) {
        try {
            // 消息类型必须是文件
            if (!(MessageType.AUDIO.equals(sendMessageVo.getType())
                    || MessageType.PICTURE.equals(sendMessageVo.getType())
                    || MessageType.FILE.equals(sendMessageVo.getType())
                    || MessageType.VIDEO.equals(sendMessageVo.getType()))) {
                throw new RuntimeException("消息类型错误");
            }

            // 转换数据
            FileVo fileVo = objectMapper.readValue(sendMessageVo.getData(), FileVo.class);

            if (MessageType.PICTURE.equals(sendMessageVo.getType())) {
                // 下载文件
                fileService.downloadFile(fileVo);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
