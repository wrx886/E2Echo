package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.dto.ConversationDto;
import com.github.wrx886.e2echo.client.entity.Conversation;
import com.github.wrx886.e2echo.client.entity.Message;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * 会话业务逻辑层。
 *
 * <p>维护当前用户的会话列表与别名：会话按登入用户隔离，最新消息随收发更新；别名读取带缓存，
 * 会话新建或保存时让对应缓存失效。</p>
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    /**
     * 自身的代理对象：自调用 {@code @Transactional} 方法时通过它，事务才会生效。
     */
    private final ObjectProvider<ConversationService> selfProvider;

    /**
     * 别名缓存，键为“数据所有者 + 会话对方”（本机可能先后登入多个用户）；用 {@code Optional}
     * 是为了把“查过但没有别名”也缓存下来。
     */
    private final ConcurrentHashMap<String, Optional<String>> aliasMap = new ConcurrentHashMap<>();

    /**
     * 会话数据访问对象。
     */
    private final ConversationRepository conversationRepository;

    /**
     * 分页列出当前用户的会话，按更新时间倒序（最近有消息的排在前面）。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 会话分页结果（DTO），已带出最新消息
     */
    public Page<ConversationDto> list(int pageNum, int pageSize) {
        return conversationRepository.findAllByOwnerOrderByUpdateTimeDescId(
                currentOwner(),
                PageRequest.of(pageNum - 1, pageSize)
        ).map(ConversationDto::fromEntity);
    }

    /**
     * 列出当前用户已启用的群聊的会话对方（群标识）。
     *
     * @return 群标识列表
     */
    public List<String> listEnabledGroup() {
        return conversationRepository
                .findAllByOwnerAndGroupAndEnabled(
                        currentOwner(), true, true)
                .stream()
                .map(Conversation::getPeer)
                .toList();
    }

    /**
     * 保存会话：新建会话，或修改已有会话的别名、是否启用等，并清除该会话对方的别名缓存。
     *
     * <p>会话按登入用户隔离：带 id 表示修改已有会话，这时会话必须存在且属于当前用户；新建会话不要
     * 带 id。群聊会话还会影响通知通道的订阅目标，所以保存后要重建通知连接。</p>
     *
     * @param conversation 待保存的会话
     * @throws E2EchoException 带 id 但会话不存在，或会话不属于当前用户
     */
    @Transactional
    public void save(Conversation conversation) {
        if (StringUtils.hasLength(conversation.getId())) {
            Conversation conversation1 = conversationRepository.findById(conversation.getId()).orElse(null);
            if (conversation1 == null) {
                throw new E2EchoException("会话ID不存在，新增请留空！");
            }
            if (!conversation1.getOwner().equals(currentOwner())) {
                throw new E2EchoException("禁止修改其他用户的会话！");
            }
        }

        conversationRepository.save(conversation);
        aliasMap.remove(currentOwner() + conversation.getPeer());
        if (Boolean.TRUE.equals(conversation.getGroup())) {
            // 群聊是否启用决定了通知通道的订阅目标，所以群聊会话保存后要重建连接
            BeanProvider.getBean(MessageService.class).connectNotice();
        }
    }

    /**
     * 查询会话对方对应的别名，结果会被缓存（没有别名时同样缓存）。
     *
     * @param peer 会话对方
     * @return 别名，没有对应会话或没有别名时返回 {@code null}
     */
    public String findAliasByPeer(String peer) {
        String aliasKey = currentOwner() + peer;
        aliasMap.computeIfAbsent(aliasKey, (k) -> Optional.ofNullable(conversationRepository.findByOwnerAndPeer(
                currentOwner(),
                peer
        )).map(Conversation::getAlias));
        Optional<String> alias = aliasMap.get(aliasKey);
        return alias == null ? null : alias.orElse(null);
    }

    /**
     * 更新会话的最新消息；会话不存在时按对方新建，别名默认取对方末尾 5 位并置为启用。
     *
     * <p>这里不调用 {@link #save(Conversation)}：那会清掉用户设置的别名缓存，而本方法只更新最新
     * 消息。只有新建会话时才需要清缓存——别名从无到有，之前缓存的“没有别名”已经失效。</p>
     *
     * @param peer    会话对方
     * @param group   是否群聊
     * @param message 最新消息
     */
    @Transactional
    public void updateLatestMessageByPeer(String peer, boolean group, Message message) {
        Conversation conversation = conversationRepository.findByOwnerAndPeer(currentOwner(), peer);
        if (conversation == null) {
            conversation = createConversation(peer, group);
            // 会话新建，别名从无到有，之前可能缓存过“没有别名”，这里让它失效
            aliasMap.remove(currentOwner() + peer);
            // 新加入的群聊要立刻订阅，重建通知连接（用 BeanProvider 取是为了避开循环依赖）
            if (group) {
                BeanProvider.getBean(MessageService.class).connectNotice();
            }
        }
        conversation.setLatestMessage(message);
        // 这里不能直接调用 save，因为这个方法存在的意义就是不破坏别名机制更新最新消息
        conversationRepository.save(conversation);
    }

    /**
     * 按会话对方查询当前用户的会话。
     *
     * @param peer 会话对方，私聊时为对方公钥、群聊时为群聊标识
     * @return 会话，不存在时返回 {@code null}
     */
    public ConversationDto findByPeer(String peer) {
        return Optional.ofNullable(conversationRepository.findByOwnerAndPeer(currentOwner(), peer))
                .map(ConversationDto::fromEntity).orElse(null);
    }

    /**
     * 保存会话（DTO 入口）：把 DTO 复制成实体，再交给 {@link #save(Conversation)}。
     *
     * <p>只复制 id、peer、alias、group、enabled：最新消息由收发消息时更新，owner 与审计字段由
     * 持久化层填充，都不能由调用方指定。这里通过代理对象调用，{@code @Transactional} 才生效。</p>
     *
     * @param conversationDto 待保存的会话
     */
    public void save(ConversationDto conversationDto) {
        final ConversationService self = selfProvider.getObject();

        Conversation conversation = new Conversation();
        BeanUtils.copyProperties(conversationDto, conversation,
                "latestMessage", "owner", "createTime", "updateTime");
        self.save(conversation);
    }

    /**
     * 按会话对方新建一个会话（不保存）：别名默认取对方末尾 5 位，会话置为启用。
     *
     * @param peer  会话对方
     * @param group 是否群聊
     * @return 新建的会话，交给调用方保存
     */
    private Conversation createConversation(String peer, boolean group) {
        Conversation conversation = new Conversation();
        conversation.setPeer(peer);
        conversation.setAlias(peer.substring(peer.length() > 5 ? peer.length() - 5 : 0));
        conversation.setGroup(group);
        conversation.setEnabled(true);
        return conversation;
    }

}
