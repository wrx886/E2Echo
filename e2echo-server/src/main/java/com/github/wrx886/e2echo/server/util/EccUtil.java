package com.github.wrx886.e2echo.server.util;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.Cipher;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

// ECC 工具类
public final class EccUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // RAW HEX 格式的密钥对
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyPairHex {
        private String publicKeyHex;
        private String privateKeyHex;
    }

    /**
     * 生成 ECC 密钥对
     * 
     * @return 包含公钥和私钥 RAW HEX 字符串的 KeyPairHex 对象
     * @throws Exception
     */
    public static KeyPairHex generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new KeyPairHex(publicKeyToRawHex(keyPair.getPublic()), privateKeyToRawHex(keyPair.getPrivate()));
    }

    /**
     * 将公钥转换为 RAW HEX 字符串 (X.509 format)
     * 
     * @param publicKey 公钥
     * @return RAW HEX 字符串格式的公钥
     * @throws Exception
     */
    private static String publicKeyToRawHex(PublicKey publicKey) throws Exception {
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
     * 从 RAW HEX 字符串恢复公钥 (X.509 format)
     * 
     * @param hexPublicKey RAW HEX 格式的公钥
     * @return 公钥
     * @throws Exception
     */
    private static PublicKey rawHexToPublicKey(String hexPublicKey) throws Exception {
        byte[] keyBytes = Hex.decode(hexPublicKey);
        if (keyBytes[0] != 0x04)
            throw new IllegalArgumentException("Invalid uncompressed public key format");
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
     * @throws Exception
     */
    private static PrivateKey rawHexToPrivateKey(String hexPrivateKey) throws Exception {
        BigInteger d = new BigInteger(hexPrivateKey, 16);
        X9ECParameters ecParams = SECNamedCurves.getByName("secp256k1");
        ECParameterSpec ecSpec = new ECParameterSpec(ecParams.getCurve(), ecParams.getG(), ecParams.getN(),
                ecParams.getH());
        ECPrivateKeySpec privKeySpec = new ECPrivateKeySpec(d, ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        return keyFactory.generatePrivate(privKeySpec);
    }

    /**
     * 使用公钥加密字符串
     * 
     * @param plainText 明文字符串
     * @param publicKey 公钥
     * @return 加密后的十六进制字符串
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
     */
    private static String decrypt(String encryptedHex, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Hex.decode(encryptedHex));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 使用 RAW HEX 格式的私钥解密字符串
     * 
     * @param encryptedHex  加密后的十六进制字符串
     * @param hexPrivateKey RAW HEX 格式的私钥
     * @return 解密后的明文字符串
     * @throws Exception
     */
    public static String decrypt(String encryptedHex, String hexPrivateKey) throws Exception {
        PrivateKey privateKey = rawHexToPrivateKey(hexPrivateKey);
        return decrypt(encryptedHex, privateKey);
    }

    /**
     * 使用私钥对数据进行签名
     * 
     * @param data       要签名的数据
     * @param privateKey 私钥
     * @return 数据的签名
     * @throws Exception
     */
    private static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
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
     * @return 数据的签名
     * @throws Exception
     */
    public static String sign(String data, String hexPrivateKey) throws Exception {
        PrivateKey privateKey = rawHexToPrivateKey(hexPrivateKey);
        return sign(data, privateKey);
    }

    /**
     * 使用公钥验证签名
     */
    /**
     * 使用公钥验证签名
     * 
     * @param data         数据
     * @param signatureHex 数据的签名
     * @param publicKey    公钥
     * @return 验证结果，true表示签名有效，false表示签名无效
     * @throws Exception
     */
    private static boolean verify(String data, String signatureHex, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Hex.decode(signatureHex);
        return signature.verify(signatureBytes);
    }

    /**
     * 使用 RAW HEX 格式的公钥验证签名
     * 
     * @param data         数据
     * @param signatureHex 数据的签名
     * @param hexPublicKey RAW HEX 格式的公钥
     * @return 验证结果，true表示签名有效，false表示签名无效
     * @throws Exception
     */
    public static boolean verify(String data, String signatureHex, String hexPublicKey) throws Exception {
        PublicKey publicKey = rawHexToPublicKey(hexPublicKey);
        return verify(data, signatureHex, publicKey);
    }

}
