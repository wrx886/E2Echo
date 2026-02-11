package com.github.wrx886.e2echo.client.srv.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.model.entity.Alias;
import com.github.wrx886.e2echo.client.srv.mapper.AliasMapper;
import com.github.wrx886.e2echo.client.srv.service.AliasService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AliasServiceImpl extends ServiceImpl<AliasMapper, Alias> implements AliasService {

    private final EccController eccController;

    /**
     * 添加或修改别名
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @param alias        别名
     */
    @Override
    public void put(String publicKeyHex, String alias) {
        // 查询别名
        Alias aliasEntity = this.getOne(new LambdaQueryWrapper<Alias>()
                .eq(Alias::getPublicKeyHex, publicKeyHex)
                .eq(Alias::getOwnerPublicKeyHex, eccController.getPublicKey()));

        // 存在则修改
        if (aliasEntity != null) {
            aliasEntity.setAlias(alias);
            this.updateById(aliasEntity);
        } else {
            // 不存在则添加
            aliasEntity = new Alias();
            aliasEntity.setPublicKeyHex(publicKeyHex);
            aliasEntity.setAlias(alias);
            aliasEntity.setOwnerPublicKeyHex(eccController.getPublicKey());
            this.save(aliasEntity);
        }
    }

    /**
     * 获取别名
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @return 别名
     */
    @Override
    public String get(String publicKeyHex) {
        Alias alias = this.getOne(new LambdaQueryWrapper<Alias>()
                .eq(Alias::getPublicKeyHex, publicKeyHex)
                .eq(Alias::getOwnerPublicKeyHex, eccController.getPublicKey()));
        return alias == null ? null : alias.getAlias();
    }

}
