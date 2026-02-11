package com.github.wrx886.e2echo.client.srv.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.client.common.model.entity.Alias;

public interface AliasService extends IService<Alias> {

    /**
     * 添加或修改别名
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @param alias        别名
     */
    void put(String publicKeyHex, String alias);

    /**
     * 获取别名
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @return 别名
     */
    String get(String publicKeyHex);

}
