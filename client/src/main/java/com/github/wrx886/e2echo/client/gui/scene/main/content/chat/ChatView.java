package com.github.wrx886.e2echo.client.gui.scene.main.content.chat;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.content.edit.UserEditView;
import com.github.wrx886.e2echo.client.model.enums.MessageType;
import com.github.wrx886.e2echo.client.model.vo.MessageVo;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.service.UserService;
import com.github.wrx886.e2echo.client.store.GuiStore;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

// 聊天界面视图
public class ChatView extends VBox {

    private MessageService messageService = BeanProvider.getBean(MessageService.class);

    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    private UserService userService = BeanProvider.getBean(UserService.class);

    // 主界面路由
    private final NodeRouter contentRouter;

    // 当前会话名称
    private final Label sessionNameLabel;

    // 修改会话属性的按钮
    private final Button updateSessionButton;

    // 聊天内容
    private final ListView<MessageVo> messageListView;

    // 要发送的消息
    private final TextArea textArea;

    // 构造函数
    public ChatView(NodeRouter contentRouter) {
        // 主界面路由
        this.contentRouter = contentRouter;

        // 获取孩子列表
        ObservableList<Node> root = getChildren();

        // 当前会话名称
        sessionNameLabel = new Label();
        sessionNameLabel.textProperty().bind(guiStore.getCurrentSessionName());
        root.add(sessionNameLabel);

        // 修改会话属性
        updateSessionButton = new Button("设置");
        updateSessionButton.setOnAction(this::updateSessionButtonOnAction);
        root.add(updateSessionButton);

        // 聊天内容
        messageListView = new ListView<>(guiStore.getCurrentMessageVos());
        messageListView.setCellFactory(param -> new ChatMessageListCell());
        root.add(messageListView);

        // 文本输入框
        textArea = new TextArea();
        root.add(textArea);

        // 发送按钮
        Button sendButton = new Button("发送");
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
    public void updateSessionButtonOnAction(Event event) {
        guiStore.getEditUser().set(userService.getById(guiStore.getCurrentSessionVo().get().getSessionId()));
        contentRouter.push(UserEditView.class);
    }

}
