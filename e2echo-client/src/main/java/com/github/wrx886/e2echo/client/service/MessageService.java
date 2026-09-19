package com.github.wrx886.e2echo.client.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.wrx886.e2echo.client.api.MessageApi;
import com.github.wrx886.e2echo.client.common.Const;
import com.github.wrx886.e2echo.client.common.ReceiveMessageHandler;
import com.github.wrx886.e2echo.client.config.MessageHandlerConfig;
import com.github.wrx886.e2echo.client.entity.Message;
import com.github.wrx886.e2echo.client.enums.ChannelEnum;
import com.github.wrx886.e2echo.client.enums.SysParamEnum;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.repository.MessageRepository;
import com.github.wrx886.e2echo.client.util.IdUtil;
import com.github.wrx886.e2echo.ecc.Ecc;

import com.github.wrx886.e2echo.ecc.EccMessage;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * 消息业务逻辑层。
 *
 * <p>发送时把本地消息按通道加密、签名后交给服务端；接收时验证并解密服务端消息、交给对应类型的
 * 处理器，最后落库。消息按登入用户隔离、只插入不更新，序号用于排序与增量拉取。</p>
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * 消息数据访问对象。
     */
    private final MessageRepository messageRepository;

    private final SysParamService sysParamService;

    private final MessageHandlerConfig messageHandlerConfig;

    private final AesKeyService aesKeyService;

    private final MessageApi messageApi;

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
                boolean encrypted = false;
                List<String> aesKeys = aesKeyService.get(
                        eccMessage.getTo(),
                        IdUtil.getTimestampFromId(eccMessage.getId()));
                for (String aesKey : aesKeys) {
                    try {
                        eccMessage.setMessage(Ecc.encryptAes(eccMessage.getMessage(), aesKey));
                        encrypted = true;
                    } catch (Exception e) {
                        // 忽略
                    }
                }

                if (!encrypted) {
                    throw new E2EchoException("消息加密失败!");
                }

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
     * 接收消息：验签、检查时间与通道，解密后交给对应类型的处理器，需要时落库。
     *
     * @param eccMessage 服务端返回的消息
     * @return 解密并填充序号后的消息；处理器已消费的消息不入库
     * @throws E2EchoException 尚未登入、验签失败、时间超限、通道不支持或解密失败
     */
    @Transactional
    public Message receive(EccMessage eccMessage) {
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

        // 2. 解密消息
        try {
            if (ChannelEnum.CHAT_PRIVATE_ECC.name().equals(eccMessage.getChannel())) {
                eccMessage = Ecc.decrypt(eccMessage);
            } else {
                boolean decrypted = false;
                List<String> aesKeys = aesKeyService.get(
                        eccMessage.getTo(),
                        IdUtil.getTimestampFromId(eccMessage.getId()));
                for (String aesKey : aesKeys) {
                    try {
                        eccMessage.setMessage(Ecc.decryptAes(eccMessage.getMessage(), aesKey));
                        decrypted = true;
                    } catch (Exception e) {
                        // 忽略
                    }
                }

                if (!decrypted) {
                    throw new E2EchoException("消息解密失败!");
                }
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

        // 4. 对消息进行特殊处理（可以改变消息内容，但要慎重）
        ReceiveMessageHandler receiveHandler = messageHandlerConfig.getReceiveHandler(message.getType());
        if (receiveHandler != null && receiveHandler.receive(message)) {
            // 消息不需要存储
            return message;
        }

        // 5. 填充seq并存储到数据库
        return save(message);
    }

    /**
     * 保存消息到本地，序号取当前最大值加一。
     *
     * @param message 待保存的消息
     * @return 保存后的消息
     */
    @Transactional
    public Message save(Message message) {
        message.setSeq(maxSeq() + 1);
        return messageRepository.save(message);
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
