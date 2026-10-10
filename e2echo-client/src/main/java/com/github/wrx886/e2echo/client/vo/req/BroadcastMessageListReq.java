package com.github.wrx886.e2echo.client.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 查询广播消息列表的请求体。
 *
 * <p>列表是按 {@code to} 拉取的：空字符串拉帖子列表，某条帖子的消息 ID 拉该帖子下的评论（含二级
 * 回复）；{@code from} 用来只看某个作者的帖子/评论。过滤项都必传，不需要限制的传空串。</p>
 *
 * @param from           作者公钥，只查这个人的帖子/评论，空串表示不按作者过滤
 * @param to             列表目标：空字符串是帖子列表，帖子的消息 ID 则该帖子的评论
 * @param startTimestamp 起始时间戳（毫秒，含），空串表示不限制
 * @param endTimestamp   结束时间戳（毫秒，含），空串表示不限制
 * @param startId        起始消息 ID，仅返回 ID 大于该值的消息，空串表示不限制
 * @param endId          结束消息 ID，仅返回 ID 小于该值的消息，空串表示不限制
 * @param order          排序方向，{@code asc} 从老到新、{@code desc} 从新到老
 * @param pageNum        页码，从 1 开始
 * @param pageSize       每页条数
 */
public record BroadcastMessageListReq(
        @NotNull String from,
        @NotNull String to,
        @NotNull String startTimestamp,
        @NotNull String endTimestamp,
        @NotNull String startId,
        @NotNull String endId,
        @NotBlank String order,
        @NotNull Integer pageNum,
        @NotNull Integer pageSize
) {
}
