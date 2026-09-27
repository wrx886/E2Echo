package com.github.wrx886.e2echo.client.dto;

import com.github.wrx886.e2echo.client.entity.AesKey;

import java.time.LocalDateTime;

/**
 * AES 密钥数据传输对象。
 *
 * <p>是 {@link AesKey} 的只读快照：密钥查询结果缓存在内存里，缓存中放的是共享对象，直接返回实体会
 * 被调用方改坏，所以统一转成不可变的记录返回。</p>
 *
 * @param id          主键 ID
 * @param owner       数据所有者，即登入用户的公钥
 * @param createTime  创建时间
 * @param updateTime  修改时间
 * @param obj         加密对象，如群聊标识
 * @param publishTime 密钥签发时间（毫秒），同时充当密钥版本
 * @param aesKey      AES 密钥（HEX 格式）
 */
public record AesKeyDto(
        String id,
        String owner,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        String obj,
        Long publishTime,
        String aesKey
) {

    /**
     * 由密钥实体生成快照。
     *
     * @param aesKey 密钥实体
     * @return 密钥快照
     */
    public static AesKeyDto fromEntity(AesKey aesKey) {
        return new AesKeyDto(
                aesKey.getId(),
                aesKey.getOwner(),
                aesKey.getCreateTime(),
                aesKey.getUpdateTime(),
                aesKey.getObj(),
                aesKey.getPublishTime(),
                aesKey.getAesKey()
        );
    }

}
