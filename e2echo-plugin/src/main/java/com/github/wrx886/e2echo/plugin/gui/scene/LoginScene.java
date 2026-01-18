package com.github.wrx886.e2echo.plugin.gui.scene;

import java.io.File;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.plugin.gui.common.BeanProvider;
import com.github.wrx886.e2echo.plugin.gui.common.SceneRouter;
import com.github.wrx886.e2echo.plugin.result.E2EchoException;
import com.github.wrx886.e2echo.plugin.result.ResultCodeEnum;
import com.github.wrx886.e2echo.plugin.service.EccService;
import com.github.wrx886.e2echo.plugin.util.EccUtil.KeyPairHex;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class LoginScene extends Scene {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final EccService eccService = BeanProvider.getBean(EccService.class);
    private final TextField publicKeyTextField;
    private final PasswordField privateKeyPasswordField;
    private final SceneRouter sceneRouter;

    public LoginScene(SceneRouter sceneRouter) {
        super(new GridPane());

        this.sceneRouter = sceneRouter;

        // 获取根节点
        GridPane root = (GridPane) getRoot();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setHgap(10);
        root.setVgap(10);

        // 放置登入提示
        Label infoLabel = new Label("欢迎使用 E2Echo 插件");
        infoLabel.setAlignment(Pos.CENTER);
        infoLabel.setStyle("-fx-font-size: 20px;");
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
    }

    private void onLoginButtonAction(Event event) {
        // 登入
        eccService.login(publicKeyTextField.getText(), privateKeyPasswordField.getText());
        publicKeyTextField.setText("");
        privateKeyPasswordField.setText("");

        // 跳转
        sceneRouter.push(MainScene.class);
        sceneRouter.get(MainScene.class).updatePublicKey(eccService.getPublicKey());
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
        File file = fileChooser.showOpenDialog(getWindow());

        // 读取 JSON 文件
        if (file != null) {
            try {
                KeyPairHex keyPairHex = objectMapper.readValue(file, KeyPairHex.class);
                publicKeyTextField.setText(keyPairHex.getPublicKeyHex());
                privateKeyPasswordField.setText(keyPairHex.getPrivateKeyHex());
            } catch (Exception e) {
                throw new E2EchoException(ResultCodeEnum.GUI_READ_JSON_FILE_FAILED);
            }
        }
    }

    private void onSaveButtonAction(Event event) {
        // 获取 JSON 文件
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存密钥对 Json 文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Json File (.json)", "*.json"));
        File file = fileChooser.showSaveDialog(getWindow());

        // 读取 JSON 文件
        if (file != null) {
            try {
                objectMapper.writeValue(file,
                        new KeyPairHex(publicKeyTextField.getText(), privateKeyPasswordField.getText()));
            } catch (Exception e) {
                throw new E2EchoException(ResultCodeEnum.GUI_SAVE_JSON_FILE_FAILED);
            }
        }
    }

}
