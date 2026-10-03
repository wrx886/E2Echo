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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
     * <p>文件来自调用方的上传（浏览器只能给出文件内容，给不出真实路径），这里先落到 {@code data/temp}
     * 下的临时文件再加密。临时文件用 {@code try-finally} 兜住：不论是保存、加密、上传还是发消息哪一步
     * 失败，方法退出前都会把临时文件删掉，避免目录越积越多。</p>
     *
     * @param to    接收者，私聊时为对方公钥、群聊时为群聊标识
     * @param group 是否群聊
     * @param file  待发送的文件
     * @throws E2EchoException 文件为空、超过大小上限，或保存、加密、上传失败
     */
    public void send(String to, boolean group, MultipartFile file) {
        // 文件大小
        if (file == null || file.isEmpty()) {
            throw new E2EchoException("文件为空！");
        }
        if (file.getSize() > Const.FILE_MAX_SIZE_BYTE) {
            throw new E2EchoException(Const.FILE_MAX_SIZE_MESSAGE);
        }

        // 临时文件：加解密接口按文件路径工作，所以先把上传内容落到 data 下的临时目录；
        // 下面整个流程（保存、加密、上传、发消息）无论在哪一步失败，最后都会删掉这两个文件
        File tempDir = Path.of(".", "data", "temp").toFile();
        File sourceFile;
        File encryptedFile;
        // 临时文件用随机 ID 命名：万一这个 ID 已经被占用（例如上次异常留下的残留文件），
        // 就换一个；连续几次都撞上说明临时目录不正常，直接报错而不是覆盖别人的文件
        int retry = 0;
        do {
            if (retry++ > 3) {
                throw new E2EchoException("临时文件冲突，请稍后重试！");
            }
            String id = IdUtil.newId();
            sourceFile = new File(tempDir, id + ".upload");
            encryptedFile = new File(tempDir, id + ".encrypted");
        } while (sourceFile.exists() || encryptedFile.exists());

        try {
            // 保存上传的文件
            tempDir.mkdirs();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                log.error("文件保存失败！", e);
                throw new E2EchoException("文件保存失败！");
            }

            // 文件加密
            String aesKey;
            try {
                aesKey = Ecc.generateAesKeyAsHex();
            } catch (Exception e) {
                log.error("AES KEY 生成失败！", e);
                throw new E2EchoException("AES KEY 生成失败！");
            }
            try {
                Ecc.encryptAesFile(sourceFile.getAbsolutePath(), encryptedFile.getAbsolutePath(), aesKey);
            } catch (Exception e) {
                log.error("文件加密失败！", e);
                throw new E2EchoException("文件加密失败！");
            }

            // 上传加密文件
            String objectKey = fileApi.upload(encryptedFile, FileApi.LIFECYCLE_DEFAULT);

            // 构建消息
            String filename = StringUtils.hasText(file.getOriginalFilename())
                    ? file.getOriginalFilename() : "未命名文件";
            ChatFileMessageVo chatFileMessageVo = new ChatFileMessageVo(
                    filename,
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
        } finally {
            // 成功或失败都删掉临时文件，避免 temp 目录越积越多
            sourceFile.delete();
            encryptedFile.delete();
        }
    }

}
