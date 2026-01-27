package com.github.wrx886.e2echo.server.test.util;

import java.util.LinkedHashMap;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.util.EccUtil;
import com.github.wrx886.e2echo.server.util.EccUtil.KeyPairHex;

// EccMessage 测试工具类
public final class EccMessageTestUtil {

    // 私有构造函数
    private EccMessageTestUtil() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成密钥对
     * 
     * @return 密钥对
     */
    public static KeyPairHex generateKeyPair() {
        try {
            return EccUtil.generateKeyPair();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 加密数据
     * 
     * @param keyPairHex 密钥对
     * @param eccMessage 未加密的 ECC 消息，UUID 和 时间戳 会自动生成
     * @return 加密后的 ECC 消息
     */
    public static EccMessage encrypt(KeyPairHex keyPairHex, EccMessage eccMessage) {
        try {
            // 生成 UUID 和时间戳
            EccMessage result = new EccMessage();
            result.setUuid(UUID.randomUUID().toString());
            result.setTimestamp(Long.toString(System.currentTimeMillis()));

            // 设置发送方和接收方公钥
            result.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
            result.setToPublicKeyHex(eccMessage.getToPublicKeyHex());

            // 加密数据
            result.setData(EccUtil.encrypt(eccMessage.getData(),
                    result.getToPublicKeyHex()));

            // 生成签名
            result.setSignature(EccUtil.sign(
                    objectMapper.writeValueAsString(toMap(result)),
                    keyPairHex.getPrivateKeyHex()));

            // 返回结果
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解密数据
     * 
     * @param keyPairHex 密钥对
     * @param eccMessage 加密后的 ECC 消息
     * @return 未加密的 ECC 消息
     */
    public static EccMessage decrypt(KeyPairHex keyPairHex, EccMessage eccMessage) {
        try {
            // 验证签名
            if (!EccUtil.verify(
                    objectMapper.writeValueAsString(toMap(eccMessage)),
                    eccMessage.getSignature(), eccMessage.getFromPublicKeyHex())) {
                return null;
            }

            // 设置基本信息
            EccMessage result = new EccMessage();
            result.setUuid(eccMessage.getUuid());
            result.setTimestamp(eccMessage.getTimestamp());
            result.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
            result.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
            result.setSignature(eccMessage.getSignature());

            // 解密消息
            result.setData(EccUtil.decrypt(eccMessage.getData(), keyPairHex.getPrivateKeyHex()));

            // 返回
            return result;
        } catch (Exception e) {
            // 解密失败，返回 null
            return null;
        }
    }

    /**
     * 签名消息
     * 
     * @param eccMessage 未签名的 ECC 消息
     * @return 已签名的 ECC 消息
     */
    public static EccMessage sign(KeyPairHex keyPairHex, EccMessage eccMessage) {
        try {
            // 设置基本信息
            EccMessage result = new EccMessage();
            result.setUuid(UUID.randomUUID().toString());
            result.setTimestamp(Long.toString(System.currentTimeMillis()));
            result.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
            result.setToPublicKeyHex(eccMessage.getToPublicKeyHex() != null ? eccMessage.getToPublicKeyHex() : "");
            result.setData(eccMessage.getData());

            // 签名
            result.setSignature(EccUtil.sign(
                    objectMapper.writeValueAsString(toMap(result)),
                    keyPairHex.getPrivateKeyHex()));

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 ECC 消息转为 Map
     * 
     * @param eccMessage ECC 消息
     * @return Map
     */
    private static LinkedHashMap<String, String> toMap(EccMessage eccMessage) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("uuid", eccMessage.getUuid());
        map.put("timestamp", eccMessage.getTimestamp());
        map.put("fromPublicKeyHex", eccMessage.getFromPublicKeyHex());
        map.put("toPublicKeyHex", eccMessage.getToPublicKeyHex());
        map.put("data", eccMessage.getData());
        return map;
    }
}
