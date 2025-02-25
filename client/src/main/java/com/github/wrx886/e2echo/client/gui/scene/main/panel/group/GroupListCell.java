package com.github.wrx886.e2echo.client.gui.scene.main.panel.group;

import com.github.wrx886.e2echo.client.model.entity.User;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class GroupListCell extends ListCell<User> {

    // 群聊名称标签
    private final Label nameLabel;

    // 群聊 UUID
    private final Label groupUuidLabel;

    // 垂直结构
    private final VBox vBox;

    public GroupListCell() {
        super();

        // 群聊名称标签
        nameLabel = new Label();

        // 群聊 UUID
        groupUuidLabel = new Label();

        // 放入
        vBox = new VBox(nameLabel, groupUuidLabel);
    }

    @Override
    protected void updateItem(User item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 群聊名称
            nameLabel.setText(item.getName());

            // 群聊 UUID
            groupUuidLabel.setText(item.getGroupUuid());

            // 放入
            setGraphic(vBox);
        }

    }

}
