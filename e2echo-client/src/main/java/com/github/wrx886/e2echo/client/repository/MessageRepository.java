package com.github.wrx886.e2echo.client.repository;

import java.util.List;
import java.util.Optional;

import com.github.wrx886.e2echo.client.entity.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 消息数据访问层。
 *
 * <p>消息按登入用户隔离，查询都带上 {@code owner}（当前登入用户的公钥）。</p>
 */
public interface MessageRepository extends JpaRepository<Message, String>, JpaSpecificationExecutor<Message> {

    /**
     * 查询当前用户序号最大的一条消息，用于增量拉取时确定本地已有的最新序号。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @return 序号最大的消息，当前用户还没有消息时为空
     */
    Optional<Message> findFirstByOwnerOrderBySeqDesc(String owner);

}
