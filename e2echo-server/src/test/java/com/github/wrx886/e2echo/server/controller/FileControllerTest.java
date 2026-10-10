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
import java.util.Arrays;
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
        return download(key, null);
    }

    /**
     * 通过 HTTP 下载文件，可指定范围。
     *
     * @param key   对象键，形如 {@code default/0000018f...}
     * @param range Range 请求头；为 {@code null} 时不带该头
     * @return 下载响应，响应体为服务端返回的字节
     * @throws Exception 请求执行失败
     */
    private HttpResponse<byte[]> download(String key, String range) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/file/" + key));
        if (range != null) {
            builder.header("Range", range);
        }
        return httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * 通过 HTTP 发送 HEAD 请求。
     *
     * @param key 对象键，形如 {@code default/0000018f...}
     * @return HEAD 响应，响应体为空
     * @throws Exception 请求执行失败
     */
    private HttpResponse<byte[]> head(String key) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/file/" + key))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
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
        // 整体下载也要声明支持 Range，客户端据此决定是否可以按范围请求
        assertThat(downloadResponse.headers().firstValue("Accept-Ranges")).contains("bytes");
        assertThat(downloadResponse.headers().firstValueAsLong("Content-Length"))
                .hasValue(content.length);
        assertThat(downloadResponse.body()).isEqualTo(content);
    }

    /**
     * 校验范围下载：指定区间、后缀区间与开区间都只返回对应的字节，并带上 206、Content-Range
     * 与片段长度。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("按范围下载：返回 206 与请求的字节片段")
    void downloadsRequestedRange() throws Exception {
        byte[] content = new byte[1024];
        new SecureRandom().nextBytes(content);

        String key = fileIdOf(upload(content, "range.bin", null));
        uploadedIds.add(key);

        // 指定区间
        HttpResponse<byte[]> ranged = download(key, "bytes=100-199");
        assertThat(ranged.statusCode()).isEqualTo(206);
        assertThat(ranged.headers().firstValue("Content-Range"))
                .contains("bytes 100-199/" + content.length);
        assertThat(ranged.headers().firstValue("Accept-Ranges")).contains("bytes");
        assertThat(ranged.headers().firstValueAsLong("Content-Length")).hasValue(100);
        assertThat(ranged.body()).isEqualTo(Arrays.copyOfRange(content, 100, 200));

        // 后缀区间：最后 50 字节
        HttpResponse<byte[]> suffix = download(key, "bytes=-50");
        assertThat(suffix.statusCode()).isEqualTo(206);
        assertThat(suffix.headers().firstValue("Content-Range"))
                .contains("bytes " + (content.length - 50) + "-" + (content.length - 1) + "/" + content.length);
        assertThat(suffix.body()).isEqualTo(Arrays.copyOfRange(content, content.length - 50, content.length));

        // 开区间：从 1000 到末尾
        HttpResponse<byte[]> open = download(key, "bytes=1000-");
        assertThat(open.statusCode()).isEqualTo(206);
        assertThat(open.body()).isEqualTo(Arrays.copyOfRange(content, 1000, content.length));
    }

    /**
     * 校验范围不可满足时返回 416，多段与格式非法的 Range 按规范忽略、整体返回。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("范围越界：返回 416；多段与非法 Range 整体返回")
    void handlesUnsatisfiableAndIgnoredRanges() throws Exception {
        byte[] content = "range content".getBytes(StandardCharsets.UTF_8);

        String key = fileIdOf(upload(content, "range.bin", null));
        uploadedIds.add(key);

        HttpResponse<byte[]> outOfRange = download(key, "bytes=100-200");
        assertThat(outOfRange.statusCode()).isEqualTo(416);
        assertThat(outOfRange.headers().firstValue("Content-Range"))
                .contains("bytes */" + content.length);

        // 多段范围与格式非法的 Range 都不按范围处理
        HttpResponse<byte[]> multiRange = download(key, "bytes=0-3,6-8");
        assertThat(multiRange.statusCode()).isEqualTo(200);
        assertThat(multiRange.body()).isEqualTo(content);

        HttpResponse<byte[]> invalidRange = download(key, "bytes=abc");
        assertThat(invalidRange.statusCode()).isEqualTo(200);
        assertThat(invalidRange.body()).isEqualTo(content);
    }

    /**
     * 校验 HEAD 只返回元信息：带上长度与 Accept-Ranges，不返回内容。
     *
     * @throws Exception 请求执行失败
     */
    @Test
    @DisplayName("HEAD：只返回长度与 Range 支持标记")
    void headsFileMetadata() throws Exception {
        byte[] content = "head content".getBytes(StandardCharsets.UTF_8);

        String key = fileIdOf(upload(content, "head.bin", null));
        uploadedIds.add(key);

        HttpResponse<byte[]> response = head(key);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValueAsLong("Content-Length")).hasValue(content.length);
        assertThat(response.headers().firstValue("Accept-Ranges")).contains("bytes");
        assertThat(response.body()).isEmpty();
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
