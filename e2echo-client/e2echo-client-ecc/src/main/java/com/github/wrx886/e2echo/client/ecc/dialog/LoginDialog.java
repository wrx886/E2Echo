package com.github.wrx886.e2echo.client.ecc.dialog;

import java.io.File;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.ecc.service.EccService;
import com.github.wrx886.e2echo.client.ecc.util.EccUtil.KeyPairHex;

import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public final class LoginDialog extends Dialog<String> {

    private final ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);
    private final EccService eccService = BeanProvider.getBean(EccService.class);
    private final TextField publicKeyTextField;
    private final PasswordField privateKeyPasswordField;

    // 构造函数
    private LoginDialog() {
        super();

        // 设置窗口大小
        this.setTitle("E2echo");
        this.setWidth(320);
        this.setHeight(240);

        // 添加按钮
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // 获取根节点
        GridPane root = new GridPane();
        this.getDialogPane().setContent(root);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setHgap(10);
        root.setVgap(10);

        // 放置登入提示
        Label infoLabel = new Label("欢迎使用 E2Echo");
        infoLabel.setAlignment(Pos.CENTER);
        infoLabel.setStyle("-fx-font-size: 20px;");
        GridPane.setHalignment(infoLabel, HPos.CENTER);
        root.add(infoLabel, 0, 0, 2, 1);

        // 放置 公钥 标签
        Label publicKeyLabel = new Label("公钥");
        root.add(publicKeyLabel, 0, 1);

        // 放置 私钥 标签
        Label privateKeyLabel = new Label("私钥");
        root.add(privateKeyLabel, 0, 2);

        // 放置 公钥 输入框
        publicKeyTextField = new TextField();
        publicKeyTextField.setPromptText("请输入公钥");
        root.add(publicKeyTextField, 1, 1);

        // 放置 私钥 输入框
        privateKeyPasswordField = new PasswordField();
        privateKeyPasswordField.setPromptText("请输入私钥");
        root.add(privateKeyPasswordField, 1, 2);

        // 放置按钮布局
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(10);
        root.add(buttonBox, 0, 3, 2, 1);

        // 放置登入按钮
        Button loginButton = new Button("登入");
        buttonBox.getChildren().add(loginButton);
        loginButton.setOnAction(this::onLoginButtonAction);

        // 放置生成按钮
        Button generateButton = new Button("生成");
        buttonBox.getChildren().add(generateButton);
        generateButton.setOnAction(this::onGenerateButtonAction);

        // 读取 按钮
        Button readButton = new Button("读取");
        buttonBox.getChildren().add(readButton);
        readButton.setOnAction(this::onReadButtonAction);

        // 保存按钮
        Button saveButton = new Button("保存");
        buttonBox.getChildren().add(saveButton);
        saveButton.setOnAction(this::onSaveButtonAction);

        // 处理取消按钮
        this.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.CLOSE) {
                return null;
            }
            return dialogButton.getText();
        });
    }

    /**
     * 显示登入对话框
     *
     * @return 登入成功后返回公钥，登入失败会抛出对应 E2EchoException 异常，对话框关闭则返回 null
     */
    public static String dialog() {
        return new LoginDialog().showAndWait().orElse(null);
    }

    private void onLoginButtonAction(Event event) {
        // 登入
        eccService.login(publicKeyTextField.getText(), privateKeyPasswordField.getText());
        publicKeyTextField.setText("");
        privateKeyPasswordField.setText("");
        this.setResult(eccService.getPublicKey());
        this.close();
    }

    private void onGenerateButtonAction(Event event) {
        KeyPairHex keyPairHex = eccService.generateKeyPair();
        publicKeyTextField.setText(keyPairHex.getPublicKeyHex());
        privateKeyPasswordField.setText(keyPairHex.getPrivateKeyHex());
    }

    private void onReadButtonAction(Event event) {
        // 选取 JSON 文件
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择密钥对 Json 文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Json File (.json)", "*.json"));
        File file = fileChooser.showOpenDialog(getOwner());

        // 读取 JSON 文件
        if (file != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> keyPairHexMap = objectMapper.readValue(file, Map.class);
                publicKeyTextField.setText(keyPairHexMap.get("publicKeyHex"));
                privateKeyPasswordField.setText(keyPairHexMap.get("privateKeyHex"));
            } catch (Exception e) {
                throw new E2EchoException(E2EchoExceptionCodeEnum.ECC_READ_JSON_FILE_FAILED);
            }
        }
    }

    private void onSaveButtonAction(Event event) {
        // 获取 JSON 文件
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存密钥对 Json 文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Json File (.json)", "*.json"));
        File file = fileChooser.showSaveDialog(getOwner());

        // 读取 JSON 文件
        if (file != null) {
            try {
                objectMapper.writeValue(file,
                        new KeyPairHex(publicKeyTextField.getText(), privateKeyPasswordField.getText()));
            } catch (Exception e) {
                throw new E2EchoException(E2EchoExceptionCodeEnum.ECC_SAVE_JSON_FILE_FAILED);
            }
        }
    }

}
