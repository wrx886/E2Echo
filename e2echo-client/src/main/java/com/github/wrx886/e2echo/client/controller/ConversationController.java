package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.dto.ConversationDto;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.service.ConversationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 会话接口。
 *
 * <p>客户端自己的 HTTP 接口，供前端使用：列出会话、按会话对方查询会话、保存会话。</p>
 */
@Validated
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    /**
     * 会话业务逻辑对象。
     */
    private final ConversationService conversationService;

    /**
     * 分页列出当前用户的会话，最近有消息的排在前面。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 会话分页结果
     */
    @GetMapping("list")
    public Result<Page<ConversationDto>> list(
            @NotNull Integer pageNum,
            @NotNull Integer pageSize
    ) {
        return Result.ok(conversationService.list(pageNum, pageSize));
    }

    /**
     * 按会话对方查询会话。
     *
     * @param peer 会话对方，私聊时为对方公钥、群聊时为群聊标识
     * @return 会话，不存在时 {@code data} 为 {@code null}
     */
    @GetMapping("{peer}")
    public Result<ConversationDto> get(
            @PathVariable @NotBlank String peer
    ) {
        return Result.ok(conversationService.findByPeer(peer));
    }

    /**
     * 保存会话：新建会话，或修改已有会话的别名、是否启用等。
     *
     * @param conversationDto 待保存的会话
     * @return 空结果
     */
    @PostMapping
    public Result<Void> save(
            @RequestBody @Valid ConversationDto conversationDto) {
        conversationService.save(conversationDto);
        return Result.ok();
    }

    /**
     * 查询会话别名：前端展示会话名时用，没有设置别名时返回 {@code null}。
     *
     * @param peer 会话对方，私聊时为对方公钥、群聊时为群聊标识
     * @return 别名，没有对应会话或没有设置别名时 {@code data} 为 {@code null}
     */
    @GetMapping("alias/{peer}")
    public Result<String> getAlias(
            @PathVariable @NotBlank String peer
    ) {
        return Result.ok(conversationService.findAliasByPeer(peer));
    }

    /**
     * 统计当前用户的未读消息总数（所有会话的未读数之和）。
     *
     * @return 未读消息总数，没有未读时是 0
     */
    @GetMapping("countUnread")
    public Result<Integer> countUnread() {
        return Result.ok(conversationService.countUnread());
    }

}
