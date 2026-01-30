package com.github.wrx886.e2echo.client.ecc.controller.impl;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.ecc.service.EccService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class EccControllerImpl implements EccController {

    private final EccService eccService;

    /**
     * 加密数据
     * 
     * @param eccMessage 未加密的 ECC 消息，UUID 和 时间戳 会自动生成
     * @return 加密后的 ECC 消息
     */
    @Override
    public EccMessage encrypt(EccMessage eccMessage) {
        return eccService.encrypt(eccMessage);
    }

    /**
     * 解密数据
     * 
     * @param eccMessage 加密后的 ECC 消息
     * @return 未加密的 ECC 消息
     */
    @Override
    public EccMessage decrypt(EccMessage eccMessage) {
        return eccService.decrypt(eccMessage);
    }

    /**
     * 签名消息
     * 
     * @param eccMessage 未签名的 ECC 消息
     * @return 已签名的 ECC 消息
     */
    @Override
    public EccMessage sign(EccMessage eccMessage) {
        return eccService.sign(eccMessage);
    }

    /**
     * 验证签名
     * 
     * @param eccMessage 待验证的 ECC 消息
     * @return true 验证通过，false 验证失败
     */
    @Override
    public boolean verify(EccMessage eccMessage) {
        return eccService.verify(eccMessage);
    }

    /**
     * 获取公钥
     * 
     * @return ROW HEX 格式的公钥
     */
    @Override
    public String getPublicKey() {
        return eccService.getPublicKey();
    }

}
