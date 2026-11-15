package com.github.wrx886.e2echo.ecc.util;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * ID 工具类。
 *
 * <p>标准 ID 为 48 位小写十六进制字符串：前 16 位是毫秒时间戳，后 32 位是无连字符 UUID。
 * 定长的时间戳前缀使 ID 的字典序与生成时间一致，可用于排序和游标查询。</p>
 */
public final class IdUtil {

    /**
     * 标准 ID 格式：48 位小写十六进制字符。
     */
    private static final Pattern ID_PATTERN = Pattern.compile("[0-9a-f]{48}");

    /**
     * ID 中十六进制时间戳前缀的长度。
     */
    public static final int TIMESTAMP_HEX_LENGTH = 16;

    /**
     * 标准 ID 的总长度。
     */
    public static final int ID_LENGTH = TIMESTAMP_HEX_LENGTH + 32;

    /**
     * 私有构造方法，防止外部实例化工具类。
     */
    private IdUtil() {
    }

    /**
     * 使用当前时间生成一个新的标准 ID。
     *
     * @return 48 位小写十六进制 ID
     */
    public static String newId() {
        return newId(System.currentTimeMillis());
    }

    /**
     * 使用指定毫秒时间戳生成一个新的标准 ID。
     *
     * <p>该方法便于测试构造时间可控的 ID。随机部分仍由 {@link UUID#randomUUID()} 生成。</p>
     *
     * @param timestampMillis 毫秒时间戳，不能为负数
     * @return 48 位小写十六进制 ID
     * @throws IllegalArgumentException 时间戳为负数
     */
    public static String newId(long timestampMillis) {
        return timestampPrefix(timestampMillis) + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从标准 ID 中解析生成时间。
     *
     * @param id 48 位小写十六进制 ID
     * @return 生成 ID 时使用的毫秒时间戳
     * @throws IllegalArgumentException ID 不符合标准格式
     */
    public static long timestampOf(String id) {
        if (!isValid(id)) {
            throw new IllegalArgumentException("ID 必须是合法的 48 位小写十六进制字符串。");
        }
        try {
            return Long.parseLong(id.substring(0, TIMESTAMP_HEX_LENGTH), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID 时间戳超出有效范围。", e);
        }
    }

    /**
     * 判断字符串是否符合标准 ID 格式。
     *
     * @param id 待校验的字符串
     * @return 符合 48 位小写十六进制格式时返回 {@code true}
     */
    public static boolean isValid(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    /**
     * 生成 16 位小写十六进制时间戳前缀。
     *
     * <p>该前缀可作为按时间查询 ID 时的包含下界。</p>
     *
     * @param timestampMillis 毫秒时间戳，不能为负数
     * @return 16 位小写十六进制时间戳
     * @throws IllegalArgumentException 时间戳为负数
     */
    public static String timestampPrefix(long timestampMillis) {
        if (timestampMillis < 0) {
            throw new IllegalArgumentException("ID 时间戳不能为负数：" + timestampMillis);
        }
        return String.format("%016x", timestampMillis);
    }
}
