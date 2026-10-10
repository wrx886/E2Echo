package com.github.wrx886.e2echo.server.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
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
 *
 * <p>下载支持 Range：范围直接下推给对象存储，只回源需要的那一段，因此拖动媒体播放进度、断点续传
 * 都不会浪费带宽，也不会把文件载入内存。</p>
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
     * 支持的范围单位：只支持按字节取范围。
     */
    private static final String RANGE_UNIT = "bytes";

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
     * 下载文件。
     *
     * <p>返回的是流，Spring MVC 负责写出与关闭，文件不会整体载入内存。不带范围时整体返回
     * （200）；带范围时把范围下推给对象存储，只回源这一段并返回 206 与 {@code Content-Range}；
     * 范围不可满足时返回 416。只支持单段范围，多段范围与格式非法的 Range 按 HTTP 规范忽略，
     * 整体返回。</p>
     *
     * @param lifecycle   生命周期前缀，取值同上传
     * @param id          文件 ID
     * @param rangeHeader Range 请求头，未带时为 {@code null}
     * @return 响应实体：整体或部分内容流，或范围不可满足的空响应
     * @throws E2EchoException 生命周期取值非法、文件不存在或下载失败
     */
    public ResponseEntity<InputStreamResource> download(String lifecycle, String id, String rangeHeader) {
        String key = parseLifecycle(lifecycle) + "/" + id;
        HttpRange range = singleRange(rangeHeader);

        // 不带 Range、多段范围或格式非法：整体返回
        if (range == null) {
            ResponseInputStream<GetObjectResponse> object = getObject(key, null);
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, RANGE_UNIT)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(object.response().contentLength())
                    .body(new InputStreamResource(object));
        }

        // 带 Range：先取长度把范围换算成闭区间，再按范围回源
        long length = length(key);
        long start = range.getRangeStart(length);
        long end = range.getRangeEnd(length);
        if (start > end) {
            // 起始位置落在文件末尾之后：按规范返回 416，并告知实际长度
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, RANGE_UNIT + " */" + length)
                    .build();
        }

        ResponseInputStream<GetObjectResponse> object = getObject(key, RANGE_UNIT + "=" + start + "-" + end);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, RANGE_UNIT)
                .header(HttpHeaders.CONTENT_RANGE, RANGE_UNIT + " " + start + "-" + end + "/" + length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(end - start + 1)
                .body(new InputStreamResource(object));
    }

    /**
     * 读取文件元信息，用于 HEAD 请求：只回源长度，不取回内容。
     *
     * @param lifecycle 生命周期前缀，取值同上传
     * @param id        文件 ID
     * @return 只带长度与范围支持标记、不含内容的响应实体
     * @throws E2EchoException 生命周期取值非法、文件不存在或读取失败
     */
    public ResponseEntity<InputStreamResource> metadata(String lifecycle, String id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, RANGE_UNIT)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(length(parseLifecycle(lifecycle) + "/" + id))
                .build();
    }

    /**
     * 向对象存储取对象。
     *
     * @param key   对象键
     * @param range 字节范围，形如 {@code bytes=0-99}；为空时取整个对象
     * @return 对象响应流，调用方负责关闭
     * @throws E2EchoException 文件不存在或下载失败
     */
    private ResponseInputStream<GetObjectResponse> getObject(String key, String range) {
        GetObjectRequest.Builder request = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key);
        if (range != null) {
            request.range(range);
        }
        try {
            return s3Client.getObject(request.build());
        } catch (S3Exception e) {
            throw downloadFailure(key, e);
        }
    }

    /**
     * 读取对象大小：只取元信息，不取回内容。
     *
     * @param key 对象键
     * @return 对象大小（字节）
     * @throws E2EchoException 文件不存在或读取失败
     */
    private long length(String key) {
        try {
            return s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build()).contentLength();
        } catch (S3Exception e) {
            throw downloadFailure(key, e);
        }
    }

    /**
     * 解析单段范围。
     *
     * <p>多段范围需要 {@code multipart/byteranges} 响应，这里按 HTTP 规范选择忽略它，与格式非法的
     * Range 一样整体返回：规范允许服务端忽略 Range 头。</p>
     *
     * @param rangeHeader Range 请求头，未带时为空
     * @return 单段范围；未带、多段或格式非法时返回 {@code null}，表示按整体返回处理
     */
    private static HttpRange singleRange(String rangeHeader) {
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return null;
        }
        try {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            return ranges.size() == 1 ? ranges.get(0) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 把对象存储的异常转换成业务异常。
     *
     * @param key 对象键
     * @param e   对象存储抛出的异常
     * @return 对应的业务异常
     */
    private static E2EchoException downloadFailure(String key, S3Exception e) {
        // 不同 S3 实现的错误码不完全一致，统一按 404 判定为文件不存在
        if (e.statusCode() == 404) {
            return new E2EchoException("文件不存在：" + key);
        }
        log.error("Failed to download file, key: {}", key, e);
        return new E2EchoException("文件下载失败！");
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
