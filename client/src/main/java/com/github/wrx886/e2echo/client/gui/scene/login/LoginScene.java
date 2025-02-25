package com.github.wrx886.e2echo.client.gui.scene.login;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.gui.common.SceneRouter;
import com.github.wrx886.e2echo.client.gui.scene.main.MainScene;
import com.github.wrx886.e2echo.client.service.LoginUserService;
import com.github.wrx886.e2echo.client.util.EccUtil;
import com.github.wrx886.e2echo.client.util.EccUtil.KeyPairHex;

import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

// 登入页面
public class LoginScene extends Scene {

    // 登入服务
    private LoginUserService loginUserService = BeanProvider.getBean(LoginUserService.class);

    // GridPane 内元素的外边距
    private static final Insets GRID_MARGIN = new Insets(5);

    // 场景路由
    private final SceneRouter sceneRouter;

    // 服务器地址标签
    private final Label urlLabel;

    // 服务器地址输入框
    private final TextField urlTesTextField;

    // 手机号标签
    private final Label phoneLabel;

    // 手机号输入框
    private final TextField phoneTextField;

    // 验证码标签
    private final Label phoneCodeLabel;

    // 验证码输入框
    private final TextField phoneCodeTextField;

    // 获取验证码
    private final Button phoneCodeGetButton;

    // 公钥标签
    private final Label publicKeyLabel;

    // 公钥输入框
    private final TextField publicKeyTextField;

    // 私钥标签
    private final Label privateKeyLabel;

    // 私钥输入框
    private final TextField privateKeyTextField;

    // 登入按钮
    private final Button loginButton;

    // 生成密钥对按钮
    private final Button generateKeyPairButton;

    // 构造函数
    public LoginScene(SceneRouter sceneRouter) {
        // 调用父类构造
        super(new GridPane());

        // 场景路由
        this.sceneRouter = sceneRouter;

        // 获取 GridPane
        GridPane root = (GridPane) getRoot();

        // 设置对齐方案：居中对齐
        root.setAlignment(Pos.CENTER);

        // 放置服务器地址标签
        urlLabel = new Label("服务器URL");
        GridPane.setMargin(urlLabel, GRID_MARGIN);
        root.add(urlLabel, 0, 0);

        // 放置服务器地址输入框
        urlTesTextField = new TextField();
        GridPane.setMargin(urlTesTextField, GRID_MARGIN);
        root.add(urlTesTextField, 1, 0);

        // 放置手机号标签
        phoneLabel = new Label("手机号");
        GridPane.setMargin(phoneLabel, GRID_MARGIN);
        root.add(phoneLabel, 0, 1);

        // 放置手机号输入框
        phoneTextField = new TextField();
        GridPane.setMargin(phoneTextField, GRID_MARGIN);
        root.add(phoneTextField, 1, 1);

        // 放置手机验证码标签
        phoneCodeLabel = new Label("验证码");
        GridPane.setMargin(phoneCodeLabel, GRID_MARGIN);
        root.add(phoneCodeLabel, 0, 2);

        // 构建 GridPane 用于存放验证码和验证码输入框
        GridPane phoneCodeGridPane = new GridPane();
        root.add(phoneCodeGridPane, 1, 2);

        // 放置手机验证码输入框
        phoneCodeTextField = new TextField();
        GridPane.setMargin(phoneCodeTextField, GRID_MARGIN);
        phoneCodeGridPane.add(phoneCodeTextField, 0, 0);

        // 获取手机验证码的按钮
        phoneCodeGetButton = new Button("获取");
        phoneCodeGetButton.setOnAction(this::onActionPhoneCodeGetButton);
        GridPane.setMargin(phoneCodeGetButton, GRID_MARGIN);
        phoneCodeGridPane.add(phoneCodeGetButton, 1, 0);

        // 公钥标签
        publicKeyLabel = new Label("公钥");
        GridPane.setMargin(publicKeyLabel, GRID_MARGIN);
        root.add(publicKeyLabel, 0, 3);

        // 公钥输入框
        publicKeyTextField = new TextField();
        GridPane.setMargin(publicKeyTextField, GRID_MARGIN);
        root.add(publicKeyTextField, 1, 3);

        // 私钥标签
        privateKeyLabel = new Label("私钥");
        GridPane.setMargin(privateKeyLabel, GRID_MARGIN);
        root.add(privateKeyLabel, 0, 4);

        // 私钥输入框
        privateKeyTextField = new TextField();
        GridPane.setMargin(privateKeyTextField, GRID_MARGIN);
        root.add(privateKeyTextField, 1, 4);

        // 登入按钮
        loginButton = new Button("登入");
        loginButton.setOnAction(this::onActionLoginButton);

        // 生成密钥对按钮
        generateKeyPairButton = new Button("生产密钥对");
        generateKeyPairButton.setOnAction(this::generateKeyPairButtonOnAction);

        // 按钮布局
        HBox buttonHBox = new HBox(loginButton, generateKeyPairButton);
        GridPane.setMargin(buttonHBox, GRID_MARGIN);
        GridPane.setHalignment(buttonHBox, HPos.CENTER);
        root.add(buttonHBox, 1, 5);
    }

    // 获取验证码
    private void onActionPhoneCodeGetButton(Event event) {
        loginUserService.getPhoneCode(urlTesTextField.getText(), phoneTextField.getText());
    }

    // 登入
    private void onActionLoginButton(Event event) {
        // 登入
        loginUserService.login(
                urlTesTextField.getText(),
                phoneTextField.getText(),
                phoneCodeTextField.getText(),
                publicKeyTextField.getText(),
                privateKeyTextField.getText());

        // 登入成功后跳转页面到主界面
        sceneRouter.push(MainScene.class);
    }

    // 生成密钥对
    private void generateKeyPairButtonOnAction(Event event) {
        if (publicKeyTextField.getText() == null || publicKeyTextField.getText().isEmpty() ||
                privateKeyTextField.getText() == null || privateKeyTextField.getText().isEmpty()) {
            KeyPairHex keyPair = EccUtil.generateKeyPair();
            publicKeyTextField.setText(keyPair.getPublicKeyHex());
            privateKeyTextField.setText(keyPair.getPrivateKeyHex());
        } else {
            throw new E2Echoxception(null, "密钥对输入栏不为空");
        }
    }
}
