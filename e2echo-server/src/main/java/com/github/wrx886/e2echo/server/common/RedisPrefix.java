package com.github.wrx886.e2echo.server.common;

public final class RedisPrefix {

    private RedisPrefix() {
    }

    // websocket session id 的前缀
    public static final String WEBSOCKET_SESSION_PREFIX = "e2echo:websocket:";

    // 会话 ID -> 接收者公钥
    public static final String SESSION_ID_2_PUBLIC_KEY_HEX = "e2echo:sessionId2PublicKeyHex:";

    // 接收者公钥 -> 会话 ID 列表
    public static final String PUBLIC_KEY_HEX_2_SESSION_ID = "e2echo:publicKeyHex2SessionId:";

    // 会话 ID -> 群聊 UUID 列表
    public static final String SESSION_ID_2_GROUP_UUID = "e2echo:sessionId2GroupUuid:";

    // 群聊 UUID -> 会话 ID 列表
    public static final String GROUP_UUID_2_SESSION_ID = "e2echo:groupUuid2SessionId:";

}
