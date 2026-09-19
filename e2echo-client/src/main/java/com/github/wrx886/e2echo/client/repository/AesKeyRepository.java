package com.github.wrx886.e2echo.client.repository;

import com.github.wrx886.e2echo.client.entity.AesKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AES 密钥数据访问层。
 *
 * <p>密钥按登入用户隔离，查询都带上 {@code owner}；按对象与签发时间范围查询，对应
 * {@code (owner, obj, publish_time)} 索引。</p>
 */
@Repository
public interface AesKeyRepository extends JpaRepository<AesKey, String> {

    /**
     * 查询某个对象在给定签发时间范围内的密钥。
     *
     * @param owner            数据所有者，即当前登入用户的公钥
     * @param obj              加密对象，如群聊标识
     * @param startPublishTime 起始签发时间（毫秒，含）
     * @param endPublishTime   结束签发时间（毫秒，含）
     * @return 范围内的密钥，按签发时间升序
     */
    List<AesKey> findAllByOwnerAndObjAndPublishTimeGreaterThanEqualAndPublishTimeLessThanEqual(
            String owner, String obj, long startPublishTime, long endPublishTime
    );

    /**
     * 查询某个对象最近一次签发的密钥。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @param obj   加密对象，如群聊标识
     * @return 签发时间最晚的密钥，没有时返回 {@code null}
     */
    AesKey findFirstByOwnerAndObjOrderByPublishTimeDesc(String owner, String obj);

}
