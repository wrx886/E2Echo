package com.github.wrx886.e2echo.client.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class FileHashUtil {
    private static final String ALGORITHM = "SHA-256";

    /**
     * 计算文件的SHA-256哈希值
     */
    public static String calculateFileHash(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

            try (InputStream is = new FileInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
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

}
