package com.github.wrx886.e2echo.client.repository;

import com.github.wrx886.e2echo.client.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会话数据访问层。
 *
 * <p>会话按登入用户隔离，查询都带上 {@code owner}（当前登入用户的公钥）。</p>
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    /**
     * 分页查询当前用户的会话，按更新时间倒序（同一时间按主键倒序），并带出最新消息。
     *
     * <p>分页是无状态的：每次翻页都是重新执行的查询，排序必须能唯一确定顺序，否则更新时间相同的
     * 记录在不同页之间的先后可能变化，导致漏掉或重复。所以主键也必须参与排序，作为排序的兜底。</p>
     *
     * @param owner    数据所有者，即当前登入用户的公钥
     * @param pageable 分页参数
     * @return 会话分页结果
     */
    @EntityGraph(attributePaths = {"latestMessage"})
    Page<Conversation> findAllByOwnerOrderByUpdateTimeDescIdDesc(String owner, Pageable pageable);

    /**
     * 查询当前用户指定类型、指定启用状态的会话。
     *
     * @param owner   数据所有者，即当前登入用户的公钥
     * @param group   是否群聊：{@code true} 群聊、{@code false} 私聊
     * @param enabled 是否启用
     * @return 会话列表
     */
    List<Conversation> findAllByOwnerAndGroupAndEnabled(String owner, Boolean group, Boolean enabled);

    /**
     * 按会话对方查询当前用户的会话。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @param peer  会话对方
     * @return 会话，不存在时返回 {@code null}
     */
    @EntityGraph(attributePaths = {"latestMessage"})
    Conversation findByOwnerAndPeer(String owner, String peer);

}
