package com.github.wrx886.e2echo.server.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.ecc.EccMessage;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.service.MessageService;
import com.github.wrx886.e2echo.server.vo.req.MessageListReqVo;

import lombok.RequiredArgsConstructor;

/**
 * 消息接口。
 *
 * <p>对外提供消息的保存与查询能力，统一以 {@link EccMessage} 作为交互视图，并以
 * {@link Result} 包装返回。</p>
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    /**
     * 消息业务逻辑对象。
     */
    private final MessageService messageService;

    /**
     * 保存消息。
     *
     * @param eccMessage 待保存的消息
     * @return 保存后的消息
     */
    @PostMapping
    public Result<EccMessage> save(@RequestBody EccMessage eccMessage) {
        return Result.ok(messageService.save(eccMessage));
    }

    /**
     * 根据 ID 查询消息。
     *
     * @param id 消息 ID
     * @return 消息
     */
    @GetMapping("/{id}")
    public Result<EccMessage> getById(@PathVariable String id) {
        return Result.ok(messageService.getById(id));
    }

    /**
     * 按条件分页查询消息列表。
     *
     * <p>过滤条件均为可选，未传时表示不参与过滤；结果默认按时间从老到新返回，可通过
     * {@code order} 改为从新到老——例如查询某个用户最新的用户信息时，可传
     * {@code channel=USERINFO}、{@code fromList={用户公钥}}、{@code order=desc}、
     * {@code pageSize=1}。</p>
     *
     * <p>翻页时按排序方向选游标：升序用 {@code startId}（上一页最后一条的消息 ID，只取更大的），
     * 降序用 {@code endId}（只取更小的）。</p>
     *
     * <p>查询条件较多，因此用请求体接收（见 {@link MessageListReqVo}），并以 POST 提交；分页深度
     * 也有限制：每页不超过 256 条、页码不超过 16，要取更深的数据请用游标翻页。</p>
     *
     * @param reqVo 分页查询参数
     * @return 分页消息列表
     */
    @PostMapping("/list")
    public Result<Page<EccMessage>> list(@RequestBody MessageListReqVo reqVo) {
        return Result.ok(messageService.list(reqVo));
    }

}
