package com.github.wrx886.e2echo.server.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.github.wrx886.e2echo.server.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.wrx886.e2echo.server.result.Result;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * 文件接口。
 *
 * <p>只负责收发文件：上传时把客户端加密后的密文原样交给对象存储，下载时原样返回。服务端不解密、
 * 不解析内容，加密与解密全部由客户端完成，因此下载响应的类型固定为
 * {@code application/octet-stream}。</p>
 *
 * <p>上传时可以指定生命周期：{@code default} 为短期存储、{@code forever} 为长期存储，不传时按
 * 短期存储处理。所谓过期并不是服务端实现的，服务端只把生命周期作为对象键前缀（如
 * {@code default/0000018f...}），实际的过期规则由对象存储按前缀配置的生命周期策略执行。</p>
 */
@RestController
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {

    /**
     * 文件业务逻辑对象。
     */
    private final FileService fileService;

    /**
     * 上传文件。
     *
     * <p>以 multipart 表单接收文件，单文件大小上限由配置决定；文件内容不做任何校验与改写。</p>
     *
     * @param file      上传的文件，表单字段名为 {@code file}
     * @param lifecycle 生命周期，{@code default} 表示短期存储（7 天后过期）、{@code forever} 表示
     *                  长期存储（永不过期），不传时按短期存储处理
     * @return 对象键，由生命周期前缀与文件 ID 拼成，调用方保存它用于后续下载
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String lifecycle) {
        return Result.ok(fileService.upload(file, lifecycle));
    }

    /**
     * 下载文件。
     *
     * <p>以流的方式返回文件内容并带上 {@code Content-Length}，客户端可以据此展示进度；服务端不会
     * 把文件整体读入内存。这里没有声明 {@code produces}，是为了让「文件不存在」这类异常仍能以
     * JSON 形式返回统一结果。</p>
     *
     * @param lifecycle 生命周期前缀，与上传时一致
     * @param id        文件 ID
     * @return 文件内容流
     */
    @GetMapping("/{lifecycle}/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String lifecycle,
            @PathVariable String id) {
        ResponseInputStream<GetObjectResponse> object = fileService.download(lifecycle, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(object.response().contentLength())
                .body(new InputStreamResource(object));
    }

}
