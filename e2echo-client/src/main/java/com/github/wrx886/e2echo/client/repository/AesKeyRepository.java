package com.github.wrx886.e2echo.client.repository;

import com.github.wrx886.e2echo.client.entity.AesKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AES 密钥数据访问层。
 *
 * <p>密钥按登入用户隔离，查询都带上 {@code owner}：加密时取某个对象最新签发的密钥，解密时按对象与
 * 签发时间取指定版本的密钥，两者都命中 {@code (owner, obj, publish_time)} 索引。</p>
 */
@Repository
public interface AesKeyRepository extends JpaRepository<AesKey, String> {

    /**
     * 查询某个对象最近一次签发的密钥。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @param obj   加密对象，如群聊标识
     * @return 签发时间最晚的密钥，没有时返回 {@code null}
     */
    AesKey findFirstByOwnerAndObjOrderByPublishTimeDesc(String owner, String obj);

    /**
     * 查询某个对象在指定签发时间签发的密钥。
     *
     * @param owner       数据所有者，即当前登入用户的公钥
     * @param obj         加密对象，如群聊标识
     * @param publishTime 密钥签发时间（毫秒）
     * @return 对应的密钥，没有时返回 {@code null}
     */
    AesKey findByOwnerAndObjAndPublishTime(String owner, String obj, long publishTime);
}
