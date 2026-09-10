package com.github.wrx886.e2echo.ecc.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM 加解密工具类。
 *
 * <p>字符串加解密结果格式为 {@code [12 字节随机 IV] + [密文及认证标签]}，整体以 HEX
 * 字符串返回；文件加解密时 IV 直接写入输出文件头部。GCM 为带认证的加密模式，密文一旦
 * 被篡改，解密时会抛出异常。文件解密要求目标文件不存在，解密失败会自动删除本次创建的
 * 目标文件。所有 HEX 输入必须是偶数长度的合法十六进制，AES 密钥固定为 32 字节
 * （256 位）。</p>
 */
public class AesUtil {

    /**
     * 私有构造方法，禁止外部实例化工具类。
     */
    private AesUtil() {
    }

    // 算法字符串 GCM 模式
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    // 新增：GCM 模式参数
    private static final int GCM_IV_LENGTH = 12; // 推荐使用 12 字节 IV
    private static final int GCM_TAG_LENGTH = 128; // 认证标签长度 (位)

    // 缓冲区大小
    private static final int BUFFER_SIZE = 8192;

    /**
     * 生成随机的 AES-256 密钥。
     *
     * @return 32 字节密钥的 HEX 字符串
     * @throws Exception 密钥生成失败
     */
    public static String generateKeyAsHex() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGen.init(KEY_SIZE, new SecureRandom());
        SecretKey secretKey = keyGen.generateKey();
        return bytesToHex(secretKey.getEncoded());
    }

    /**
     * 使用 AES-GCM 加密明文。
     *
     * <p>加密结果格式为 {@code [12 字节随机 IV] + [密文]}，IV 不固定且每次随机生成。</p>
     *
     * @param plainText 待加密的明文
     * @param hexKey    HEX 字符串形式的 AES 密钥
     * @return 密文的 HEX 字符串（IV 位于头部）
     * @throws IllegalArgumentException 密钥 HEX 非法或密钥长度不是 32 字节
     * @throws Exception                加密算法不可用或加密失败
     */
    public static String encrypt(String plainText, String hexKey) throws Exception {
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
    }

    /**
     * 使用 AES-GCM 解密由 {@link #encrypt(String, String)} 生成的密文。
     *
     * <p>解密时会先从密文头部提取 12 字节 IV，再对剩余部分进行解密和完整性校验。</p>
     *
     * @param cipherText 密文的 HEX 字符串（IV 位于头部）
     * @param hexKey     HEX 字符串形式的 AES 密钥
     * @return 解密后的明文
     * @throws IllegalArgumentException 密钥或密文 HEX 非法、密钥长度不是 32 字节、密文过短
     * @throws Exception                密文被篡改或解密失败
     */
    public static String decrypt(String cipherText, String hexKey) throws Exception {
        SecretKey secretKey = loadKeyFromHex(hexKey);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

        // 将 HEX 字符串转回字节数组
        byte[] combined = hexToBytes(cipherText);
        if (combined.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Cipher text is too short: it must contain at least the 12-byte IV.");
        }

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
    }

    /**
     * 使用 AES-GCM 加密文件。
     *
     * <p>加密时将 12 字节随机 IV 写入输出文件头部，再写入加密后的文件内容。</p>
     *
     * @param inputFile  待加密的输入文件路径
     * @param outputFile 加密结果的输出文件路径
     * @param hexKey     HEX 字符串形式的 AES 密钥
     * @throws IllegalArgumentException 密钥 HEX 非法或密钥长度不是 32 字节
     * @throws Exception                文件读写失败、加密算法不可用或加密失败
     */
    public static void encryptFile(String inputFile, String outputFile, String hexKey) throws Exception {
        SecretKey secretKey = loadKeyFromHex(hexKey);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

        // 生成随机 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // 使用 GCMParameterSpec 初始化
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            // 写入 iv
            outputStream.write(iv);

            // 开始加密数据
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(cipher.update(buffer, 0, bytesRead));
            }

            // 最终加密
            byte[] finalBytes = cipher.doFinal();
            outputStream.write(finalBytes);
        }
    }

    /**
     * 使用 AES-GCM 解密由 {@link #encryptFile(String, String, String)} 加密的文件。
     *
     * <p>解密时会先从输入文件头部读取 12 字节 IV，再对剩余内容进行解密和完整性校验。
     * 目标文件必须不存在，否则方法会拒绝执行；若解密过程中失败，本次创建的目标文件会被
     * 自动删除，不会留下部分明文。</p>
     *
     * @param inputFile  待解密的输入文件路径
     * @param outputFile 解密结果的输出文件路径
     * @param hexKey     HEX 字符串形式的 AES 密钥
     * @throws IllegalArgumentException 密钥 HEX 非法或密钥长度不是 32 字节
     * @throws IOException              输出文件已存在、文件读写失败或文件过短
     * @throws Exception                密文被篡改或解密失败
     */
    public static void decryptFile(String inputFile, String outputFile, String hexKey) throws Exception {
        Path outputPath = Path.of(outputFile);
        if (Files.exists(outputPath)) {
            throw new IOException("Output file already exists: " + outputFile + ". Refusing to overwrite it.");
        }

        SecretKey secretKey = loadKeyFromHex(hexKey);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

        // 使用 CREATE_NEW 避免并发下覆盖其他线程创建的目标文件
        boolean outputCreated = false;
        try (OutputStream outputStream = Files.newOutputStream(outputPath, StandardOpenOption.CREATE_NEW)) {
            outputCreated = true;

            try (FileInputStream inputStream = new FileInputStream(inputFile)) {
                // 提取 IV（readNBytes 会读取满 12 字节或读到 EOF）
                byte[] iv = new byte[GCM_IV_LENGTH];
                int ivBytesRead = inputStream.readNBytes(iv, 0, iv.length);
                if (ivBytesRead != GCM_IV_LENGTH) {
                    throw new IOException("Encrypted file is too short: missing the 12-byte IV header.");
                }

                // 使用提取的 IV 初始化解密器
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

                // 开始解密数据
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(cipher.update(buffer, 0, bytesRead));
                }

                // 最终解密并校验认证标签
                byte[] finalBytes = cipher.doFinal();
                outputStream.write(finalBytes);
            }
        } catch (Exception e) {
            if (outputCreated) {
                try {
                    Files.deleteIfExists(outputPath);
                } catch (IOException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }
            throw e;
        }
    }

    /**
     * 将字节数组转换为 HEX 字符串。
     *
     * @param bytes 字节数组
     * @return 对应的 HEX 字符串
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
     * 将 HEX 字符串转换为字节数组。
     *
     * @param hex HEX 字符串
     * @return 对应的字节数组
     * @throws IllegalArgumentException hex 为 null/空、长度非偶数或包含非十六进制字符
     */
    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException("HEX string must not be null or empty.");
        }
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("HEX string must contain an even number of characters.");
        }

        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("HEX string contains non-hexadecimal characters.");
            }
            data[i / 2] = (byte) ((high << 4) | low);
        }
        return data;
    }

    /**
     * 从 HEX 字符串恢复 AES 密钥。
     *
     * @param hexKey HEX 字符串形式的 AES 密钥
     * @return 恢复出的 AES 密钥
     */
    private static SecretKey loadKeyFromHex(String hexKey) {
        byte[] rawKey = hexToBytes(hexKey);
        if (rawKey.length != 32) {
            throw new IllegalArgumentException(
                    "AES key must be 32 bytes (256 bits), but got " + rawKey.length + " bytes.");
        }
        return new SecretKeySpec(rawKey, KEY_ALGORITHM);
    }

}
