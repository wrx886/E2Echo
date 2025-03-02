package com.github.wrx886.e2echo.client.gui.scene.main.content.edit;

import java.util.UUID;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.gui.common.NodeRouter;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.model.enums.UserType;
import com.github.wrx886.e2echo.client.service.GroupUserService;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.service.UserService;
import com.github.wrx886.e2echo.client.store.GuiStore;
import com.github.wrx886.e2echo.client.store.LoginUserStore;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class UserEditView extends GridPane {

    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    private UserService userService = BeanProvider.getBean(UserService.class);

    private LoginUserStore loginUserStore = BeanProvider.getBean(LoginUserStore.class);

    private MessageService messageService = BeanProvider.getBean(MessageService.class);

    private GroupUserService groupUserService = BeanProvider.getBean(GroupUserService.class);

    // GridPane 内元素的外边距
    private static final Insets GRID_MARGIN = new Insets(5);

    // 主界面路由
    private final NodeRouter contenRouter;

    // 名称标签
    private final Label nameLabel;

    // 名称输入框
    private final TextField nameTextField;

    // 公钥标签
    private final Label publicKeyLabel;

    // 公钥输入框
    private final TextField publicKeyTextField;

    // 用户类型
    private final Label typeLabel;

    // 用户类型按钮管理
    private final ToggleGroup typeToggleGroup;

    // 用户类型-用户 按钮
    private final RadioButton typeUserRadioButton;

    // 用户类型-群聊 按钮
    private final RadioButton typeGroupRadioButton;

    // 群聊uuid标签
    private final Label groupUuidLabel;

    // 群聊uuid输入框
    private final TextField groupUuidTextField;

    // 群聊 UUID 生成按钮
    private final Button groupUuidButton;

    // 保存按钮
    private final Button saveButton;

    // 取消按钮
    private final Button cancelButton;

    public UserEditView(NodeRouter contenRouter) {
        // 主界面路由
        this.contenRouter = contenRouter;

        // 设置宽度
        setPrefWidth(520);

        // 设置高度
        setPrefHeight(690);

        // 设置对齐方案
        setAlignment(Pos.CENTER);

        // 名称标签
        nameLabel = new Label("名称");
        GridPane.setMargin(nameLabel, GRID_MARGIN);
        add(nameLabel, 0, 0);

        // 名称输入框
        nameTextField = new TextField();
        GridPane.setMargin(nameTextField, GRID_MARGIN);
        add(nameTextField, 1, 0);

        // 公钥标签
        publicKeyLabel = new Label("公钥");
        GridPane.setMargin(publicKeyLabel, GRID_MARGIN);
        add(publicKeyLabel, 0, 1);

        // 公钥输入框
        publicKeyTextField = new TextField();
        GridPane.setMargin(publicKeyTextField, GRID_MARGIN);
        add(publicKeyTextField, 1, 1);

        // 类型
        typeLabel = new Label("类型");
        GridPane.setMargin(typeLabel, GRID_MARGIN);
        add(typeLabel, 0, 2);

        // 类型布局容器
        GridPane typeGridPane = new GridPane();
        add(typeGridPane, 1, 2);

        // 类型管理
        typeToggleGroup = new ToggleGroup();

        // 类型-用户
        typeUserRadioButton = new RadioButton("用户");
        typeUserRadioButton.setToggleGroup(typeToggleGroup);
        GridPane.setMargin(typeUserRadioButton, GRID_MARGIN);
        typeGridPane.add(typeUserRadioButton, 0, 0);

        // 类型-群聊
        typeGroupRadioButton = new RadioButton("群聊");
        typeGroupRadioButton.setToggleGroup(typeToggleGroup);
        GridPane.setMargin(typeGroupRadioButton, GRID_MARGIN);
        typeGridPane.add(typeGroupRadioButton, 1, 0);

        // 群聊 UUID 标签
        groupUuidLabel = new Label("群聊UUID");
        GridPane.setMargin(groupUuidLabel, GRID_MARGIN);
        add(groupUuidLabel, 0, 3);

        // 群聊 UUID 输入框
        groupUuidTextField = new TextField();
        GridPane.setMargin(groupUuidTextField, GRID_MARGIN);
        add(groupUuidTextField, 1, 3);

        // 群聊 UUID 按钮
        groupUuidButton = new Button("生成");
        groupUuidButton.setOnAction(this::groupUuidButtonOnAction);
        GridPane.setMargin(groupUuidButton, GRID_MARGIN);
        add(groupUuidButton, 2, 3);

        // 保存按钮
        saveButton = new Button("保存");
        saveButton.setOnAction(this::saveButtonOnAction);

        // 取消按钮
        cancelButton = new Button("取消");
        cancelButton.setOnAction(this::cancelButtonOnAction);

        // 放置
        HBox buttonHBox = new HBox(cancelButton, saveButton);
        buttonHBox.setSpacing(5);
        buttonHBox.setAlignment(Pos.CENTER);
        add(buttonHBox, 1, 4);

        // 监听器
        guiStore.getEditUser().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                // 清空所有
                nameTextField.clear();
                publicKeyTextField.clear();
                groupUuidTextField.clear();
                typeToggleGroup.selectToggle(null);
                groupUuidTextField.setEditable(true);
                groupUuidButton.setDisable(false);
                publicKeyTextField.setEditable(true);
                typeUserRadioButton.setDisable(false);
                typeGroupRadioButton.setDisable(false);
            } else {
                nameTextField.setText(newValue.getName());
                publicKeyTextField.setText(newValue.getPublicKey());
                groupUuidTextField.setText(newValue.getGroupUuid());
                if (newValue.getType().equals(UserType.PERSON)) {
                    typeToggleGroup.selectToggle(typeUserRadioButton);
                } else if (newValue.getType().equals(UserType.GROUP)) {
                    typeToggleGroup.selectToggle(typeGroupRadioButton);
                } else {
                    typeToggleGroup.selectToggle(null);
                }
                groupUuidTextField.setEditable(false);
                groupUuidButton.setDisable(true);
                publicKeyTextField.setEditable(false);
                typeUserRadioButton.setDisable(true);
                typeGroupRadioButton.setDisable(true);
            }
        });

    }

    // 保存按钮激活
    public void saveButtonOnAction(Event event) {
        // 获取用户
        User user = guiStore.getEditUser().get();
        if (user == null) {
            user = new User();
            user.setOwnerId(loginUserStore.getId());
            user.setPublicKey(publicKeyTextField.getText());
            user.setGroupUuid(groupUuidTextField.getText());
            if (typeUserRadioButton.equals(typeToggleGroup.getSelectedToggle())) {
                user.setType(UserType.PERSON);
            } else if (typeGroupRadioButton.equals(typeToggleGroup.getSelectedToggle())) {
                user.setType(UserType.GROUP);
            }
        }

        // 设置属性
        user.setName(nameTextField.getText());

        // 保存
        try {
            userService.saveOrUpdate(user);
        } catch (Exception e) {
            throw new E2Echoxception(null, "存在非法属性");
        }

        // 如果是群主，保存当前用户到群成员
        if (loginUserStore.getPublicKey().equals(user.getPublicKey()) && UserType.GROUP.equals(user.getType())) {
            groupUserService.putLoginUserToMember(user.getId());
        }

        messageService.updateSessionVos();
        contenRouter.pop();
    }

    // 取消按钮激活
    public void cancelButtonOnAction(Event event) {
        // 获取用户
        User user = guiStore.getEditUser().get();

        // 刷新数据
        if (user == null) {
            // 清空所有
            nameTextField.clear();
            publicKeyTextField.clear();
            groupUuidTextField.clear();
            typeToggleGroup.selectToggle(null);
        } else {
            nameTextField.setText(user.getName());
            publicKeyTextField.setText(user.getPublicKey());
            groupUuidTextField.setText(user.getGroupUuid());
            if (user.getType().equals(UserType.PERSON)) {
                typeToggleGroup.selectToggle(typeUserRadioButton);
            } else if (user.getType().equals(UserType.GROUP)) {
                typeToggleGroup.selectToggle(typeGroupRadioButton);
            } else {
                typeToggleGroup.selectToggle(null);
            }
        }

        // 返回
        contenRouter.pop();
    }

    // 群聊 UUID 生成按钮
    private void groupUuidButtonOnAction(Event event) {
        if (groupUuidTextField.isEditable() &&
                (groupUuidTextField.getText() == null || groupUuidTextField.getText().isEmpty())) {
            // 填充 UUID
            groupUuidTextField.setText(UUID.randomUUID().toString());
        }
    }

}
