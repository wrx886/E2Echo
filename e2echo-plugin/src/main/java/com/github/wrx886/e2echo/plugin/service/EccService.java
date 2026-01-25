package com.github.wrx886.e2echo.plugin.service;

import java.util.LinkedHashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.plugin.model.entity.EccMessage;
import com.github.wrx886.e2echo.plugin.result.E2EchoException;
import com.github.wrx886.e2echo.plugin.result.ResultCodeEnum;
import com.github.wrx886.e2echo.plugin.store.EccKeyStore;
import com.github.wrx886.e2echo.plugin.util.EccUtil;
import com.github.wrx886.e2echo.plugin.util.EccUtil.KeyPairHex;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class EccService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final EccKeyStore eccKeyStore;

    /**
     * 生成密钥对
     * 
     * @return 密钥对
     */
    public KeyPairHex generateKeyPair() {
        try {
            return EccUtil.generateKeyPair();
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.ECC_KEY_PAIR_GENERATION_FAILED);
        }
    }

    /**
     * 登入
     * 
     * @param publicKeyHex  公钥
     * @param privateKeyHex 私钥
     */
    public void login(String publicKeyHex, String privateKeyHex) {
        try {
            // 检查公钥和私钥是否匹配
            String plain = UUID.randomUUID().toString();
            String cipher = EccUtil.encrypt(plain, publicKeyHex);
            String decrypted = EccUtil.decrypt(cipher, privateKeyHex);
            if (!plain.equals(decrypted)) {
                // 公私钥不匹配
                throw new E2EchoException(ResultCodeEnum.ECC_KEY_PAIR_INVALID);
            }
            String sign = EccUtil.sign(plain, privateKeyHex);
            if (!EccUtil.verify(plain, sign, publicKeyHex)) {
                // 公私钥不匹配
                throw new E2EchoException(ResultCodeEnum.ECC_KEY_PAIR_INVALID);
            }

            // 设置密钥对
            eccKeyStore.set(publicKeyHex, privateKeyHex);
        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.ECC_KEY_PAIR_INVALID);
        }
    }

    /**
     * 登出
     */
    public void logout() {
        eccKeyStore.clear();
    }

    /**
     * 获取公钥
     * 
     * @return ROW HEX 格式的公钥
     */
    public String getPublicKey() {
        KeyPairHex keyPairHex = eccKeyStore.get();
        // 用户未登入
        if (keyPairHex.getPublicKeyHex() == null || keyPairHex.getPrivateKeyHex() == null) {
            throw new E2EchoException(ResultCodeEnum.ECC_NOT_LOGIN);
        }

        return keyPairHex.getPublicKeyHex();
    }

    /**
     * 加密数据
     * 
     * @param eccMessage 未加密的 ECC 消息，UUID 和 时间戳 会自动生成
     * @return 加密后的 ECC 消息
     */
    public EccMessage encrypt(EccMessage eccMessage) {
        KeyPairHex keyPairHex = eccKeyStore.get();

        // 用户未登入
        if (keyPairHex.getPublicKeyHex() == null || keyPairHex.getPrivateKeyHex() == null) {
            throw new E2EchoException(ResultCodeEnum.ECC_NOT_LOGIN);
        }

        // 检查接收方公钥是否存在
        if (eccMessage.getToPublicKeyHex() == null || eccMessage.getToPublicKeyHex().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_IS_EMPTY);
        }

        // 检查发送方公钥是否存在
        if (eccMessage.getFromPublicKeyHex() == null || eccMessage.getFromPublicKeyHex().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_IS_EMPTY);
        }

        // 检查发送方公钥是否匹配
        if (!eccMessage.getFromPublicKeyHex().equals(keyPairHex.getPublicKeyHex())) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_NOT_MATCH);
        }

        // 检查数据是否为空
        if (eccMessage.getData() == null) {
            throw new E2EchoException(ResultCodeEnum.ECC_DATA_IS_NULL);
        }

        // 生成 UUID 和时间戳
        EccMessage result = new EccMessage();
        result.setUuid(UUID.randomUUID().toString());
        result.setTimestamp(Long.toString(System.currentTimeMillis()));

        // 设置发送方和接收方公钥
        result.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        result.setToPublicKeyHex(eccMessage.getToPublicKeyHex());

        // 加密数据
        try {
            result.setData(EccUtil.encrypt(eccMessage.getData(),
                    result.getToPublicKeyHex()));
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.ECC_ENCRYPT_FAILED);
        }

        // 生成签名
        try {
            result.setSignature(EccUtil.sign(
                    objectMapper.writeValueAsString(toMap(result)),
                    keyPairHex.getPrivateKeyHex()));
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.ECC_SIGN_FAILED);
        }

        // 返回结果
        return result;
    }

    /**
     * 解密数据
     * 
     * @param eccMessage 加密后的 ECC 消息
     * @return 未加密的 ECC 消息
     */
    public EccMessage decrypt(EccMessage eccMessage) {
        KeyPairHex keyPairHex = eccKeyStore.get();

        // 用户未登入
        if (keyPairHex.getPublicKeyHex() == null || keyPairHex.getPrivateKeyHex() == null) {
            throw new E2EchoException(ResultCodeEnum.ECC_NOT_LOGIN);
        }

        // 接收方公钥不存在
        if (eccMessage.getToPublicKeyHex() == null || eccMessage.getToPublicKeyHex().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_IS_EMPTY);
        }

        // 接收方公钥不匹配
        if (!eccMessage.getToPublicKeyHex().equals(keyPairHex.getPublicKeyHex())) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_NOT_MATCH);
        }

        // 发送方公钥不存在
        if (eccMessage.getFromPublicKeyHex() == null || eccMessage.getFromPublicKeyHex().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_IS_EMPTY);
        }

        // 验证签名
        try {
            if (!EccUtil.verify(
                    objectMapper.writeValueAsString(toMap(eccMessage)),
                    eccMessage.getSignature(), eccMessage.getFromPublicKeyHex())) {
                throw new E2EchoException(ResultCodeEnum.ECC_SIGNATURE_NOT_MATCH);
            }
        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.ECC_SIGNATURE_VERIFY_FAILED);
        }

        // 设置基本信息
        EccMessage result = new EccMessage();
        result.setUuid(eccMessage.getUuid());
        result.setTimestamp(eccMessage.getTimestamp());
        result.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        result.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        result.setSignature(eccMessage.getSignature());

        // 解密消息
        try {
            result.setData(EccUtil.decrypt(eccMessage.getData(), keyPairHex.getPrivateKeyHex()));
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.ECC_DECRYPT_FAILED);
        }

        // 返回
        return result;
    }

    /**
     * 签名消息
     * 
     * @param eccMessage 未签名的 ECC 消息
     * @return 已签名的 ECC 消息
     */
    public EccMessage sign(EccMessage eccMessage) {
        KeyPairHex keyPairHex = eccKeyStore.get();

        // 用户未登入
        if (keyPairHex.getPublicKeyHex() == null || keyPairHex.getPrivateKeyHex() == null) {
            throw new E2EchoException(ResultCodeEnum.ECC_NOT_LOGIN);
        }

        // 签名方公钥为空
        if (eccMessage.getFromPublicKeyHex() == null || eccMessage.getFromPublicKeyHex().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_IS_EMPTY);
        }

        // 签名方公钥不匹配
        if (!eccMessage.getFromPublicKeyHex().equals(keyPairHex.getPublicKeyHex())) {
            throw new E2EchoException(ResultCodeEnum.ECC_PUBLIC_KEY_NOT_MATCH);
        }

        // 检查数据是否为空
        if (eccMessage.getData() == null) {
            throw new E2EchoException(ResultCodeEnum.ECC_DATA_IS_NULL);
        }

        // 设置基本信息
        EccMessage result = new EccMessage();
        result.setUuid(UUID.randomUUID().toString());
        result.setTimestamp(Long.toString(System.currentTimeMillis()));
        result.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        result.setToPublicKeyHex(eccMessage.getToPublicKeyHex() != null ? eccMessage.getToPublicKeyHex() : "");
        result.setData(eccMessage.getData());

        // 签名
        try {
            result.setSignature(EccUtil.sign(
                    objectMapper.writeValueAsString(toMap(result)),
                    keyPairHex.getPrivateKeyHex()));
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.ECC_SIGN_FAILED);
        }

        return result;
    }

    /**
     * 验证签名
     * 
     * @param eccMessage 待验证的 ECC 消息
     * @return true 验证通过，false 验证失败
     */
    public static boolean verify(EccMessage eccMessage) {
        try {
            // 验证签名
            return EccUtil.verify(
                    objectMapper.writeValueAsString(toMap(eccMessage)),
                    eccMessage.getSignature(), eccMessage.getFromPublicKeyHex());
        } catch (Exception e) {
            // 出现异常，返回失败
            return false;
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
