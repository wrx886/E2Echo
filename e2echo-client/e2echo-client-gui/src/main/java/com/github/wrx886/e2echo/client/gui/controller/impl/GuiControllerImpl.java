package com.github.wrx886.e2echo.client.gui.controller.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
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

                // 刷新所有会话
                // 获取消息
                List<Session> sessions = sessionController.list();
                // 放入列表
                guiStore.getSessionList().clear();
                guiStore.getSessionList().setAll(sessions);

                // 刷新当前会话
                if (guiStore.getCurrentSession().get() != null) {
                    String session = guiStore.getCurrentSession().get().getPublicKeyHex();
                    guiStore.getCurrentMessage().clear();
                    guiStore.getCurrentMessage().addAll(messageController.listOneBySession(session));
                    Collections.reverse(guiStore.getCurrentMessage());
                    guiStore.getCurrentSessionName().set(
                            aliasController.get(session));
                }

            });
        } catch (Throwable t) {
            log.error(null, t);
        }
    }

}
