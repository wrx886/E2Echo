package com.github.wrx886.e2echo.client.repository;

import com.github.wrx886.e2echo.client.entity.GroupMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 群成员数据访问层。
 *
 * <p>成员按登入用户隔离，查询都带上 {@code owner}（当前登入用户的公钥）。</p>
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, String> {

    /**
     * 分页查询某个群的成员，按主键升序。
     *
     * @param owner    数据所有者，即当前登入用户的公钥
     * @param group    群标识
     * @param pageable 分页参数
     * @return 成员分页结果
     */
    Page<GroupMember> findAllByOwnerAndGroupOrderById(String owner, String group, Pageable pageable);

    /**
     * 查询某个群的全部成员，按主键升序。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @param group 群标识
     * @return 成员列表
     */
    List<GroupMember> findAllByOwnerAndGroupOrderById(String owner, String group);

    /**
     * 删除当前用户某个成员记录。
     *
     * @param owner 数据所有者，即当前登入用户的公钥
     * @param id    成员记录的主键
     */
    void deleteByOwnerAndId(String owner, String id);

}
