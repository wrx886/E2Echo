package com.github.wrx886.e2echo.ecc;

import java.util.UUID;

import com.github.wrx886.e2echo.ecc.util.AesUtil;
import com.github.wrx886.e2echo.ecc.util.EccUtil;
import com.github.wrx886.e2echo.ecc.util.EccUtil.KeyPairHex;

/**
 * ECC / AES 加解密工具门面。
 *
 * <p>
 * 该类在进程内维护一份“当前用户”的 ECC 密钥对，并对外提供 ECC 消息的加密、解密、
 * 签名、验签，以及 AES 密钥生成、数据与文件加解密等能力。
 * </p>
 *
 * <p>
 * 密钥对保存在静态字段中，适用于单用户、单会话场景。每次加密/解密/签名操作都会先
 * 取得当前密钥对的局部快照，避免操作过程中密钥对被其他线程清空或替换导致状态不一致。
 * </p>
 */
public final class Ecc {

    /**
     * 当前保存的 ECC 密钥对，使用 volatile 保证跨线程可见性。
     */
    private static volatile KeyPairHex keyPairHex;

    /**
     * 私有构造方法，防止外部实例化工具类。
     */
    private Ecc() {
    }

    /**
     * 校验公钥与私钥是否匹配，匹配成功后将其保存为当前密钥对。
     *
     * <p>
     * 校验方式：使用传入的私钥对随机数据签名，再用传入的公钥验签，只有两者属于同一
     * 密钥对时才允许保存。
     * </p>
     *
     * @param publicKeyHex  RAW HEX 格式的 secp256k1 公钥
     * @param privateKeyHex RAW HEX 格式的 secp256k1 私钥
     * @throws Exception 公钥与私钥不匹配，或密钥格式非法导致无法完成签名/验签
     */
    public static void store(String publicKeyHex, String privateKeyHex) throws Exception {
        String data = UUID.randomUUID().toString();
        String sign = EccUtil.sign(data, privateKeyHex);
        if (EccUtil.verify(data, sign, publicKeyHex)) {
            keyPairHex = new KeyPairHex(publicKeyHex, privateKeyHex);
        } else {
            throw new Exception("The provided public key does not match the provided private key.");
        }
    }

    /**
     * 清空当前保存的密钥对。
     *
     * <p>
     * 清空后，{@link #encrypt(EccMessage)}、{@link #decrypt(EccMessage)} 和
     * {@link #sign(EccMessage)} 会因缺少密钥对而抛出异常。
     * </p>
     */
    public static void clear() {
        keyPairHex = null;
    }

    /**
     * 生成一对新的 ECC（secp256k1 曲线）密钥对。
     *
     * @return 包含公钥和私钥 RAW HEX 字符串的密钥对
     * @throws Exception 密钥对生成失败（例如底层加密提供方不可用）
     */
    public static KeyPairHex generateKeyPair() throws Exception {
        return EccUtil.generateKeyPair();
    }

    /**
     * 使用接收者公钥对消息进行 ECC 加密，并使用当前密钥对的私钥签名。
     *
     * <p>
     * 要求当前已保存密钥对，且 {@code message.getFrom()} 与已保存公钥一致。
     * 返回消息的 id 和 timestamp 会由本方法重新生成，正文为加密后的十六进制字符串。
     * </p>
     *
     * @param message 待加密消息，from 为发送者（当前用户）公钥，to 为接收者公钥
     * @return 加密并签名后的新消息
     * @throws Exception 消息或消息正文为 null、发送者/接收者为空、密钥对缺失、
     *                   发送者与已存公钥不匹配或加密/签名失败
     */
    public static EccMessage encrypt(EccMessage message) throws Exception {
        if (message == null) {
            throw new Exception("EccMessage must not be null.");
        }

        // 发送者不能为空
        if (message.getFrom() == null || message.getFrom().isBlank()) {
            throw new Exception("EccMessage.from must not be null or blank.");
        }

        // 接收者不能为空
        if (message.getTo() == null || message.getTo().isBlank()) {
            throw new Exception("EccMessage.to must not be null or blank.");
        }

        // 消息正文不能为 null
        if (message.getMessage() == null) {
            throw new Exception("EccMessage.message must not be null.");
        }

        // 密钥对为空
        KeyPairHex keyPair = keyPairHex;
        if (keyPair == null) {
            throw new Exception("No ECC key pair has been stored. Store one before using this operation.");
        }

        // 公钥不匹配
        if (!keyPair.publicKeyHex().equals(message.getFrom())) {
            throw new Exception("EccMessage.from does not match the public key of the stored key pair.");
        }

        // 构造返回值
        EccMessage eccMessage = new EccMessage();
        eccMessage.setId( // 两段式：时间戳 + UUID，共 48 位
                String.format("%016x", System.currentTimeMillis()) +
                        UUID.randomUUID().toString().replace("-", ""));
        eccMessage.setFrom(message.getFrom());
        eccMessage.setTo(message.getTo());
        eccMessage.setMessage(EccUtil.encrypt(message.getMessage(), message.getTo()));
        eccMessage.setType(message.getType());
        eccMessage.setChannel(message.getChannel());
        eccMessage.setTimestamp(Long.toString(System.currentTimeMillis()));
        eccMessage.setInfo(message.getInfo());
        eccMessage.setSign(EccUtil.sign(eccMessage.toStringWithoutSign(), keyPair.privateKeyHex()));

        return eccMessage;
    }

    /**
     * 验证消息签名后，使用当前密钥对的私钥解密消息正文。
     *
     * <p>
     * 要求当前已保存接收者自己的密钥对，且 {@code eccMessage.getTo()} 与已保存公钥一致。
     * 验签不依赖当前密钥对，而是使用消息 from 字段中的发送者公钥。
     * </p>
     *
     * @param eccMessage 已加密并携带签名的消息
     * @return 解密后的消息，元数据（id/from/to/type/channel/timestamp/info/sign）保持不变
     * @throws Exception 消息为 null、验签失败、密钥对缺失、接收者与已存公钥不匹配或解密失败
     */
    public static EccMessage decrypt(EccMessage eccMessage) throws Exception {
        if (eccMessage == null) {
            throw new Exception("EccMessage must not be null.");
        }

        // 验证消息
        if (!verify(eccMessage)) {
            throw new Exception("Signature verification failed. The message may have been tampered with.");
        }

        // 密钥对为空
        KeyPairHex keyPair = keyPairHex;
        if (keyPair == null) {
            throw new Exception("No ECC key pair has been stored. Store one before using this operation.");
        }

        // 接收者不匹配
        if (!keyPair.publicKeyHex().equals(eccMessage.getTo())) {
            throw new Exception("EccMessage.to does not match the public key of the stored key pair.");
        }

        // 解密消息
        EccMessage message = new EccMessage();
        message.setId(eccMessage.getId());
        message.setFrom(eccMessage.getFrom());
        message.setTo(eccMessage.getTo());
        message.setMessage(EccUtil.decrypt(eccMessage.getMessage(), keyPair.privateKeyHex()));
        message.setType(eccMessage.getType());
        message.setChannel(eccMessage.getChannel());
        message.setTimestamp(eccMessage.getTimestamp());
        message.setInfo(eccMessage.getInfo());
        message.setSign(eccMessage.getSign());

        return message;
    }

    /**
     * 使用当前密钥对的私钥对消息内容进行签名。
     *
     * <p>
     * 要求当前已保存密钥对，且 {@code message.getFrom()} 与已保存公钥一致。
     * 返回消息的 id 和 timestamp 会由本方法重新生成，消息正文保持明文。
     * </p>
     *
     * @param message 待签名消息，from 为签名者（当前用户）公钥
     * @return 携带新签名的新消息
     * @throws Exception 消息为 null、发送者为空、密钥对缺失、发送者与已存公钥不匹配或签名失败
     */
    public static EccMessage sign(EccMessage message) throws Exception {
        if (message == null) {
            throw new Exception("EccMessage must not be null.");
        }

        // 发送者不能为空
        if (message.getFrom() == null || message.getFrom().isBlank()) {
            throw new Exception("EccMessage.from must not be null or blank.");
        }

        // 密钥对为空
        KeyPairHex keyPair = keyPairHex;
        if (keyPair == null) {
            throw new Exception("No ECC key pair has been stored. Store one before using this operation.");
        }

        // 公钥不匹配
        if (!keyPair.publicKeyHex().equals(message.getFrom())) {
            throw new Exception("EccMessage.from does not match the public key of the stored key pair.");
        }

        // 构造返回值
        EccMessage eccMessage = new EccMessage();
        eccMessage.setId( // 两段式：时间戳 + UUID，共 48 位
                String.format("%016x", System.currentTimeMillis()) +
                        UUID.randomUUID().toString().replace("-", ""));
        eccMessage.setFrom(message.getFrom());
        eccMessage.setTo(message.getTo());
        eccMessage.setMessage(message.getMessage());
        eccMessage.setType(message.getType());
        eccMessage.setChannel(message.getChannel());
        eccMessage.setTimestamp(Long.toString(System.currentTimeMillis()));
        eccMessage.setInfo(message.getInfo());
        eccMessage.setSign(EccUtil.sign(eccMessage.toStringWithoutSign(), keyPair.privateKeyHex()));

        return eccMessage;
    }

    /**
     * 使用消息 from 字段中的公钥验证消息签名，不依赖当前保存的密钥对。
     *
     * <p>
     * 消息字段被篡改、签名缺失、公钥或签名格式非法时均返回 {@code false}。
     * </p>
     *
     * @param eccMessage 待验证的消息
     * @return 签名有效返回 {@code true}，否则返回 {@code false}
     */
    public static boolean verify(EccMessage eccMessage) {
        try {
            return EccUtil.verify(
                    eccMessage.toStringWithoutSign(),
                    eccMessage.getSign(),
                    eccMessage.getFrom());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成一个随机的 AES-256 密钥。
     *
     * @return HEX 字符串形式的 AES 密钥
     * @throws Exception 密钥生成失败
     */
    public static String generateAesKeyAsHex() throws Exception {
        return AesUtil.generateKeyAsHex();
    }

    /**
     * 使用 AES-GCM 对明文加密（无需 ECC 密钥对）。
     *
     * @param plainText 待加密的明文
     * @param hexKey    HEX 字符串形式的 AES 密钥
     * @return HEX 字符串形式的密文，包含随机 IV
     * @throws Exception 密钥非法或加密失败
     */
    public static String encryptAes(String plainText, String hexKey) throws Exception {
        return AesUtil.encrypt(plainText, hexKey);
    }

    /**
     * 使用 AES-GCM 解密密文（无需 ECC 密钥对）。
     *
     * @param cipherText HEX 字符串形式的密文，IV 位于密文头部
     * @param hexKey     HEX 字符串形式的 AES 密钥
     * @return 解密后的明文
     * @throws Exception 密钥非法、密文被篡改或解密失败
     */
    public static String decryptAes(String cipherText, String hexKey) throws Exception {
        return AesUtil.decrypt(cipherText, hexKey);
    }

    /**
     * 使用 AES-GCM 加密文件（无需 ECC 密钥对），IV 会写入输出文件头部。
     *
     * @param inputFile  待加密的输入文件路径
     * @param outputFile 加密结果的输出文件路径
     * @param keyHex     HEX 字符串形式的 AES 密钥
     * @throws Exception 文件读写失败、密钥非法或加密失败
     */
    public static void encryptAesFile(String inputFile, String outputFile, String keyHex) throws Exception {
        AesUtil.encryptFile(inputFile, outputFile, keyHex);
    }

    /**
     * 使用 AES-GCM 解密文件（无需 ECC 密钥对），IV 从输入文件头部读取。
     *
     * @param inputFile  待解密的输入文件路径
     * @param outputFile 解密结果的输出文件路径
     * @param keyHex     HEX 字符串形式的 AES 密钥
     * @throws Exception 输出文件已存在、文件读写失败、密钥非法、密文被篡改或解密失败
     */
    public static void decryptAesFile(String inputFile, String outputFile, String keyHex) throws Exception {
        AesUtil.decryptFile(inputFile, outputFile, keyHex);
    }

}
