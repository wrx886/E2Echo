package com.github.wrx886.e2echo.client.api;

import java.io.File;
import java.util.Optional;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.util.ApiUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 文件接口。
 *
 * <p>对应服务端的 {@code /file} 系列接口：上传与下载。服务端只把文件当字节流，加密与解密由调用方
 * 完成。</p>
 *
 * <p>上传可指定生命周期：{@link #LIFECYCLE_DEFAULT} 短期存储、{@link #LIFECYCLE_FOREVER} 长期
 * 存储，不指定按短期处理。</p>
 */
@Component
@RequiredArgsConstructor
public class FileApi {

    /**
     * 短期存储：对象存储 7 天后删除。
     */
    public static final String LIFECYCLE_DEFAULT = "default";

    /**
     * 长期存储：永不过期。
     */
    public static final String LIFECYCLE_FOREVER = "forever";

    /**
     * WebClient，其 baseUrl 为登入时填写的服务器地址。
     */
    private final WebClient webClient;

    /**
     * 时间戳接口，请求前校验本机与服务器的时间。
     */
    private final TimestampApi timestampApi;

    /**
     * 上传文件。
     *
     * @param file      待上传的文件，内容应为加密后的密文
     * @param lifecycle 生命周期，取 {@link #LIFECYCLE_DEFAULT} 或 {@link #LIFECYCLE_FOREVER}，
     *                  为空时按短期存储处理
     * @return 对象键，形如 {@code default/文件 ID}，下载时原样传回
     * @throws E2EchoException 请求失败，或服务端返回失败状态（例如文件为空、生命周期非法）
     */
    public String upload(File file, String lifecycle) {
        timestampApi.checkTime();

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new FileSystemResource(file));
        if (lifecycle != null && !lifecycle.isBlank()) {
            parts.add("lifecycle", lifecycle);
        }

        return ApiUtil.apiGet(() -> webClient.post()
                .uri("file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {
                })
                .block());
    }

    /**
     * 下载文件。
     *
     * <p>内容以流的方式写入目标文件；中途失败可能留下不完整的文件，由调用方决定是否删除。</p>
     *
     * @param lifecycle 生命周期前缀，与上传时一致
     * @param id        文件 ID
     * @param target    保存到的文件
     * @throws E2EchoException 请求失败，或服务端返回失败状态（例如文件不存在）
     */
    public void download(String lifecycle, String id, File target) {
        timestampApi.checkTime();

        ApiUtil.apiCall(() -> webClient.get()
                .uri("file/{lifecycle}/{id}", lifecycle, id)
                .exchangeToMono(response -> {
                    // 失败时服务端返回的是 JSON（状态码仍为 200），按响应类型区分，避免把 JSON 写进文件
                    if (isJson(response.headers().contentType())) {
                        return response.bodyToMono(new ParameterizedTypeReference<Result<Void>>() {
                        }).flatMap(result -> Mono.error(new E2EchoException(
                                result.message() == null ? "下载失败！" : result.message())));
                    }
                    if (response.statusCode().isError()) {
                        return Mono.error(new E2EchoException("服务器状态异常！"));
                    }
                    return DataBufferUtils.write(response.bodyToFlux(DataBuffer.class), target.toPath());
                })
                .block());
    }

    /**
     * 下载文件（使用上传时返回的对象键）。
     *
     * @param objectKey 上传时返回的对象键，形如 {@code default/文件 ID}
     * @param target    保存到的文件
     * @throws E2EchoException 对象键格式错误、请求失败，或服务端返回失败状态
     */
    public void download(String objectKey, File target) {

        int index = objectKey == null ? -1 : objectKey.indexOf('/');
        if (index < 1 || index == objectKey.length() - 1) {
            throw new E2EchoException("对象键格式错误：" + objectKey);
        }
        download(objectKey.substring(0, index), objectKey.substring(index + 1), target);
    }

    /**
     * 判断响应的内容类型是否为 JSON。
     *
     * @param contentType 响应内容类型，响应未声明内容类型时为空
     * @return 内容类型为 JSON 时返回 {@code true}
     */
    private static boolean isJson(Optional<MediaType> contentType) {
        return contentType.map(MediaType.APPLICATION_JSON::isCompatibleWith).orElse(false);
    }

}
