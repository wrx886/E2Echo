package com.github.wrx886.e2echo.client.ecc.service;

import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.ecc.util.EccUtil.KeyPairHex;

// ECC 服务
public interface EccService {

    /**
     * 生成密钥对
     * 
     * @return 密钥对
     */
    KeyPairHex generateKeyPair();

    /**
     * 登入
     * 
     * @param publicKeyHex  公钥
     * @param privateKeyHex 私钥
     */
    void login(String publicKeyHex, String privateKeyHex);

    /**
     * 登出
     */
    void logout();

    /**
     * 获取公钥
     * 
     * @return ROW HEX 格式的公钥
     */
    String getPublicKey();

    /**
     * 加密数据
     * 
     * @param eccMessage 未加密的 ECC 消息，UUID 和 时间戳 会自动生成
     * @return 加密后的 ECC 消息
     */
    EccMessage encrypt(EccMessage eccMessage);

    /**
     * 解密数据
     * 
     * @param eccMessage 加密后的 ECC 消息
     * @return 未加密的 ECC 消息
     */
    EccMessage decrypt(EccMessage eccMessage);

    /**
     * 签名消息
     * 
     * @param eccMessage 未签名的 ECC 消息
     * @return 已签名的 ECC 消息
     */
    EccMessage sign(EccMessage eccMessage);

    /**
     * 验证签名
     * 
     * @param eccMessage 待验证的 ECC 消息
     * @return true 验证通过，false 验证失败
     */
    boolean verify(EccMessage eccMessage);

}
