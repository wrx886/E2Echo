package com.github.wrx886.e2echo.server.controller;

import com.github.wrx886.e2echo.server.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 通知接口。
 *
 * <p>为客户端提供 SSE 长连接：客户端连接时声明自己关心的目标，此后这些目标发生变化时，
 * 服务端通过该连接把通知推送给客户端。</p>
 */
@RestController
@RequestMapping("notice")
@RequiredArgsConstructor
public class NoticeController {

    /**
     * 通知业务逻辑对象。
     */
    private final NoticeService noticeService;

    /**
     * 建立通知连接。
     *
     * @param client 客户端 ID，同一实例上不可重复
     * @param tos    需要订阅的目标列表
     * @return SSE 连接对象
     */
    @PostMapping(path = "/connect/{client}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String client, @RequestBody List<String> tos) {
        return noticeService.connect(client, tos);
    }

}
