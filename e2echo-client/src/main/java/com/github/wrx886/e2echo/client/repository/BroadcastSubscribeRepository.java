package com.github.wrx886.e2echo.client.repository;

import com.github.wrx886.e2echo.client.entity.BroadcastSubscribe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 广播订阅数据访问层。
 *
 * <p>订阅按登入用户隔离，查询都带上 {@code owner}（当前登入用户的公钥）。</p>
 */
@Repository
public interface BroadcastSubscribeRepository extends JpaRepository<BroadcastSubscribe, String>, JpaSpecificationExecutor<BroadcastSubscribe> {

    /**
     * 查询当前用户对某个作者的订阅。
     *
     * @param owner     数据所有者，即当前登入用户的公钥
     * @param publicKey 被订阅者的公钥
     * @return 订阅，没有订阅时返回 {@code null}
     */
    BroadcastSubscribe getByOwnerAndPublicKey(String owner, String publicKey);

    /**
     * 查询当前用户订阅的所有人。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @return 订阅列表，没有订阅时为空列表
     */
    List<BroadcastSubscribe> findAllByOwner(String owner);

}
