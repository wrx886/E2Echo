package com.github.wrx886.e2echo.client.handler;

import com.github.wrx886.e2echo.client.api.FileApi;
import com.github.wrx886.e2echo.client.common.Const;
import com.github.wrx886.e2echo.client.common.MessageHandler;
import com.github.wrx886.e2echo.client.entity.Message;
import com.github.wrx886.e2echo.client.enums.ChannelEnum;
import com.github.wrx886.e2echo.client.enums.MessageTypeEnum;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.util.IdUtil;
import com.github.wrx886.e2echo.client.vo.message.ChatFileMessageVo;
import com.github.wrx886.e2echo.ecc.Ecc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Path;

/**
 * 聊天文件消息处理器。
 *
 * <p>发送时先用一次性 AES 密钥把文件本体加密并上传到对象存储，再把“文件名 + 密钥 + 对象键”作为消息
 * 正文发出去（正文本身还会被通道加密）；接收时不需要额外处理，文件等用户点开时再下载解密。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatFileMessageHandler implements MessageHandler {

    /**
     * 文件接口，用于上传加密后的文件。
     */
    private final FileApi fileApi;

    /**
     * 消息业务对象，用于把文件消息发给对方。
     */
    private final MessageService messageService;

    /**
     * JSON 对象映射器，用于文件消息正文的序列化。
     */
    private final ObjectMapper objectMapper;

    /**
     * 接收文件消息：文件在用户点开时才下载解密，这里不需要处理。
     *
     * @param message 收到的消息
     */
    @Override
    public void receive(Message message) {
    }

    /**
     * 消息正文的类型。
     *
     * @return {@link ChatFileMessageVo}
     */
    @Override
    public Class<?> getMessageType() {
        return ChatFileMessageVo.class;
    }

    /**
     * 发送文件消息：加密文件本体并上传，然后发出带对象键与密钥的消息。
     *
     * @param to    接收者，私聊时为对方公钥、群聊时为群聊标识
     * @param group 是否群聊
     * @param path  待发送文件的本地路径
     * @throws E2EchoException 路径不是文件、文件超过大小上限、加密或上传失败
     */
    public void send(String to, boolean group, String path) {
        // 待发送的文件必须是真实存在的文件（目录、不存在的路径都要拦下）
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new E2EchoException("非文件或文件不存在");
        }

        // 文件大小
        if (file.length() > Const.FILE_MAX_SIZE_BYTE) {
            throw new E2EchoException(Const.FILE_MAX_SIZE_MESSAGE);
        }

        // 文件加密
        File encryptedFile = Path.of(".", "temp", IdUtil.newId() + ".encrypted").toFile();
        // 临时目录可能还不存在，先建出来，否则写密文会直接失败
        encryptedFile.getParentFile().mkdirs();
        String aesKey;
        try {
            aesKey = Ecc.generateAesKeyAsHex();
        } catch (Exception e) {
            log.error("AES KEY 生成失败！", e);
            throw new E2EchoException("AES KEY 生成失败！");
        }
        try {
            Ecc.encryptAesFile(file.getAbsolutePath(), encryptedFile.getAbsolutePath(), aesKey);
        } catch (Exception e) {
            log.error("文件加密失败！", e);
            throw new E2EchoException("文件加密失败！");
        }

        // 上传加密文件
        String objectKey;
        try {
            objectKey = fileApi.upload(encryptedFile, FileApi.LIFECYCLE_DEFAULT);
        } finally {
            // 上传成功与否，临时密文都不再需要，直接删掉，避免 temp 目录越积越多
            encryptedFile.delete();
        }

        // 构建消息
        ChatFileMessageVo chatFileMessageVo = new ChatFileMessageVo(
                file.getName(),
                aesKey,
                objectKey
        );

        // 发送消息
        Message message = new Message();
        message.setFrom(Ecc.getPublicKey());
        message.setTo(to);
        message.setMessage(objectMapper.writeValueAsString(chatFileMessageVo));
        message.setType(MessageTypeEnum.CHAT_FILE.name());
        message.setChannel(group ? ChannelEnum.CHAT_GROUP_AES.name() : ChannelEnum.CHAT_PRIVATE_ECC.name());
        message.setInfo("{}");
        messageService.send(message, !group);
    }

}
