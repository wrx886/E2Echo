package com.github.wrx886.e2echo.client.gui.panel.group;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupManageController;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupMemberController;
import com.github.wrx886.e2echo.client.gui.content.group.GroupContent;
import com.github.wrx886.e2echo.client.gui.layout.SidebarPanelContentLayout;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GroupPanel extends VBox {

    private final GuiController guiController = BeanProvider.getBean(GuiController.class);
    private final GroupManageController groupManageController = BeanProvider.getBean(GroupManageController.class);
    private final GroupMemberController groupMemberController = BeanProvider.getBean(GroupMemberController.class);
    private final GuiStore guiStore = BeanProvider.getBean(GuiStore.class);
    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);

    // 标题标签
    private final Label titleLabel;

    // 群聊列表
    private final ListView<String> groupList;

    // 创建群聊
    private final Button createGroupButton;

    // 主布局
    private final SidebarPanelContentLayout layout;

    // 构造函数
    public GroupPanel(SidebarPanelContentLayout layout) {

        // 主界面路由
        this.layout = layout;

        // 设置大小
        setPrefWidth(210);
        setPrefHeight(500);

        // 设置间距
        setSpacing(5);

        // 设置对齐
        setAlignment(Pos.TOP_LEFT);

        // 获取根节点
        ObservableList<Node> root = getChildren();

        // 放入标签
        titleLabel = new Label("群聊");
        root.addAll(titleLabel);

        // 创建群聊按钮
        createGroupButton = new Button("创建群聊");
        createGroupButton.setOnAction((event) -> {
            groupManageController.create();
        });
        root.add(createGroupButton);

        // 放入列表
        groupList = new ListView<>(guiStore.getGroups());
        groupList.setOnMouseClicked(this::groupListOnMouseClicked);
        groupList.setCellFactory(param -> new GroupPanelCell());
        VBox.setVgrow(groupList, Priority.ALWAYS);
        root.add(groupList);
    }

    // 群聊选择
    private void groupListOnMouseClicked(Event event) {
        // 获取当前选择的群聊
        String groupUuid = groupList.getSelectionModel().getSelectedItem();
        if (groupUuid != null) {
            guiStore.getCurrentGroup().set(groupUuid);
            guiStore.getCurrentGroupName().set(aliasController.get(groupUuid));
            guiController.flushAsync();
            layout.getContentRouter().push(GroupContent.class);
        }
    }

}
