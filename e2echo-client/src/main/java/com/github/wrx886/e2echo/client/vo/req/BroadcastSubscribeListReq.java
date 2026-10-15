package com.github.wrx886.e2echo.client.vo.req;

import jakarta.validation.constraints.NotNull;

/**
 * 查询广播订阅列表的请求体。
 *
 * @param pageNum          页码，从 1 开始
 * @param pageSize         每页条数
 * @param publicKeyOrAlias 关键词，按被订阅者公钥或别名模糊匹配（不区分大小写）；为空表示不过滤
 */
public record BroadcastSubscribeListReq(
        @NotNull Integer pageNum,
        @NotNull Integer pageSize,
        String publicKeyOrAlias
) {
}
