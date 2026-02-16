package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKey;
import com.github.wrx886.e2echo.client.srv.mapper.GroupKeyMapper;
import com.github.wrx886.e2echo.client.srv.service.GroupKeyService;
import com.github.wrx886.e2echo.client.srv.service.SessionService;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
@AllArgsConstructor
public class GroupKeyServiceImpl extends ServiceImpl<GroupKeyMapper, GroupKey> implements GroupKeyService {

    private final EccController eccController;
    private final SessionService sessionService;

    @Data
    @AllArgsConstructor
    private static final class Pair {
        private final String groupUuid;
        private final Long timestamp;
    }

    // 缓存机制
    private final ConcurrentHashMap<Pair, String> groupKeyMap = new ConcurrentHashMap<>();

    /**
     * 添加/更新群密钥
     * 
     * @param groupUuid 群聊UUID
     * @param timestamp 时间戳
     * @param aesKey    密钥
     * @return 密钥ID
     */
    @Override
    public void put(String groupUuid, Long timestamp, String aesKey) {
        // 获取
        GroupKey groupKey = getOne(new LambdaQueryWrapper<GroupKey>()
                .eq(GroupKey::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupKey::getGroupUuid, groupUuid)
                .eq(GroupKey::getTimestamp, timestamp));
        // 存在密钥
        if (groupKey != null) {
            // 直接无视，用一群不可能存在多个同时间戳的密钥
            return;
        }

        // 创建
        groupKey = new GroupKey();
        groupKey.setOwnerPublicKeyHex(eccController.getPublicKey());
        groupKey.setGroupUuid(groupUuid);
        groupKey.setTimestamp(timestamp);
        groupKey.setAesKey(aesKey);
        this.save(groupKey);

        // 更新会话
        sessionService.putGroupKey(groupUuid, groupKey.getId());

        // 缓存
        groupKeyMap.put(new Pair(groupUuid, timestamp), aesKey);
    }

    /**
     * 获取群密钥
     * 
     * @param groupUuid 群聊UUID
     * @param timestamp 时间戳
     * @return 密钥
     */
    @Override
    public String get(String groupUuid, Long timestamp) {
        // 缓存机制
        String aesKey = groupKeyMap.get(new Pair(groupUuid, timestamp));
        if (aesKey != null) {
            return aesKey;
        }

        // 查询
        GroupKey groupKey = getOne(new LambdaQueryWrapper<GroupKey>()
                .eq(GroupKey::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(GroupKey::getGroupUuid, groupUuid)
                .eq(GroupKey::getTimestamp, timestamp));
        // 缓存
        if (groupKey != null) {
            groupKeyMap.put(new Pair(groupUuid, timestamp), groupKey.getAesKey());
            return groupKey.getAesKey();
        }
        // 查询失败
        return null;
    }

}
