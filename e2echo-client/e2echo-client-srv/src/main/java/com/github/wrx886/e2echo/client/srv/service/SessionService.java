package com.github.wrx886.e2echo.client.srv.service;

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

}
