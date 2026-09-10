package com.github.wrx886.e2echo.ecc;

/**
 * 端到端消息的数据载体。
 *
 * <p>{@code from} 始终是发送者公钥；{@code to} 决定正文的加密方式：为接收者公钥时使用
 * ECC 加密，为群聊标识时使用 AES 加密，留空时正文不加密。</p>
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
     * 接收者信息：接收者公钥（ECC 加密）、群聊标识（AES 加密）或留空（不加密）。
     */
    private String to;

    /**
     * 消息正文：根据 {@code to} 的取值可为 ECC 密文、AES 密文或明文。
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
     * @return 接收者公钥（RAW HEX）、群聊标识，或 {@code null}/空白表示不加密
     */
    public String getTo() {
        return to;
    }

    /**
     * 设置接收者信息。
     *
     * @param to 接收者公钥（RAW HEX）、群聊标识，或 {@code null}/空白表示不加密
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * 获取消息正文。
     *
     * @return 消息正文（可为 ECC 密文、AES 密文或明文）
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
     * 计算消息的哈希值，参与计算的字段与 {@link #equals(Object)} 保持一致。
     *
     * @return 消息哈希值
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((to == null) ? 0 : to.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
        result = prime * result + ((sign == null) ? 0 : sign.hashCode());
        return result;
    }

    /**
     * 比较两个消息对象的所有字段是否相等。
     *
     * @param obj 待比较对象
     * @return 所有字段均相等返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EccMessage other = (EccMessage) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (from == null) {
            if (other.from != null)
                return false;
        } else if (!from.equals(other.from))
            return false;
        if (to == null) {
            if (other.to != null)
                return false;
        } else if (!to.equals(other.to))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!channel.equals(other.channel))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        if (sign == null) {
            return other.sign == null;
        } else return sign.equals(other.sign);
    }

    /**
     * 返回包含全部字段（含签名）的字符串表示，主要用于日志输出。
     *
     * @return 消息的字符串表示
     */
    @Override
    public String toString() {
        return "EccMessage [id=" + id + ", from=" + from + ", to=" + to + ", message=" + message + ", type=" + type
                + ", channel=" + channel + ", timestamp=" + timestamp + ", sign=" + sign + "]";
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
        return "EccMessage [id=" + id + ", from=" + from + ", to=" + to + ", message=" + message + ", type=" + type
                + ", channel=" + channel + ", timestamp=" + timestamp + "]";
    }

}
