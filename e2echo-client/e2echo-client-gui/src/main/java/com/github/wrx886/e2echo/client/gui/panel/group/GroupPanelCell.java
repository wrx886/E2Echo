package com.github.wrx886.e2echo.client.gui.panel.group;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.VBox;

public class GroupPanelCell extends ListCell<String> {

    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);

    // 群聊名称标签
    private final Label nameLabel;

    // 群聊 UUID
    private final Label groupUuidLabel;

    // 垂直结构
    private final VBox vBox;

    public GroupPanelCell() {
        super();

        // 群聊名称标签
        nameLabel = new Label();
        nameLabel.setMaxWidth(250);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        // 群聊 UUID
        groupUuidLabel = new Label();

        // 放入
        vBox = new VBox(nameLabel, groupUuidLabel);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 群聊名称
            nameLabel.setText(aliasController.get(item));

            // 群聊 UUID
            groupUuidLabel.setText(item);

            // 放入
            setGraphic(vBox);
        }

    }

}
