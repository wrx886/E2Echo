package com.github.wrx886.e2echo.client.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;

public class AesFileUtil {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256; // 使用256位密钥长度
    private static final int BUFFER_SIZE = 8192;

    /**
     * 生成随机密钥并返回其HEX表示
     */
    public static String generateKeyAsHex() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return bytesToHex(secretKey.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 将字节数组转换为HEX字符串
     */
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

    /**
     * 将HEX字符串转换为字节数组
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 从HEX字符串加载密钥
     */
    private static SecretKey loadKeyFromHex(String hexKey) {
        byte[] rawKey = hexToBytes(hexKey);
        return new SecretKeySpec(rawKey, ALGORITHM);
    }

    /**
     * 加密文件
     */
    public static void encrypt(String inputFile, String outputFile, String hexKey) {
        try {
            SecretKey secretKey = loadKeyFromHex(hexKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // 确保父目录存在，如果不存在则尝试创建
            File file = new File(outputFile);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs(); // 创建多级目录
                if (dirCreated) {
                    System.out.println("父目录创建成功: " + parentDir.getAbsolutePath());
                } else {
                    System.out.println("无法创建父目录.");
                    return; // 如果不能创建目录，则不继续尝试写文件
                }
            }

            try (FileInputStream fis = new FileInputStream(inputFile);
                    FileOutputStream fos = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] outputBytes = cipher.update(buffer, 0, bytesRead);
                    if (outputBytes != null) {
                        fos.write(outputBytes);
                    }
                }

                byte[] outputBytes = cipher.doFinal();
                if (outputBytes != null) {
                    fos.write(outputBytes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 解密文件
     */
    public static void decrypt(String inputFile, String outputFile, String hexKey) {
        try {
            SecretKey secretKey = loadKeyFromHex(hexKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            // 确保父目录存在，如果不存在则尝试创建
            File file = new File(outputFile);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs(); // 创建多级目录
                if (dirCreated) {
                    System.out.println("父目录创建成功: " + parentDir.getAbsolutePath());
                } else {
                    System.out.println("无法创建父目录.");
                    return; // 如果不能创建目录，则不继续尝试写文件
                }
            }

            try (FileInputStream fis = new FileInputStream(inputFile);
                    FileOutputStream fos = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] outputBytes = cipher.update(buffer, 0, bytesRead);
                    if (outputBytes != null) {
                        fos.write(outputBytes);
                    }
                }

                byte[] outputBytes = cipher.doFinal();
                if (outputBytes != null) {
                    fos.write(outputBytes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

}
