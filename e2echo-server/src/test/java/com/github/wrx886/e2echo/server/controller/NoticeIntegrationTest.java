package com.github.wrx886.e2echo.server.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import com.github.wrx886.e2echo.ecc.Ecc;
import com.github.wrx886.e2echo.ecc.EccMessage;
import com.github.wrx886.e2echo.ecc.util.EccUtil.KeyPairHex;
import com.github.wrx886.e2echo.server.common.RedisPrefix;
import com.github.wrx886.e2echo.server.repository.MessageRepository;
import com.github.wrx886.e2echo.server.service.NoticeService;

/**
 * 通知功能的集成测试。
 *
 * <p>启动真实的 Web 容器后，用 HTTP 客户端建立真实的 SSE 长连接，验证通知从产生到送达客户端
 * 的完整链路：连接注册、Redis 中的订阅关系、建连推送、通知发布、客户端收到推送。建连推送与后续
 * 通知的推送内容一致，因此判断通知是否送达时只看触发通知之后新增的内容。</p>
 *
 * <p>用例数据与线上一致：连接使用随机的客户端 ID 与订阅目标，保存消息时使用
 * {@link Ecc#generateKeyPair()} 生成的真实密钥对。用例结束后会关闭连接、删除 Redis 中的订阅
 * 关系与数据库中的消息，不在环境里留下数据。</p>
 */
// 关停时上下文会在用例结束后立即关闭：NoticeService 监听 ContextClosedEvent 先结束 SSE 连接，
// 所以即便 Web 容器是默认的优雅关停，也不会为等待长连接而阻塞
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NoticeIntegrationTest {

    /**
     * 等待推送与状态变更的最长时间（毫秒）。
     */
    private static final long TIMEOUT_MILLIS = 10_000L;

    /**
     * 后台读取 SSE 响应用的线程池。
     */
    private static final ExecutorService READERS = Executors.newCachedThreadPool();

    /**
     * 测试用 Web 容器端口。
     */
    @LocalServerPort
    private int port;

    /**
     * 待测通知业务逻辑对象。
     */
    @Autowired
    private NoticeService noticeService;

    /**
     * 消息接口，用于验证「保存消息后通知订阅者」的完整链路。
     */
    @Autowired
    private MessageController messageController;

    /**
     * 消息数据访问对象，用于清理用例中保存的消息。
     */
    @Autowired
    private MessageRepository messageRepository;

    /**
     * Redis 操作对象，用于核对订阅关系。
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发起 HTTP 请求的客户端。
     */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 本次用例建立的 SSE 连接。
     */
    private final List<SseConnection> connections = new ArrayList<>();

    /**
     * 本次用例创建并写入 Redis 的键。
     */
    private final List<String> createdKeys = new ArrayList<>();

    /**
     * 本次用例保存的消息 ID。
     */
    private final List<String> createdMessageIds = new ArrayList<>();

    /**
     * 结束后台读取线程池。
     */
    @AfterAll
    static void shutdownReaders() {
        READERS.shutdownNow();
    }

    /**
     * 关闭连接并清理本次用例产生的 Redis 数据、消息数据与 {@link Ecc} 中的密钥对。
     */
    @AfterEach
    void tearDown() {
        connections.forEach(SseConnection::close);
        connections.clear();

        if (!createdKeys.isEmpty()) {
            stringRedisTemplate.delete(createdKeys);
            createdKeys.clear();
        }
        if (!createdMessageIds.isEmpty()) {
            createdMessageIds.forEach(messageId -> messageRepository.findByMessageId(messageId)
                    .ifPresent(messageRepository::delete));
            createdMessageIds.clear();
        }
        Ecc.clear();
    }

    /**
     * 建立一条 SSE 连接，订阅指定目标。
     *
     * <p>连接建立后先等待订阅关系出现在 Redis 中再返回：{@code connect} 是同步写入订阅关系的，
     * 等到它出现说明服务端已经处理完连接请求，后续的通知不会早于订阅生效。</p>
     *
     * @param clientOriginal 客户端原始 ID
     * @param to             订阅的目标
     * @return 连接句柄
     * @throws Exception 请求失败，或等待订阅关系写入、建连推送到达超时
     */
    private SseConnection connect(String clientOriginal, String to) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/notice/connect/" + clientOriginal))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("[\"" + to + "\"]"))
                .build();

        SseConnection connection = new SseConnection(
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()));
        connections.add(connection);

        String client = awaitSubscriber(to);
        assertThat(client).as("订阅关系应写入 Redis").isNotNull();

        connection.bind(client);
        createdKeys.add(RedisPrefix.CLIENT2TOS + client);
        createdKeys.add(RedisPrefix.TO2CLIENTS + to);
        connection.startReading();

        // 等建连推送到达再返回：用例拿「已收内容长度」当界判断后续通知是否送达，这个界必须落在
        // 建连推送之后，否则断言会被它满足；推送本身由「建立连接：客户端立即收到首条推送」验证
        connection.awaitContent("reflash", TIMEOUT_MILLIS);

        return connection;
    }

    /**
     * 等待订阅目标下出现客户端。
     *
     * @param to 订阅的目标
     * @return 带实例前缀的客户端 ID，超时返回 {@code null}
     * @throws InterruptedException 等待被中断
     */
    private String awaitSubscriber(String to) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            Set<String> members = stringRedisTemplate.opsForSet().members(RedisPrefix.TO2CLIENTS + to);
            if (members != null && !members.isEmpty()) {
                return members.iterator().next();
            }
            Thread.sleep(20);
        }
        return null;
    }

    /**
     * 触发通知并等待客户端收到新的推送。
     *
     * <p>建连时服务端已经推过一条通知，因此以触发通知前的已收内容为界，只判断之后新增的内容。</p>
     *
     * @param to         通知目标
     * @param connection 期望收到推送的连接
     * @throws Exception 等待被中断
     */
    private void noticeAndAwait(String to, SseConnection connection) throws Exception {
        int mark = connection.receivedSize();
        noticeService.notice(to);

        assertThat(connection.awaitContent("reflash", mark, TIMEOUT_MILLIS))
                .as("客户端应收到推送，实际收到：%s", connection.received())
                .isTrue();
    }

    /**
     * 生成一个本次用例独有的订阅目标。
     *
     * @return 随机订阅目标
     */
    private static String uniqueTo() {
        return "test-to-" + UUID.randomUUID();
    }

    /**
     * 生成一个本次用例独有的客户端 ID。
     *
     * @return 随机客户端 ID
     */
    private static String uniqueClient() {
        return "test-client-" + UUID.randomUUID();
    }

    /**
     * 建立连接的测试。
     */
    @Test
    @DisplayName("建立连接：订阅关系双向写入 Redis")
    void writesSubscriptionIntoRedis() throws Exception {
        String to = uniqueTo();
        SseConnection connection = connect(uniqueClient(), to);

        assertThat(stringRedisTemplate.opsForSet()
                .isMember(RedisPrefix.CLIENT2TOS + connection.client(), to)).isTrue();
        assertThat(stringRedisTemplate.opsForSet()
                .isMember(RedisPrefix.TO2CLIENTS + to, connection.client())).isTrue();
    }

    /**
     * 建连推送的测试：连接建立后不触发任何通知，客户端就应收到服务端在注册完成后立即推送的通知。
     */
    @Test
    @DisplayName("建立连接：客户端立即收到首条推送")
    void pushesNoticeOnConnect() throws Exception {
        SseConnection connection = connect(uniqueClient(), uniqueTo());

        assertThat(connection.awaitContent("reflash", TIMEOUT_MILLIS))
                .as("建连后应立即收到首条推送，实际收到：%s", connection.received())
                .isTrue();
    }

    /**
     * 推送通知的测试：通知订阅目标后，订阅它的客户端应通过 SSE 收到新的推送。
     */
    @Test
    @DisplayName("通知订阅目标：客户端收到推送")
    void pushesNoticeToSubscriber() throws Exception {
        String to = uniqueTo();
        SseConnection connection = connect(uniqueClient(), to);

        noticeAndAwait(to, connection);
    }

    /**
     * 保存消息触发通知的测试：保存一条接收者为某客户端的消息后，该客户端应收到推送。
     *
     * <p>覆盖的是完整链路：消息接口保存消息，业务层调用通知服务，通知经 Redis 发布订阅转发到
     * 持有连接的实例，最终推送给客户端。</p>
     *
     * @throws Exception 密钥生成、消息加密或请求执行失败
     */
    @Test
    @DisplayName("保存消息：订阅接收者的客户端收到通知")
    void pushesNoticeWhenMessageSaved() throws Exception {
        KeyPairHex senderKeyPair = Ecc.generateKeyPair();
        KeyPairHex receiverKeyPair = Ecc.generateKeyPair();
        Ecc.store(senderKeyPair.publicKeyHex(), senderKeyPair.privateKeyHex());

        // 私聊消息的接收者就是订阅目标
        String to = receiverKeyPair.publicKeyHex();
        SseConnection connection = connect(uniqueClient(), to);

        // 建连时已经收到过一条推送，只判断保存消息之后新增的内容
        int mark = connection.receivedSize();

        EccMessage plain = new EccMessage();
        plain.setFrom(senderKeyPair.publicKeyHex());
        plain.setTo(to);
        plain.setMessage("hello, e2echo");
        plain.setType("text");
        plain.setChannel("private");
        plain.setInfo("extra-info");

        EccMessage message = Ecc.encrypt(plain);
        createdMessageIds.add(message.getId());

        assertThat(messageController.save(message).code()).isEqualTo("0");

        assertThat(connection.awaitContent("reflash", mark, TIMEOUT_MILLIS))
                .as("保存消息后订阅者应收到通知，实际收到：%s", connection.received())
                .isTrue();
    }

    /**
     * 一条 SSE 连接的句柄：后台读取服务端推送的内容，供用例等待与断言。
     */
    private static class SseConnection {

        /**
         * 响应对象，收到响应头后才可用。
         */
        private final CompletableFuture<HttpResponse<InputStream>> response;

        /**
         * 已收到的推送内容。
         */
        private final StringBuilder received = new StringBuilder();

        /**
         * 带实例前缀的客户端 ID。
         */
        private String client;

        /**
         * 响应体，用于关闭连接。
         */
        private volatile InputStream body;

        /**
         * 后台读取任务。
         */
        private Future<?> reader;

        /**
         * 使用响应对象构造连接。
         *
         * @param response SSE 响应
         */
        SseConnection(CompletableFuture<HttpResponse<InputStream>> response) {
            this.response = response;
        }

        /**
         * 记录带实例前缀的客户端 ID。
         *
         * @param client 带实例前缀的客户端 ID
         */
        void bind(String client) {
            this.client = client;
        }

        /**
         * 获取带实例前缀的客户端 ID。
         *
         * @return 带实例前缀的客户端 ID
         */
        String client() {
            return client;
        }

        /**
         * 启动后台读取，持续读取推送内容直到连接关闭。
         */
        void startReading() {
            reader = READERS.submit(() -> {
                try (InputStream in = response.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).body()) {
                    body = in;
                    byte[] buffer = new byte[256];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        synchronized (received) {
                            received.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                        }
                    }
                } catch (Exception e) {
                    // 连接关闭或读取被取消时会走到这里，用例结束前无需处理
                }
            });
        }

        /**
         * 等待收到包含指定内容的数据。
         *
         * @param content       期望出现的内容
         * @param timeoutMillis 最长等待时间（毫秒）
         * @return 收到返回 {@code true}，超时返回 {@code false}
         * @throws InterruptedException 等待被中断
         */
        boolean awaitContent(String content, long timeoutMillis) throws InterruptedException {
            return awaitContent(content, 0, timeoutMillis);
        }

        /**
         * 等待新收到包含指定内容的数据。
         *
         * <p>只在 {@code fromIndex} 之后的内容里查找：建连时服务端已经推过一条通知，判断后续通知
         * 是否送达必须跳过这段已有内容，否则断言会被建连推送直接满足。</p>
         *
         * @param content       期望出现的内容
         * @param fromIndex     从该偏移之后开始查找，取触发通知前的 {@link #receivedSize()}
         * @param timeoutMillis 最长等待时间（毫秒）
         * @return 收到返回 {@code true}，超时返回 {@code false}
         * @throws InterruptedException 等待被中断
         */
        boolean awaitContent(String content, int fromIndex, long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                synchronized (received) {
                    if (received.indexOf(content, fromIndex) >= 0) {
                        return true;
                    }
                }
                Thread.sleep(20);
            }
            return false;
        }

        /**
         * 获取已收内容的长度，作为「只看之后新增内容」的起点。
         *
         * @return 已收到的字符数
         */
        int receivedSize() {
            synchronized (received) {
                return received.length();
            }
        }

        /**
         * 获取已收到的推送内容，用于断言失败时输出实际结果。
         *
         * @return 已收到的内容
         */
        String received() {
            synchronized (received) {
                return received.toString();
            }
        }

        /**
         * 关闭连接并停止后台读取。
         */
        void close() {
            if (reader != null) {
                reader.cancel(true);
            }
            InputStream in = body;
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // 关闭失败无需处理
                }
            }
        }

    }

}
