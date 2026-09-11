package com.github.wrx886.e2echo.server.controller;

import com.github.wrx886.e2echo.server.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 时间戳接口。
 *
 * <p>用于向客户端提供服务器当前时间，便于客户端校准本地时间、控制消息发送时间的偏差。</p>
 */
@RestController
@RequestMapping("timestamp")
public class TimestampController {

    /**
     * 获取服务器当前时间戳。
     *
     * @return 当前时间的毫秒数（十进制字符串）
     */
    @GetMapping
    public Result<String> timestamp() {
        return Result.ok(Long.toString(System.currentTimeMillis()));
    }

}
