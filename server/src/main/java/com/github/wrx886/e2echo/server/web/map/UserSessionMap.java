package com.github.wrx886.e2echo.server.web.map;

import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ConcurrentHashMultiset;

import lombok.Data;

@Data
public class UserSessionMap {

    // 将 user id 转为 session id
    private final ConcurrentHashMap<Long, ConcurrentHashMultiset<String>> userId2SessionIds = new ConcurrentHashMap<>();

    // 将 Session Id 转为 userId
    private final ConcurrentHashMap<String, Long> sessionId2UserId = new ConcurrentHashMap<>();

    // 通过 Session ID 获取 用户 ID
    public Long getUserIdBySessionId(String sessionId) {
        return sessionId2UserId.get(sessionId);
    }

    // 通过 用户 ID 获取 Session ID
    public ConcurrentHashMultiset<String> getSessionIdsByUserId(Long userId) {
        return userId2SessionIds.get(userId);
    }

    // 保存一对 SessionId 和 UserId
    public void addSessionIdAndUserId(String sessionId, Long userId) {
        // sessionId2UserId
        sessionId2UserId.put(sessionId, userId);

        // userId2SessionIds
        userId2SessionIds.computeIfAbsent(userId, k -> ConcurrentHashMultiset.create()).add(sessionId);
    }

    public void removeSessionIdAndUserId(String sessionId) {
        // 1. 原子性地获取并移除 sessionId2UserId 中的 userId
        Long userId = sessionId2UserId.remove(sessionId);
        if (userId == null) {
            // 如果 sessionId 不存在，直接返回
            return;
        }

        // 2. 原子性地更新 userId2SessionIds
        userId2SessionIds.compute(userId, (k, set) -> {
            if (set != null) {
                set.remove(sessionId);
                if (set.isEmpty()) {
                    // 如果 set 为空，移除键值对
                    return null;
                }
            }
            return set;
        });
    }
}
