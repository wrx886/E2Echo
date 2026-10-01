package com.github.wrx886.e2echo.client.repository;

import java.util.Optional;

import com.github.wrx886.e2echo.client.entity.SysParam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统参数数据访问层。
 *
 * <p>参数按登入用户隔离（实体继承 {@code BaseEntity} 因此带有 {@code owner}），同一台机器上可能
 * 有多个用户的数据，所以按参数名查询、删除时都要带上 {@code owner}。</p>
 */
@Repository
public interface SysParamRepository extends JpaRepository<SysParam, String> {

    /**
     * 查询当前用户的某个参数。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @param key   参数名
     * @return 参数，未设置时为空
     */
    Optional<SysParam> findByOwnerAndKey(String owner, String key);

    /**
     * 删除当前用户的某个参数。
     *
     * <p>派生删除语句需要事务，调用方（{@code SysParamService.remove}）已标注
     * {@code @Transactional}。</p>
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @param key   参数名
     * @return 删除的记录数，参数不存在时为 0
     */
    long deleteByOwnerAndKey(String owner, String key);

}
