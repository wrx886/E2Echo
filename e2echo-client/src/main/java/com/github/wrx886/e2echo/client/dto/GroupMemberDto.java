package com.github.wrx886.e2echo.client.dto;

import com.github.wrx886.e2echo.client.entity.GroupMember;
import com.github.wrx886.e2echo.client.util.BeanCopyUtils;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * 群成员数据传输对象。
 *
 * <p>在群成员实体与接口之间传递数据：{@code group}、{@code member} 由调用方给出，其余字段由持久化
 * 层填充。</p>
 *
 * @param id         主键 ID
 * @param owner      数据所有者，即登入用户的公钥
 * @param createTime 创建时间
 * @param updateTime 修改时间
 * @param group      群标识
 * @param member     成员的公钥
 */
public record GroupMemberDto(
        String id,
        String owner,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        @NotBlank String group,
        @NotBlank String member
) {

    /**
     * 由群成员实体生成 DTO。
     *
     * @param groupMember 群成员实体
     * @return 群成员 DTO
     */
    public static GroupMemberDto fromEntity(GroupMember groupMember) {
        return new GroupMemberDto(
                groupMember.getId(),
                groupMember.getOwner(),
                groupMember.getCreateTime(),
                groupMember.getUpdateTime(),
                groupMember.getGroup(),
                groupMember.getMember()
        );
    }

    /**
     * 转成群成员实体（只复制非空属性，owner 与审计字段留给持久化层）。
     *
     * @return 群成员实体
     */
    public GroupMember toEntity() {
        GroupMember groupMember = new GroupMember();
        BeanCopyUtils.copyNonNullProperties(this, groupMember);
        return groupMember;
    }

}
