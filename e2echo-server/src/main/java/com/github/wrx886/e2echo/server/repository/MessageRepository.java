package com.github.wrx886.e2echo.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.github.wrx886.e2echo.server.entity.Message;

/**
 * 消息数据访问层。
 *
 * <p>基于 Spring Data JPA 提供消息实体的基础增删改查能力，并通过
 * {@link JpaSpecificationExecutor} 支持动态条件查询。</p>
 */
public interface MessageRepository extends JpaRepository<Message, String>, JpaSpecificationExecutor<Message> {

    /**
     * 按消息 ID 查询消息。
     *
     * <p>消息 ID 由客户端生成，与数据库主键是两个字段，因此不能用 {@code findById} 查询。</p>
     *
     * @param messageId 消息 ID
     * @return 对应的消息，不存在时为空
     */
    Optional<Message> findByMessageId(String messageId);

}
