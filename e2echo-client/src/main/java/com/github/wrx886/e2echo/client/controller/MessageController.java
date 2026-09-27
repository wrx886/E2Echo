package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.handler.ChatTextMessageHandler;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.vo.MessageVo;
import com.github.wrx886.e2echo.client.vo.SendTestMessageReqVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 消息接口。
 *
 * <p>客户端自己的 HTTP 接口，供前端使用：查询某个会话的消息、发送文字聊天消息。</p>
 */
@Validated
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    /**
     * 消息业务逻辑对象。
     */
    private final MessageService messageService;

    /**
     * 文字聊天消息处理器。
     */
    private final ChatTextMessageHandler chatTextMessageHandler;

    /**
     * 分页查询某个会话的消息（收 + 发），最新的在前。
     *
     * @param peer     会话对方，私聊时为对方公钥、群聊时为群聊标识
     * @param endSeq   倒序翻页的游标（序号的十进制字符串），仅返回序号小于该值的消息，为空表示从头开始
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 消息分页结果
     * @throws E2EchoException endSeq 不是合法的整数
     */
    @GetMapping("findConversation")
    public Result<Page<MessageVo>> findConversation(
            @NotBlank String peer,
            String endSeq,
            @NotNull Integer pageNum,
            @NotNull Integer pageSize
    ) {
        Long endSeqLong = null;
        if (StringUtils.hasLength(endSeq)) {
            try {
                endSeqLong = Long.parseLong(endSeq);
            } catch (Exception e) {
                throw new E2EchoException("endSeq 应该为空或int64字符串！");
            }
        }
        return Result.ok(messageService.findConversation(peer, endSeqLong, pageNum, pageSize));
    }

    /**
     * 发送文字聊天消息。
     *
     * @param sendTestMessageReqVo 接收者、是否群聊与消息正文
     * @return 空结果
     */
    @PostMapping("sendTextMessage")
    public Result<Void> sendTextMessage(@Valid @RequestBody SendTestMessageReqVo sendTestMessageReqVo) {
        chatTextMessageHandler.send(sendTestMessageReqVo.to(), sendTestMessageReqVo.group(), sendTestMessageReqVo.text());
        return Result.ok();
    }

}
