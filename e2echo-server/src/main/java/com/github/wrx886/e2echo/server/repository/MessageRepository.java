package com.github.wrx886.e2echo.server.repository;

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
}
