package com.github.wrx886.e2echo.client.gui.layout;

import com.github.wrx886.e2echo.client.gui.common.NodeRouter;

import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;

@Getter
public class SidebarPanelContentLayout extends GridPane {

    // 组件
    private final VBox sidebarVBox = new VBox();
    private final Pane panelPane = new Pane();
    private final Pane contentPane = new Pane();

    // 路由
    private final NodeRouter panelRouter = new NodeRouter(panelPane);
    private final NodeRouter contentRouter = new NodeRouter(contentPane);

    public SidebarPanelContentLayout() {
        super();

        // 设置间距
        setMargin(panelPane, new Insets(5));
        setMargin(sidebarVBox, new Insets(5));
        setMargin(contentPane, new Insets(5));

        // 定义列约束
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(8);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(27);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(65);
        this.getColumnConstraints().addAll(col1, col2, col3);

        // 添加到布局
        this.add(sidebarVBox, 0, 0);
        this.add(panelPane, 1, 0);
        this.add(contentPane, 2, 0);
    }

}
