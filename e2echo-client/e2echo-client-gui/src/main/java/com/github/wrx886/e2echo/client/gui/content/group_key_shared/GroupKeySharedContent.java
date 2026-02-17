package com.github.wrx886.e2echo.client.gui.content.group_key_shared;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupKeySharedController;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKeyShared;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GroupKeySharedContent extends VBox {

    private final GuiStore guiStore = BeanProvider.getBean(GuiStore.class);
    private final GroupKeySharedController groupKeySharedController = BeanProvider
            .getBean(GroupKeySharedController.class);

    // 页面名称
    private final Label pageLabel;

    // 重新执行所有规则
    private final Button resendAllButton;

    // 添加规则
    private final Button addButton;

    // 群密钥共享列表
    private final ListView<GroupKeyShared> groupKeySharedListView;

    public GroupKeySharedContent() {
        super();

        // 设置宽度
        setPrefWidth(500);

        // 设置高度
        setPrefHeight(500);

        // 这是间隙和对其方式
        setAlignment(Pos.TOP_LEFT);
        setSpacing(5);

        // 页面名称
        this.pageLabel = new Label("群密钥共享规则");
        getChildren().add(pageLabel);

        // 按钮区
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(5);
        buttonBox.setAlignment(Pos.TOP_LEFT);
        getChildren().add(buttonBox);

        // 添加按钮
        addButton = new Button("添加");
        addButton.setOnAction(this::addButtonOnAction);
        buttonBox.getChildren().add(addButton);

        // 重新执行所有规则
        this.resendAllButton = new Button("重新执行所有规则");
        resendAllButton.setOnAction(this::resendAllButtonOnAction);
        buttonBox.getChildren().add(resendAllButton);

        // 群密钥共享列表
        this.groupKeySharedListView = new ListView<>(guiStore.getGroupKeyShareds());
        this.groupKeySharedListView.setCellFactory(param -> new GroupKeySharedContentCell());
        VBox.setVgrow(groupKeySharedListView, Priority.ALWAYS);
        getChildren().add(groupKeySharedListView);
    }

    private void resendAllButtonOnAction(Event event) {
        groupKeySharedController.resendAll();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("重新共享所有密钥");
        alert.setHeaderText(null);
        alert.setContentText("密钥已重新分发");
        alert.showAndWait();
    }

    private void addButtonOnAction(Event event) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setTitle("添加规则");

        // 布局
        GridPane gridPane = new GridPane();
        dialog.getDialogPane().setContent(gridPane);
        gridPane.setPadding(new Insets(20));
        gridPane.setHgap(5);
        gridPane.setVgap(5);

        // 群聊UUID
        Label groupUuidLabel = new Label("群聊UUID");
        gridPane.add(groupUuidLabel, 0, 0);

        // 群聊UUID输入框
        TextField groupUuidTextField = new TextField();
        gridPane.add(groupUuidTextField, 1, 0);

        // from 标签
        Label fromLabel = new Label("from");
        gridPane.add(fromLabel, 0, 1);

        // from 输入框
        TextField fromTextField = new TextField();
        gridPane.add(fromTextField, 1, 1);

        // to 标签
        Label toLabel = new Label("to");
        gridPane.add(toLabel, 0, 2);

        // to 输入框
        TextField toTextField = new TextField();
        gridPane.add(toTextField, 1, 2);

        // 数据处理
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                groupKeySharedController.add(
                        groupUuidTextField.getText(),
                        fromTextField.getText(),
                        toTextField.getText());
            }
            return null;
        });

        dialog.showAndWait();
    }

}
