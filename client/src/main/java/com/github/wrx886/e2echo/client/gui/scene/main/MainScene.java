package com.github.wrx886.e2echo.client.gui.scene.main;

import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.common.SceneRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.content.chat.ChatView;
import com.github.wrx886.e2echo.client.gui.scene.main.content.edit.UserEditView;
import com.github.wrx886.e2echo.client.gui.scene.main.content.group.GroupMemberView;
import com.github.wrx886.e2echo.client.gui.scene.main.panel.group.GroupListView;
import com.github.wrx886.e2echo.client.gui.scene.main.panel.session.SessionListView;
import com.github.wrx886.e2echo.client.gui.scene.main.sidebar.Sidebar;

import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class MainScene extends Scene {

    // 场景路由
    // private final SceneRouter sceneRouter;

    public MainScene(SceneRouter sceneRouter) {
        super(new HBox());

        // 场景路由
        // this.sceneRouter = sceneRouter;

        // 获取根节点
        HBox root = (HBox) this.getRoot();

        // 主界面
        Pane content = new Pane();

        // 主界面路由
        NodeRouter contenNodeRouter = new NodeRouter(content);
        contenNodeRouter.register(new ChatView(contenNodeRouter));
        contenNodeRouter.register(new UserEditView(contenNodeRouter));
        contenNodeRouter.register(new GroupMemberView(contenNodeRouter));

        // 面板栏
        Pane panel = new Pane();

        // 面板栏路由
        NodeRouter panelNodeRouter = new NodeRouter(panel);
        panelNodeRouter.register(new SessionListView(contenNodeRouter));
        panelNodeRouter.register(new GroupListView(contenNodeRouter));

        // 侧边栏
        Sidebar sidebar = new Sidebar(panelNodeRouter);

        root.getChildren().addAll(sidebar, panel, content);

    }

}
