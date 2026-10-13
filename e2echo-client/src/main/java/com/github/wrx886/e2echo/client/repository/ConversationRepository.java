package com.github.wrx886.e2echo.client.repository;

import com.github.wrx886.e2echo.client.entity.Conversation;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会话数据访问层。
 *
 * <p>会话按登入用户隔离，查询都带上 {@code owner}（当前登入用户的公钥）。</p>
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String>, JpaSpecificationExecutor<Conversation> {

    /**
     * 按条件分页查询会话。
     *
     * <p>覆盖父类方法只是为了挂上 {@link EntityGraph}：一次把最新消息带出来，会话列表映射 DTO 时就不用
     * 逐条触发懒加载（事务外还会直接报懒加载异常）。</p>
     *
     * @param spec     查询条件（如所有者、关键词）
     * @param pageable 分页与排序
     * @return 会话分页结果
     */
    @Override
    @NullMarked
    @EntityGraph(attributePaths = {"latestMessage"})
    Page<Conversation> findAll(Specification<Conversation> spec, Pageable pageable);

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

    /**
     * 统计当前用户所有会话的未读消息总数。
     *
     * <p>直接在数据库里聚合求和，不把会话全查出来再累加；一条会话都没有时 {@code SUM} 为空，
     * 返回 {@code null}。</p>
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @return 未读消息总数，没有会话时返回 {@code null}
     */
    @Query("SELECT SUM(c.unread) FROM Conversation c WHERE c.owner = :owner")
    Integer countUnread(String owner);

}
