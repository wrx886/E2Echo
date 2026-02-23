package com.github.wrx886.e2echo.client.gui.panel.session;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.controller.srv.MessageController;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.model.vo.FileVo;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SessionPanelCell extends ListCell<Session> {

    private final ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);
    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);
    private final MessageController messageController = BeanProvider.getBean(MessageController.class);

    // 日期格式化工具
    private final SimpleDateFormat simpleDateFormat = BeanProvider.getBean(SimpleDateFormat.class);

    // 会话名称
    private final Label sessionNameLabel;

    // 更新时间
    private final Label lastTimeLabel;

    // 消息
    private final Label fromAndmessageLabel;

    // 总容器
    private final VBox vBox;

    // 构造函数
    public SessionPanelCell() {

        // 会话名称
        sessionNameLabel = new Label();
        sessionNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        sessionNameLabel.setMaxWidth(50);

        // 更新时间
        lastTimeLabel = new Label();

        // 发送者和消息
        fromAndmessageLabel = new Label();
        fromAndmessageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        fromAndmessageLabel.setMaxWidth(190);

        // 空白填充
        Region hBox1Region = new Region();
        HBox.setHgrow(hBox1Region, Priority.ALWAYS);

        // 容器1：存放会话名称和更新时间
        HBox hBox = new HBox(sessionNameLabel, hBox1Region, lastTimeLabel);

        // 容器2：存放 容器1 和 容器2
        vBox = new VBox(hBox, fromAndmessageLabel);
    }

    @Override
    protected void updateItem(Session item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 会话名称
            sessionNameLabel.setText(aliasController.get(item.getPublicKeyHex()));

            // 更新时间
            if (item.getTimestamp() != null) {
                Date date = new Date(item.getTimestamp());
                lastTimeLabel.setText(simpleDateFormat.format(date));
            } else {
                lastTimeLabel.setText(null);
            }

            // 发送者和消息
            if (item.getMessageId() != null) {
                Message message = messageController.getById(item.getMessageId());
                if (MessageType.TEXT.equals(message.getType())) {
                    // 文字消息
                    fromAndmessageLabel
                            .setText(aliasController.get(message.getFromPublicKeyHex()) + ": " + message.getData());
                } else if (MessageType.FILE.equals(message.getType())
                        || MessageType.PICTURE.equals(message.getType())) {
                    // 文件展示
                    FileVo fileVo;
                    try {
                        fileVo = objectMapper.readValue(message.getData(), FileVo.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                    }
                    String label = MessageType.FILE.equals(message.getType()) ? "[文件] " : "[图片] ";
                    fromAndmessageLabel
                            .setText(aliasController.get(message.getFromPublicKeyHex()) + ": " + label
                                    + fileVo.getFileName());
                } else {
                    fromAndmessageLabel
                            .setText(aliasController.get(message.getFromPublicKeyHex()) + ": " + "不支持的消息类型！");
                }
            } else {
                fromAndmessageLabel.setText(null);
            }

            // 放入
            setGraphic(vBox);
        }
    }

}
