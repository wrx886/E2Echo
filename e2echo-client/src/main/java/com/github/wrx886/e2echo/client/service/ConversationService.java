package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.entity.Conversation;
import com.github.wrx886.e2echo.client.entity.Message;
import com.github.wrx886.e2echo.client.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * @return 会话分页结果，已带出最新消息
     */
    public Page<Conversation> list(int pageNum, int pageSize) {
        return conversationRepository.findAllByOwnerOrderByUpdateTimeDescId(
                currentOwner(),
                PageRequest.of(pageNum - 1, pageSize)
        );
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
     * 保存会话，并清除该会话对方的别名缓存。
     *
     * @param conversation 待保存的会话
     */
    public void save(Conversation conversation) {
        conversationRepository.save(conversation);
        aliasMap.remove(currentOwner() + conversation.getPeer());
        if (Boolean.TRUE.equals(conversation.getGroup())) {
            // 群聊是否启用决定了通知通道的订阅目标，所以群聊会话保存后要重建连接
            BeanProvider.getBean(MessageService.class).connectNotice();
        }
    }

    /**
     * 按主键查询会话。
     *
     * @param id 会话主键
     * @return 会话，不存在时返回 {@code null}
     */
    public Conversation findById(String id) {
        return conversationRepository.findById(id).orElse(null);
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
    public void updateLatestMessageByPeer(String peer, Boolean group, Message message) {
        Conversation conversation = conversationRepository.findByOwnerAndPeer(currentOwner(), peer);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setPeer(peer);
            conversation.setAlias(peer.substring(peer.length() > 5 ? peer.length() - 5 : 0));
            conversation.setGroup(group);
            conversation.setEnabled(true);
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

}
