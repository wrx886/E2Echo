package com.github.wrx886.e2echo.client.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 查询广播消息列表的请求体。
 *
 * <p>列表按 {@code to} 区分：传 {@code *} 拿帖子列表，传某条帖子的消息 ID 拿该帖子下的评论（含二级
 * 回复）。{@code from}、{@code to} 都是列表，可以为后续的订阅一次查多个作者/多个目标；两者都必传，
 * 不需要过滤的方向传空列表，但 {@code to} 不能整体留空，否则帖子和评论会混在一起。</p>
 *
 * @param from           作者公钥列表，只查这些人的帖子/评论，空列表表示不按作者过滤
 * @param to             列表目标：{@code *} 是帖子列表，帖子的消息 ID 是该帖子下的评论
 * @param startTimestamp 起始时间戳（毫秒，含），空串表示不限制
 * @param endTimestamp   结束时间戳（毫秒，含），空串表示不限制
 * @param startId        起始消息 ID，仅返回 ID 大于该值的消息，空串表示不限制
 * @param endId          结束消息 ID，仅返回 ID 小于该值的消息，空串表示不限制
 * @param order          排序方向，{@code asc} 从老到新、{@code desc} 从新到老
 * @param pageNum        页码，从 1 开始
 * @param pageSize       每页条数
 */
public record BroadcastMessageListReq(
        @NotNull List<String> from,
        @NotNull List<String> to,
        @NotNull String startTimestamp,
        @NotNull String endTimestamp,
        @NotNull String startId,
        @NotNull String endId,
        @NotBlank String order,
        @NotNull Integer pageNum,
        @NotNull Integer pageSize
) {
}
