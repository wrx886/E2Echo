package com.github.wrx886.e2echo.client.srv.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.entity.Session;

public interface SessionService extends IService<Session> {

    /**
     * 更新会话
     * 
     * @param publicKeyHex 公钥或群聊UUID
     * @param message      消息
     * @param group        是否是群聊
     */
    void updateSession(String publicKeyHex, Message message, boolean group);

    /**
     * 创建会话
     * 
     * @param publicKeyHex 对方公钥或群聊UUID
     * @param group        是否是群聊
     */
    void create(String publicKeyHex, boolean group);

    /**
     * 更新群密钥
     * 
     * @param groupUuid  群聊UUID
     * @param groupKeyId 群密钥ID
     */
    void putGroupKey(String groupUuid, Long groupKeyId);

    /**
     * 会话是否存在
     * 
     * @param publicKeyHex
     * @return true：存在，false：不存在
     */
    boolean contain(String publicKeyHex);

    /**
     * 获取会话列表
     * 
     * @return 会话列表（根据时间戳降序排列）
     */
    List<Session> listSession();

    /**
     * 修改群聊启用状态
     * 
     * @param groupUuid 群聊 UUID
     * @param enabled   启用状态
     */
    void setGroupEnabled(String groupUuid, boolean enabled);

    /**
     * 获取会话
     * 
     * @param publicKeyHex 公钥或群聊 UUID
     * @return 会话
     */
    Session getSession(String publicKeyHex);

}
