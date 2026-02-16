package com.github.wrx886.e2echo.client.srv.controller.impl;

import java.util.List;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.srv.SessionController;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.srv.service.SessionService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class SessionControllerImpl implements SessionController {

    private final SessionService sessionService;

    /**
     * 获取会话列表
     * 
     * @return 会话列表（根据时间戳降序排列）
     */
    @Override
    public List<Session> list() {
        return sessionService.listSession();
    }

    /**
     * 创建会话
     * 
     * @param publicKeyHex 对方公钥或群聊UUID
     * @param group        是否是群聊
     */
    @Override
    public void create(String publicKeyHex, boolean group) {
        sessionService.create(publicKeyHex, group);
    }

    @Override
    public boolean contain(String publicKeyHex) {
        return sessionService.contain(publicKeyHex);
    }

    /**
     * 修改群聊启用状态
     * 
     * @param groupUuid 群聊 UUID
     * @param enabled   启用状态
     */
    @Override
    public void setGroupEnabled(String groupUuid, boolean enabled) {
        sessionService.setGroupEnabled(groupUuid, enabled);
    }

}
