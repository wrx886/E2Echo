package com.github.wrx886.e2echo.server.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class EccUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 生成 ECC 密钥对
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp521r1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 将公钥转换为 HEX 字符串
     */
    public static String publicKeyToHex(PublicKey publicKey) {
        return Hex.toHexString(publicKey.getEncoded());
    }

    /**
     * 将私钥转换为 HEX 字符串
     */
    public static String privateKeyToHex(PrivateKey privateKey) {
        return Hex.toHexString(privateKey.getEncoded());
    }

    /**
     * 从 HEX 字符串恢复公钥
     */
    public static PublicKey hexToPublicKey(String hexPublicKey) throws Exception {
        byte[] keyBytes = Hex.decode(hexPublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 从 HEX 字符串恢复私钥
     */
    public static PrivateKey hexToPrivateKey(String hexPrivateKey) throws Exception {
        byte[] keyBytes = Hex.decode(hexPrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 使用公钥加密字符串
     */
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Hex.toHexString(encryptedBytes);
    }

    /**
     * 使用 HEX 格式的公钥加密字符串
     */
    public static String encrypt(String plainText, String hexPublicKey) throws Exception {
        PublicKey publicKey = hexToPublicKey(hexPublicKey);
        return encrypt(plainText, publicKey);
    }

    /**
     * 使用私钥解密字符串
     */
    public static String decrypt(String encryptedHex, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Hex.decode(encryptedHex));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 使用 HEX 格式的私钥解密字符串
     */
    public static String decrypt(String encryptedHex, String hexPrivateKey) throws Exception {
        PrivateKey privateKey = hexToPrivateKey(hexPrivateKey);
        return decrypt(encryptedHex, privateKey);
    }

    /**
     * 使用私钥对数据进行签名
     */
    public static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Hex.toHexString(signatureBytes);
    }

    /**
     * 使用 HEX 格式的私钥对数据进行签名
     */
    public static String sign(String data, String hexPrivateKey) throws Exception {
        PrivateKey privateKey = hexToPrivateKey(hexPrivateKey);
        return sign(data, privateKey);
    }

    /**
     * 使用公钥验证签名
     */
    public static boolean verify(String data, String signatureHex, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Hex.decode(signatureHex);
        return signature.verify(signatureBytes);
    }

    /**
     * 使用 HEX 格式的公钥验证签名
     */
    public static boolean verify(String data, String signatureHex, String hexPublicKey) throws Exception {
        PublicKey publicKey = hexToPublicKey(hexPublicKey);
        return verify(data, signatureHex, publicKey);
    }

    public static void main(String[] args) throws Exception {
        // 生成密钥对
        KeyPair keyPair = generateKeyPair();
        String publicKeyHex = publicKeyToHex(keyPair.getPublic());
        String privateKeyHex = privateKeyToHex(keyPair.getPrivate());

        System.out.println("Public Key (HEX): " + publicKeyHex);
        System.out.println("Private Key (HEX): " + privateKeyHex);

        // 加密字符串
        String plainText = "Hello, ECC!";
        String encryptedHex = encrypt(plainText, publicKeyHex); // 使用 HEX 公钥加密
        System.out.println("Encrypted (HEX): " + encryptedHex);

        // 解密字符串
        String decryptedText = decrypt(encryptedHex, privateKeyHex); // 使用 HEX 私钥解密
        System.out.println("Decrypted: " + decryptedText);

        // 签名和验证
        String data = "This is a test message.";
        String signatureHex = sign(data, privateKeyHex); // 使用 HEX 私钥签名
        System.out.println("Signature (HEX): " + signatureHex);

        boolean isVerified = verify(data, signatureHex, publicKeyHex); // 使用 HEX 公钥验证
        System.out.println("Signature verified: " + isVerified);
    }
}