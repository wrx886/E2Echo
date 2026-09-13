package com.github.wrx886.e2echo.server.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.github.wrx886.e2echo.server.result.Result;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 文件接口的集成测试。
 *
 * <p>启动真实 Web 容器后走完整的 HTTP 流程：构造 multipart 请求上传一段随机字节，再按返回的
 * 对象键下载，断言拿到的字节与上传的完全一致；同时覆盖生命周期前缀与失败路径。文件会真实写入
 * 对象存储，用例结束后删除。</p>
 *
 * <p>用例需要对象存储处于可用状态，与其它集成测试依赖 PostgreSQL、Redis 的情况一致。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileControllerTest {

    /**
     * 解析响应结果的 JSON 解析器。
     */
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    /**
     * 测试用 Web 容器端口。
     */
    @LocalServerPort
    private int port;

    /**
     * 对象存储客户端，仅用于清理用例上传的对象。
     */
    @Autowired
    private S3Client s3Client;

    /**
     * 对象存储桶名。
     */
    @Value("${s3.bucket-name}")
    private String bucketName;

    /**
     * 发起 HTTP 请求的客户端。
     */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 本次用例上传的文件 ID，结束后删除对应对象。
     */
    private final List<String> uploadedIds = new ArrayList<>();

    /**
     * 删除本次用例上传的对象。
     */
    @AfterEach
    void tearDown() {
        for (String id : uploadedIds) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(id)
                    .build());
        }
        uploadedIds.clear();
    }

    /**
     * 通过 HTTP 上传一个文件。
     *
     * @param content  文件内容
     * @param filename 文件名
     * @param lifecycle 生命周期，为 {@code null} 时不传该参数
     * @return 上传响应，响应体为统一结果 JSON
     * @throws Exception 请求执行失败
     */
    private HttpResponse<String> upload(byte[] content, String filename, String lifecycle) throws Exception {
        String boundary = "----e2echo" + UUID.randomUUID().toString().replace("-", "");
        String url = "http://localhost:" + port + "/file"
                + (lifecycle == null ? "" : "?lifecycle=" + lifecycle);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(boundary, content, filename)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     * 构造只包含一个文件字段的 multipart 请求体。
     *
     * @param boundary 分隔符
     * @param content  文件内容
     * @param filename 文件名
     * @return 请求体字节
     * @throws Exception 写出失败
     */
    private static byte[] multipartBody(String boundary, byte[] content, String filename) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }

    /**
     * 通过 HTTP 下载文件。
     *
     * @param key 对象键，形如 {@code default/0000018f...}
     * @return 下载响应，响应体为文件原始字节
     * @throws Exception 请求执行失败
     */
    private HttpResponse<byte[]> download(String key) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/file/" + key))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * 解析上传响应，取出其中的对象键。
     *
     * @param response 上传响应
     * @return 对象键，形如 {@code default/0000018f...}
     */
    @SuppressWarnings("unchecked")
    private static String fileIdOf(HttpResponse<String> response) {
        Result<String> result = OBJECT_MAPPER.readValue(response.body(), Result.class);
        return result.data();
    }

    /**
     * 校验上传与下载的完整流程，并确认不传生命周期时默认使用短期存储前缀。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("上传后下载：字节一致，且默认使用 default 前缀")
    void uploadsAndDownloadsFile() throws Exception {
        byte[] content = new byte[2 * 1024 * 1024];
        new SecureRandom().nextBytes(content);

        HttpResponse<String> uploadResponse = upload(content, "test.bin", null);
        assertThat(uploadResponse.statusCode()).isEqualTo(200);
        assertThat(uploadResponse.body()).contains("\"code\":\"0\"");

        String key = fileIdOf(uploadResponse);
        uploadedIds.add(key);
        assertThat(key).startsWith("default/");
        assertThat(key).hasSize("default/".length() + 48);

        HttpResponse<byte[]> downloadResponse = download(key);
        assertThat(downloadResponse.statusCode()).isEqualTo(200);
        assertThat(downloadResponse.headers().firstValue("Content-Type"))
                .contains("application/octet-stream");
        assertThat(downloadResponse.headers().firstValueAsLong("Content-Length"))
                .hasValue(content.length);
        assertThat(downloadResponse.body()).isEqualTo(content);
    }

    /**
     * 校验指定长期存储时对象键带上 forever 前缀，且同样可以下载。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("指定生命周期：对象键带 forever 前缀")
    void uploadsFileWithForeverLifecycle() throws Exception {
        byte[] content = "long term content".getBytes(StandardCharsets.UTF_8);

        HttpResponse<String> uploadResponse = upload(content, "test.bin", "forever");
        assertThat(uploadResponse.statusCode()).isEqualTo(200);

        String key = fileIdOf(uploadResponse);
        uploadedIds.add(key);
        assertThat(key).startsWith("forever/");

        assertThat(download(key).body()).isEqualTo(content);
    }

    /**
     * 校验生命周期取值非法时直接拒绝，不会写入任何对象。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("生命周期非法：返回 code=-1")
    void returnsFailureWhenLifecycleInvalid() throws Exception {
        HttpResponse<String> response = upload("content".getBytes(StandardCharsets.UTF_8), "test.bin", "unknown");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"code\":\"-1\"")
                .contains("生命周期只能是 default 或 forever：unknown");
    }

    /**
     * 校验下载不存在的文件时返回统一失败结果，而不是错误页。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("下载不存在的文件：返回 code=-1 与文件不存在提示")
    void returnsFailureWhenFileMissing() throws Exception {
        String missingKey = "default/" + "0".repeat(48);

        HttpResponse<byte[]> response = download(missingKey);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8))
                .contains("\"code\":\"-1\"")
                .contains("文件不存在：" + missingKey);
    }

    /**
     * 校验上传空文件时直接拒绝，不产生空对象。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("上传空文件：返回 code=-1 与上传文件不能为空提示")
    void returnsFailureWhenFileEmpty() throws Exception {
        HttpResponse<String> response = upload(new byte[0], "empty.bin", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"code\":\"-1\"")
                .contains("上传文件不能为空！");
    }

}
