package com.github.wrx886.e2echo.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Persistable;

/**
 * 群成员实体，对应数据库 {@code group_member} 表。
 *
 * <p>记录某个群（{@code group_}）里的成员（{@code member} 是成员的公钥）：群主用它决定把群密钥
 * 分发给谁。继承 {@link BaseEntity} 获得主键与审计字段，数据按登入用户隔离；同一用户、同一个群的
 * 同一个成员只允许一条记录（唯一约束）。</p>
 *
 * <p>{@code group} 是数据库保留字，因此对应列名改为 {@code group_}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "group_member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_member_owner_group_member", columnNames = {"owner", "group_", "member"})
})
public class GroupMember extends BaseEntity implements Persistable<String> {

    /**
     * 群标识，由群主创建时生成（以群主公钥开头）。
     */
    @Column(name = "group_", nullable = false)
    private String group;

    /**
     * 成员的公钥。
     */
    @Column(nullable = false)
    private String member;

    /**
     * 判断实体是否为新建，恒为 {@code true}。
     *
     * <p>成员记录只新增、不修改，恒按新建处理，使保存始终执行插入（带 id 保存也按新增处理）。</p>
     *
     * @return 恒为 {@code true}
     */
    @Override
    public boolean isNew() {
        return true;
    }

}
