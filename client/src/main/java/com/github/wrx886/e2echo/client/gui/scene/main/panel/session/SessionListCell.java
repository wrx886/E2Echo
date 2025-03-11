package com.github.wrx886.e2echo.client.gui.scene.main.panel.session;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.model.entity.File;
import com.github.wrx886.e2echo.client.model.enums.MessageType;
import com.github.wrx886.e2echo.client.model.vo.SessionVo;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// 会话列表单元格
public class SessionListCell extends ListCell<SessionVo> {

    // 日期格式化工具
    private SimpleDateFormat simpleDateFormat = BeanProvider.getBean(SimpleDateFormat.class);

    // json
    private ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);

    // 会话名称
    private final Label sessionNameLabel;

    // 更新时间
    private final Label lastTimeLabel;

    // 消息
    private final Label fromAndmessageLabel;

    // 总容器
    private final VBox vBox;

    // 构造函数
    public SessionListCell() {

        // 会话名称
        sessionNameLabel = new Label();
        sessionNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        sessionNameLabel.setMaxWidth(130);

        // 更新时间
        lastTimeLabel = new Label();

        // 发送者和消息
        fromAndmessageLabel = new Label();
        fromAndmessageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        fromAndmessageLabel.setMaxWidth(250);

        // 空白填充
        Region hBox1Region = new Region();
        HBox.setHgrow(hBox1Region, Priority.ALWAYS);

        // 容器1：存放会话名称和更新时间
        HBox hBox = new HBox(sessionNameLabel, hBox1Region, lastTimeLabel);

        // 容器2：存放 容器1 和 容器2
        vBox = new VBox(hBox, fromAndmessageLabel);
    }

    @Override
    protected void updateItem(SessionVo item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 会话名称
            sessionNameLabel.setText(item.getSessionName());

            // 更新时间
            if (item.getLastTime() != null) {
                lastTimeLabel.setText(simpleDateFormat.format(item.getLastTime()));
            } else {
                lastTimeLabel.setText(null);
            }

            // 发送者和消息
            if (item.getFromName() != null && item.getMessage() != null) {
                if (MessageType.TEXT.equals(item.getMessageType())) {
                    fromAndmessageLabel.setText(item.getFromName() + ": " + item.getMessage());
                } else if (MessageType.FILE.equals(item.getMessageType())) {
                    try {
                        File file = objectMapper.readValue(item.getMessage(), File.class);
                        fromAndmessageLabel.setText(item.getFromName() + ": " + "[文件] " + file.getFileName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                } else if (MessageType.PICTURE.equals(item.getMessageType())) {
                    try {
                        File file = objectMapper.readValue(item.getMessage(), File.class);
                        fromAndmessageLabel.setText(item.getFromName() + ": " + "[图片] " + file.getFileName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                } else {
                    throw new RuntimeException("不支持的消息类型！");
                }
            } else {
                fromAndmessageLabel.setText(null);
            }

            // 放入
            setGraphic(vBox);
        }
    }

}
