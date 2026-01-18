package com.github.wrx886.e2echo.plugin.gui.scene;

import org.springframework.boot.autoconfigure.web.ServerProperties;

import com.github.wrx886.e2echo.plugin.gui.common.BeanProvider;
import com.github.wrx886.e2echo.plugin.gui.common.SceneRouter;
import com.github.wrx886.e2echo.plugin.service.EccService;

import javafx.application.HostServices;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;

public class MainScene extends Scene {

    private final EccService eccService = BeanProvider.getBean(EccService.class);
    private final ServerProperties serverProperties = BeanProvider.getBean(ServerProperties.class);
    private final TextField publicKeyTextField;

    public MainScene(SceneRouter sceneRouter, HostServices hostServices) {
        super(new GridPane());

        // 获取根节点
        GridPane root = (GridPane) getRoot();
        root.setPadding(new Insets(10));
        root.setVgap(10);
        root.setHgap(10);
        root.setAlignment(Pos.CENTER);

        // 标题
        Label titleLabel = new Label("E2Echo 插件");
        titleLabel.setStyle("-fx-font-size: 20px;");
        GridPane.setHalignment(titleLabel, HPos.CENTER);
        root.add(titleLabel, 0, 0, 2, 1);

        // 注销按钮
        Button logoutButton = new Button("注销");
        root.add(logoutButton, 2, 0);
        logoutButton.setOnAction(event -> {
            eccService.logout();
            sceneRouter.push(LoginScene.class);
        });

        // 公钥标签
        Label publicKeyLabel = new Label("公钥");
        root.add(publicKeyLabel, 0, 1);

        // 公钥显示框
        publicKeyTextField = new TextField();
        publicKeyTextField.setEditable(false);
        root.add(publicKeyTextField, 1, 1);

        // 复制按钮
        Button copyButton = new Button("复制");
        copyButton.setOnAction(event -> {
            // 复制公钥
            ClipboardContent content = new ClipboardContent();
            content.putString(publicKeyTextField.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        root.add(copyButton, 2, 1);

        // 显示访问地址
        Label addressLabel = new Label("访问地址");
        root.add(addressLabel, 0, 2);

        // 访问地址
        TextField addressTextField = new TextField("http://localhost:" + serverProperties.getPort());
        addressTextField.setEditable(false);
        root.add(addressTextField, 1, 2);

        // 复制按钮
        Button addressCopyButton = new Button("复制");
        addressCopyButton.setOnAction(event -> {
            // 复制公钥
            ClipboardContent content = new ClipboardContent();
            content.putString(addressTextField.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        root.add(addressCopyButton, 2, 2);

        // API 文档标签
        Label apiLabel = new Label("API 文档");
        root.add(apiLabel, 0, 3);

        // API 文档地址
        TextField apiTextField = new TextField(addressTextField.getText() + "/doc.html");
        apiTextField.setEditable(false);
        root.add(apiTextField, 1, 3);

        // 打开 API 文档按钮
        Button openApiButton = new Button("打开");
        openApiButton.setOnAction(event -> {
            // 网页浏览器打开
            hostServices.showDocument(
                    addressTextField.getText() + "/doc.html");
        });
        root.add(openApiButton, 2, 3);

    }

    public void updatePublicKey(String publicKey) {
        publicKeyTextField.setText(publicKey);
    }

}
