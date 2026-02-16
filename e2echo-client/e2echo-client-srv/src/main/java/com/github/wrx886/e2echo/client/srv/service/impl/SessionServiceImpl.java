package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
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
    private final GuiController guiController;

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
                .eq(Session::getPublicKeyHex, publicKeyHex));

        // 存在会话
        if (session != null) {
            // 错误
            if (group != session.getGroup()) {
                throw new RuntimeException();
            }

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
            session.setGroupKeyId(-1L);
            session.setGroupEnabled(true);
            this.save(session);
        }

        // 刷新
        guiController.flushAsync();
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
                .eq(Session::getPublicKeyHex, publicKeyHex));

        if (session != null) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_SESSION_EXISTS);
        }

        // 创建会话
        session = new Session();
        session.setOwnerPublicKeyHex(eccController.getPublicKey());
        session.setPublicKeyHex(publicKeyHex);
        session.setTimestamp(System.currentTimeMillis());
        session.setGroup(group);
        session.setGroupKeyId(-1L);
        session.setGroupEnabled(true);
        this.save(session);

        // 刷新
        guiController.flushAsync();
    }

    /**
     * 更新群密钥
     * 
     * @param groupUuid  群聊UUID
     * @param groupKeyId 群密钥ID
     */
    @Override
    public void putGroupKey(String groupUuid, Long groupKeyId) {
        // 获取会话
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Session::getPublicKeyHex, groupUuid)
                .eq(Session::getGroup, true));

        // 更新
        if (session == null) {
            session = new Session();
            session.setOwnerPublicKeyHex(eccController.getPublicKey());
            session.setPublicKeyHex(groupUuid);
            session.setTimestamp(System.currentTimeMillis());
            session.setGroup(true);
            session.setGroupKeyId(groupKeyId);
            this.save(session);
        } else {
            // 错误
            if (!session.getGroup()) {
                throw new RuntimeException();
            }
            session.setGroupKeyId(groupKeyId);
            this.updateById(session);
        }
    }

    /**
     * 会话是否存在
     * 
     * @param publicKeyHex
     * @return true：存在，false：不存在
     */
    @Override
    public boolean contain(String publicKeyHex) {
        return count(new LambdaQueryWrapper<Session>()
                .eq(Session::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Session::getPublicKeyHex, publicKeyHex)) > 0;
    }

    /**
     * 获取会话列表
     * 
     * @return 会话列表（根据时间戳降序排列）
     */
    @Override
    public List<Session> listSession() {
        return list(new LambdaQueryWrapper<Session>()
                .eq(Session::getOwnerPublicKeyHex, eccController.getPublicKey())
                .orderByDesc(Session::getTimestamp));
    }

    /**
     * 修改群聊启用状态
     * 
     * @param groupUuid 群聊 UUID
     * @param enabled   启用状态
     */
    @Override
    public void setGroupEnabled(String groupUuid, boolean enabled) {
        // 获取会话
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Session::getPublicKeyHex, groupUuid));
        if (session == null) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_SESSION_NOT_EXIST);
        }

        // 修改
        session.setGroupEnabled(enabled);
        this.updateById(session);
    }

}
