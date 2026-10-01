package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.dto.GroupMemberDto;
import com.github.wrx886.e2echo.client.entity.GroupMember;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.handler.ChatGroupKeyMessageHandler;
import com.github.wrx886.e2echo.client.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * 群成员业务逻辑层。
 *
 * <p>维护当前用户记录的群成员名单（群主用它决定把群密钥分发给谁）：新增成员时会顺带把最新的群密钥
 * 私聊发给该成员。数据按登入用户隔离，且只有群主（群标识以自己公钥开头）能改动名单。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMemberService {

    /**
     * 群成员数据访问对象。
     */
    private final GroupMemberRepository repository;

    /**
     * 删除群成员。
     *
     * @param id 群成员记录的主键
     */
    public void deleteById(String id) {
        repository.deleteByOwnerAndId(currentOwner(), id);
    }

    /**
     * 分页查询某个群的成员，按主键升序。
     *
     * @param group    群标识
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 成员分页结果
     */
    public Page<GroupMemberDto> listByGroup(String group, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        return repository.findAllByOwnerAndGroupOrderById(currentOwner(), group, pageable)
                .map(GroupMemberDto::fromEntity);
    }

    /**
     * 查询某个群的全部成员，按主键升序（分发密钥时用）。
     *
     * @param group 群标识
     * @return 成员列表
     */
    public List<GroupMemberDto> listByGroup(String group) {
        return repository.findAllByOwnerAndGroupOrderById(currentOwner(), group)
                .stream()
                .map(GroupMemberDto::fromEntity)
                .toList();
    }

    /**
     * 新增群成员（DTO 入口）。
     *
     * @param groupMemberDto 待新增的成员
     */
    public void save(GroupMemberDto groupMemberDto) {
        save(groupMemberDto.toEntity());
    }

    /**
     * 新增群成员：只有群主能加人，加完把最新的群密钥私聊发给该成员。
     *
     * @param groupMember 待新增的成员
     * @throws E2EchoException 自己不是群主
     */
    public void save(GroupMember groupMember) {
        // 判断是否为群主
        if (!currentOwner().equals(groupMember.getGroup().substring(0, currentOwner().length()))) {
            throw new E2EchoException("非群主，禁止管理！");
        }
        repository.save(groupMember);

        // 分发密钥到这个人
        BeanProvider.getBean(ChatGroupKeyMessageHandler.class)
                .send(groupMember.getGroup(), groupMember.getMember());
    }

}
