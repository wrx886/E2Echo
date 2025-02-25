package com.github.wrx886.e2echo.client.gui.scene.main.panel.group;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.content.group.GroupMemberView;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.service.GroupUserService;
import com.github.wrx886.e2echo.client.store.GuiStore;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

// 群聊列表
public class GroupListView extends VBox {

    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    private GroupUserService groupUserService = BeanProvider.getBean(GroupUserService.class);

    // 主界面路由
    private final NodeRouter contentRouter;

    // 群聊列表
    private final ListView<User> groupList;

    // 构造函数
    public GroupListView(NodeRouter contentRouter) {

        // 主界面路由
        this.contentRouter = contentRouter;

        // 获取根节点
        ObservableList<Node> root = getChildren();

        // 放入列表
        groupList = new ListView<>(guiStore.getGroups());
        groupList.setOnMouseClicked(this::groupListOnMouseClicked);
        groupList.setCellFactory(param -> new GroupListCell());
        root.add(groupList);
    }

    // 群聊选择
    private void groupListOnMouseClicked(Event event) {
        // 获取当前选择的群聊
        User group = groupList.getSelectionModel().getSelectedItem();
        if (group != null) {
            guiStore.getCurrentGroup().set(group);
            guiStore.getCurrentGroupName().set(group.getName());
            groupUserService.updateGroupMenbers(group.getId());
            contentRouter.push(GroupMemberView.class);
        }
    }

}
