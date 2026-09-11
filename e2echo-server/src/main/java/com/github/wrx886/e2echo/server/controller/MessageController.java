package com.github.wrx886.e2echo.server.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.ecc.EccMessage;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.service.MessageService;

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
     * {@code channel=USERINFO&fromList={用户公钥}&order=desc&pageSize=1}。</p>
     *
     * @param fromList       发送者公钥列表
     * @param toList         接收者信息列表
     * @param channel        消息通道
     * @param startTimestamp 起始时间戳（毫秒，含）
     * @param endTimestamp   结束时间戳（毫秒，含）
     * @param startId        起始消息 ID，仅返回 ID 大于该值的消息
     * @param order          排序方向，{@code asc} 从老到新（默认）、{@code desc} 从新到老
     * @param pageNum        页码，从 1 开始（必填）
     * @param pageSize       每页条数（必填）
     * @return 分页消息列表
     */
    @GetMapping
    public Result<Page<EccMessage>> list(
            @RequestParam(required = false) List<String> fromList,
            @RequestParam(required = false) List<String> toList,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String startTimestamp,
            @RequestParam(required = false) String endTimestamp,
            @RequestParam(required = false) String startId,
            @RequestParam(required = false) String order,
            @RequestParam int pageNum,
            @RequestParam int pageSize) {
        return Result.ok(messageService.list(fromList, toList, channel, startTimestamp, endTimestamp, startId,
                order, pageNum, pageSize));
    }

}
