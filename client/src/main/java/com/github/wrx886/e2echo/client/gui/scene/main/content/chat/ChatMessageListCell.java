package com.github.wrx886.e2echo.client.gui.scene.main.content.chat;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.model.entity.File;
import com.github.wrx886.e2echo.client.model.enums.MessageType;
import com.github.wrx886.e2echo.client.model.vo.MessageVo;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.awt.Desktop;
import java.io.IOException;

public class ChatMessageListCell extends ListCell<MessageVo> {

    // 日期格式化工具
    private SimpleDateFormat simpleDateFormat = BeanProvider.getBean(SimpleDateFormat.class);

    // ObejctMapper
    private ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);

    // 发送者
    private final Label fromNameLabel;

    // 发送时间
    private final Label sendTimeLabel;

    // 内容-文字
    private final Label dataTextLabel;

    // 打开文件
    private final Button openButton;

    // 展示容器
    private final VBox vBox;

    // 构造函数
    public ChatMessageListCell() {

        // 发送者
        fromNameLabel = new Label();

        // 发送时间
        sendTimeLabel = new Label();

        HBox hBox1 = new HBox(fromNameLabel, sendTimeLabel);

        // 内容-文字
        dataTextLabel = new Label();

        // 打开文件
        openButton = new Button("打开");

        HBox hBox2 = new HBox(dataTextLabel, openButton);

        // 容器
        vBox = new VBox(hBox1, hBox2);
    }

    @Override
    protected void updateItem(MessageVo item, boolean empty) {
        super.updateItem(item, empty);

        try {
            if (empty || item == null) {
                // 单元格为空
                setText(null);
                setGraphic(null);
            } else {
                // 单元格不为空

                // 发送者
                fromNameLabel.setText(item.getFromName());

                // 发送时间
                sendTimeLabel.setText(simpleDateFormat.format(item.getSendTime()));

                if (MessageType.TEXT.equals(item.getType())) {
                    // 文字展示
                    dataTextLabel.setText(item.getData());
                    openButton.setDisable(true);
                } else if (MessageType.FILE.equals(item.getType())) {
                    // 解析文件
                    File file = objectMapper.readValue(item.getData(), File.class);

                    // 写入文件名
                    dataTextLabel.setText(file.getFileName());

                    // 开启按钮
                    openButton.setDisable(false);
                    openButton.setOnAction((event) -> {
                        openFile(new java.io.File(file.getPath()).getAbsolutePath());
                    });
                } else {
                    throw new UnsupportedOperationException("数据类型未处理！");
                }

                setGraphic(vBox);
            }
        } catch (E2Echoxception e) {
            e.printStackTrace();
            throw new E2Echoxception(e.getCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void openFile(String filePath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String osName = System.getProperty("os.name").toLowerCase();

        try {
            if (osName.contains("win")) {
                // Windows
                processBuilder.command("cmd", "/c", "\"" + filePath + "\"");
            } else if (osName.contains("mac")) {
                // macOS
                processBuilder.command("open", "\"" + filePath + "\"");
            } else {
                // Linux或其他Unix-like系统
                processBuilder.command("xdg-open", "\"" + filePath + "\"");
            }

            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
