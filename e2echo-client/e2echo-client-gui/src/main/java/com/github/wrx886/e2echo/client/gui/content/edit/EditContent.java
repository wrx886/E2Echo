package com.github.wrx886.e2echo.client.gui.content.edit;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.controller.srv.SessionController;
import com.github.wrx886.e2echo.client.gui.layout.SidebarPanelContentLayout;
import com.github.wrx886.e2echo.client.gui.modol.vo.EditVo;
import com.github.wrx886.e2echo.client.gui.store.GuiStore;

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

public class EditContent extends GridPane {

    private final SessionController sessionController = BeanProvider.getBean(SessionController.class);
    private final GuiStore guiStore = BeanProvider.getBean(GuiStore.class);
    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);

    // GridPane 内元素的外边距
    private static final Insets GRID_MARGIN = new Insets(5);

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

    // 保存按钮
    private final Button saveButton;

    // 取消按钮
    private final Button cancelButton;

    // 主布局
    private final SidebarPanelContentLayout layout;

    public EditContent(SidebarPanelContentLayout layout) {
        // 主界面路由
        this.layout = layout;

        // 设置宽度
        setPrefWidth(500);

        // 设置高度
        setPrefHeight(500);

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
        guiStore.getEditVo().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                // 清空所有
                nameTextField.clear();
                publicKeyTextField.clear();
                typeToggleGroup.selectToggle(null);
                publicKeyTextField.setEditable(true);
                typeUserRadioButton.setDisable(false);
                typeGroupRadioButton.setDisable(false);
            } else {
                String sessionName = aliasController.get(newValue.getPublicKeyHex());
                if (sessionName == null) {
                    if (!newValue.getGroup()) {
                        sessionName = newValue.getPublicKeyHex().substring(0, 5);
                    } else {
                        sessionName = newValue.getPublicKeyHex().split(":")[1].substring(0, 5);
                    }
                }
                nameTextField.setText(sessionName);
                publicKeyTextField.setText(newValue.getPublicKeyHex());
                if (!newValue.getGroup()) {
                    typeToggleGroup.selectToggle(typeUserRadioButton);
                } else {
                    typeToggleGroup.selectToggle(typeGroupRadioButton);
                }
                publicKeyTextField.setEditable(false);
                typeUserRadioButton.setDisable(true);
                typeGroupRadioButton.setDisable(true);
            }
        });

    }

    // 取消按钮激活
    public void cancelButtonOnAction(Event event) {
        if (guiStore.getEditVo().get() != null) {
            EditVo editVo = new EditVo();
            editVo.setPublicKeyHex(guiStore.getCurrentSession().get().getPublicKeyHex());
            editVo.setGroup(guiStore.getCurrentSession().get().getGroup());

            String sessionName = aliasController.get(editVo.getPublicKeyHex());
            if (sessionName == null) {
                if (!editVo.getGroup()) {
                    sessionName = editVo.getPublicKeyHex().substring(0, 5);
                } else {
                    sessionName = editVo.getPublicKeyHex().split(":")[1].substring(0, 5);
                }
            }

            editVo.setAlias(sessionName);

            guiStore.getEditVo().set(editVo);
        } else {
            guiStore.getEditVo().set(null);
            // 清空所有
            nameTextField.clear();
            publicKeyTextField.clear();
            typeToggleGroup.selectToggle(null);
        }

        // 返回
        layout.getContentRouter().pop();
    }

    // 保存按钮激活
    public void saveButtonOnAction(Event event) {
        // 如果不存在
        if (!sessionController.contain(publicKeyTextField.getText())) {
            // 创建会话
            sessionController.create(publicKeyTextField.getText(),
                    typeGroupRadioButton.equals(typeToggleGroup.getSelectedToggle()));
        }

        // 设置别名
        aliasController.put(publicKeyTextField.getText(), nameTextField.getText());

        layout.getContentRouter().pop();
    }

}
