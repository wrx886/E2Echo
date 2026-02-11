package com.github.wrx886.e2echo.client.gui.panel.session;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.gui.content.chat.ChatContent;
import com.github.wrx886.e2echo.client.gui.content.edit.EditContent;
import com.github.wrx886.e2echo.client.gui.layout.SidebarPanelContentLayout;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SessionPanel extends VBox {

    private final GuiController guiController = BeanProvider.getBean(GuiController.class);
    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);
    private final GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    // 标题标签
    private final Label titleLabel;

    // 添加会话按钮
    private final Button addSessionButton;

    // 刷新按钮
    private final Button reflushButton;

    // 会话列表
    private final ListView<Session> sessionListView;

    // 主布局
    private final SidebarPanelContentLayout layout;

    // 构造函数
    public SessionPanel(SidebarPanelContentLayout layout) {
        // 设置主界面路由
        this.layout = layout;

        // 设置宽度
        setPrefWidth(210);

        // 设置高度
        setPrefHeight(500);

        // 设置元素间距
        setSpacing(5);

        // 设置对齐方案
        setAlignment(Pos.BOTTOM_CENTER);

        // 获取根节点
        ObservableList<Node> root = getChildren();

        // 标题标签
        titleLabel = new Label("会话");
        root.add(titleLabel);

        // 按钮布局
        HBox buttonHBox = new HBox();
        buttonHBox.setSpacing(5);
        buttonHBox.setAlignment(Pos.CENTER_LEFT);
        root.add(buttonHBox);

        // 添加会话按钮
        addSessionButton = new Button("添加");
        addSessionButton.setOnAction(this::addSessionButtonOnAction);
        buttonHBox.getChildren().add(addSessionButton);

        // 刷新按钮
        reflushButton = new Button("刷新");
        reflushButton.setOnAction(this::reflushButtonOnAction);
        buttonHBox.getChildren().add(reflushButton);

        // 会话列表
        sessionListView = new ListView<>(guiStore.getSessionList());
        sessionListView.setCellFactory(param -> new SessionPanelCell());
        sessionListView.setOnMouseClicked(this::sessionListViewOnMouseClicked);
        VBox.setVgrow(sessionListView, Priority.ALWAYS);
        root.add(sessionListView);

    }

    // 刷新按钮事件
    private void reflushButtonOnAction(Event event) {
        guiController.flushAsync();
    }

    // 当选择会话事件
    private void sessionListViewOnMouseClicked(Event event) {
        // 获取当前选择的会话
        Session selectedItem = sessionListView.getSelectionModel().getSelectedItem();
        // 如果不为空，就需要更新聊天窗口的内容
        if (selectedItem != null) {
            guiStore.getCurrentSession().set(selectedItem);
            // 更新消息列表
            guiController.flushAsync();
            layout.getContentRouter().push(ChatContent.class);
        }
    }

    // 添加会话事件
    private void addSessionButtonOnAction(Event event) {
        guiStore.getEditVo().set(null);
        layout.getContentRouter().push(EditContent.class);
    }

}
