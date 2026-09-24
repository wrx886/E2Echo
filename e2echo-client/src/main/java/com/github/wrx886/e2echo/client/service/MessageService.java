package com.github.wrx886.e2echo.client.service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.wrx886.e2echo.client.api.MessageApi;
import com.github.wrx886.e2echo.client.api.NoticeApi;
import com.github.wrx886.e2echo.client.common.Const;
import com.github.wrx886.e2echo.client.common.ReceiveMessageHandler;
import com.github.wrx886.e2echo.client.config.MessageHandlerConfig;
import com.github.wrx886.e2echo.client.dto.AesKeyDto;
import com.github.wrx886.e2echo.client.entity.Message;
import com.github.wrx886.e2echo.client.enums.ChannelEnum;
import com.github.wrx886.e2echo.client.enums.SysParamEnum;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.repository.MessageRepository;
import com.github.wrx886.e2echo.client.result.PageData;
import com.github.wrx886.e2echo.client.util.IdUtil;
import com.github.wrx886.e2echo.ecc.Ecc;

import com.github.wrx886.e2echo.ecc.EccMessage;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * 消息业务逻辑层。
 *
 * <p>发送时把本地消息按通道加密、签名后交给服务端；接收时验证并解密服务端消息，先落库、再交给
 * 对应类型的处理器。消息按登入用户隔离、只插入不更新，序号用于排序与增量拉取。</p>
 *
 * <p>私聊消息用 ECC 加密，群聊消息用 AES 加密：群聊密文前面拼的是 16 位十六进制的密钥签发时间
 * （密钥版本），解密方据此取到同一版本的密钥。</p>
 *
 * <p>除了定期拉取，登入后还会建立 notice 长连接：服务端发现变化时推送通知，客户端收到通知立刻
 * 拉取，见 {@link #connectNotice()}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * 自身的代理对象：自调用 {@code @Transactional} 方法时通过它，事务才会生效。
     */
    private final ObjectProvider<MessageService> selfProvider;

    /**
     * 消息数据访问对象。
     */
    private final MessageRepository messageRepository;

    /**
     * 系统参数业务逻辑对象，用于读写上次拉取时间。
     */
    private final SysParamService sysParamService;

    /**
     * 消息处理器配置，用于按消息类型取处理器。
     */
    private final MessageHandlerConfig messageHandlerConfig;

    /**
     * AES 密钥业务逻辑对象，用于群聊消息的加解密。
     */
    private final AesKeyService aesKeyService;

    /**
     * 消息接口，用于与服务端交互。
     */
    private final MessageApi messageApi;

    /**
     * 会话业务逻辑对象，用于取当前用户订阅的群聊。
     */
    private final ConversationService conversationService;

    /**
     * 通知接口，用于建立 notice 通道。
     */
    private final NoticeApi noticeApi;

    /**
     * 当前的通知连接；为空表示还没有连接，重建连接时用它断开旧连接。
     */
    private Disposable disposable = null;

    /**
     * 是否有拉取正在执行：保证同一时刻只有一个拉取在跑。
     */
    private final AtomicBoolean pulling = new AtomicBoolean();

    /**
     * 是否需要补一次拉取：拉取期间收到的通知都合并到这里。
     */
    private final AtomicBoolean pullPending = new AtomicBoolean();

    /**
     * 建立通知通道并自动接收消息：订阅“当前用户 + 已启用的群聊”，收到通知就拉取消息。
     *
     * <p>订阅目标会随会话变化（新加入群聊、群聊被停用等），所以每次调用都先断开旧连接，再按最新
     * 的目标重建。容器启动时由 {@link PostConstruct} 调用一次，会话变化时由
     * {@link ConversationService} 调用。建连之后还会补一次拉取：建连之前到达的消息不会有通知，
     * 只能靠这次拉取带回来。</p>
     *
     * <p>回调里只登记一次拉取请求（{@link #requestPull()}），不直接拉取：拉取用的接口是阻塞式的
     * （{@code block()}），不能跑在 Reactor Netty 的事件循环线程上；而且通知经常挤在一起到达，
     * 交给 {@code requestPull} 合并后在 boundedElastic 线程上串行执行更划算。</p>
     */
    @PostConstruct
    public synchronized void connectNotice() {
        // 先断开旧连接，避免重复订阅
        if (disposable != null) {
            disposable.dispose();
        }

        // 订阅目标：自己（私聊消息）+ 当前用户已启用的群聊
        List<String> tos = new ArrayList<>(conversationService.listEnabledGroup());
        tos.add(currentOwner());

        // 通知只表示“数据变了，该重新拉取”，这里只登记请求，合并与执行交给 requestPull
        disposable = noticeApi.connect(tos)
                .subscribe((msg) -> {
                    if (NoticeApi.NOTICE_REFLASH.equals(msg)) {
                        requestPull();
                    }
                });

        // 建连之前到达的消息不会有通知，这里补一次拉取
        requestPull();
    }

    /**
     * 请求拉取：同一时刻只跑一个拉取，拉取期间收到的多次通知合并成一次。
     *
     * <p>本方法立即返回：当前没有拉取在跑时，在 {@link Schedulers#boundedElastic()} 上异步起一次
     * （拉取用的接口是阻塞式的，不能跑在通知回调所在的事件循环线程上）；已经有拉取在跑时，只把
     * {@link #pullPending} 置真，交给正在跑的那次拉取收尾时补拉一次。这样挤在一起的 N 条通知最多
     * 只产生 2 次拉取（正在跑的那次 + 补拉一次），多出来的通知本来也拉不到新消息。</p>
     */
    public void requestPull() {
        // 登记一次拉取请求：拉取期间再来的通知也登记在这里
        pullPending.set(true);

        // 已经有拉取在跑，本次请求被合并，由它收尾时补拉
        if (pulling.compareAndSet(false, true)) {
            Schedulers.boundedElastic().schedule(this::drainPull);
        }
    }

    /**
     * 执行拉取：拉完一轮后，如果期间又收到通知就再拉一轮，然后结束。
     */
    private void drainPull() {
        try {
            // 拉取前先清掉标记：拉取期间新到的通知会重新把标记置真
            do {
                pullPending.set(false);
                pullMessage();
            } while (pullPending.get());
        } catch (Exception e) {
            // 拉取失败只记日志，不影响通知通道本身
            log.error("拉取消息失败", e);
        } finally {
            pulling.set(false);
        }

        // 退出竞争时又来了通知就重新起一次，保证最后一次通知不会被丢掉
        if (pullPending.get() && pulling.compareAndSet(false, true)) {
            Schedulers.boundedElastic().schedule(this::drainPull);
        }
    }

    /**
     * 拉取消息：按私聊、群聊两个通道，分批拉取上次拉取时间之后的消息。
     *
     * <p>服务端按游标只返回 ID 大于该值的消息，所以这里必须按 ID 升序（从老到新）拉取、并拿本批
     * 最大 ID 作为下一次的游标，才能保证不重不漏；若按降序拉，第一批拿到的是最新的消息，游标取最大
     * ID 后下一批必然为空，超过一批的消息会被静默丢弃。</p>
     *
     * <p>每批按消息 ID 升序处理：验签、去重、解密入库，单条失败只记录日志、不影响整批。一批拉满就
     * 继续拉，拉到不满一批说明该通道已经拉完，再切换到下一个通道；两个通道都拉完后更新上次拉取时间，
     * 时间取开始拉取时的时刻，避免漏掉拉取过程中新到的消息。</p>
     */
    public void pullMessage() {
        final MessageService self = selfProvider.getObject();

        // 获取上次更新时间并减去 60 作为窗口
        String lastPullTime = Long.toString(Optional.ofNullable(sysParamService.find(SysParamEnum.LAST_PULL_TIME))
                .map(Long::parseLong).orElse(System.currentTimeMillis()) - Const.MAX_MESSAGE_DIFF_MS);

        // 拉取时间
        String pullTime = Long.toString(System.currentTimeMillis());

        // 拉取消息
        String startId = null;
        PageData<EccMessage> pageData = null;
        ChannelEnum channel = ChannelEnum.CHAT_PRIVATE_ECC;
        while (pageData == null || pageData.content().size() >= Const.MESSAGE_PULL_BATCH_SIZE) {
            // 接收者过滤条件：私聊为当前用户，群聊为当前用户订阅的群聊
            List<String> toList = channel.equals(ChannelEnum.CHAT_PRIVATE_ECC)
                    ? List.of(currentOwner())
                    : conversationService.listEnabledGroup();
            // 群聊通道没有订阅任何群聊时不能直接查：过滤条件为空会退化成不过滤，拉到服务端上所有群聊的消息
            if (toList.isEmpty()) {
                log.info("没有可拉取的 {} 通道消息，跳过", channel);
                break;
            }

            // 拉取消息
            pageData = messageApi.list(
                    null,
                    toList,
                    channel.name(),
                    lastPullTime,
                    null,
                    startId,
                    null, // 降序才用 endId，这里按 ID 升序拉取、游标用 startId
                    "ASC",
                    1,
                    Const.MESSAGE_PULL_BATCH_SIZE
            );
            log.info("接收到 {} 条消息", pageData.content().size());

            // 排序
            List<EccMessage> eccMessages = new ArrayList<>(pageData.content());
            eccMessages.sort((a, b) -> Objects.compare(a.getId(), b.getId(), Comparator.naturalOrder()));

            // 去掉游标消息
            if (startId != null && !eccMessages.isEmpty() && startId.equals(eccMessages.get(0).getId())) {
                eccMessages.remove(0);
            }

            // 更新游标
            startId = eccMessages.isEmpty() ? null : eccMessages.get(eccMessages.size() - 1).getId();

            // 开始处理消息
            for (EccMessage eccMessage : eccMessages) {
                try {
                    self.receive(eccMessage);
                } catch (E2EchoException e) {
                    // 验签失败、消息重复等属于可预期的拒绝，记警告即可
                    log.warn("消息被拒绝：{}", e.getMessage());
                } catch (Exception e) {
                    log.error("消息接收失败", e);
                }
            }

            // 切换通道
            if (channel.equals(ChannelEnum.CHAT_PRIVATE_ECC) &&
                    pageData.content().size() < Const.MESSAGE_PULL_BATCH_SIZE) {
                // 切换到群聊通道
                pageData = null;
                channel = ChannelEnum.CHAT_GROUP_AES;
                startId = null;
            }
        }

        // 更新上次获取时间
        sysParamService.put(SysParamEnum.LAST_PULL_TIME, pullTime);
        log.info("更新上次接收时间为 {}", pullTime);
    }

    /**
     * 发送消息：按通道加密（私聊 ECC、群聊 AES）并签名后提交给服务端。
     *
     * @param message 待发送的消息
     * @param save    是否在发送前先保存到本地
     * @throws E2EchoException 尚未登入、通道不支持或加密失败
     */
    @Transactional
    public void send(Message message, boolean save) {
        // 提前确认已登入：审计填充 owner 依赖当前用户的公钥
        currentOwner();

        // 消息通道
        if (!ChannelEnum.CHAT_PRIVATE_ECC.name().equals(message.getChannel()) &&
                !ChannelEnum.CHAT_GROUP_AES.name().equals(message.getChannel())
        ) {
            throw new E2EchoException("不支持的消息通道：" + message.getChannel());
        }

        // 实体上这些字段不可为空，提前校验，避免落到数据库才报错
        if (message.getFrom() == null || message.getTo() == null || message.getMessage() == null
                || message.getType() == null || message.getInfo() == null) {
            throw new E2EchoException("消息内容不完整！");
        }

        // 构造 eccMessage
        EccMessage eccMessage = new EccMessage();
        eccMessage.setFrom(message.getFrom());
        eccMessage.setTo(message.getTo());
        eccMessage.setMessage(message.getMessage());
        eccMessage.setType(message.getType());
        eccMessage.setChannel(message.getChannel());
        eccMessage.setInfo(message.getInfo());

        // 开始加密
        try {
            if (ChannelEnum.CHAT_PRIVATE_ECC.name().equals(message.getChannel())) {
                eccMessage = Ecc.encrypt(eccMessage);
            } else {
                AesKeyDto aesKey = aesKeyService.getLast(eccMessage.getTo());
                if (aesKey == null) throw new E2EchoException("找不到对应的 AES KEY");
                // 群聊密文 = 16 位十六进制密钥签发时间（密钥版本）+ 密文，解密方按它取同一版本的密钥
                eccMessage.setMessage(String.format("%016x", aesKey.publishTime()) + Ecc.encryptAes(eccMessage.getMessage(), aesKey.aesKey()));
                // 签名
                eccMessage = Ecc.sign(eccMessage);
            }
        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new E2EchoException("消息加密失败！");
        }

        // 是否存储到数据库
        message.setMessageId(eccMessage.getId());
        if (save) {
            save(message);
        }

        // 发送消息
        messageApi.save(eccMessage);
    }

    /**
     * 接收并处理一条服务端消息：验签、检查时间与通道、去重，解密后入库，最后交给类型对应的处理器。
     *
     * @param eccMessage 服务端返回的消息
     * @throws E2EchoException 尚未登入、验签失败、时间超限、通道不支持、消息已存在或解密失败
     */
    public void receive(EccMessage eccMessage) {
        final MessageService self = selfProvider.getObject();

        // 提前确认已登入：审计填充 owner 依赖当前用户的公钥
        currentOwner();

        // 1. 验证消息
        // 1.1 验证签名
        if (!Ecc.verify(eccMessage)) {
            throw new E2EchoException("消息验证失败！");
        }
        // 1.2 验证发送时间
        long lastPullTime = Long.parseLong(
                Optional.ofNullable(sysParamService.find(SysParamEnum.LAST_PULL_TIME))
                        .orElse(Long.toString(System.currentTimeMillis())));
        long messageTImeStamp = IdUtil.getTimestampFromId(eccMessage.getId());
        if (lastPullTime - messageTImeStamp > Const.MAX_MESSAGE_DIFF_MS || // 历史消息
                messageTImeStamp - System.currentTimeMillis() > Const.MAX_MESSAGE_DIFF_MS // 未来消息
        ) {
            throw new E2EchoException("拒收历史和未来消息！");
        }
        // 1.3 消息通道
        if (!ChannelEnum.CHAT_PRIVATE_ECC.name().equals(eccMessage.getChannel()) &&
                !ChannelEnum.CHAT_GROUP_AES.name().equals(eccMessage.getChannel())
        ) {
            throw new E2EchoException("不支持的消息通道：" + eccMessage.getChannel());
        }
        // 1.4 消息存在
        if (messageRepository.existsByOwnerAndMessageId(currentOwner(), eccMessage.getId())) {
            throw new E2EchoException("消息已存在！");
        }

        // 2. 解密消息
        try {
            if (ChannelEnum.CHAT_PRIVATE_ECC.name().equals(eccMessage.getChannel())) {
                eccMessage = Ecc.decrypt(eccMessage);
            } else {
                // 密文前 16 位是加密时用的密钥签发时间，按“对象 + 签发时间”取对应版本的密钥
                AesKeyDto aesKey = aesKeyService.getOne(eccMessage.getTo(),
                        Long.parseLong(eccMessage.getMessage().substring(0, 16), 16)
                );
                if (aesKey == null) throw new E2EchoException("找不到对应的 AES KEY");
                eccMessage.setMessage(Ecc.decryptAes(eccMessage.getMessage().substring(16), aesKey.aesKey()));
            }
        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new E2EchoException("消息解密失败！");
        }

        // 3. 构造 message
        Message message = new Message();
        message.setMessageId(eccMessage.getId());
        message.setFrom(eccMessage.getFrom());
        message.setTo(eccMessage.getTo());
        message.setMessage(eccMessage.getMessage());
        message.setType(eccMessage.getType());
        message.setChannel(eccMessage.getChannel());
        message.setInfo(eccMessage.getInfo());

        // 4. 填充seq并存储到数据库
        self.save(message);

        // 5. 对消息进行特殊处理（可以改变消息内容，但没有意义）
        ReceiveMessageHandler receiveHandler = messageHandlerConfig.getReceiveHandler(message.getType());
        if (receiveHandler != null) {
            receiveHandler.receive(message);
        }
    }

    /**
     * 保存消息到本地，序号取当前最大值加一。
     *
     * @param message 待保存的消息
     */
    @Transactional
    public void save(Message message) {
        message.setSeq(maxSeq() + 1);
        messageRepository.save(message);
    }

    /**
     * 查询当前用户与某个会话方的消息（收 + 发），按序号升序。
     *
     * @param peer    会话方，私聊时为对方公钥
     * @param startId 仅返回ID大于该值的消息，传空表示全部
     * @return 会话内的消息，按序号升序
     */
    public Page<Message> findConversation(
            String peer,
            String startId,
            int pageNum,
            int pageSize
    ) {
        Specification<Message> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // owner
            predicates.add(criteriaBuilder.equal(root.get("owner"), currentOwner()));

            // from to
            Predicate fromPredicate = root.get("from_").in(peer, currentOwner());
            Predicate toPredicate = root.get("to_").in(peer, currentOwner());
            predicates.add(criteriaBuilder.or(fromPredicate, toPredicate));

            // channel
            predicates.add(root.get("channel").in(ChannelEnum.CHAT_GROUP_AES.name(), ChannelEnum.CHAT_PRIVATE_ECC.name()));

            // startId
            if (StringUtils.hasLength(startId)) {
                predicates.add(criteriaBuilder.greaterThan(root.get("id"), startId));
            }

            return criteriaBuilder.and(predicates);
        };

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return messageRepository.findAll(specification, pageable);
    }

    /**
     * 获取当前用户本地已有的最大消息序号，用于增量拉取。
     *
     * @return 最大序号，本地还没有消息时返回 0
     */
    public long maxSeq() {
        return messageRepository.findFirstByOwnerOrderBySeqDesc(currentOwner())
                .map(Message::getSeq)
                .orElse(0L);
    }

}
