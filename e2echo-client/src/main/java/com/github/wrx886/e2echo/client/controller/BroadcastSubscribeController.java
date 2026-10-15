package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.dto.BroadcastSubscribeDto;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.service.BroadcastSubscribeService;
import com.github.wrx886.e2echo.client.vo.req.BroadcastSubscribeListReq;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 广播订阅接口。
 *
 * <p>客户端自己的 HTTP 接口：管理当前用户的订阅（订阅、取消订阅、改别名、分页查询）。按订阅拉帖子在
 * {@code BroadcastController} 里。</p>
 */
@Validated
@RestController
@RequestMapping("api/broadcast/subscribe")
@RequiredArgsConstructor
public class BroadcastSubscribeController {

    /**
     * 订阅业务对象。
     */
    private final BroadcastSubscribeService service;

    /**
     * 查询当前用户对某个作者的订阅。
     *
     * @param publicKey 被订阅者的公钥
     * @return 订阅，没有订阅时 {@code data} 为 {@code null}
     */
    @GetMapping("{publicKey}")
    public Result<BroadcastSubscribeDto> getBroadcastSubscribe(@NotBlank @PathVariable String publicKey) {
        return Result.ok(service.getOne(publicKey));
    }

    /**
     * 订阅某个作者。
     *
     * @param publicKey 被订阅者的公钥
     * @return 空结果
     */
    @PostMapping("subscribe/{publicKey}")
    public Result<Void> subscribe(@NotBlank @PathVariable String publicKey) {
        service.subscribe(publicKey);
        return Result.ok();
    }

    /**
     * 取消订阅。
     *
     * @param publicKey 被订阅者的公钥
     * @return 空结果
     */
    @PostMapping("unsubscribe/{publicKey}")
    public Result<Void> unsubscribe(@NotBlank @PathVariable String publicKey) {
        service.unsubscribe(publicKey);
        return Result.ok();
    }

    /**
     * 修改订阅别名：给已订阅的作者起一个本机展示用的名字，传空表示清除别名。
     *
     * @param publicKey 被订阅者的公钥
     * @param alias     新别名，可以为空（空表示清除）
     * @return 空结果
     */
    @PostMapping("setAlias/{publicKey}")
    public Result<Void> setAlias(@NotBlank @PathVariable String publicKey, String alias) {
        service.setAlias(publicKey, alias);
        return Result.ok();
    }

    /**
     * 分页查询当前用户的订阅。
     *
     * @param req 查询条件（关键词、分页）
     * @return 订阅分页结果
     */
    @PostMapping("list")
    public Result<Page<BroadcastSubscribeDto>> list(@RequestBody @Valid BroadcastSubscribeListReq req) {
        return Result.ok(service.list(req.pageNum(), req.pageSize(), req.publicKeyOrAlias()));
    }

}
