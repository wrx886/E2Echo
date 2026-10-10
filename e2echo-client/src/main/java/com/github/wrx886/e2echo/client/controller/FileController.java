package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.api.FileApi;
import com.github.wrx886.e2echo.client.service.FileService;
import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastFileVo;
import com.github.wrx886.e2echo.client.vo.message.ChatFileMessageVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

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
     * 文件接口，用于把对象存储里的文件直接透传给前端。
     */
    private final FileApi fileApi;

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

    /**
     * 广播文件下载：把对象存储里的内容直接透传给前端。
     *
     * <p>广播通道（{@code BROADCAST_*_NONE}）不加密，文件本体就是原样存进对象存储的，所以这里不需要
     * 解密、也不用落盘：直接把文件流写进响应流。</p>
     *
     * <p>响应体用 {@link StreamingResponseBody} 而不是 {@code Resource}：文件在服务端的对象存储里，
     * 本地没有对应的文件可指。</p>
     *
     * @param broadcastFileVo 广播文件信息（对象键与文件名）
     * @return 文件内容流
     */
    @PostMapping("broadcast")
    public ResponseEntity<StreamingResponseBody> broadcast(@Valid @RequestBody BroadcastFileVo broadcastFileVo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(broadcastFileVo.filename(), StandardCharsets.UTF_8).build().toString())
                .body(outputStream -> fileApi.download(broadcastFileVo.objectKey(), outputStream));
    }

}
