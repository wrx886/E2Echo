package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.service.FileService;
import com.github.wrx886.e2echo.client.vo.message.ChatFileMessageVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 文件接口。
 *
 * <p>客户端自己的 HTTP 接口：前端按文件消息里的信息取回并解密文件，文件在本地会缓存一份。</p>
 */
@Validated
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    /**
     * 文件业务对象。
     */
    private final FileService fileService;

    /**
     * 下载（并解密）聊天文件。
     *
     * <p>参数是文件消息正文，用请求体传，所以是 POST 而不是 GET。</p>
     *
     * @param chatFileMessageVo 文件消息正文（文件名、密钥、对象键）
     * @return 解密后的文件流
     * @throws Exception 下载或解密失败
     */
    @PostMapping("chat")
    public ResponseEntity<Resource> download(@Valid @RequestBody ChatFileMessageVo chatFileMessageVo) throws Exception {
        Resource object = fileService.getChatFile(chatFileMessageVo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(object.contentLength())
                .body(new InputStreamResource(object));
    }

}
