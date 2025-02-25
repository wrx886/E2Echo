package com.github.wrx886.e2echo.client.gui.scene.main.content.chat;

import java.text.SimpleDateFormat;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.model.vo.MessageVo;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ChatMessageListCell extends ListCell<MessageVo> {

    // 日期格式化工具
    private SimpleDateFormat simpleDateFormat = BeanProvider.getBean(SimpleDateFormat.class);

    @Override
    protected void updateItem(MessageVo item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 发送者
            Label fromNameLabel = new Label(item.getFromName());

            // 发送时间
            Label sendTimeLabel = new Label(simpleDateFormat.format(item.getSendTime()));

            // 结合
            HBox hBox = new HBox(fromNameLabel, sendTimeLabel);

            // 垂直
            VBox vBox = new VBox(hBox, new Text(item.getData()));

            setGraphic(vBox);
        }
    }

}
