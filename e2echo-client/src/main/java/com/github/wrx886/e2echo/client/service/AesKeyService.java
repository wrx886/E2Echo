package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.dto.AesKeyDto;
import com.github.wrx886.e2echo.client.entity.AesKey;
import com.github.wrx886.e2echo.client.repository.AesKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * AES 密钥业务逻辑层。
 *
 * <p>保存与查询群聊消息用的 AES 密钥：发送时取某个对象最新签发的密钥，解密时按密文里带的对象与
 * 签发时间取对应版本的密钥。密钥按登入用户隔离，查询结果带缓存（查过但没有同样缓存），保存新密钥
 * 时让对应缓存失效。</p>
 *
 * <p>对外返回 {@link AesKeyDto} 快照：缓存里放的是共享对象，直接返回实体会被调用方改坏。</p>
 */
@Service
@RequiredArgsConstructor
public class AesKeyService {

    /**
     * AES 密钥数据访问对象。
     */
    private final AesKeyRepository aesKeyRepository;

    /**
     * 按“加密对象 + 签发时间”缓存的密钥；签发时间格式化成定宽十六进制，拼接不会产生歧义。
     */
    private final ConcurrentHashMap<String, Optional<AesKeyDto>> objPublishTime2AesKey = new ConcurrentHashMap<>();

    /**
     * 按“加密对象”缓存的最新密钥，发送群聊消息时用。
     */
    private final ConcurrentHashMap<String, Optional<AesKeyDto>> obj2AesKey = new ConcurrentHashMap<>();

    /**
     * 保存密钥。
     *
     * @param aesKey 待保存的密钥
     */
    public void save(AesKey aesKey) {
        aesKeyRepository.save(aesKey);
        // 新签发的密钥会影响“最新密钥”
        obj2AesKey.remove(currentOwner() + aesKey.getObj());
        objPublishTime2AesKey.remove(currentOwner() + aesKey.getObj() + String.format("%016x", aesKey.getPublishTime()));
    }

    /**
     * 查询某个对象在指定签发时间签发的密钥。
     *
     * @param obj         加密对象，如群聊标识
     * @param publishTime 密钥签发时间（毫秒），取自密文前缀
     * @return 对应版本的密钥，不存在时返回 {@code null}
     */
    public AesKeyDto getOne(String obj, long publishTime) {
        String key = currentOwner() + obj + String.format("%016x", publishTime);
        objPublishTime2AesKey.computeIfAbsent(key, (k) -> Optional.ofNullable(
                        aesKeyRepository.findByOwnerAndObjAndPublishTime(currentOwner(), obj, publishTime)).
                map(AesKeyDto::fromEntity)
        );
        return objPublishTime2AesKey.get(key).orElse(null);
    }

    /**
     * 查询某个对象最近一次签发的密钥，用于加密待发送的群聊消息。
     *
     * @param obj 加密对象，如群聊标识
     * @return 最新签发的密钥，不存在时返回 {@code null}
     */
    public AesKeyDto getLast(String obj) {
        String key = currentOwner() + obj;
        obj2AesKey.computeIfAbsent(key, (k) -> Optional.ofNullable(
                        aesKeyRepository.findFirstByOwnerAndObjOrderByPublishTimeDesc(currentOwner(), obj))
                .map(AesKeyDto::fromEntity)
        );
        return obj2AesKey.get(key).orElse(null);
    }

}
