package com.github.wrx886.e2echo.client.gui.controller.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupManageController;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupMemberController;
import com.github.wrx886.e2echo.client.common.controller.srv.MessageController;
import com.github.wrx886.e2echo.client.common.controller.srv.SessionController;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

import javafx.application.Platform;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@AllArgsConstructor
public class GuiControllerImpl implements GuiController {

    private final GuiStore guiStore;

    /**
     * 刷新
     */
    @Override
    public void flushAsync() {
        try {
            Platform.runLater(() -> {
                // 执行刷新操作
                MessageController messageController = BeanProvider.getBean(MessageController.class);
                SessionController sessionController = BeanProvider.getBean(SessionController.class);
                AliasController aliasController = BeanProvider.getBean(AliasController.class);
                GroupManageController groupManageController = BeanProvider.getBean(GroupManageController.class);
                GroupMemberController groupMemberController = BeanProvider.getBean(GroupMemberController.class);

                // 刷新所有会话
                // 获取消息
                List<Session> sessions = sessionController.list();
                // 放入列表
                guiStore.getSessionList().clear();
                guiStore.getSessionList().setAll(sessions);

                // 刷新当前会话
                if (guiStore.getCurrentSession().get() != null) {
                    Session session = guiStore.getCurrentSession().get();
                    guiStore.getCurrentMessage().clear();
                    if (session.getGroup()) {
                        guiStore.getCurrentMessage()
                                .setAll(messageController.listGroupBySession(session.getPublicKeyHex()));
                    } else {
                        guiStore.getCurrentMessage()
                                .addAll(messageController.listOneBySession(session.getPublicKeyHex()));
                    }
                    Collections.reverse(guiStore.getCurrentMessage());
                    guiStore.getCurrentSessionName().set(
                            aliasController.get(session.getPublicKeyHex()));
                }

                // 刷新群聊列表
                guiStore.getGroups().clear();
                guiStore.getGroups().addAll(groupManageController.listGroup());

                // 刷新群聊成员
                if (guiStore.getCurrentGroup().get() != null) {
                    guiStore.getCurrentGroupMembers().clear();
                    guiStore.getCurrentGroupMembers()
                            .addAll(groupMemberController.listMember(guiStore.getCurrentGroup().get()));
                }

            });
        } catch (Throwable t) {
            log.error(null, t);
        }
    }

}
