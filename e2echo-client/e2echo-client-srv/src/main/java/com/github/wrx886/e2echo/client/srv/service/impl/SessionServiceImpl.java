package com.github.wrx886.e2echo.client.srv.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.srv.mapper.SessionMapper;
import com.github.wrx886.e2echo.client.srv.service.SessionService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    private final EccController eccController;

    /**
     * 更新会话
     * 
     * @param publicKeyHex 公钥或群聊UUID
     * @param message      消息
     * @param group        是否是群聊
     */
    @Override
    public void updateSession(String publicKeyHex, Message message, boolean group) {
        // 获取会话
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Session::getPublicKeyHex, publicKeyHex)
                .eq(Session::getGroup, group));

        // 存在会话
        if (session != null) {
            // 更新会话
            session.setMessageId(message.getId());
            session.setTimestamp(message.getTimestamp());
            this.updateById(session);
        } else {
            // 创建会话
            session = new Session();
            session.setOwnerPublicKeyHex(eccController.getPublicKey());
            session.setPublicKeyHex(publicKeyHex);
            session.setMessageId(message.getId());
            session.setTimestamp(message.getTimestamp());
            session.setGroup(group);
            this.save(session);
        }
    }

    /**
     * 创建会话
     * 
     * @param publicKeyHex 对方公钥或群聊UUID
     * @param group        是否是群聊
     */
    @Override
    public void create(String publicKeyHex, boolean group) {
        // 获取会话
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Session::getPublicKeyHex, publicKeyHex)
                .eq(Session::getGroup, group));

        if (session != null) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_SESSION_EXISTS);
        }

        // 创建会话
        session = new Session();
        session.setOwnerPublicKeyHex(eccController.getPublicKey());
        session.setPublicKeyHex(publicKeyHex);
        session.setTimestamp(System.currentTimeMillis());
        session.setGroup(group);
        this.save(session);
    }

}
