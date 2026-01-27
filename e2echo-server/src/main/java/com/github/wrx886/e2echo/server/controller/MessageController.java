package com.github.wrx886.e2echo.server.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "消息")
@RestController
@RequestMapping("/server/message")
@AllArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "发送消息")
    @PostMapping("/sendOne")
    public Result<Void> sendOne(
            @Parameter(description = "消息") @RequestBody EccMessage eccMessage) {
        messageService.sendOne(eccMessage);
        return Result.ok();
    }

    @Operation(summary = "接收消息")
    @PostMapping("/receiveOne")
    public Result<List<EccMessage>> receiveOne(
            @Parameter(description = "接收者公钥") String toPublicKeyHex,
            @Parameter(description = "起始时间") String startTimestamp) {
        return Result.ok(messageService.receiveOne(toPublicKeyHex, startTimestamp));
    }

    @Operation(summary = "发送群聊消息")
    @PostMapping("/sendGroup")
    public Result<Void> sendGroup(
            @Parameter(description = "消息，toPublicKeyHex字段填写群聊 UUID（格式：{群主公钥}:{群聊UUID}）") @RequestBody EccMessage eccMessage) {
        messageService.sendGroup(eccMessage);
        return Result.ok();
    }

    @Operation(summary = "接收群聊消息")
    @PostMapping("/receiveGroup")
    public Result<List<EccMessage>> receiveGroup(
            @Parameter(description = "群聊 UUID（格式：{群主公钥}:{群聊UUID}）") String groupUuid,
            @Parameter(description = "起始时间") String startTimestamp) {
        return Result.ok(messageService.receiveGroup(groupUuid, startTimestamp));
    }

}
