package com.github.wrx886.e2echo.client.util;

import java.util.UUID;

/**
 * ID 工具类。
 *
 * <p>生成的 ID 为 48 位字符串，前 16 位是十六进制的毫秒时间戳，后 32 位是去掉连字符的 UUID，
 * 因此 ID 自带生成时间且不会重复。</p>
 */
public final class IdUtil {

    /**
     * 生成一个新的 ID。
     *
     * @return 48 位 ID，前 16 位为十六进制毫秒时间戳，后 32 位为随机 UUID
     */
    public static String newId() {
        // 两段式：时间戳 + UUID，共 48 位
        return String.format("%016x", System.currentTimeMillis()) +
                UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从 ID 中解析生成时间。
     *
     * @param id 由 {@link #newId()} 生成的 ID
     * @return 生成该 ID 时的毫秒时间戳
     * @throws NumberFormatException           ID 前 16 位不是合法的十六进制数
     * @throws StringIndexOutOfBoundsException ID 长度不足 16 位
     */
    public static long getTimestampFromId(String id) {
        return Long.parseLong(id.substring(0, 16), 16);
    }

}
