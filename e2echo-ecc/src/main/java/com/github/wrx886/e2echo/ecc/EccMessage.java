package com.github.wrx886.e2echo.ecc;

import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * 端到端消息的数据载体。
 *
 * <p>{@code from} 始终是发送者公钥；正文的加密方式由 {@code channel} 决定，不同通道
 * 使用不同的加密方案，{@code to} 仅表示接收者信息。</p>
 *
 * <p>消息对外有两种字符串形式，均为 JSON 对象，字段严格按 id、from、to、message、type、
 * channel、info、sign 的顺序排列：{@link #toString()} 包含全部字段，用于日志输出与调试；
 * {@link #toStringWithoutSign()} 不含 {@code sign} 字段，是签名与验签的原文。除
 * {@code sign} 外的任一字段被修改后，原有签名都会验签失败。加密时 {@code message} 保存
 * 密文；不加密时保存明文。</p>
 *
 * <p>由于序列化结果直接作为签名原文，字段顺序、分隔符与转义规则都必须稳定：本类始终使用
 * 同一套序列化规则，其他语言或实现若要参与签名与验签，也必须生成字节完全一致的 JSON 文本。</p>
 */
public class EccMessage {

    /**
     * 消息字符串化使用的 JSON 序列化器，{@link ObjectMapper} 是线程安全的，因此作为静态
     * 字段复用，避免每次序列化都重新创建。
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 消息 ID，由“16 位十六进制时间戳 + 32 位无连字符 UUID”组成，共 48 位。
     */
    private String id;

    /**
     * 发送者身份，即发送者的 secp256k1 公钥（RAW HEX 格式）。
     */
    private String from;

    /**
     * 接收者信息，具体含义由通道决定，如私聊时为接收者公钥、群聊时为群聊标识。
     */
    private String to;

    /**
     * 消息正文：加密方式由 {@code channel} 决定，可为密文或明文。
     */
    private String message;

    /**
     * 消息类型，由业务方自行定义。
     */
    private String type;

    /**
     * 消息通道（如私聊、群聊），由业务方自行定义。
     */
    private String channel;

    /**
     * 消息附加信息，由业务方自行定义。
     */
    private String info;

    /**
     * 消息签名，基于 {@link #toStringWithoutSign()} 原文使用 SHA256withECDSA 计算。
     */
    private String sign;

    /**
     * 创建一个空消息。
     */
    public EccMessage() {
    }

    /**
     * 获取消息 ID。
     *
     * @return 消息 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置消息 ID。
     *
     * @param id 消息 ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取发送者公钥。
     *
     * @return 发送者的 secp256k1 公钥（RAW HEX 格式）
     */
    public String getFrom() {
        return from;
    }

    /**
     * 设置发送者公钥。
     *
     * @param from 发送者的 secp256k1 公钥（RAW HEX 格式）
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * 获取接收者信息。
     *
     * @return 接收者信息，具体含义由通道决定
     */
    public String getTo() {
        return to;
    }

    /**
     * 设置接收者信息。
     *
     * @param to 接收者信息，具体含义由通道决定
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * 获取消息正文。
     *
     * @return 消息正文（加密方式由通道决定，可为密文或明文）
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置消息正文。
     *
     * @param message 消息正文
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取消息类型。
     *
     * @return 消息类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置消息类型。
     *
     * @param type 消息类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取消息通道。
     *
     * @return 消息通道
     */
    public String getChannel() {
        return channel;
    }

    /**
     * 设置消息通道。
     *
     * @param channel 消息通道
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * 获取消息签名。
     *
     * @return 消息签名（HEX 字符串）
     */
    public String getSign() {
        return sign;
    }

    /**
     * 设置消息签名。
     *
     * @param sign 消息签名
     */
    public void setSign(String sign) {
        this.sign = sign;
    }

    /**
     * 获取消息附加信息。
     *
     * @return 消息附加信息
     */
    public String getInfo() {
        return info;
    }

    /**
     * 设置消息附加信息。
     *
     * @param info 消息附加信息
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * 比较两个消息对象的所有字段是否相等。
     *
     * @param o 待比较对象
     * @return 所有字段均相等返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EccMessage that = (EccMessage) o;
        return Objects.equals(id, that.id) && Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(message, that.message) && Objects.equals(type, that.type) && Objects.equals(channel, that.channel) && Objects.equals(info, that.info) && Objects.equals(sign, that.sign);
    }

    /**
     * 计算消息的哈希值，参与计算的字段与 {@link #equals(Object)} 保持一致。
     *
     * @return 消息哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, from, to, message, type, channel, info, sign);
    }

    /**
     * 返回包含全部字段（含签名）的 JSON 字符串，主要用于日志输出与调试查看。
     *
     * <p>字段顺序固定为 id、from、to、message、type、channel、info、sign，值为 {@code null}
     * 的字段序列化为 JSON 的 {@code null}。该方法包含 {@code sign} 字段，不能作为签名原文。</p>
     *
     * @return 含 sign 字段的 JSON 字符串
     */
    @Override
    public String toString() {
        // 使用 LinkedHashMap 固定字段顺序，保证同一消息每次序列化都得到完全相同的文本
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("from", from);
        map.put("to", to);
        map.put("message", message);
        map.put("type", type);
        map.put("channel", channel);
        map.put("info", info);
        map.put("sign", sign);
        return objectMapper.writeValueAsString(map);
    }

    /**
     * 返回不包含签名字段的消息 JSON 字符串，作为签名与验签的原文。
     *
     * <p>字段顺序固定为 id、from、to、message、type、channel、info，值为 {@code null}
     * 的字段序列化为 JSON 的 {@code null}。发送方计算签名、接收方校验签名时都必须使用该
     * 方法：既避免把签名自身纳入签名原文导致自引用，也保证双方对同一消息得到完全相同的
     * 文本，从而可以通过验签。</p>
     *
     * @return 不含 sign 字段的 JSON 字符串
     */
    public String toStringWithoutSign() {
        // 使用 LinkedHashMap 固定字段顺序，保证同一消息每次序列化都得到完全相同的文本
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("from", from);
        map.put("to", to);
        map.put("message", message);
        map.put("type", type);
        map.put("channel", channel);
        map.put("info", info);
        return objectMapper.writeValueAsString(map);
    }

}
