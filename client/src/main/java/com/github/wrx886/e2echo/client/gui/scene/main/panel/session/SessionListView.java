package com.github.wrx886.e2echo.client.gui.scene.main.panel.session;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.content.chat.ChatView;
import com.github.wrx886.e2echo.client.gui.scene.main.content.edit.UserEditView;
import com.github.wrx886.e2echo.client.model.enums.MessageApiType;
import com.github.wrx886.e2echo.client.model.vo.SessionVo;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.store.GuiStore;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// 会话窗口列表
public class SessionListView extends VBox {

    private MessageService messageService = BeanProvider.getBean(MessageService.class);

    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    // 标题标签
    private final Label titleLabel;

    // 主界面路由
    private final NodeRouter contenNodeRouter;

    // 添加会话按钮
    private final Button addSessionButton;

    // 刷新按钮
    private final Button reflushButton;

    // 自动接收按钮
    private final ToggleButton autoReceiveToggleButton;

    // 会话列表
    private final ListView<SessionVo> sessionListView;

    // 构造函数
    public SessionListView(NodeRouter contenNodeRouter) {
        // 设置主界面路由
        this.contenNodeRouter = contenNodeRouter;

        // 设置宽度
        setPrefWidth(270);

        // 设置高度
        setPrefHeight(690);

        // 设置元素间距
        setSpacing(5);

        // 设置对齐方案
        setAlignment(Pos.BOTTOM_CENTER);

        // 获取根节点
        ObservableList<Node> root = getChildren();

        // 标题标签
        titleLabel = new Label("会话");
        root.add(titleLabel);

        // 按钮布局
        HBox buttonHBox = new HBox();
        buttonHBox.setSpacing(5);
        buttonHBox.setAlignment(Pos.CENTER_LEFT);
        root.add(buttonHBox);

        // 添加会话按钮
        addSessionButton = new Button("添加");
        addSessionButton.setOnAction(this::addSessionButtonOnAction);
        buttonHBox.getChildren().add(addSessionButton);

        // 刷新按钮
        reflushButton = new Button("刷新");
        reflushButton.setOnAction(this::reflushButtonOnAction);
        buttonHBox.getChildren().add(reflushButton);

        // 自动接收按钮
        autoReceiveToggleButton = new ToggleButton("自动接收和群聊已关闭");
        autoReceiveToggleButton.setOnAction(this::autoReceiveToggleButtonOnAction);
        buttonHBox.getChildren().add(autoReceiveToggleButton);

        // 会话列表
        sessionListView = new ListView<>(guiStore.getSessionVos());
        sessionListView.setCellFactory(param -> new SessionListCell());
        sessionListView.setOnMouseClicked(this::sessionListViewOnMouseClicked);
        VBox.setVgrow(sessionListView, Priority.ALWAYS);
        root.add(sessionListView);

    }

    // 当选择会话事件
    private void sessionListViewOnMouseClicked(Event event) {
        // 获取当前选择的会话
        SessionVo selectedItem = sessionListView.getSelectionModel().getSelectedItem();
        // 如果不为空，就需要更新聊天窗口的内容
        if (selectedItem != null) {
            guiStore.getCurrentSessionVo().set(selectedItem);
            guiStore.getCurrentSessionName().set(selectedItem.getSessionName());
            messageService.updateMessageVosBySession(selectedItem);
            contenNodeRouter.push(ChatView.class);
        }
    }

    // 刷新按钮事件
    private void reflushButtonOnAction(Event event) {
        messageService.receive();
        messageService.updateSessionVos();
        if (guiStore.getCurrentSessionVo().get() != null) {
            messageService.updateMessageVosBySession(guiStore.getCurrentSessionVo().get());
        }
    }

    // 添加会话事件
    private void addSessionButtonOnAction(Event event) {
        guiStore.getEditUser().set(null);
        contenNodeRouter.push(UserEditView.class);
    }

    // 自动接收开关
    private void autoReceiveToggleButtonOnAction(Event event) {
        if (autoReceiveToggleButton.isSelected()) {
            messageService.registryAutoReceive(MessageApiType.GROUP);
            autoReceiveToggleButton.setText("自动接收和群聊已开启");
        } else {
            messageService.registryAutoReceive(null);
            autoReceiveToggleButton.setText("自动接收和群聊已关闭");
        }
    }

}
