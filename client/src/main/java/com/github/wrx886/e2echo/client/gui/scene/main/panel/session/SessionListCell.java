package com.github.wrx886.e2echo.client.gui.scene.main.panel.session;

import java.text.SimpleDateFormat;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.model.vo.SessionVo;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// 会话列表单元格
public class SessionListCell extends ListCell<SessionVo> {

    // 日期格式化工具
    private SimpleDateFormat simpleDateFormat = BeanProvider.getBean(SimpleDateFormat.class);

    @Override
    protected void updateItem(SessionVo item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 横向布局1：会话名称 + 更新时间
            HBox hBox1 = new HBox();

            // 会话名称
            Label sessionNameLabel = new Label(item.getSessionName());
            hBox1.getChildren().add(sessionNameLabel);

            // 更新时间
            if (item.getLastTime() != null) {
                Label lastTimeLabel = new Label(simpleDateFormat.format(item.getLastTime()));
                hBox1.getChildren().add(lastTimeLabel);
            }

            // 纵向布局
            VBox vBox = new VBox();
            vBox.getChildren().add(hBox1);

            // 组合发送者和消息
            Label messageLabel = new Label();
            if (item.getFromName() != null && item.getMessage() != null) {
                String messageString = item.getFromName() + ": " + item.getMessage();
                messageLabel.setText(messageString);
            }
            vBox.getChildren().add(messageLabel);

            // 放入
            setGraphic(vBox);
        }
    }

}
