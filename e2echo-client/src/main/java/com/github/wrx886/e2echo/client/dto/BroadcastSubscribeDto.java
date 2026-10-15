package com.github.wrx886.e2echo.client.dto;

import com.github.wrx886.e2echo.client.entity.BroadcastSubscribe;

import java.time.LocalDateTime;

/**
 * 广播订阅数据传输对象。
 *
 * <p>把订阅实体摊平成前端要用的字段，带出主键与审计时间；别名可能为空。</p>
 *
 * @param id         主键 ID
 * @param owner      数据所有者，即订阅方的公钥
 * @param createTime 创建时间
 * @param updateTime 修改时间
 * @param publicKey  被订阅者的公钥
 * @param alias      被订阅者别名，可以为空
 */
public record BroadcastSubscribeDto(
        String id,
        String owner,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        String publicKey,
        String alias
) {

    /**
     * 由订阅实体生成 DTO。
     *
     * @param broadcastSubscribe 订阅实体
     * @return 订阅 DTO
     */
    public static BroadcastSubscribeDto fromEntity(BroadcastSubscribe broadcastSubscribe) {
        return new BroadcastSubscribeDto(
                broadcastSubscribe.getId(),
                broadcastSubscribe.getOwner(),
                broadcastSubscribe.getCreateTime(),
                broadcastSubscribe.getUpdateTime(),
                broadcastSubscribe.getPublicKey(),
                broadcastSubscribe.getAlias()
        );
    }

}
