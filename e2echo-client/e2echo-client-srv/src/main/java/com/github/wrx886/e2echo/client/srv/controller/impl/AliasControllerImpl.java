package com.github.wrx886.e2echo.client.srv.controller.impl;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.srv.service.AliasService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class AliasControllerImpl implements AliasController {

    private final AliasService aliasService;

    /**
     * 添加或修改别名
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @param alias        别名
     */
    @Override
    public void put(String publicKeyHex, String alias) {
        aliasService.put(publicKeyHex, alias);
    }

    /**
     * 获取别名
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @return 别名
     */
    @Override
    public String get(String publicKeyHex) {
        return aliasService.get(publicKeyHex);
    }

}
