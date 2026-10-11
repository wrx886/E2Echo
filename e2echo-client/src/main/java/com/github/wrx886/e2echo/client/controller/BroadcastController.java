package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.result.PageData;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.service.BroadcastService;
import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastUserinfo;
import com.github.wrx886.e2echo.client.vo.req.BroadcastMessageListReq;
import com.github.wrx886.e2echo.client.vo.req.BroadcastMessageReq;
import com.github.wrx886.e2echo.client.vo.resp.BroadcastMessageRespVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 广播接口。
 *
 * <p>客户端自己的 HTTP 接口：读取或发布广播类的数据——用户资料、帖子与评论。广播只存服务端、不落
 * 本地库，所以这里都是直接与服务器交互。</p>
 */
@Validated
@RestController
@RequestMapping("api/broadcast")
@RequiredArgsConstructor
public class BroadcastController {

    /**
     * 广播业务对象。
     */
    private final BroadcastService service;

    /**
     * 查询某个用户的资料。
     *
     * @param uid 用户公钥
     * @return 用户资料，没有资料时 {@code data} 为 {@code null}
     */
    @GetMapping("userinfo/{uid}")
    public Result<BroadcastUserinfo> userinfo(@NotBlank @PathVariable String uid) {
        return Result.ok(service.userinfo(uid));
    }

    /**
     * 发布当前用户的资料（签名后作为广播消息发到服务端）。
     *
     * @param userinfo 用户资料
     * @return 空结果
     */
    @PostMapping("userinfo")
    public Result<Void> userinfo(@Valid @RequestBody BroadcastUserinfo userinfo) {
        service.userinfo(userinfo);
        return Result.ok();
    }

    /**
     * 发布广播消息：{@code to} 传空字符串是发帖，填帖子消息 ID 是在该帖子下评论（回复某条评论时在
     * 正文的 {@code replyToId} 里指回去）。
     *
     * @param req 发布目标与广播正文
     * @return 空结果
     */
    @PostMapping("message")
    public Result<Void> message(@Valid @RequestBody BroadcastMessageReq req) {
        service.message(req.message(), req.to());
        return Result.ok();
    }

    /**
     * 按消息 ID 查询一条广播消息（帖子或评论）。
     *
     * @param id 消息 ID
     * @return 广播消息，消息不存在时 {@code data} 为 {@code null}
     */
    @GetMapping("message/{id}")
    public Result<BroadcastMessageRespVo> message(@PathVariable @NotBlank String id) {
        return Result.ok(service.message(id));
    }

    /**
     * 分页查询广播消息列表：{@code to} 传 {@code *} 拿帖子列表，传帖子消息 ID 拿该帖子的评论；{@code from}
     * 传作者公钥列表则只看这些人的内容（空列表表示不限作者）。
     *
     * @param req 查询条件（作者、目标、时间范围、游标、分页）
     * @return 广播消息分页结果
     */
    @PostMapping("message/list")
    public Result<PageData<BroadcastMessageRespVo>> listMessage(@Valid @RequestBody BroadcastMessageListReq req) {
        return Result.ok(service.listMessage(req));
    }

}
