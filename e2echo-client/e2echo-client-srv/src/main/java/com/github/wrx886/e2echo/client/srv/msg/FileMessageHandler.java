package com.github.wrx886.e2echo.client.srv.msg;

import java.io.File;
import java.io.FileOutputStream;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.model.vo.FileVo;
import com.github.wrx886.e2echo.client.srv.feign.FileFeign;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;
import com.github.wrx886.e2echo.client.srv.util.AesUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileMessageHandler extends BaseMessageHandler<FileVo> {

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

    private final FileFeign fileFeign;
    private final ObjectMapper objectMapper;

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

            // 判断文件是否存在
            String decryptedFilePath = "./download/" + fileVo.getFileId() + ".decrypted";
            if (new File(decryptedFilePath).exists()) {
                // 文件存在，不需要处理
                return;
            }

            // 创建文件夹
            File dir = new File("./download");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            dir = new File("./temp");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 下载文件
            String outputPath = "./temp/" + fileVo.getFileId();
            ResponseEntity<Resource> resource = fileFeign.download(fileVo.getFileId());
            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                resource.getBody().getInputStream().transferTo(outputStream);
            }

            // 解密文件
            AesUtil.decryptFile(outputPath, decryptedFilePath, fileVo.getAesKey());

            // 删除临时文件
            new File(outputPath).delete();
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
