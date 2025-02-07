package com.github.wrx886.e2echo.server.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.server.common.LoginHold;
import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.model.vo.message.MessageVo;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.web.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "消息收发")
@RestController
@RequestMapping("server/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Operation(summary = "发送消息")
    @PostMapping("send")
    public Result<Void> send(@RequestBody Message message) {
        messageService.send(LoginHold.getUser(), message);
        return Result.ok();
    }

    @Operation(summary = "接收消息")
    @PostMapping("receive")
    public Result<List<Message>> receive(@RequestBody MessageVo messageVo) {
        return Result.ok(messageService.receive(LoginHold.getUser(), messageVo));
    }

}
