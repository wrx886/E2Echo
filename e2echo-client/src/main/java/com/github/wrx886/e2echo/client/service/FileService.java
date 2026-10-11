package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.api.FileApi;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastFileVo;
import com.github.wrx886.e2echo.client.vo.message.ChatFileMessageVo;
import com.github.wrx886.e2echo.ecc.Ecc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;

/**
 * 文件业务逻辑层。
 *
 * <p>负责聊天文件的取回（从对象存储下载密文、解密后缓存在 {@code data/download} 下，同一个文件只下载
 * 解密一次；下载用的密文是用完即删的临时文件），以及广播附件的上传（不加密、长期存储）。</p>
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
        // 构建文件（和本地数据库一样放在 data 目录下）
        File file = Path.of(".", "data", "download",
                chatFileMessageVo.objectKey() + ".encrypted" + ".decrypted").toFile();

        // 文件不存在，则下载
        if (!file.exists()) {
            // 下载目录可能还不存在，先建出来
            file.getParentFile().mkdirs();
            // 下载文件
            File downloadFile = Path.of(".", "data", "download",
                    chatFileMessageVo.objectKey() + ".encrypted").toFile();
            try {
                fileApi.download(chatFileMessageVo.objectKey(), downloadFile);
                Ecc.decryptAesFile(downloadFile.getAbsolutePath(), file.getAbsolutePath(), chatFileMessageVo.aesKey());
            } catch (E2EchoException e) {
                throw e;
            } catch (Exception e) {
                log.error("文件解密失败！", e);
                throw new E2EchoException("文件解密失败！");
            } finally {
                // 解密完成后密文就没用了，删掉免得同目录下留两份
                downloadFile.delete();
            }
        }

        // 返回系统文件
        return new FileSystemResource(file);
    }

    /**
     * 上传广播附件：广播通道不加密，原文件直接上传，并附上展示与下载所需的信息。
     *
     * <p>广播附件随帖子长期存在，所以用长期存储（{@link FileApi#LIFECYCLE_FOREVER}），免得 7 天后
     * 帖子里的图片、视频失效。</p>
     *
     * @param file 待上传的附件
     * @return 附件信息（对象键、文件名、内容类型、大小），原样放进广播正文的 {@code files}
     * @throws E2EchoException 文件为空、超过大小上限，或上传失败
     */
    public BroadcastFileVo uploadBroadcast(MultipartFile file) {
        String objectKey = fileApi.upload(file, FileApi.LIFECYCLE_FOREVER);
        String filename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "未命名文件";
        // 浏览器没给内容类型时兜个底：广播正文校验要求它非空
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return new BroadcastFileVo(objectKey, filename, contentType, file.getSize());
    }

}
