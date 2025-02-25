package com.github.wrx886.e2echo.client.gui.scene.main.sidebar;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.panel.group.GroupListView;
import com.github.wrx886.e2echo.client.gui.scene.main.panel.session.SessionListView;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.service.UserService;

import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

// 侧边栏
public class Sidebar extends VBox {

    private MessageService messageService = BeanProvider.getBean(MessageService.class);

    private UserService userService = BeanProvider.getBean(UserService.class);

    // 侧边栏路由
    private NodeRouter panelNodeRouter;

    // 会话
    private final Button sessionButton;

    // 群聊
    private final Button groupButton;

    public Sidebar(NodeRouter panelNodeRouter) {

        // 侧边栏路由
        this.panelNodeRouter = panelNodeRouter;

        // 会话按钮
        sessionButton = new Button("会话");
        sessionButton.setOnAction(this::sessionButtonOnAction);
        getChildren().add(sessionButton);

        // 群聊按钮
        groupButton = new Button("群聊");
        groupButton.setOnAction(this::groupButtonOnAction);
        getChildren().add(groupButton);

    }

    // 会话按钮动作
    private void sessionButtonOnAction(Event event) {
        messageService.updateSessionVos();
        panelNodeRouter.push(SessionListView.class);
    }

    // 群聊按钮动作
    private void groupButtonOnAction(Event event) {
        userService.updateGroupList();
        panelNodeRouter.push(GroupListView.class);
    }

}
