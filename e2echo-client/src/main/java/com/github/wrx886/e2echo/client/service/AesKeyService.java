package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.common.Const;
import com.github.wrx886.e2echo.client.entity.AesKey;
import com.github.wrx886.e2echo.client.repository.AesKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * AES 密钥业务逻辑层。
 *
 * <p>保存与查询群聊消息用的 AES 密钥，数据按登入用户隔离。</p>
 */
@Service
@RequiredArgsConstructor
public class AesKeyService {

    /**
     * AES 密钥数据访问对象。
     */
    private final AesKeyRepository aesKeyRepository;

    /**
     * 保存密钥。
     *
     * @param aesKey 待保存的密钥
     */
    public void save(AesKey aesKey) {
        aesKeyRepository.save(aesKey);
    }

    /**
     * 查询某个对象在指定时间可用的 AES 密钥。
     *
     * <p>先取签发时间在 {@code timestamp} 前后 {@link Const#AES_FIND_TIME_RANGE_MS} 内的密钥；
     * 一个都没有时退回最近一次签发的密钥，保证解密方至少能拿到一个候选密钥。</p>
     *
     * @param obj       加密对象，如群聊标识
     * @param timestamp 消息时间（毫秒）
     * @return 候选密钥列表，没有可用密钥时返回空列表
     */
    public List<String> get(String obj, long timestamp) {
        long findRange = Const.AES_FIND_TIME_RANGE_MS;
        // 查询发送时间前后 5 min 内的密钥
        List<AesKey> aesKeys = aesKeyRepository.findAllByOwnerAndObjAndPublishTimeGreaterThanEqualAndPublishTimeLessThanEqual(
                currentOwner(), obj, timestamp - findRange, timestamp + findRange
        );

        // 如果没有,就查询上一个密钥
        if (!aesKeys.isEmpty()) {
            return aesKeys.stream().map(AesKey::getAesKey).toList();
        } else {
            AesKey aesKey = aesKeyRepository.findFirstByOwnerAndObjOrderByPublishTimeDesc(currentOwner(), obj);
            if (aesKey != null) {
                return List.of(aesKey.getAesKey());
            } else {
                return List.of();
            }
        }

    }

}
