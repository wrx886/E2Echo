package com.github.wrx886.e2echo.server.vo.req;

import java.util.List;

/**
 * 分页查询消息列表的请求体。
 *
 * <p>{@code channel} 必填，其余过滤条件可选，为 {@code null} 或空白时不参与过滤。翻页按排序方向
 * 选游标：升序用 {@code startId}（只取更大的），降序用 {@code endId}（只取更小的）。</p>
 *
 * <p>时间戳参数仍按毫秒语义接收；服务端会将其转换为消息 ID 的字典序范围处理，不额外保存时间字段。</p>
 *
 * @param fromList       发送者公钥列表，为空表示不过滤
 * @param toList         接收者信息列表，为空表示不过滤
 * @param channel        消息通道（必填）
 * @param startTimestamp 起始时间戳（毫秒，含），为空表示不限制
 * @param endTimestamp   结束时间戳（毫秒，含），为空表示不限制
 * @param startId        起始消息 ID，仅返回 ID 大于该值的消息，为空表示不限制
 * @param endId          结束消息 ID，仅返回 ID 小于该值的消息，为空表示不限制
 * @param order          排序方向，{@code asc} 从老到新（默认）、{@code desc} 从新到老
 * @param pageNum        页码，从 1 开始、不超过 16（必填）
 * @param pageSize       每页条数，不超过 256（必填）
 */
public record MessageListReqVo(
        List<String> fromList,
        List<String> toList,
        String channel,
        String startTimestamp,
        String endTimestamp,
        String startId,
        String endId,
        String order,
        Integer pageNum,
        Integer pageSize
) {
}
