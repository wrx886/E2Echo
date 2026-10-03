package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.api.FileApi;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.vo.message.ChatFileMessageVo;
import com.github.wrx886.e2echo.ecc.Ecc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

/**
 * 文件业务逻辑层。
 *
 * <p>负责聊天文件的取回：按消息里的对象键从对象存储下载密文、解密后落盘复用（同一个文件只下载解密
 * 一次），再把解密后的文件交给调用方。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    /**
     * 文件接口，用于下载加密后的文件。
     */
    private final FileApi fileApi;

    /**
     * 取回聊天文件：本地已有解密后的文件就直接用，否则下载密文并解密。
     *
     * @param chatFileMessageVo 文件消息正文（文件名、密钥、对象键）
     * @return 解密后的文件资源
     * @throws E2EchoException 下载或解密失败
     */
    public Resource getChatFile(ChatFileMessageVo chatFileMessageVo) {
        // 构建文件
        File file = Path.of(".", "download", chatFileMessageVo.objectKey() + ".encrypted" + ".decrypted").toFile();

        // 文件不存在，则下载
        if (!file.exists()) {
            // 下载目录可能还不存在，先建出来
            file.getParentFile().mkdirs();
            // 下载文件
            File downloadFile = Path.of(".", "download", chatFileMessageVo.objectKey() + ".encrypted").toFile();
            fileApi.download(chatFileMessageVo.objectKey(), downloadFile);
            try {
                Ecc.decryptAesFile(downloadFile.getAbsolutePath(), file.getAbsolutePath(), chatFileMessageVo.aesKey());
            } catch (Exception e) {
                log.error("文件解密失败！", e);
                throw new E2EchoException("文件解密失败！");
            }
        }

        // 返回系统文件
        return new FileSystemResource(file);
    }

}
