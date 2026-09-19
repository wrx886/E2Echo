package com.github.wrx886.e2echo.server.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import com.github.wrx886.e2echo.ecc.Ecc;
import com.github.wrx886.e2echo.ecc.EccMessage;
import com.github.wrx886.e2echo.ecc.util.EccUtil;
import com.github.wrx886.e2echo.ecc.util.EccUtil.KeyPairHex;
import com.github.wrx886.e2echo.server.entity.Message;
import com.github.wrx886.e2echo.server.exception.E2EchoException;
import com.github.wrx886.e2echo.server.repository.MessageRepository;
import com.github.wrx886.e2echo.server.result.Result;

/**
 * {@link MessageController} 的测试。
 *
 * <p>启动完整的 Spring 上下文后直接调用控制器方法，因此会真实经过
 * {@code MessageService}、{@code MessageRepository} 与数据库，插入的数据可以在库中查到。</p>
 *
 * <p>测试数据不使用伪造的公钥与签名，而是由 {@link Ecc#generateKeyPair()} 生成真实密钥对，
 * 并按客户端发送私聊消息的真实链路生成消息，使请求数据与线上完全一致。</p>
 *
 * <p>测试会在自己的方法内创建数据，并在方法结束后清理，不会在库中残留记录；查询类用例
 * 一律按本次测试独有的公钥过滤，因此不会受到库中已有数据的影响。</p>
 */
@SpringBootTest
class MessageControllerTest {

    /**
     * 消息通道，仅用于填装测试数据。
     */
    private static final String PRIVATE_CHANNEL = "private";

    /**
     * 用户信息消息的通道，用于验证「取某个用户最新用户信息」的场景。
     */
    private static final String USERINFO_CHANNEL = "USERINFO";

    /**
     * 待测控制器。
     */
    @Autowired
    private MessageController messageController;

    /**
     * 消息数据访问对象，用于直接核对数据库中的记录。
     */
    @Autowired
    private MessageRepository messageRepository;

    /**
     * 本次测试创建的发送者密钥对。
     */
    private KeyPairHex senderKeyPair;

    /**
     * 本次测试创建的接收者密钥对，每次测试都不同，用于把查询限定在本次测试的数据上。
     */
    private KeyPairHex receiverKeyPair;

    /**
     * 本次测试写入数据库的消息 ID，测试结束后据此清理。
     */
    private final List<String> createdIds = new ArrayList<>();

    /**
     * 生成本次测试使用的密钥对，并把发送者密钥对保存到 {@link Ecc} 中，使后续能够以发送者
     * 身份加密与签名。
     *
     * @throws Exception 密钥生成失败
     */
    @BeforeEach
    void setUp() throws Exception {
        senderKeyPair = Ecc.generateKeyPair();
        receiverKeyPair = Ecc.generateKeyPair();
        Ecc.store(senderKeyPair.publicKeyHex(), senderKeyPair.privateKeyHex());
    }

    /**
     * 清理本次测试写入的数据与 {@link Ecc} 中保存的静态密钥对。
     */
    @AfterEach
    void tearDown() {
        createdIds.forEach(messageId -> messageRepository.findByMessageId(messageId)
                .ifPresent(messageRepository::delete));
        createdIds.clear();
        Ecc.clear();
    }

    /**
     * 按客户端发送私聊消息的真实链路生成一条消息：用接收者公钥加密正文，用发送者私钥签名，
     * 并生成真实的 48 位消息 ID。
     *
     * @param plainText 消息明文
     * @return 加密并签名后的消息，已登记 ID 以便测试结束后清理
     * @throws Exception 加密或签名失败
     */
    private EccMessage newMessage(String plainText) throws Exception {
        EccMessage plain = new EccMessage();
        plain.setFrom(senderKeyPair.publicKeyHex());
        plain.setTo(receiverKeyPair.publicKeyHex());
        plain.setMessage(plainText);
        plain.setType("text");
        plain.setChannel(PRIVATE_CHANNEL);
        plain.setInfo("extra-info");

        EccMessage sent = Ecc.encrypt(plain);
        createdIds.add(sent.getId());
        return sent;
    }

    /**
     * 直接向数据库写入一条消息，用于构造查询用例所需的、时间可控的数据。
     *
     * @param timestamp 消息时间戳（毫秒）
     * @param sequence  同一次测试内的序号，用于保证 ID 有序且不重复
     * @param channel   消息通道
     * @return 写入数据库的消息实体
     */
    private Message persistMessage(long timestamp, int sequence, String channel) {
        // ID 前 16 位是十六进制时间戳，后 32 位保证同一次测试内不重复，且序号部分保证 ID 有序
        String id = String.format("%016x", timestamp)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 24)
                + String.format("%08x", sequence);

        Message message = new Message();
        message.setMessageId(id);
        message.setFrom(senderKeyPair.publicKeyHex());
        message.setTo(receiverKeyPair.publicKeyHex());
        message.setMessage("cipher-text");
        message.setType("text");
        message.setChannel(channel);
        message.setInfo("extra-info");
        message.setSign("sign");
        message.setTimestamp(timestamp);

        createdIds.add(id);
        return messageRepository.save(message);
    }

    /**
     * 按本次测试的接收者公钥与条件分页查询，把结果限定在本次测试写入的数据上。
     *
     * @param channel        消息通道，可为 {@code null}
     * @param startTimestamp 起始时间戳（毫秒），可为 {@code null}
     * @param endTimestamp   结束时间戳（毫秒），可为 {@code null}
     * @param startId        起始消息 ID，可为 {@code null}
     * @param order          排序方向，可为 {@code null}（默认升序）
     * @param pageNum        页码，从 1 开始
     * @param pageSize       每页条数
     * @return 分页查询结果
     */
    private Page<EccMessage> list(String channel, String startTimestamp, String endTimestamp,
            String startId, String order, int pageNum, int pageSize) {
        return messageController.list(
                null,
                List.of(receiverKeyPair.publicKeyHex()),
                channel,
                startTimestamp,
                endTimestamp,
                startId,
                order,
                pageNum,
                pageSize).data();
    }

    /**
     * 消息保存接口的测试。
     */
    @Nested
    @DisplayName("保存消息")
    class Save {

        /**
         * 校验保存成功后消息确实写入了数据库，且各字段与消息 ID 中的时间戳都被正确保存。
         *
         * @throws Exception 消息生成失败
         */
        @Test
        @DisplayName("保存成功：返回成功结果，并在数据库中生成记录")
        void savesMessageIntoDatabase() throws Exception {
            EccMessage sent = newMessage("hello, e2echo");

            Result<EccMessage> result = messageController.save(sent);

            assertThat(result.code()).isEqualTo("0");
            assertThat(result.message()).isEqualTo("OK");
            assertThat(result.data()).isEqualTo(sent);

            Message stored = messageRepository.findByMessageId(sent.getId()).orElse(null);
            assertThat(stored).isNotNull();
            assertThat(stored.getMessageId()).isEqualTo(sent.getId());
            assertThat(stored.getFrom()).isEqualTo(senderKeyPair.publicKeyHex());
            assertThat(stored.getTo()).isEqualTo(receiverKeyPair.publicKeyHex());
            assertThat(stored.getMessage()).isEqualTo(sent.getMessage());
            assertThat(stored.getType()).isEqualTo("text");
            assertThat(stored.getChannel()).isEqualTo(PRIVATE_CHANNEL);
            assertThat(stored.getInfo()).isEqualTo("extra-info");
            assertThat(stored.getSign()).isEqualTo(sent.getSign());
            // 时间戳取自消息 ID 前 16 位十六进制数，用于后续按时间查询
            assertThat(stored.getTimestamp())
                    .isEqualTo(Long.parseLong(sent.getId().substring(0, 16), 16));
            // 审计字段由 JPA 审计自动填充
            assertThat(stored.getCreateTime()).isNotNull();
            assertThat(stored.getUpdateTime()).isNotNull();
        }

        /**
         * 校验正文被篡改后签名失效，消息不会被写入数据库。
         *
         * @throws Exception 消息生成失败
         */
        @Test
        @DisplayName("签名校验失败：拒绝保存，且数据库中没有新记录")
        void rejectsTamperedMessage() throws Exception {
            EccMessage tampered = newMessage("hello, e2echo");
            tampered.setMessage(tampered.getMessage() + "00");

            assertThatThrownBy(() -> messageController.save(tampered))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("消息校验失败！");

            assertThat(messageRepository.findByMessageId(tampered.getId())).isEmpty();
        }

        /**
         * 校验消息 ID 携带的发送时间与服务器当前时间差距过大时拒绝保存。
         *
         * @throws Exception 消息生成失败
         */
        @Test
        @DisplayName("发送时间偏差过大：拒绝保存，且数据库中没有新记录")
        void rejectsStaleMessage() throws Exception {
            EccMessage stale = newMessage("hello, e2echo");
            // 把 ID 中的时间戳改为 60 秒前，并重新签名，使签名有效但时间超限
            stale.setId(String.format("%016x", System.currentTimeMillis() - 60_000)
                    + stale.getId().substring(16));
            stale.setSign(EccUtil.sign(stale.toStringWithoutSign(), senderKeyPair.privateKeyHex()));

            assertThatThrownBy(() -> messageController.save(stale))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("发送时间与服务器当前时间差距过大！");

            assertThat(messageRepository.findByMessageId(stale.getId())).isEmpty();
        }

        /**
         * 校验必填字段为空时以业务异常拒绝保存，而不是把空值带到数据库的非空约束上。
         *
         * @throws Exception 消息生成失败
         */
        @Test
        @DisplayName("字段为空：拒绝保存，且数据库中没有新记录")
        void rejectsIncompleteMessage() throws Exception {
            // info 为空：加密不校验该字段，因此能构造出这种请求
            EccMessage plain = new EccMessage();
            plain.setFrom(senderKeyPair.publicKeyHex());
            plain.setTo(receiverKeyPair.publicKeyHex());
            plain.setMessage("hello, e2echo");
            plain.setType("text");
            plain.setChannel(PRIVATE_CHANNEL);
            EccMessage incomplete = Ecc.encrypt(plain);

            assertThatThrownBy(() -> messageController.save(incomplete))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("消息字段不完整：info！");

            assertThat(messageRepository.findByMessageId(incomplete.getId())).isEmpty();
        }

    }

    /**
     * 按 ID 查询消息接口的测试。
     */
    @Nested
    @DisplayName("按 ID 查询消息")
    class GetById {

        /**
         * 校验保存后能够按 ID 从数据库中读回消息。
         *
         * @throws Exception 消息生成失败
         */
        @Test
        @DisplayName("查询成功：读回刚保存的消息")
        void returnsStoredMessage() throws Exception {
            EccMessage sent = newMessage("hello, e2echo");
            messageController.save(sent);

            Result<EccMessage> result = messageController.getById(sent.getId());

            assertThat(result.code()).isEqualTo("0");
            assertThat(result.data()).isEqualTo(sent);
        }

        /**
         * 校验查询不存在的消息时抛出业务异常。
         */
        @Test
        @DisplayName("消息不存在：抛出业务异常")
        void throwsWhenMessageNotFound() {
            String missingId = String.format("%016x", System.currentTimeMillis()) + "f".repeat(32);

            assertThatThrownBy(() -> messageController.getById(missingId))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("消息不存在：" + missingId);
        }

    }

    /**
     * 分页查询消息接口的测试。
     */
    @Nested
    @DisplayName("分页查询消息")
    class ListByPage {

        /**
         * 校验发送者、接收者与通道三个过滤条件。
         */
        @Test
        @DisplayName("按发送者、接收者与通道过滤")
        void filtersByFromToAndChannel() {
            long base = System.currentTimeMillis();
            persistMessage(base, 1, PRIVATE_CHANNEL);
            persistMessage(base, 2, PRIVATE_CHANNEL);
            persistMessage(base, 3, "group");

            Page<EccMessage> privatePage = messageController.list(
                    List.of(senderKeyPair.publicKeyHex()),
                    List.of(receiverKeyPair.publicKeyHex()),
                    PRIVATE_CHANNEL,
                    null,
                    null,
                    null,
                    null,
                    1,
                    10).data();
            assertThat(privatePage.getTotalElements()).isEqualTo(2);
            assertThat(privatePage.getContent())
                    .allSatisfy(eccMessage -> assertThat(eccMessage.getChannel()).isEqualTo(PRIVATE_CHANNEL));

            Page<EccMessage> groupPage = list("group", null, null, null, null, 1, 10);
            assertThat(groupPage.getTotalElements()).isEqualTo(1);
        }

        /**
         * 校验时间区间与起始 ID 两个过滤条件。
         */
        @Test
        @DisplayName("按时间区间与起始 ID 过滤")
        void filtersByTimestampAndStartId() {
            long base = System.currentTimeMillis();
            Message first = persistMessage(base, 1, PRIVATE_CHANNEL);
            Message second = persistMessage(base + 1_000, 2, PRIVATE_CHANNEL);
            persistMessage(base + 2_000, 3, PRIVATE_CHANNEL);

            // 起始与结束时间戳均包含边界
            Page<EccMessage> ranged = list(null, Long.toString(second.getTimestamp()),
                    Long.toString(second.getTimestamp()), null, null, 1, 10);
            assertThat(ranged.getContent()).extracting(EccMessage::getId)
                    .containsExactly(second.getMessageId());

            // 只返回 ID 大于该值的消息
            Page<EccMessage> afterFirst = list(null, null, null, first.getMessageId(), null, 1, 10);
            assertThat(afterFirst.getTotalElements()).isEqualTo(2);
            assertThat(afterFirst.getContent()).extracting(EccMessage::getId)
                    .allSatisfy(id -> assertThat(id).isGreaterThan(first.getMessageId()));
        }

        /**
         * 校验倒序查询：通道为 USERINFO 时，按发送者过滤并取倒序第一条，即该用户最新的
         * 用户信息。
         */
        @Test
        @DisplayName("倒序查询：取某个用户最新的用户信息")
        void returnsLatestMessageWhenOrderDesc() {
            long base = System.currentTimeMillis();
            Message first = persistMessage(base, 1, USERINFO_CHANNEL);
            Message second = persistMessage(base + 1_000, 2, USERINFO_CHANNEL);
            Message third = persistMessage(base + 2_000, 3, USERINFO_CHANNEL);

            // 倒序取第一条即最新一条
            Page<EccMessage> latest = messageController.list(
                    List.of(senderKeyPair.publicKeyHex()),
                    null,
                    USERINFO_CHANNEL,
                    null,
                    null,
                    null,
                    "desc",
                    1,
                    1).data();
            assertThat(latest.getContent()).extracting(EccMessage::getId)
                    .containsExactly(third.getMessageId());

            // 整页倒序：从新到老
            Page<EccMessage> all = list(USERINFO_CHANNEL, null, null, null, "desc", 1, 10);
            assertThat(all.getContent()).extracting(EccMessage::getId)
                    .containsExactly(third.getMessageId(), second.getMessageId(), first.getMessageId());

            // 排序方向不区分大小写
            Page<EccMessage> upperCase = list(USERINFO_CHANNEL, null, null, null, "DESC", 1, 10);
            assertThat(upperCase.getContent()).extracting(EccMessage::getId)
                    .containsExactly(third.getMessageId(), second.getMessageId(), first.getMessageId());

            // 不传排序方向时默认升序，取第一条即最早一条
            Page<EccMessage> oldest = list(USERINFO_CHANNEL, null, null, null, null, 1, 1);
            assertThat(oldest.getContent()).extracting(EccMessage::getId)
                    .containsExactly(first.getMessageId());
        }

        /**
         * 校验结果按消息 ID 升序排列，且分页元信息正确。
         */
        @Test
        @DisplayName("按 ID 升序返回，并正确分页")
        void sortsByIdAndPaginates() {
            long base = System.currentTimeMillis();
            Message first = persistMessage(base, 1, PRIVATE_CHANNEL);
            Message second = persistMessage(base + 1_000, 2, PRIVATE_CHANNEL);
            Message third = persistMessage(base + 2_000, 3, PRIVATE_CHANNEL);

            Page<EccMessage> pageOne = list(null, null, null, null, null, 1, 2);
            assertThat(pageOne.getContent()).extracting(EccMessage::getId)
                    .containsExactly(first.getMessageId(), second.getMessageId());
            assertThat(pageOne.getTotalElements()).isEqualTo(3);
            assertThat(pageOne.getTotalPages()).isEqualTo(2);
            assertThat(pageOne.getNumber()).isZero();
            assertThat(pageOne.getSize()).isEqualTo(2);

            Page<EccMessage> pageTwo = list(null, null, null, null, null, 2, 2);
            assertThat(pageTwo.getContent()).extracting(EccMessage::getId)
                    .containsExactly(third.getMessageId());
        }

        /**
         * 校验非法的分页参数与时间戳参数。
         */
        @Test
        @DisplayName("参数非法：抛出业务异常")
        void throwsWhenArgumentsInvalid() {
            assertThatThrownBy(() -> list(null, null, null, null, null, 0, 10))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("页码必须大于等于 1！");

            assertThatThrownBy(() -> list(null, null, null, null, null, 1, 0))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("每页条数必须大于等于 1！");

            assertThatThrownBy(() -> list(null, "abc", null, null, null, 1, 10))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("时间戳格式错误：abc");

            assertThatThrownBy(() -> list(null, null, null, null, "unknown", 1, 10))
                    .isInstanceOf(E2EchoException.class)
                    .hasMessage("排序方向只能是 asc 或 desc：unknown");
        }

    }

}
