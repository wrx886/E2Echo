package com.github.wrx886.e2echo.client.gui.content.chat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.model.vo.FileVo;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatContentCell extends ListCell<Message> {

    private final ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);
    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);

    // 日期格式化工具
    private final SimpleDateFormat simpleDateFormat = BeanProvider.getBean(SimpleDateFormat.class);

    // 发送者
    private final Label fromNameLabel;

    // 发送时间
    private final Label sendTimeLabel;

    // 内容-图片
    private final ImageView imageView;

    // 内容-文字
    private final Label dataTextLabel;

    // 内容-另存为
    private final Button saveButton;

    // 内容-打开
    private final Button openButton;

    // 展示容器
    private final VBox vBox;
    private final HBox hBox;
    private final VBox dataVBox;

    // 构造函数
    public ChatContentCell() {

        // 发送者
        fromNameLabel = new Label();
        fromNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        fromNameLabel.setMaxWidth(330);

        // 发送时间
        sendTimeLabel = new Label();

        // 空白填充
        Region hBox1Region = new Region();
        HBox.setHgrow(hBox1Region, Priority.ALWAYS);

        // 发送者和发送时间容器
        hBox = new HBox(fromNameLabel, hBox1Region, sendTimeLabel);

        // 内容-图片
        imageView = new ImageView();

        // 内容-文字
        dataTextLabel = new Label();
        dataTextLabel.setMaxWidth(430);
        dataTextLabel.setWrapText(true);

        // 内容-另存为
        saveButton = new Button("另存为");
        saveButton.setOnAction(this::onSaveButtonAction);

        // 内容-打开
        openButton = new Button("打开");
        openButton.setOnAction(this::onOpenButtonAction);

        // 数据容器
        dataVBox = new VBox(dataTextLabel);
        dataVBox.setSpacing(5);

        // 容器
        vBox = new VBox(hBox, dataVBox);
    }

    @Override
    protected void updateItem(Message item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 设置文本高度最小值
            dataTextLabel.setMinHeight(0);

            // 发送者
            fromNameLabel.setText(aliasController.get(item.getFromPublicKeyHex()));

            // 发送时间
            Date date = new Date(item.getTimestamp());
            sendTimeLabel.setText(simpleDateFormat.format(date));
            dataVBox.getChildren().clear();

            if (MessageType.TEXT.equals(item.getType())) {
                // 文字展示
                dataTextLabel.setText(item.getData());
                dataTextLabel.setMinHeight(getTextHeight(dataTextLabel.getText()));
                dataVBox.getChildren().add(dataTextLabel);
            } else if (MessageType.FILE.equals(item.getType()) || MessageType.PICTURE.equals(item.getType())) {
                // 文件展示
                FileVo fileVo;
                try {
                    fileVo = objectMapper.readValue(item.getData(), FileVo.class);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
                if (MessageType.FILE.equals(item.getType())) {
                    dataTextLabel.setText("[文件] " + fileVo.getFileName());
                    dataTextLabel.setMinHeight(getTextHeight(dataTextLabel.getText()));
                    dataVBox.getChildren().addAll(dataTextLabel, saveButton);
                } else {
                    String filePath = "./download/" + fileVo.getFileId() + ".decrypted";
                    if (!new File(filePath).exists()) {
                        dataTextLabel.setText("[图片] 图片不存在");
                        dataTextLabel.setMinHeight(getTextHeight(dataTextLabel.getText()));
                        dataVBox.getChildren().addAll(dataTextLabel, saveButton);
                    } else {
                        try (FileInputStream fis = new FileInputStream(filePath)) {
                            // 设置标签
                            dataTextLabel.setText("[图片] " + fileVo.getFileName());
                            dataTextLabel.setMinHeight(getTextHeight(dataTextLabel.getText()));

                            // 设置图片
                            Image image = new Image(fis);
                            imageView.setImage(image);
                            double radio = 450 / Double.max(image.getHeight(), image.getWidth());
                            imageView.setFitWidth(radio < 1 ? image.getWidth() * radio : image.getWidth());
                            imageView.setFitHeight(radio < 1 ? image.getHeight() * radio : image.getHeight());

                            // 添加按钮
                            HBox buttonHBox = new HBox(openButton, saveButton);
                            buttonHBox.setSpacing(5);
                            dataVBox.getChildren().addAll(dataTextLabel, imageView, buttonHBox);
                        } catch (Exception e) {
                            log.error("", e);
                            dataTextLabel.setText("[图片] 图片加载错误");
                            dataTextLabel.setMinHeight(getTextHeight(dataTextLabel.getText()));
                            dataVBox.getChildren().addAll(dataTextLabel, saveButton);
                        }
                    }
                }
            } else {
                dataTextLabel.setText("不支持的消息类型");
                dataTextLabel.setMinHeight(getTextHeight(dataTextLabel.getText()));
            }

            setGraphic(vBox);
        }
    }

    // 计算文本高度
    private int getTextHeight(String text) {
        Text text1 = new Text(text + "\n");
        text1.setFont(dataTextLabel.getFont());
        text1.setWrappingWidth(dataTextLabel.getMaxWidth());
        return (int) text1.getLayoutBounds().getHeight();
    }

    // 另存为
    private void onSaveButtonAction(Event event) {
        // 获取所选项
        Message message = getItem();
        // 转为 FileVo
        FileVo fileVo;
        try {
            fileVo = objectMapper.readValue(message.getData(), FileVo.class);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        // 选择文件夹
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择保存文件夹");
        File dir = directoryChooser.showDialog(null);
        if (dir == null) {
            return;
        }
        // 保存文件
        String filePath = "./download/" + fileVo.getFileId() + ".decrypted";
        if (!new File(filePath).exists()) {
            throw new E2EchoException("文件不存在");
        }
        String savePath = dir.getAbsolutePath() + "/" + fileVo.getFileName();
        if (new File(savePath).exists()) {
            throw new E2EchoException("文件已存在");
        }
        try (FileOutputStream fos = new FileOutputStream(savePath);
                FileInputStream fis = new FileInputStream(filePath);) {
            fis.transferTo(fos);
        } catch (Exception e) {
            throw new E2EchoException(e.getMessage());
        }
        // 提示
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText("保存成功");
        alert.showAndWait();
    }

    // 打开
    private void onOpenButtonAction(Event event) {
        // 获取所选项
        Message message = getItem();
        // 转为 FileVo
        FileVo fileVo;
        try {
            fileVo = objectMapper.readValue(message.getData(), FileVo.class);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        // 图片
        if (MessageType.PICTURE.equals(message.getType())) {
            openPicture(fileVo);
        } else {
            throw new E2EchoException("不支持的消息类型");
        }
    }

    // 打开图片
    private void openPicture(FileVo fileVo) {
        // 图片不存在
        String filePath = "./download/" + fileVo.getFileId() + ".decrypted";
        if (!new File(filePath).exists()) {
            throw new E2EchoException("图片不存在");
        }
        // 读取图片
        ImageView imageView = new ImageView();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Image image = new Image(fis);
            imageView.setImage(image);
            double radioHeight = 700 / image.getHeight();
            double radioWidth = 1250 / image.getWidth();
            double radio = Math.min(radioHeight, radioWidth);
            imageView.setFitWidth(radio < 1 ? image.getWidth() * radio : image.getWidth());
            imageView.setFitHeight(radio < 1 ? image.getHeight() * radio : image.getHeight());
        } catch (Exception e) {
            log.error("", e);
            throw new E2EchoException("图片加载错误");
        }

        // 创建 Pane
        GridPane gridPane = new GridPane();
        gridPane.add(imageView, 0, 0);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setPrefWidth(1280);
        gridPane.setPrefHeight(768);

        // 创建舞台
        Stage stage = new Stage();
        stage.setScene(new Scene(gridPane));
        stage.setTitle("图片");
        stage.setWidth(1280);
        stage.setHeight(768);
        stage.show();
    }
}
