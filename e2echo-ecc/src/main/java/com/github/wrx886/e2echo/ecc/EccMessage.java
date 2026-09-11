package com.github.wrx886.e2echo.ecc;

import java.util.Objects;

/**
 * 端到端消息的数据载体。
 *
 * <p>{@code from} 始终是发送者公钥；正文的加密方式由 {@code channel} 决定，不同通道
 * 使用不同的加密方案，{@code to} 仅表示接收者信息。</p>
 *
 * <p>{@link #toStringWithoutSign()} 是签名与验签的原文：其中除 {@code sign} 外的所有字段
 * 共同参与签名。除 {@code sign} 外的任一字段被修改后，原有签名都会验签失败。加密时
 * {@code message} 保存密文；不加密时保存明文。</p>
 */
public class EccMessage {

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
     * 消息时间戳，保存为系统当前毫秒数的十进制字符串。
     */
    private String timestamp;

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
     * 获取消息时间戳。
     *
     * @return 消息时间戳（毫秒数的十进制字符串）
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * 设置消息时间戳。
     *
     * @param timestamp 消息时间戳
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EccMessage that = (EccMessage) o;
        return Objects.equals(id, that.id) && Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(message, that.message) && Objects.equals(type, that.type) && Objects.equals(channel, that.channel) && Objects.equals(timestamp, that.timestamp) && Objects.equals(info, that.info) && Objects.equals(sign, that.sign);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, from, to, message, type, channel, timestamp, info, sign);
    }

    /**
     * 返回包含全部字段（含签名）的字符串表示，主要用于日志输出。
     *
     * @return 消息的字符串表示
     */
    @Override
    public String toString() {
        return "EccMessage{" +
                "id='" + id + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", channel='" + channel + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", info='" + info + '\'' +
                ", sign='" + sign + '\'' +
                '}';
    }

    /**
     * 返回不包含签名字段的消息表示，作为签名与验签的原文。
     *
     * <p>签名、验签、传输前计算签名及接收后校验时都应使用该方法，避免把签名自身纳入
     * 签名原文导致自引用。</p>
     *
     * @return 不含 sign 字段的消息字符串
     */
    public String toStringWithoutSign() {
        return "EccMessage{" +
                "id='" + id + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", channel='" + channel + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", info='" + info + '\'' +
                '}';
    }

}
