package com.github.wrx886.e2echo.client.srv.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKey;

public interface GroupKeyService extends IService<GroupKey> {

    /**
     * 添加/更新群密钥
     * 
     * @param groupUuid 群聊UUID
     * @param timestamp 时间戳
     * @param aesKey    密钥
     * @return 密钥ID
     */
    void put(String groupUuid, Long timestamp, String aesKey);

    /**
     * 获取群密钥
     * 
     * @param groupUuid 群聊UUID
     * @param timestamp 时间戳
     * @return 密钥
     */
    String get(String groupUuid, Long timestamp);

    /**
     * 获取群密钥
     * 
     * @param id
     * @return
     */
    GroupKey getById(Long id);

}
