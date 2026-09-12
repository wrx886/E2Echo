package com.github.wrx886.e2echo.server.common;

/**
 * Redis 键前缀。
 *
 * <p>集中定义各业务在 Redis 中使用的键前缀，避免字符串字面量散落在各处导致拼写不一致。</p>
 */
public final class RedisPrefix {

    /**
     * 客户端到订阅目标的映射（Set）：键为 {@code 前缀 + 客户端 ID}，成员为该客户端订阅的
     * 全部目标，用于连接断开时找到需要清理的订阅关系。
     */
    public static final String CLIENT2TOS = "e2echo:message:client2tos:";

    /**
     * 订阅目标到客户端的映射（Set）：键为 {@code 前缀 + 目标}，成员为订阅了该目标的全部
     * 客户端，用于通知时找到需要通知的连接。
     */
    public static final String TO2CLIENTS = "e2echo:message:to2clients:";

    /**
     * 通知频道的键前缀，实际频道名为 {@code 前缀 + 实例 ID}。
     */
    public static final String CHANNEL = "e2echo:message:channel:";
}
