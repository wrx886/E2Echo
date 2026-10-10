package com.github.wrx886.e2echo.client.api;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import com.github.wrx886.e2echo.client.common.Const;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.util.ApiUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
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
     * <p>上传前先校验文件大小，超过 {@link Const#FILE_MAX_SIZE_BYTE} 的文件直接拒收，不发起请求。</p>
     *
     * @param file      待上传的文件，内容应为加密后的密文
     * @param lifecycle 生命周期，取 {@link #LIFECYCLE_DEFAULT} 或 {@link #LIFECYCLE_FOREVER}，
     *                  为空时按短期存储处理
     * @return 对象键，形如 {@code default/文件 ID}，下载时原样传回
     * @throws E2EchoException 文件不存在、超过大小上限，请求失败，或服务端返回失败状态
     *                         （例如文件为空、生命周期非法）
     */
    public String upload(File file, String lifecycle) {
        // 文件大小校验：超限的直接拒收，避免白白上传一遍
        if (file == null || !file.isFile()) {
            throw new E2EchoException("非文件或文件不存在");
        }
        if (file.length() > Const.FILE_MAX_SIZE_BYTE) {
            throw new E2EchoException(Const.FILE_MAX_SIZE_MESSAGE);
        }

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
     * 上传文件（直接用调用方给的流，不落临时文件）。
     *
     * <p>与 {@link #upload(File, String)} 的区别只是文件内容从哪来：这里直接把 {@code MultipartFile}
     * 的内容作为请求体的一部分发出去，适合"文件已经在上传缓冲区里、不需要先写一份到磁盘"的场景；
     * 大小校验、生命周期、返回对象键的语义都一致。</p>
     *
     * @param file      待上传的文件，内容应为加密后的密文
     * @param lifecycle 生命周期，取 {@link #LIFECYCLE_DEFAULT} 或 {@link #LIFECYCLE_FOREVER}，
     *                  为空时按短期存储处理
     * @return 对象键，形如 {@code default/文件 ID}，下载时原样传回
     * @throws E2EchoException 文件为空、超过大小上限，请求失败，或服务端返回失败状态
     *                         （例如生命周期非法）
     */
    public String upload(MultipartFile file, String lifecycle) {
        // 文件大小校验：超限的直接拒收，避免白白上传一遍
        if (file == null || file.isEmpty()) {
            throw new E2EchoException("文件为空！");
        }
        if (file.getSize() > Const.FILE_MAX_SIZE_BYTE) {
            throw new E2EchoException(Const.FILE_MAX_SIZE_MESSAGE);
        }

        timestampApi.checkTime();

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
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
     * <p>下载前先按响应头里的 {@code Content-Length} 校验大小：拿不到长度、或者超过
     * {@link Const#FILE_MAX_SIZE_BYTE} 的直接拒收，一个字节都不写（拿不到长度时不冒险下载，
     * 免得白下载一个超大文件）。</p>
     *
     * @param lifecycle 生命周期前缀，与上传时一致
     * @param id        文件 ID
     * @param target    保存到的文件
     * @throws E2EchoException 文件超过大小上限，请求失败，或服务端返回失败状态（例如文件不存在）
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
                    return checkDownloadSize(response.headers())
                            .then(DataBufferUtils.write(response.bodyToFlux(DataBuffer.class), target.toPath()));
                })
                .block());
    }

    /**
     * 下载文件到输出流（使用上传时返回的对象键，不落临时文件）。
     *
     * @param objectKey    上传时返回的对象键，形如 {@code default/文件 ID}
     * @param outputStream 内容写入的输出流，由调用方负责关闭
     * @throws E2EchoException 对象键格式错误、文件超过大小上限、请求失败，或服务端返回失败状态
     */
    public void download(String objectKey, OutputStream outputStream) {

        int index = objectKey == null ? -1 : objectKey.indexOf('/');
        if (index < 1 || index == objectKey.length() - 1) {
            throw new E2EchoException("对象键格式错误：" + objectKey);
        }
        download(objectKey.substring(0, index), objectKey.substring(index + 1), outputStream);
    }

    /**
     * 下载文件到输出流（按生命周期前缀与文件 ID，不落临时文件）。
     *
     * <p>与写文件版本的区别只是内容去哪：这里直接写进调用方给的输出流，适合"拿到内容就要转发出去"
     * 的场景（例如直接写进 HTTP 响应），不用先在磁盘上落一份。大小校验一致：拿不到
     * {@code Content-Length}、或者超过 {@link Const#FILE_MAX_SIZE_BYTE} 的直接拒收，一个字节都不写；
     * 输出流由调用方负责关闭，本方法不关。</p>
     *
     * @param lifecycle    生命周期前缀，与上传时一致
     * @param id           文件 ID
     * @param outputStream 内容写入的输出流，由调用方负责关闭
     * @throws E2EchoException 文件超过大小上限，请求失败，或服务端返回失败状态（例如文件不存在）
     */
    public void download(String lifecycle, String id, OutputStream outputStream) {
        timestampApi.checkTime();

        ApiUtil.apiCall(() -> webClient.get()
                .uri("file/{lifecycle}/{id}", lifecycle, id)
                .exchangeToMono(response -> {
                    // 失败时服务端返回的是 JSON（状态码仍为 200），按响应类型区分，避免把 JSON 写进流
                    if (isJson(response.headers().contentType())) {
                        return response.bodyToMono(new ParameterizedTypeReference<Result<Void>>() {
                        }).flatMap(result -> Mono.error(new E2EchoException(
                                result.message() == null ? "下载失败！" : result.message())));
                    }
                    if (response.statusCode().isError()) {
                        return Mono.error(new E2EchoException("服务器状态异常！"));
                    }
                    return checkDownloadSize(response.headers())
                            // 写流的重载返回的是 Flux（每个 DataBuffer 都会发出），这里只关心写完
                            .then(DataBufferUtils.write(response.bodyToFlux(DataBuffer.class), outputStream).then());
                })
                .block());
    }

    /**
     * 下载文件（使用上传时返回的对象键）。
     *
     * @param objectKey 上传时返回的对象键，形如 {@code default/文件 ID}
     * @param target    保存到的文件
     * @throws E2EchoException 对象键格式错误、文件超过大小上限、请求失败，或服务端返回失败状态
     */
    public void download(String objectKey, File target) {

        int index = objectKey == null ? -1 : objectKey.indexOf('/');
        if (index < 1 || index == objectKey.length() - 1) {
            throw new E2EchoException("对象键格式错误：" + objectKey);
        }
        download(objectKey.substring(0, index), objectKey.substring(index + 1), target);
    }

    /**
     * 透传下载（使用上传时返回的对象键），把范围与响应原样转给浏览器。
     *
     * @param objectKey   上传时返回的对象键，形如 {@code default/文件 ID}
     * @param rangeHeader 浏览器的 {@code Range} 请求头，未带时为空
     * @param response    待写入的 Servlet 响应
     * @throws E2EchoException 对象键格式错误、文件超过大小上限、请求失败，或服务端返回失败状态
     */
    public void download(String objectKey, String rangeHeader, HttpServletResponse response) {

        int index = objectKey == null ? -1 : objectKey.indexOf('/');
        if (index < 1 || index == objectKey.length() - 1) {
            throw new E2EchoException("对象键格式错误：" + objectKey);
        }
        download(objectKey.substring(0, index), objectKey.substring(index + 1), rangeHeader, response);
    }

    /**
     * 透传下载（按生命周期前缀与文件 ID），把服务端的响应原样转给浏览器。
     *
     * <p>用于本地没有副本、要直接把对象存储的内容转发出去的场景（广播附件）：浏览器的 {@code Range}
     * 头原样转给服务端，再把服务端的状态码与 Range 相关响应头镜像回来，内容边收边写，所以里面的视频、
     * 音频也能拖动进度条，且一个字节都不会落盘。</p>
     *
     * <p>响应类型与文件名由调用方在进入本方法前设置，本方法只负责状态码、{@code Content-Range}、
     * {@code Content-Length}、{@code Accept-Ranges} 与响应体。</p>
     *
     * @param lifecycle   生命周期前缀，与上传时一致
     * @param id          文件 ID
     * @param rangeHeader 浏览器的 {@code Range} 请求头，未带时为空
     * @param response    待写入的 Servlet 响应
     * @throws E2EchoException 文件超过大小上限、请求失败，或服务端返回失败状态（例如文件不存在）
     */
    public void download(String lifecycle, String id, String rangeHeader, HttpServletResponse response) {
        timestampApi.checkTime();

        OutputStream outputStream;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            throw new E2EchoException("下载失败！");
        }

        ApiUtil.apiCall(() -> webClient.get()
                .uri("file/{lifecycle}/{id}", lifecycle, id)
                .headers(headers -> {
                    if (StringUtils.hasText(rangeHeader)) {
                        headers.set(HttpHeaders.RANGE, rangeHeader);
                    }
                })
                .exchangeToMono(clientResponse -> writeResponse(clientResponse, response, outputStream))
                .block());
    }

    /**
     * 把服务端的下载响应写进 Servlet 响应：镜像状态与 Range 相关响应头，内容边收边写。
     *
     * @param clientResponse 服务端响应
     * @param response       待写入的 Servlet 响应
     * @param outputStream   Servlet 响应的输出流
     * @return 写完后的空流
     */
    private static Mono<Void> writeResponse(ClientResponse clientResponse, HttpServletResponse response,
            OutputStream outputStream) {
        // 失败时服务端返回的是 JSON（状态码仍为 200），按响应类型区分，避免把 JSON 写进响应
        if (isJson(clientResponse.headers().contentType())) {
            return clientResponse.bodyToMono(new ParameterizedTypeReference<Result<Void>>() {
            }).flatMap(result -> Mono.error(new E2EchoException(
                    result.message() == null ? "下载失败！" : result.message())));
        }

        HttpStatusCode status = clientResponse.statusCode();
        HttpHeaders headers = clientResponse.headers().asHttpHeaders();

        // 范围不可满足：服务端没有内容，只把状态与 Content-Range 转给浏览器
        if (status.value() == HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value()) {
            mirrorRangeHeaders(headers, response);
            response.setStatus(status.value());
            return Mono.empty();
        }
        if (status.isError()) {
            return Mono.error(new E2EchoException("服务器状态异常！"));
        }
        return checkDownloadSize(clientResponse.headers())
                // 先写响应头与状态码，再收内容，免得状态码还没定就先写出了内容
                .then(Mono.fromRunnable(() -> {
                    mirrorRangeHeaders(headers, response);
                    response.setStatus(status.value());
                }))
                .then(DataBufferUtils.write(clientResponse.bodyToFlux(DataBuffer.class), outputStream).then());
    }

    /**
     * 把服务端响应里的 Range 相关响应头原样转给浏览器。
     *
     * @param headers  服务端响应头
     * @param response 待写入的 Servlet 响应
     */
    private static void mirrorRangeHeaders(HttpHeaders headers, HttpServletResponse response) {
        for (String name : List.of(HttpHeaders.CONTENT_RANGE, HttpHeaders.CONTENT_LENGTH, HttpHeaders.ACCEPT_RANGES)) {
            List<String> values = headers.get(name);
            if (values != null) {
                values.forEach(value -> response.addHeader(name, value));
            }
        }
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

    /**
     * 校验下载响应的大小。
     *
     * <p>直接用响应头里的 {@code Content-Length} 判断，好在写盘、写流之前就拒收：拿不到长度、或者
     * 超过 {@link Const#FILE_MAX_SIZE_BYTE} 都算失败（拿不到长度时不冒险下载，免得白下载一个超大
     * 文件）。</p>
     *
     * @param headers 下载响应头
     * @return 校验通过返回空流，否则返回携带失败原因的流
     */
    private static Mono<Void> checkDownloadSize(ClientResponse.Headers headers) {
        OptionalLong contentLength = headers.contentLength();
        if (contentLength.isEmpty() || contentLength.getAsLong() > Const.FILE_MAX_SIZE_BYTE) {
            return Mono.error(new E2EchoException(Const.FILE_MAX_SIZE_MESSAGE));
        }
        return Mono.empty();
    }

}
