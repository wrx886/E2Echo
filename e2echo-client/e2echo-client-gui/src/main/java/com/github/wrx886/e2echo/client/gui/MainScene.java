package com.github.wrx886.e2echo.client.gui;

import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.content.chat.ChatContent;
import com.github.wrx886.e2echo.client.gui.content.edit.EditContent;
import com.github.wrx886.e2echo.client.gui.layout.SidebarPanelContentLayout;
import com.github.wrx886.e2echo.client.gui.panel.session.SessionPanel;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class MainScene extends Scene {

    public MainScene() {
        super(new SidebarPanelContentLayout());

        // 获取根布局
        SidebarPanelContentLayout layout = (SidebarPanelContentLayout) getRoot();

        // 会话按钮
        Button sessionButton = new Button("会话");

        // 左侧
        VBox sidebarVBox = layout.getSidebarVBox();
        sidebarVBox.getChildren().add(sessionButton);

        // 中间
        NodeRouter panelRouter = layout.getPanelRouter();
        panelRouter.register(new SessionPanel(layout));
        panelRouter.push(SessionPanel.class);

        // 右侧
        NodeRouter contentRouter = layout.getContentRouter();
        contentRouter.register(new ChatContent(layout));
        contentRouter.register(new EditContent(layout));
        contentRouter.push(ChatContent.class);

    }

}
