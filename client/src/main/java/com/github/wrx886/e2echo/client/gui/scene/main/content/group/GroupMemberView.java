package com.github.wrx886.e2echo.client.gui.scene.main.content.group;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.service.GroupUserService;
import com.github.wrx886.e2echo.client.store.GuiStore;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// 群成员管理视图
public class GroupMemberView extends VBox {

    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    private GroupUserService groupUserService = BeanProvider.getBean(GroupUserService.class);

    // 群聊名称
    private final Label groupNameLabel;

    // 群成员列表
    private final ListView<User> groupMemberListView;

    // 新群成员标签
    private final Label addMemberPublicKeyLabel;

    // 新群成员公钥
    private final TextField addMemberPublicKeyTextField;

    // 添加群成员按钮
    private final Button addMemberButton;

    // 构造函数
    public GroupMemberView(NodeRouter contenNodeRouter) {

        // 设置宽度
        setPrefWidth(520);

        // 设置高度
        setPrefHeight(690);

        // 这是间隙和对其方式
        setAlignment(Pos.TOP_LEFT);
        setSpacing(5);

        // 群聊名称
        groupNameLabel = new Label();
        groupNameLabel.textProperty().bind(guiStore.getCurrentGroupName());
        getChildren().add(groupNameLabel);

        // 放入群成员列表
        groupMemberListView = new ListView<>(guiStore.getCurrentGroupMembers());
        groupMemberListView.setCellFactory(param -> new GroupMemberCell());
        VBox.setVgrow(groupMemberListView, Priority.ALWAYS);
        getChildren().add(groupMemberListView);

        // 添加新成员标签
        addMemberPublicKeyLabel = new Label("新成员公钥");
        getChildren().add(addMemberPublicKeyLabel);

        // 添加新成员公钥
        addMemberPublicKeyTextField = new TextField();
        getChildren().add(addMemberPublicKeyTextField);

        // 添加群成员按钮
        addMemberButton = new Button("添加");
        addMemberButton.setOnAction(this::addMemberButtonOnAction);
        getChildren().add(addMemberButton);

    }

    // 添加新成员
    private void addMemberButtonOnAction(Event event) {
        if (addMemberPublicKeyTextField.getText() != null &&
                !addMemberPublicKeyTextField.getText().isBlank()) {
            groupUserService.putToMember(guiStore.getCurrentGroup().get().getId(),
                    addMemberPublicKeyTextField.getText());
            addMemberPublicKeyTextField.clear();
            groupUserService.updateGroupMenbers(guiStore.getCurrentGroup().get().getId());
        } else {
            throw new E2Echoxception(null, "群成员不能为空");
        }
    }

}
