package com.github.wrx886.e2echo.ecc.util;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * ECC 加密工具类。
 *
 * <p>基于 Bouncy Castle 提供 secp256k1 密钥对生成、ECIES 加密/解密与
 * SHA256withECDDSA 签名/验签。公钥和私钥均以 RAW HEX 字符串对外传递：
 * 公钥为 {@code 0x04 || X || Y} 的非压缩点格式，私钥为密钥值（大整数）的十六进制。</p>
 *
 * <p>签名算法名 {@code SHA256withECDDSA} 由 Bouncy Castle 提供（JDK 自带提供方不认识该
 * 名称），是签名过程不引入随机数的确定性 ECDSA 变体，同一私钥对同一数据多次签名会得到
 * 完全相同的签名。它的签名格式与 {@code SHA256withECDSA} 一致，均为 ASN.1 DER 编码，两者
 * 产生的签名可以互相验证。本类方法均为静态方法且不保存状态，可安全地在多线程环境中使用。</p>
 */
public final class EccUtil {

    /**
     * 私有构造方法，禁止外部实例化工具类。
     */
    private EccUtil() {
    }

    /**
     * 类初始化时注册 Bouncy Castle 提供方，供本类中的签名、加密等操作使用。
     *
     * <p>注册结果是全局的：与 JVM 中其他使用 BC 的代码共用同一份提供方。若同名提供方已
     * 存在，{@link Security#addProvider} 会忽略本次注册，不会覆盖已有实例。</p>
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 将 HEX 字符串解码为字节数组。
     *
     * @param hex       HEX 字符串
     * @param valueName 值的名称，用于异常提示
     * @return 解码后的字节数组
     * @throws IllegalArgumentException hex 为 null/空、长度非偶数或包含非十六进制字符
     */
    private static byte[] decodeHex(String hex, String valueName) {
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException(valueName + " must not be null or empty.");
        }
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException(
                    valueName + " must contain an even number of hexadecimal characters.");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException(
                        valueName + " contains non-hexadecimal characters.");
            }
            bytes[i / 2] = (byte) ((high << 4) | low);
        }
        return bytes;
    }

    /**
     * RAW HEX 字符串形式的 ECC 密钥对。
     *
     * @param publicKeyHex  RAW HEX 格式的 secp256k1 公钥
     * @param privateKeyHex RAW HEX 格式的 secp256k1 私钥
     */
    public record KeyPairHex(String publicKeyHex, String privateKeyHex) {
    }

    /**
     * 生成 ECC 密钥对
     *
     * @return 包含公钥和私钥 RAW HEX 字符串的 KeyPairHex 对象
     * @throws Exception 密钥算法或底层加密提供方初始化失败
     */
    public static KeyPairHex generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new KeyPairHex(publicKeyToRawHex(keyPair.getPublic()), privateKeyToRawHex(keyPair.getPrivate()));
    }

    /**
     * 将公钥转换为 RAW HEX 字符串（非压缩点格式：0x04 || X || Y）
     *
     * @param publicKey 公钥
     * @return RAW HEX 字符串格式的公钥
     */
    private static String publicKeyToRawHex(PublicKey publicKey) {
        ECPoint ecPoint = ((ECPublicKey) publicKey).getQ();
        byte[] x = ecPoint.getAffineXCoord().getEncoded();
        byte[] y = ecPoint.getAffineYCoord().getEncoded();
        byte[] result = new byte[1 + x.length + y.length];
        result[0] = 0x04; // Uncompressed point format
        System.arraycopy(x, 0, result, 1, x.length);
        System.arraycopy(y, 0, result, 1 + x.length, y.length);
        return Hex.toHexString(result);
    }

    /**
     * 将私钥转换为 RAW HEX 字符串
     *
     * @param privateKey 私钥
     * @return RAW HEX 字符串格式的私钥
     */
    private static String privateKeyToRawHex(PrivateKey privateKey) {
        BigInteger d = ((org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey) privateKey).getD();
        byte[] encoded = d.toByteArray();
        if (encoded[0] == 0) { // Remove leading zero byte if present
            byte[] trimmed = new byte[encoded.length - 1];
            System.arraycopy(encoded, 1, trimmed, 0, trimmed.length);
            return Hex.toHexString(trimmed);
        }
        return Hex.toHexString(encoded);
    }

    /**
     * 从 RAW HEX 字符串（非压缩点格式：0x04 || X || Y）恢复公钥
     *
     * @param hexPublicKey RAW HEX 格式的公钥
     * @return 公钥
     * @throws IllegalArgumentException 公钥 HEX 非法（null、空、长度或字符不符合要求）
     * @throws Exception                无法从该公钥生成 Java 公钥对象
     */
    private static PublicKey rawHexToPublicKey(String hexPublicKey) throws Exception {
        byte[] keyBytes = decodeHex(hexPublicKey, "Public key");
        if (keyBytes.length != 65) {
            throw new IllegalArgumentException(
                    "Invalid uncompressed public key length: expected 65 bytes, but got " + keyBytes.length + ".");
        }
        if (keyBytes[0] != 0x04) {
            throw new IllegalArgumentException("Invalid uncompressed public key format");
        }
        byte[] x = new byte[32];
        byte[] y = new byte[32];
        System.arraycopy(keyBytes, 1, x, 0, 32);
        System.arraycopy(keyBytes, 33, y, 0, 32);

        X9ECParameters ecParams = SECNamedCurves.getByName("secp256k1");
        ECParameterSpec ecSpec = new ECParameterSpec(ecParams.getCurve(), ecParams.getG(), ecParams.getN(),
                ecParams.getH());

        ECPoint point = ecSpec.getCurve().createPoint(new BigInteger(1, x), new BigInteger(1, y));
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        return keyFactory.generatePublic(pubKeySpec);
    }

    /**
     * 从 RAW HEX 字符串恢复私钥
     *
     * @param hexPrivateKey RAW HEX 格式的私钥
     * @return 私钥
     * @throws IllegalArgumentException 私钥 HEX 非法（null、空、长度或字符不符合要求）或不在有效范围内
     * @throws Exception                无法从该私钥生成 Java 私钥对象
     */
    private static PrivateKey rawHexToPrivateKey(String hexPrivateKey) throws Exception {
        X9ECParameters ecParams = SECNamedCurves.getByName("secp256k1");
        byte[] keyBytes = decodeHex(hexPrivateKey, "Private key");
        BigInteger d = new BigInteger(1, keyBytes);
        if (d.signum() == 0 || d.compareTo(ecParams.getN()) >= 0) {
            throw new IllegalArgumentException("Private key is out of the valid range for secp256k1.");
        }

        ECParameterSpec ecSpec = new ECParameterSpec(ecParams.getCurve(), ecParams.getG(), ecParams.getN(),
                ecParams.getH());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(d, ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        return keyFactory.generatePrivate(privateKeySpec);
    }

    /**
     * 使用公钥加密字符串
     *
     * @param plainText 明文字符串
     * @param publicKey 公钥
     * @return 加密后的十六进制字符串
     * @throws Exception 加密算法或密钥不可用，或加密失败
     */
    private static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Hex.toHexString(encryptedBytes);
    }

    /**
     * 使用 RAW HEX 格式的公钥加密字符串
     *
     * @param plainText    明文字符串
     * @param hexPublicKey RAW HEX 格式的公钥
     * @return 加密后的十六进制字符串
     * @throws IllegalArgumentException 公钥 HEX 非法（null、空、长度或字符不符合要求）
     * @throws Exception                加密算法不可用或加密失败
     */
    public static String encrypt(String plainText, String hexPublicKey) throws Exception {
        PublicKey publicKey = rawHexToPublicKey(hexPublicKey);
        return encrypt(plainText, publicKey);
    }

    /**
     * 使用私钥解密字符串
     *
     * @param encryptedHex 加密后的十六进制字符串
     * @param privateKey   私钥
     * @return 解密后的明文字符串
     * @throws Exception 密文格式非法、私钥与密文不匹配或解密失败
     */
    private static String decrypt(String encryptedHex, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(decodeHex(encryptedHex, "Encrypted data"));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 使用 RAW HEX 格式的私钥解密字符串
     *
     * @param encryptedHex  加密后的十六进制字符串
     * @param hexPrivateKey RAW HEX 格式的私钥
     * @return 解密后的明文字符串
     * @throws IllegalArgumentException 私钥或密文 HEX 非法
     * @throws Exception                私钥与密文不匹配或解密失败
     */
    public static String decrypt(String encryptedHex, String hexPrivateKey) throws Exception {
        PrivateKey privateKey = rawHexToPrivateKey(hexPrivateKey);
        return decrypt(encryptedHex, privateKey);
    }

    /**
     * 使用私钥对数据进行签名
     *
     * <p>算法为 SHA256withECDDSA（SHA-256 摘要 + 确定性 ECDSA），待签名的字符串按 UTF-8
     * 编码转换为字节，签名结果为 ASN.1 DER 编码。该算法不依赖随机数，同一私钥对同一数据
     * 多次签名会得到完全相同的签名，因此不要把签名结果当作每次调用都不同的随机值使用。</p>
     *
     * @param data       要签名的数据
     * @param privateKey 私钥
     * @return ASN.1 DER 编码的签名（HEX 字符串）
     * @throws Exception 签名算法或私钥不可用，或签名失败
     */
    private static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDDSA", "BC");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Hex.toHexString(signatureBytes);
    }

    /**
     * 使用 RAW HEX 格式的私钥对数据进行签名
     *
     * @param data          要签名的数据
     * @param hexPrivateKey RAW HEX 格式的私钥
     * @return ASN.1 DER 编码的签名（HEX 字符串），可直接交给 {@link #verify(String, String, String)} 验签
     * @throws IllegalArgumentException 私钥 HEX 非法
     * @throws Exception                签名算法不可用或签名失败
     */
    public static String sign(String data, String hexPrivateKey) throws Exception {
        PrivateKey privateKey = rawHexToPrivateKey(hexPrivateKey);
        return sign(data, privateKey);
    }

    /**
     * 使用公钥验证签名
     *
     * <p>待验签的字符串按 UTF-8 编码转换为字节，因此签名方与验签方必须使用相同的字符
     * 编码。签名必须是 ASN.1 DER 编码的 ECDSA 签名（SHA256withECDDSA 与 SHA256withECDSA
     * 的结果都可接受），其他编码形式（例如直接把 ECDSA 的 r、s 拼接成定长字节）无法验签。</p>
     *
     * @param data         数据
     * @param signatureHex 数据的签名，SHA256withECDDSA 生成的 ASN.1 DER 编码签名（HEX 字符串）
     * @param publicKey    公钥
     * @return 验证结果，true表示签名有效，false表示签名无效
     * @throws Exception 签名或公钥格式非法，或验签算法不可用
     */
    private static boolean verify(String data, String signatureHex, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = decodeHex(signatureHex, "Signature");
        return signature.verify(signatureBytes);
    }

    /**
     * 使用 RAW HEX 格式的公钥验证签名
     *
     * @param data         数据
     * @param signatureHex SHA256withECDDSA 生成的 ASN.1 DER 编码签名（HEX 字符串）
     * @param hexPublicKey RAW HEX 格式的公钥
     * @return 验证结果，true表示签名有效，false表示签名无效
     * @throws IllegalArgumentException 公钥或签名 HEX 非法
     * @throws Exception                验签算法不可用
     */
    public static boolean verify(String data, String signatureHex, String hexPublicKey) throws Exception {
        PublicKey publicKey = rawHexToPublicKey(hexPublicKey);
        return verify(data, signatureHex, publicKey);
    }

}
