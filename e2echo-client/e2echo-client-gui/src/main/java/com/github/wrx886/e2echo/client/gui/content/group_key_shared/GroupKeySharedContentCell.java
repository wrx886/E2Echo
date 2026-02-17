package com.github.wrx886.e2echo.client.gui.content.group_key_shared;

import java.util.Optional;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.GroupKeySharedController;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKeyShared;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class GroupKeySharedContentCell extends ListCell<GroupKeyShared> {

    private final GroupKeySharedController groupKeySharedController = BeanProvider
            .getBean(GroupKeySharedController.class);

    // 显示内容
    private final Label groupUuidLabel;
    private final Label fromLabel;
    private final Label toLabel;

    // 删除按钮
    private final Button removeButton;

    // 容器
    private final VBox vBox;

    public GroupKeySharedContentCell() {
        super();

        // 显示内容
        groupUuidLabel = new Label();
        fromLabel = new Label();
        toLabel = new Label();

        // 删除按钮
        removeButton = new Button("删除");
        removeButton.setOnAction(this::removeButtonOnAction);

        // 容器
        vBox = new VBox(groupUuidLabel, fromLabel, toLabel, removeButton);
    }

    @Override
    protected void updateItem(GroupKeyShared item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
            setText(null);
            setGraphic(null);
        } else {
            groupUuidLabel.setText("群聊UUID：" + item.getGroupUuid());
            fromLabel.setText("发送者：" + item.getFrom());
            toLabel.setText("接收者：" + item.getTo());
            setGraphic(vBox);
        }
    }

    private void removeButtonOnAction(Event event) {
        GroupKeyShared groupKeyShared = getItem();
        // 弹出确认框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除成员");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除规则 %s: %s -> %s 吗？".formatted(
                groupKeyShared.getGroupUuid(),
                groupKeyShared.getFrom(),
                groupKeyShared.getTo()));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 删除谷子额
            groupKeySharedController.remove(
                    groupKeyShared.getGroupUuid(),
                    groupKeyShared.getFrom(),
                    groupKeyShared.getTo());
        }
    }

}
