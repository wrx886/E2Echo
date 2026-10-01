package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.dto.GroupMemberDto;
import com.github.wrx886.e2echo.client.handler.ChatGroupKeyMessageHandler;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.service.GroupMemberService;
import com.github.wrx886.e2echo.client.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 群聊管理接口。
 *
 * <p>客户端自己的 HTTP 接口，供前端使用：群成员名单的增删查、生成群标识、轮换群密钥、重发群密钥。</p>
 */
@Validated
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    /**
     * 群聊管理业务对象。
     */
    private final GroupService service;

    /**
     * 群成员业务对象。
     */
    private final GroupMemberService groupMemberService;

    /**
     * 群密钥消息处理器，用于重发群密钥。
     */
    private final ChatGroupKeyMessageHandler chatGroupKeyMessageHandler;

    /**
     * 分页查询某个群的成员。
     *
     * @param group    群标识
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 成员分页结果
     */
    @GetMapping("member/list")
    public Result<Page<GroupMemberDto>> listMember(
            @NotBlank String group,
            @NotNull Integer pageNum,
            @NotNull Integer pageSize
    ) {
        return Result.ok(groupMemberService.listByGroup(group, pageNum, pageSize));
    }

    /**
     * 新增群成员：只有群主能加人，加完会把最新的群密钥私聊发给该成员。
     *
     * <p>成员已经在名单里、或者群还没有密钥时接口会失败，这两种情况都不会写入成员记录（新增与
     * 分发在同一事务里）。</p>
     *
     * @param groupMemberDto 待新增的成员（群标识 + 成员公钥）
     * @return 空结果
     */
    @PostMapping("member")
    public Result<Void> saveMember(@Valid @RequestBody GroupMemberDto groupMemberDto) {
        groupMemberService.save(groupMemberDto);
        return Result.ok();
    }

    /**
     * 删除群成员。
     *
     * @param id 群成员记录的主键
     * @return 空结果
     */
    @DeleteMapping("member/{id}")
    public Result<Void> deleteMemberById(@PathVariable String id) {
        groupMemberService.deleteById(id);
        return Result.ok();
    }

    /**
     * 生成一个新的群标识（以当前用户公钥开头，即当前用户是群主）。
     *
     * @return 群标识
     */
    @GetMapping("generateGroupId")
    public Result<String> generateGroupId() {
        return Result.ok(service.generateGroupId());
    }

    /**
     * 轮换群密钥：生成新密钥并分发给全部成员。
     *
     * @param group 群标识
     * @return 空结果
     */
    @PostMapping("updateGroupKey")
    public Result<Void> updateGroupKey(@NotBlank String group) {
        service.updateGroupKey(group);
        return Result.ok();
    }

    /**
     * 重发群密钥：把当前最新的群密钥再分发给全部成员。
     *
     * @param group 群标识
     * @return 空结果
     */
    @PostMapping("resendGroupKey")
    public Result<Void> resendGroupKey(@NotBlank String group) {
        chatGroupKeyMessageHandler.send(group);
        return Result.ok();
    }

}
