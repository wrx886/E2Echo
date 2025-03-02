package com.github.wrx886.e2echo.client.gui.scene.main;

import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.common.SceneRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.content.chat.ChatView;
import com.github.wrx886.e2echo.client.gui.scene.main.content.edit.UserEditView;
import com.github.wrx886.e2echo.client.gui.scene.main.content.group.GroupMemberView;
import com.github.wrx886.e2echo.client.gui.scene.main.panel.group.GroupListView;
import com.github.wrx886.e2echo.client.gui.scene.main.panel.session.SessionListView;
import com.github.wrx886.e2echo.client.gui.scene.main.sidebar.Sidebar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class MainScene extends Scene {

    // GridPane 内元素的外边距
    private static final Insets GRID_MARGIN = new Insets(5);

    // 场景路由
    // private final SceneRouter sceneRouter;

    public MainScene(SceneRouter sceneRouter) {
        super(new GridPane());

        // 场景路由
        // this.sceneRouter = sceneRouter;

        // 获取根节点
        GridPane root = (GridPane) this.getRoot();
        root.setAlignment(Pos.TOP_LEFT);

        // 主界面
        Pane content = new Pane();
        GridPane.setMargin(content, GRID_MARGIN);
        root.add(content, 2, 0);

        // 主界面路由
        NodeRouter contenNodeRouter = new NodeRouter(content);
        contenNodeRouter.register(new ChatView(contenNodeRouter));
        contenNodeRouter.register(new UserEditView(contenNodeRouter));
        contenNodeRouter.register(new GroupMemberView(contenNodeRouter));

        // 面板栏
        Pane panel = new Pane();
        GridPane.setMargin(panel, GRID_MARGIN);
        root.add(panel, 1, 0);

        // 面板栏路由
        NodeRouter panelNodeRouter = new NodeRouter(panel);
        panelNodeRouter.register(new SessionListView(contenNodeRouter));
        panelNodeRouter.register(new GroupListView(contenNodeRouter));
        panelNodeRouter.push(SessionListView.class);

        // 侧边栏
        Sidebar sidebar = new Sidebar(panelNodeRouter);
        GridPane.setMargin(sidebar, GRID_MARGIN);
        root.add(sidebar, 0, 0);
    }

}
