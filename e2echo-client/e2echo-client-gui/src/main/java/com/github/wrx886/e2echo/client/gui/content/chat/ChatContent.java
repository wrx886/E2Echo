package com.github.wrx886.e2echo.client.gui.content.chat;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.controller.srv.MessageController;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.gui.content.edit.EditContent;
import com.github.wrx886.e2echo.client.gui.layout.SidebarPanelContentLayout;
import com.github.wrx886.e2echo.client.gui.modol.vo.EditVo;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

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

public class ChatContent extends VBox {

    private AliasController aliasController = BeanProvider.getBean(AliasController.class);
    private MessageController messageController = BeanProvider.getBean(MessageController.class);
    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    // 当前会话名称
    private final Label sessionNameLabel;

    // 修改会话属性的按钮
    private final Button updateSessionButton;

    // 聊天内容
    private final ListView<Message> messageListView;

    // 要发送的消息
    private final TextArea textArea;

    // 发送按钮
    private final Button sendButton;

    // 主界面路由
    private final SidebarPanelContentLayout layout;

    // 构造函数
    public ChatContent(SidebarPanelContentLayout layout) {
        // 主界面路由
        this.layout = layout;

        // 设置宽度
        setPrefWidth(500);

        // 设置高度
        setPrefHeight(500);

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
        messageListView = new ListView<>(guiStore.getCurrentMessage());
        messageListView.setCellFactory(param -> new ChatContentCell());
        root.add(messageListView);

        // 设置监听器，默认滑到底部
        guiStore.getCurrentMessage().addListener((ListChangeListener<Message>) change -> {
            if (!guiStore.getCurrentMessage().isEmpty()) {
                messageListView.scrollTo(guiStore.getCurrentMessage().size() - 1);
            }
        });

        // 发送条
        HBox sendLineHBox = new HBox();
        sendLineHBox.setSpacing(5);
        sendLineHBox.setAlignment(Pos.CENTER_LEFT);
        root.add(sendLineHBox);

        // 文本输入框
        textArea = new TextArea();
        textArea.setMaxWidth(500);
        textArea.setWrapText(true);
        root.add(textArea);

        // 发送按钮
        sendButton = new Button("发送");
        sendButton.setOnAction(this::sendButtonOnAction);
        root.add(sendButton);
    }

    // 发送消息事件
    private void sendButtonOnAction(Event event) {
        Session session = guiStore.getCurrentSession().get();
        // 发送消息
        if (session.getGroup()) {
            messageController.sendGroupMessage(
                    session.getPublicKeyHex(),
                    textArea.getText(),
                    MessageType.TEXT);
        } else {
            messageController.sendOneMessage(
                    session.getPublicKeyHex(),
                    textArea.getText(),
                    MessageType.TEXT);
        }

        // 发送成功后清空信息
        textArea.setText(null);
    }

    // 用户信息修改
    private void updateSessionButtonOnAction(Event event) {
        EditVo editVo = new EditVo();
        editVo.setPublicKeyHex(guiStore.getCurrentSession().get().getPublicKeyHex());
        editVo.setGroup(guiStore.getCurrentSession().get().getGroup());
        editVo.setAlias(aliasController.get(editVo.getPublicKeyHex()));
        guiStore.getEditVo().set(editVo);
        layout.getContentRouter().push(EditContent.class);
    }

}
