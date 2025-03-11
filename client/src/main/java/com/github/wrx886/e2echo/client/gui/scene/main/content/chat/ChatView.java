package com.github.wrx886.e2echo.client.gui.scene.main.content.chat;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.content.edit.UserEditView;
import com.github.wrx886.e2echo.client.model.entity.File;
import com.github.wrx886.e2echo.client.model.enums.MessageType;
import com.github.wrx886.e2echo.client.model.vo.MessageVo;
import com.github.wrx886.e2echo.client.service.FileService;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.service.UserService;
import com.github.wrx886.e2echo.client.store.GuiStore;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

// 聊天界面视图
public class ChatView extends VBox {

    private MessageService messageService = BeanProvider.getBean(MessageService.class);

    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    private UserService userService = BeanProvider.getBean(UserService.class);

    private FileService fileService = BeanProvider.getBean(FileService.class);

    // 主界面路由
    private final NodeRouter contentRouter;

    // 当前会话名称
    private final Label sessionNameLabel;

    // 修改会话属性的按钮
    private final Button updateSessionButton;

    // 聊天内容
    private final ListView<MessageVo> messageListView;

    // 发送文件
    private final Button fileSendButton;

    // 发送图片
    private final Button pictureSendButton;

    // 要发送的消息
    private final TextArea textArea;

    // 发送按钮
    private final Button sendButton;

    // 构造函数
    public ChatView(NodeRouter contentRouter) {
        // 主界面路由
        this.contentRouter = contentRouter;

        // 设置宽度
        setPrefWidth(520);

        // 设置高度
        setPrefHeight(690);

        // 设置元素间距
        setSpacing(5);

        // 设置对其方案
        setAlignment(Pos.TOP_LEFT);

        // 获取孩子列表
        ObservableList<Node> root = getChildren();

        // 当前会话名称
        sessionNameLabel = new Label();
        sessionNameLabel.textProperty().bind(guiStore.getCurrentSessionName());
        VBox.setVgrow(sessionNameLabel, Priority.ALWAYS);
        root.add(sessionNameLabel);

        // 修改会话属性
        updateSessionButton = new Button("设置");
        updateSessionButton.setOnAction(this::updateSessionButtonOnAction);
        root.add(updateSessionButton);

        // 聊天内容
        messageListView = new ListView<>(guiStore.getCurrentMessageVos());
        messageListView.setCellFactory(param -> new ChatMessageListCell());
        root.add(messageListView);

        // 设置监听器，默认滑到底部
        guiStore.getCurrentMessageVos().addListener((ListChangeListener<MessageVo>) change -> {
            if (!guiStore.getCurrentMessageVos().isEmpty()) {
                messageListView.scrollTo(guiStore.getCurrentMessageVos().size() - 1);
            }
        });

        // 发送条
        HBox sendLineHBox = new HBox();
        sendLineHBox.setSpacing(5);
        sendLineHBox.setAlignment(Pos.CENTER_LEFT);
        root.add(sendLineHBox);

        // 文件选择器
        fileSendButton = new Button("文件");
        fileSendButton.setOnAction(this::fileSendButtonOnAction);
        sendLineHBox.getChildren().add(fileSendButton);

        // 图片发送器
        pictureSendButton = new Button("图片");
        pictureSendButton.setOnAction(this::pictureSendButtonOnAction);
        sendLineHBox.getChildren().add(pictureSendButton);

        // 文本输入框
        textArea = new TextArea();
        textArea.setMaxWidth(520);
        textArea.setWrapText(true);
        root.add(textArea);

        // 发送按钮
        sendButton = new Button("发送");
        sendButton.setOnAction(this::sendButtonOnAction);
        root.add(sendButton);
    }

    // 发送消息事件
    private void sendButtonOnAction(Event event) {
        // 发送消息
        messageService.send(guiStore.getCurrentSessionVo().get().getSessionId(), textArea.getText(), MessageType.TEXT);
        // 发送成功后清空信息
        textArea.setText(null);
        // 刷新
        messageService.updateMessageVosBySession(guiStore.getCurrentSessionVo().get());
        messageService.updateSessionVos();
    }

    // 用户信息修改
    private void updateSessionButtonOnAction(Event event) {
        guiStore.getEditUser().set(userService.getById(guiStore.getCurrentSessionVo().get().getSessionId()));
        contentRouter.push(UserEditView.class);
    }

    // 发送文件
    private void fileSendButtonOnAction(Event event) {
        // 获取文件
        String filePath = fileChoose();
        if (filePath == null) {
            return;
        }

        // 上传文件
        File file = fileService.upload(filePath);
        // 发送文件
        messageService.send(guiStore.getCurrentSessionVo().get().getSessionId(), file, MessageType.FILE);

        // 刷新
        messageService.updateMessageVosBySession(guiStore.getCurrentSessionVo().get());
        messageService.updateSessionVos();
    }

    // 发送图片
    private void pictureSendButtonOnAction(Event event) {
        // 获取文件
        String filePath = fileChoose("图片类型", "*.jpg", "*.png", "*.gif", "*.jfif");
        if (filePath == null) {
            return;
        }

        // 上传文件
        File file = fileService.upload(filePath);

        // 发送文件
        messageService.send(guiStore.getCurrentSessionVo().get().getSessionId(), file, MessageType.PICTURE);

        // 刷新
        messageService.updateMessageVosBySession(guiStore.getCurrentSessionVo().get());
        messageService.updateSessionVos();
    }

    // 文件选择器
    private String fileChoose(String description, String... extensions) {
        // 创建文件选择器实例
        FileChooser fileChooser = new FileChooser();

        // 设置文件选择器的标题
        fileChooser.setTitle("选择文件");

        // 设置初始目录（可选）
        fileChooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));

        // 添加文件类型过滤器（可选）
        if (description != null && extensions.length > 0) {
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                    description, extensions);
            fileChooser.getExtensionFilters().add(extFilter);
        }

        // 显示文件选择对话框并获取用户选择的文件
        java.io.File selectedFile = fileChooser.showOpenDialog(null);

        return selectedFile != null ? selectedFile.getAbsolutePath() : null;
    }

    // 文件选择器
    private String fileChoose() {
        return fileChoose(null);
    }
}
