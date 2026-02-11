package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.model.entity.Alias;
import com.github.wrx886.e2echo.client.srv.mapper.AliasMapper;
import com.github.wrx886.e2echo.client.srv.service.AliasService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AliasServiceImpl extends ServiceImpl<AliasMapper, Alias> implements AliasService {

    private final EccController eccController;
    private final GuiController guiController;
    private final ConcurrentHashMap<String, String> aliasMap = new ConcurrentHashMap<>();

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

        // 放入缓存
        aliasMap.put(publicKeyHex, alias);

        // 刷新主界面
        guiController.flushAsync();
    }

    /**
     * 获取别名
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @return 别名
     */
    @Override
    public String get(String publicKeyHex) {
        if (!aliasMap.containsKey(publicKeyHex)) {
            // 查询数据库
            Alias alias = this.getOne(new LambdaQueryWrapper<Alias>()
                    .eq(Alias::getPublicKeyHex, publicKeyHex)
                    .eq(Alias::getOwnerPublicKeyHex, eccController.getPublicKey()));
            // 放入缓存
            if (alias != null) {
                aliasMap.put(publicKeyHex, alias.getAlias());
            } else {
                // 不存在，取最后4位作为默认名称
                String aliasString = publicKeyHex.substring(publicKeyHex.length() - 5, publicKeyHex.length() - 1);
                this.put(publicKeyHex, aliasString);
                aliasMap.put(publicKeyHex, aliasString);
            }
        }
        return aliasMap.containsKey(publicKeyHex) ? aliasMap.get(publicKeyHex) : null;
    }

}
