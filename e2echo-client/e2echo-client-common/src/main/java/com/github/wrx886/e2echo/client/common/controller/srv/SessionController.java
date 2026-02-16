package com.github.wrx886.e2echo.client.common.controller.srv;

import java.util.List;

import com.github.wrx886.e2echo.client.common.model.entity.Session;

// 会话 api
public interface SessionController {

    /**
     * 获取会话列表
     * 
     * @return 会话列表（根据时间戳降序排列）
     */
    List<Session> list();

    /**
     * 创建会话
     * 
     * @param publicKeyHex 对方公钥或群聊UUID
     * @param group        是否是群聊
     */
    void create(String publicKeyHex, boolean group);

    /**
     * 判断是否包含会话
     * 
     * @param publicKeyHex
     * @return 是否包含会话
     */
    boolean contain(String publicKeyHex);

    /**
     * 修改群聊启用状态
     * 
     * @param groupUuid 群聊 UUID
     * @param enabled   启用状态
     */
    void setGroupEnabled(String groupUuid, boolean enabled);

}
