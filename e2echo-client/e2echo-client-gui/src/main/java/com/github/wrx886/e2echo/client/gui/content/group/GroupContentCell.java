package com.github.wrx886.e2echo.client.gui.content.group;

import java.util.Optional;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupMemberController;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class GroupContentCell extends ListCell<String> {

    private final GuiStore guiStore = BeanProvider.getBean(GuiStore.class);
    private final GroupMemberController groupMemberController = BeanProvider.getBean(GroupMemberController.class);
    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);

    // 名称
    private final Label memberNameLabel;

    // 公钥
    private final Label memberPublicKeyLabel;

    // 删除
    private final Button memberDeleteButton;

    // 容器
    private final VBox vBox;

    // 构造函数
    public GroupContentCell() {
        // 名称
        memberNameLabel = new Label();

        // 公钥
        memberPublicKeyLabel = new Label();

        // 删除
        memberDeleteButton = new Button("删除");
        memberDeleteButton.setOnAction(this::memberDeleteButtonOnAction);

        // 容器
        vBox = new VBox(memberNameLabel, memberPublicKeyLabel, memberDeleteButton);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
            setText(null);
            setGraphic(null);
        } else {
            memberNameLabel.setText(aliasController.get(item));
            memberPublicKeyLabel.setText(item);
            setGraphic(vBox);
        }
    }

    // 删除用户信息
    private void memberDeleteButtonOnAction(Event event) {
        String member = getItem();
        // 弹出确认框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除成员");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除成员 %s 吗？".formatted(aliasController.get(member)));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 删除成员
            groupMemberController.removeMember(guiStore.getCurrentGroup().get(), member);
        }
    }

}
