package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.api.MessageApi;
import com.github.wrx886.e2echo.client.enums.ChannelEnum;
import com.github.wrx886.e2echo.client.enums.MessageTypeEnum;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.PageData;
import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastMessageVo;
import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastUserinfo;
import com.github.wrx886.e2echo.client.vo.req.BroadcastMessageListReq;
import com.github.wrx886.e2echo.client.vo.req.MessageListReqVo;
import com.github.wrx886.e2echo.client.vo.resp.BroadcastMessageRespVo;
import com.github.wrx886.e2echo.ecc.Ecc;
import com.github.wrx886.e2echo.ecc.EccMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 广播业务逻辑层。
 *
 * <p>广播只存服务端、不落本地库，所以这里都是直接和服务端打交道：读取按通道与类型查询，再用消息里
 * 的签名确认数据没有被篡改。广播分三种用途，都靠通道或 {@code to} 区分：用户资料（通道
 * {@code BROADCAST_USERINFO_NONE}）、帖子（{@code to = *}）、评论（{@code to = 帖子的消息 ID}）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    /**
     * 发帖时 {@code to} 的占位符。
     *
     * <p>帖子没有具体接收者，而服务端要求 {@code to} 非空，所以用 {@code *} 占位。调用方不用关心它：
     * 发帖、拉帖子列表时都传空字符串，这里会换成这个占位符（服务端只是存下来，不会拿它去匹配）。</p>
     */
    private static final String POST_TO = "*";

    /**
     * 消息接口，用于读写服务端上的广播消息。
     */
    private final MessageApi messageApi;

    /**
     * JSON 对象映射器，用于广播正文的序列化与反序列化。
     */
    private final ObjectMapper objectMapper;

    /**
     * 查询某个用户的资料：取该用户最新的一条用户资料广播。
     *
     * @param uid 用户公钥
     * @return 用户资料，没有资料或签名校验不通过时返回 {@code null}
     */
    public BroadcastUserinfo userinfo(@NotBlank String uid) {
        PageData<EccMessage> eccMessages = messageApi.list(new MessageListReqVo(
                List.of(uid),
                null,
                ChannelEnum.BROADCAST_USERINFO_NONE.name(),
                MessageTypeEnum.BROADCAST_USERINFO.name(),
                null,
                null,
                null,
                null,
                "DESC",
                1,
                1
        ));

        // 用户没有用户数据
        if (eccMessages.content().isEmpty()) {
            return null;
        }

        // 获取用户数据
        EccMessage eccMessage = eccMessages.content().get(0);

        // 验证签名
        if (!Ecc.verify(eccMessage)) {
            return null;
        }

        // 正式返回数据
        return objectMapper.readValue(eccMessage.getMessage(), BroadcastUserinfo.class);
    }

    /**
     * 发布当前用户的资料：签名后作为一条广播消息发给服务端。
     *
     * @param userinfo 用户资料
     * @throws E2EchoException 签名失败，或发送失败
     */
    public void userinfo(@Valid BroadcastUserinfo userinfo) {
        EccMessage eccMessage = new EccMessage();
        eccMessage.setFrom(Ecc.getPublicKey());
        // 广播类消息没有具体接收者，但服务端要求 to 非空，这里统一用 * 占位（只起占位作用，没有实际
        // 含义：拉取按通道和类型过滤，不会拿 to 去匹配）
        eccMessage.setTo("*");
        eccMessage.setMessage(objectMapper.writeValueAsString(userinfo));
        eccMessage.setType(MessageTypeEnum.BROADCAST_USERINFO.name());
        eccMessage.setChannel(ChannelEnum.BROADCAST_USERINFO_NONE.name());
        eccMessage.setInfo("{}");
        try {
            eccMessage = Ecc.sign(eccMessage);
        } catch (Exception e) {
            throw new E2EchoException("消息签名失败！");
        }

        // 发送
        messageApi.save(eccMessage);
    }

    /**
     * 发布一条广播消息：帖子或评论。
     *
     * @param messageVo 广播正文（文本 + 附件）
     * @param to        发布目标：空字符串是发帖，帖子的消息 ID 则是在该帖子下评论
     * @throws E2EchoException 签名失败，或发送失败
     */
    public void message(@Valid BroadcastMessageVo messageVo, @NotNull String to) {
        EccMessage eccMessage = new EccMessage();
        eccMessage.setFrom(Ecc.getPublicKey());
        // 传空就当发帖（用占位符）；否则把帖子 ID 填进 to，服务端据此知道这是该帖子下的评论
        eccMessage.setTo(StringUtils.hasText(to) ? to : POST_TO);
        eccMessage.setMessage(objectMapper.writeValueAsString(messageVo));
        eccMessage.setType(MessageTypeEnum.BROADCAST_MESSAGE.name());
        eccMessage.setChannel(ChannelEnum.BROADCAST_MESSAGE_NONE.name());
        eccMessage.setInfo("{}");
        try {
            eccMessage = Ecc.sign(eccMessage);
        } catch (Exception e) {
            throw new E2EchoException("消息签名失败！");
        }

        // 发送
        messageApi.save(eccMessage);
    }

    /**
     * 按消息 ID 查询一条广播消息（帖子或评论）。
     *
     * @param id 消息 ID
     * @return 广播消息，消息不存在或签名校验不通过时返回 {@code null}
     */
    public BroadcastMessageRespVo message(@NotBlank String id) {
        EccMessage eccMessage = messageApi.getById(id);
        // 验证签名
        if (eccMessage == null || !Ecc.verify(eccMessage)) {
            return null;
        }
        return new BroadcastMessageRespVo(
                eccMessage.getId(),
                eccMessage.getFrom(),
                eccMessage.getTo(),
                objectMapper.readValue(eccMessage.getMessage(), BroadcastMessageVo.class),
                eccMessage.getType(),
                eccMessage.getChannel(),
                eccMessage.getInfo()
        );
    }

    /**
     * 分页查询广播消息列表：{@code to} 传空字符串是帖子列表，传帖子 ID 是该帖子下的评论；{@code from}
     * 传作者公钥就只看这个人的帖子/评论，传空字符串表示不按作者过滤。
     *
     * <p>每一条都会验签，验签失败（或正文解析失败）的那条在结果里占位为 {@code null}，这样前端还能
     * 按顺序展示其余消息，不会因为一条坏数据整页失败。</p>
     *
     * @param req 查询条件（作者、目标、时间范围、游标、分页）；目标是空字符串时查帖子列表
     * @return 广播消息分页结果，坏数据对应 {@code null}
     */
    public PageData<BroadcastMessageRespVo> listMessage(@Valid BroadcastMessageListReq req) {
        // 拉取消息
        PageData<EccMessage> eccMessages = messageApi.list(new MessageListReqVo(
                StringUtils.hasText(req.from()) ? List.of(req.from()) : null,
                List.of(StringUtils.hasText(req.to()) ? req.to() : POST_TO),
                ChannelEnum.BROADCAST_MESSAGE_NONE.name(),
                MessageTypeEnum.BROADCAST_MESSAGE.name(),
                req.startTimestamp(),
                req.endTimestamp(),
                req.startId(),
                req.endId(),
                req.order(),
                req.pageNum(),
                req.pageSize()
        ));

        // 消息转换：验签失败或正文解析失败的保留为 null，占住位置，前端按顺序展示其余消息
        List<BroadcastMessageRespVo> broadcastMessageRespVos = eccMessages.content().stream().map((eccMessage -> {
            try {
                // 校验签名
                if (!Ecc.verify(eccMessage)) {
                    return null;
                }
                return new BroadcastMessageRespVo(
                        eccMessage.getId(),
                        eccMessage.getFrom(),
                        eccMessage.getTo(),
                        objectMapper.readValue(eccMessage.getMessage(), BroadcastMessageVo.class),
                        eccMessage.getType(),
                        eccMessage.getChannel(),
                        eccMessage.getInfo()
                );
            } catch (Exception e) {
                log.error("广播消息处理失败！", e);
                return null;
            }
        })).toList();

        return new PageData<>(
                broadcastMessageRespVos,
                eccMessages.number(),
                eccMessages.size(),
                eccMessages.totalElements()
        );
    }
}
