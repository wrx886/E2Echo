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
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.FileInputStream;
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

    // 内容-图片
    private final ImageView dataImageView;

    // 打开文件
    private final Button openButton;

    // 展示容器
    private final VBox vBox;
    private final VBox vBox1;

    // 构造函数
    public ChatMessageListCell() {

        // 发送者
        fromNameLabel = new Label();
        fromNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        fromNameLabel.setMaxWidth(360);

        // 发送时间
        sendTimeLabel = new Label();

        // 空白填充
        Region hBox1Region = new Region();
        HBox.setHgrow(hBox1Region, Priority.ALWAYS);

        // 发送者和发送时间容器
        HBox hBox1 = new HBox(fromNameLabel, hBox1Region, sendTimeLabel);

        // 内容-文字
        dataTextLabel = new Label();
        dataTextLabel.setMaxWidth(430);
        dataTextLabel.setWrapText(true);

        // 内容-图片
        dataImageView = new ImageView();

        // 打开文件
        openButton = new Button("打开");

        // 文本的开启按钮容器
        vBox1 = new VBox(dataTextLabel);
        vBox1.setSpacing(5);

        // 容器
        vBox = new VBox(hBox1, vBox1);
        vBox.setSpacing(5);
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

                // 删除容器
                vBox1.getChildren().remove(dataImageView);
                vBox1.getChildren().remove(openButton);

                if (MessageType.TEXT.equals(item.getType())) {
                    // 文字展示
                    dataTextLabel.setText(item.getData());
                } else if (MessageType.FILE.equals(item.getType()) || MessageType.PICTURE.equals(item.getType())) {
                    // 解析文件
                    File file = objectMapper.readValue(item.getData(), File.class);

                    // 图片显示
                    if (MessageType.PICTURE.equals(item.getType())) {
                        // 增加图片容器
                        vBox1.getChildren().add(dataImageView);

                        Image image = new Image(new FileInputStream(file.getPath()));
                        dataImageView.setImage(image);

                        // 计算图片显示大小
                        double scale = 430 / Math.max(image.getHeight(), image.getWidth());
                        dataImageView.setFitWidth(scale * image.getWidth());
                        dataImageView.setFitHeight(scale * image.getHeight());
                        dataTextLabel.setText("[图片] " + file.getFileName());
                    } else {
                        dataImageView.setImage(null);
                        dataTextLabel.setText("[文件] " + file.getFileName());
                    }

                    // 增加打开按钮
                    vBox1.getChildren().add(openButton);

                    // 开启按钮
                    openButton.setDisable(false);
                    openButton.setVisible(true);
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
            if (osName.contains("windows")) {
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
