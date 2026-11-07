package com.github.wrx886.e2echo.client.repository;

import java.util.Optional;

import com.github.wrx886.e2echo.client.dto.MessageFromDto;
import com.github.wrx886.e2echo.client.entity.Message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 消息数据访问层。
 *
 * <p>消息按登入用户隔离，查询都带上 {@code owner}（当前登入用户的公钥）。</p>
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String>, JpaSpecificationExecutor<Message> {

    /**
     * 查询当前用户序号最大的一条消息，用于增量拉取时确定本地已有的最新序号。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @return 序号最大的消息，当前用户还没有消息时为空
     */
    Optional<Message> findFirstByOwnerOrderBySeqDesc(String owner);

    /**
     * 判断本地是否已经存过某条服务端消息。
     *
     * <p>判断同样按登入用户隔离：同一条服务端消息（例如群聊消息）可能被本机上的多个用户分别收到，
     * 每个用户各存一份，与 {@code (owner, message_id)} 唯一约束保持一致。</p>
     *
     * @param owner     数据所有者，即当前登入用户的公钥
     * @param messageId 服务端消息 ID
     * @return 该用户下已存在返回 {@code true}
     */
    boolean existsByOwnerAndMessageId(String owner, String messageId);

    /**
     * 分页查询某个会话中出现过的发送者公钥，按公钥升序排列并去重。
     *
     * <p>与 {@code MessageService#findConversation} 一样直接通过消息确定会话：私聊统计当前用户与
     * 该会话对方的双向消息，群聊统计所有发往该群的消息。发送者个人别名通过左连接会话取得，
     * 同时用于公钥或别名的模糊过滤；关键词为 {@code %%} 时匹配全部发送者。</p>
     *
     * @param owner            数据所有者，即当前登入用户的公钥
     * @param peer             会话对方，私聊时为对方公钥、群聊时为群聊标识
     * @param publicKeyOrAlias 已转小写并带 {@code %} 的公钥或别名模糊匹配条件
     * @param startPublicKey   发送者公钥游标（不含），为空表示从头开始
     * @param privateChannel   私聊通道值
     * @param groupChannel     群聊通道值
     * @param pageable         分页参数
     * @return 去重后的发送者公钥与个人别名分页结果
     */
    @Query(value = """
            select distinct new com.github.wrx886.e2echo.client.dto.MessageFromDto(
                    m.from,
                    c.alias)
            from Message m
            left join Conversation c
                   on c.owner = m.owner
                  and c.peer = m.from
            where m.owner = :owner
              and m.channel in (:privateChannel, :groupChannel)
              and (
                    (m.channel = :groupChannel
                     and m.to = :peer)
                 or (m.channel = :privateChannel
                     and ((m.from = :owner and m.to = :peer)
                       or (m.from = :peer and m.to = :owner)))
              )
              and (lower(m.from) like :publicKeyOrAlias
                or lower(c.alias) like :publicKeyOrAlias)
              and (:startPublicKey is null or m.from > :startPublicKey)
            order by m.from asc
            """,
            countQuery = """
                    select count(distinct m.from)
                    from Message m
                    left join Conversation c
                           on c.owner = m.owner
                          and c.peer = m.from
                    where m.owner = :owner
                      and m.channel in (:privateChannel, :groupChannel)
                      and (
                            (m.channel = :groupChannel
                             and m.to = :peer)
                         or (m.channel = :privateChannel
                             and ((m.from = :owner and m.to = :peer)
                               or (m.from = :peer and m.to = :owner)))
                      )
                      and (lower(m.from) like :publicKeyOrAlias
                        or lower(c.alias) like :publicKeyOrAlias)
                      and (:startPublicKey is null or m.from > :startPublicKey)
                    """)
    Page<MessageFromDto> listFrom(
            @Param("owner") String owner,
            @Param("peer") String peer,
            @Param("publicKeyOrAlias") String publicKeyOrAlias,
            @Param("startPublicKey") String startPublicKey,
            @Param("privateChannel") String privateChannel,
            @Param("groupChannel") String groupChannel,
            Pageable pageable
    );
}
