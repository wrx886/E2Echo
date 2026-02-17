package com.github.wrx886.e2echo.client.gui.content.group;

import java.util.Optional;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupManageController;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupMemberController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.gui.layout.SidebarPanelContentLayout;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GroupContent extends VBox {

    private final GroupManageController groupManageController = BeanProvider.getBean(GroupManageController.class);
    private final GroupMemberController groupMemberController = BeanProvider.getBean(GroupMemberController.class);
    private final GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    // 群聊名称
    private final Label groupNameLabel;

    // 群成员列表
    private final ListView<String> groupMemberListView;

    // 添加群成员按钮
    private final Button addMemberButton;

    // 刷新密钥
    private final Button refreshKeyButton;

    // 重新分发密钥
    private final Button redistributeKeyButton;

    // 构造函数
    public GroupContent(SidebarPanelContentLayout layout) {

        // 设置宽度
        setPrefWidth(500);

        // 设置高度
        setPrefHeight(500);

        // 这是间隙和对其方式
        setAlignment(Pos.TOP_LEFT);
        setSpacing(5);

        // 群聊名称
        groupNameLabel = new Label();
        groupNameLabel.textProperty().bind(guiStore.getCurrentGroupName());
        getChildren().add(groupNameLabel);

        HBox hBox = new HBox();
        getChildren().add(hBox);

        // 添加群成员按钮
        addMemberButton = new Button("添加");
        addMemberButton.setOnAction(this::addMemberButtonOnAction);
        hBox.getChildren().add(addMemberButton);

        // 重新生成并分发密钥
        refreshKeyButton = new Button("刷新密钥");
        refreshKeyButton.setOnAction(this::refreshKeyButtonOnAction);
        hBox.getChildren().add(refreshKeyButton);

        // 重新分发密钥
        redistributeKeyButton = new Button("重新分发密钥");
        redistributeKeyButton.setOnAction(this::redistributeKeyButtonOnAction);
        hBox.getChildren().add(redistributeKeyButton);

        // 放入群成员列表
        groupMemberListView = new ListView<>(guiStore.getCurrentGroupMembers());
        groupMemberListView.setCellFactory(param -> new GroupContentCell());
        VBox.setVgrow(groupMemberListView, Priority.ALWAYS);
        getChildren().add(groupMemberListView);

    }

    private void addMemberButtonOnAction(Event event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加群成员");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入新成员公钥：");
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String publicKeyHex = dialog.getEditor().getText();
                if (publicKeyHex == null || publicKeyHex.isEmpty()) {
                    throw new E2EchoException(E2EchoExceptionCodeEnum.GUI_PUBLIC_KEY_IS_EMPTY);
                }
                groupMemberController.addMember(guiStore.getCurrentGroup().get(), publicKeyHex);
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void refreshKeyButtonOnAction(Event event) {
        // 弹出确认框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("刷新密钥");
        alert.setHeaderText(null);
        alert.setContentText("确定要重新生成并分发密钥？");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 刷新密钥
            groupManageController.reflushKey(guiStore.getCurrentGroup().get());
        }
    }

    private void redistributeKeyButtonOnAction(Event event) {
        groupManageController.redistributeKey(guiStore.getCurrentGroup().get());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("重新分发密钥");
        alert.setHeaderText(null);
        alert.setContentText("密钥已重新分发");
        alert.showAndWait();
    }

}
