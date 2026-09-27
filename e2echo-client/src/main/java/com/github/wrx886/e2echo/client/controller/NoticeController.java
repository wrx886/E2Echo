package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 前端通知接口。
 *
 * <p>客户端自己的 HTTP 接口：前端用 SSE 连上来，之后客户端有数据变化时通过这条连接推
 * {@code reflash}，内容与 notice 通道一致，只表示“该重新拉取了”。</p>
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
     * @return SSE 连接对象
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        return noticeService.connect();
    }

}
