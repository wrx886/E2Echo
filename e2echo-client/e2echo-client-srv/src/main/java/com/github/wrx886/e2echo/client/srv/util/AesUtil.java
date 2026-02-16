package com.github.wrx886.e2echo.client.srv.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesUtil {
    // 算法字符串 GCM 模式
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    // 新增：GCM 模式参数
    private static final int GCM_IV_LENGTH = 12; // 推荐使用 12 字节 IV
    private static final int GCM_TAG_LENGTH = 128; // 认证标签长度 (位)

    /**
     * 生成随机密钥并返回其HEX表示
     */
    public static String generateKeyAsHex() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return bytesToHex(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 加密
     * 加密结果格式变为 [IV(12字节)] + [密文]
     */
    public static String encrypt(String plainText, String hexKey) {
        try {
            SecretKey secretKey = loadKeyFromHex(hexKey);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 使用 GCMParameterSpec 初始化
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // 加密
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将 IV 拼接到密文前面，一起返回
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return bytesToHex(combined);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密
     * 解密时需先提取 IV
     */
    public static String decrypt(String cipherText, String hexKey) {
        try {
            SecretKey secretKey = loadKeyFromHex(hexKey);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

            // 将 HEX 字符串转回字节数组
            byte[] combined = hexToBytes(cipherText);

            // 提取前 12 字节作为 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // 提取后面的密文部分
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

            // 使用提取的 IV 初始化解密器
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // 解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 以下辅助方法保持不变
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static SecretKey loadKeyFromHex(String hexKey) {
        byte[] rawKey = hexToBytes(hexKey);
        return new SecretKeySpec(rawKey, KEY_ALGORITHM);
    }
}