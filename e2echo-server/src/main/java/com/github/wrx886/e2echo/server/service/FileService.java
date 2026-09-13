package com.github.wrx886.e2echo.server.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.wrx886.e2echo.server.config.S3Config;
import com.github.wrx886.e2echo.server.exception.E2EchoException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 文件业务逻辑层。
 *
 * <p>把上传的文件原样存入对象存储，并按 ID 取回。是否加密、如何加解密全部由客户端决定，服务端
 * 只把文件当作不透明的字节流：不解析内容、不改写、不识别类型，所以对象统一使用
 * {@code application/octet-stream}。</p>
 *
 * <p>文件 ID 同时作为对象键，由 16 位十六进制时间戳加 32 位无连字符 UUID 组成，与消息 ID 保持
 * 一致：随机部分让键不可枚举，时间戳前缀让按键排序即为上传顺序，便于运维巡检与按时间清理。</p>
 *
 * <p>对象键还会带一个生命周期前缀，形如 {@code 生命周期前缀/文件 ID}。过期策略由对象存储按前缀
 * 配置的生命周期规则执行，服务端只负责按调用方要求打前缀，不自己实现过期逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    /**
     * 短期存储前缀，由对象存储的生命周期规则在 7 天后删除。
     */
    private static final String LIFECYCLE_DEFAULT = "default";

    /**
     * 长期存储前缀，对象存储不设置过期。
     */
    private static final String LIFECYCLE_FOREVER = "forever";

    /**
     * 对象存储客户端。
     */
    private final S3Client s3Client;

    /**
     * 对象存储配置，提供默认桶名。
     */
    private final S3Config s3Config;

    /**
     * 保存上传的文件。
     *
     * @param file      上传的文件，内容应为客户端加密后的密文
     * @param lifecycle 生命周期，{@code default} 表示短期存储、{@code forever} 表示长期存储，
     *                  为空时按短期存储处理
     * @return 对象键，由生命周期前缀与文件 ID 拼成，可原样用于下载
     * @throws E2EchoException 文件为空、生命周期取值非法或上传失败
     */
    public String upload(MultipartFile file, String lifecycle) {
        if (file == null || file.isEmpty()) {
            throw new E2EchoException("上传文件不能为空！");
        }

        String key = objectKey(lifecycle);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .contentLength(file.getSize())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .build(), requestBody(file));
        } catch (SdkException | UncheckedIOException e) {
            log.error("Failed to upload file, key: {}, size: {}", key, file.getSize(), e);
            throw new E2EchoException("文件上传失败！");
        }
        return key;
    }

    /**
     * 取回文件。
     *
     * <p>返回的是流，调用方负责关闭；文件较大时不会整体载入内存。</p>
     *
     * @param lifecycle 生命周期前缀，取值同上传
     * @param id        文件 ID
     * @return 对象响应流，可通过 {@code response()} 读取长度等元信息
     * @throws E2EchoException 生命周期取值非法、文件不存在或下载失败
     */
    public ResponseInputStream<GetObjectResponse> download(String lifecycle, String id) {
        String key = parseLifecycle(lifecycle) + "/" + id;
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            // 不同 S3 实现的错误码不完全一致，统一按 404 判定为文件不存在
            if (e.statusCode() == 404) {
                throw new E2EchoException("文件不存在：" + key);
            }
            log.error("Failed to download file, key: {}", key, e);
            throw new E2EchoException("文件下载失败！");
        }
    }

    /**
     * 生成对象键：生命周期前缀 + 文件 ID。
     *
     * @param lifecycle 生命周期，为空时按短期存储处理
     * @return 对象键
     * @throws E2EchoException 生命周期取值非法
     */
    private static String objectKey(String lifecycle) {
        return parseLifecycle(lifecycle) + "/" + newFileId();
    }

    /**
     * 解析生命周期前缀。
     *
     * @param lifecycle 生命周期，{@code default} 表示短期存储（由对象存储的生命周期规则在 7 天后
     *                  删除）、{@code forever} 表示长期存储（不设置过期），为空时默认短期存储；
     *                  取值不区分大小写
     * @return 对象键前缀
     * @throws E2EchoException 取值不是 default 或 forever
     */
    private static String parseLifecycle(String lifecycle) {
        if (lifecycle == null || lifecycle.isBlank()) {
            return LIFECYCLE_DEFAULT;
        }
        return switch (lifecycle.trim().toLowerCase(Locale.ROOT)) {
            case LIFECYCLE_DEFAULT -> LIFECYCLE_DEFAULT;
            case LIFECYCLE_FOREVER -> LIFECYCLE_FOREVER;
            default -> throw new E2EchoException("生命周期只能是 default 或 forever：" + lifecycle);
        };
    }

    /**
     * 构造上传请求体。
     *
     * <p>用 {@link RequestBody#fromContentProvider} 而不是 {@link RequestBody#fromInputStream}：请求
     * 签名与重试都可能让 SDK 重复读取请求体，前者每次都会打开一个新的文件流，后者交出的始终是
     * 同一个流，在不支持标记的流上 SDK 会把整个文件缓冲进内存。</p>
     *
     * @param file 上传的文件
     * @return 上传请求体
     */
    private static RequestBody requestBody(MultipartFile file) {
        return RequestBody.fromContentProvider(() -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, file.getSize(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /**
     * 生成文件 ID：16 位十六进制时间戳 + 32 位无连字符 UUID，共 48 位。
     *
     * @return 文件 ID
     */
    private static String newFileId() {
        return String.format("%016x", System.currentTimeMillis())
                + UUID.randomUUID().toString().replace("-", "");
    }

}
