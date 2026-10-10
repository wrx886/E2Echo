package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.api.FileApi;
import com.github.wrx886.e2echo.client.service.FileService;
import com.github.wrx886.e2echo.client.vo.message.ChatFileMessageVo;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

/**
 * 文件接口。
 *
 * <p>客户端自己的 HTTP 接口：前端按文件消息或广播附件里的信息取回文件，下载统一走 GET。</p>
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
     * <p>播放器（{@code <video>} / {@code <audio>}）只会用 GET 取媒体，拖动进度条时还会带上
     * {@code Range} 头，所以参数放查询串、返回 {@code Resource}，由 Spring MVC 自己处理区间请求
     * （命中区间时回 {@code 206} + {@code Content-Range} + {@code Accept-Ranges}）。</p>
     *
     * <p>不带 {@code Range} 时就是整包下载。文件在本地会缓存到 {@code data/download}。</p>
     *
     * @param objectKey   对象存储里的对象键
     * @param aesKey      文件加密用的 AES 密钥（HEX 格式）
     * @param filename    文件名，用于响应头里的展示名，可以为空
     * @param contentType 内容类型（MIME，如 {@code video/mp4}），为空按
     *                    {@code application/octet-stream} 处理
     * @return 解密后的文件，支持区间请求
     */
    @GetMapping("chat")
    public ResponseEntity<Resource> download(
            @NotBlank String objectKey,
            @NotBlank String aesKey,
            String filename,
            String contentType
    ) {
        ChatFileMessageVo chatFileMessageVo = new ChatFileMessageVo(
                StringUtils.hasText(filename) ? filename : "file", aesKey, objectKey);
        Resource object = fileService.getChatFile(chatFileMessageVo);

        return ResponseEntity.ok()
                .contentType(mediaTypeOf(contentType))
                // 用 inline：图片、播放器就地展示，而不是让浏览器弹出"另存为"
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(chatFileMessageVo.filename(), StandardCharsets.UTF_8).build().toString())
                .body(object);
    }

    /**
     * 广播文件下载：把对象存储里的内容直接透传给前端。
     *
     * <p>广播通道（{@code BROADCAST_*_NONE}）不加密，文件本体就是原样存进对象存储的，所以这里不需要
     * 解密、也不用落盘：直接把文件流写进响应流。</p>
     *
     * <p>响应体用 {@link StreamingResponseBody} 而不是 {@code Resource}：文件在服务端的对象存储里，
     * 本地没有对应的文件可指，因此也不支持 {@code Range}（视频拖动进度条）。</p>
     *
     * @param objectKey   对象存储里的对象键
     * @param filename    文件名，用于响应头里的展示名，可以为空
     * @param contentType 内容类型（MIME，如 {@code image/png}），为空按
     *                    {@code application/octet-stream} 处理
     * @return 文件内容流
     */
    @GetMapping("broadcast")
    public ResponseEntity<StreamingResponseBody> broadcast(
            @NotBlank String objectKey,
            String filename,
            String contentType
    ) {
        String name = StringUtils.hasText(filename) ? filename : "file";
        return ResponseEntity.ok()
                .contentType(mediaTypeOf(contentType))
                // 用 inline：图片、播放器就地展示，而不是让浏览器弹出"另存为"
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(name, StandardCharsets.UTF_8).build().toString())
                .body(outputStream -> fileApi.download(objectKey, outputStream));
    }

    /**
     * 把请求里的内容类型字符串转成 {@link MediaType}，为空或非法时按二进制流处理。
     *
     * @param contentType 内容类型字符串
     * @return 解析后的内容类型
     */
    private static MediaType mediaTypeOf(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

}
