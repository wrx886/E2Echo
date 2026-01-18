package com.github.wrx886.e2echo.plugin.gui;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import com.github.wrx886.e2echo.plugin.util.BeanProviderUtil;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

// GUI 主程序
public class GuiMain extends Application {

    public final ApplicationContext applicationContext = BeanProviderUtil.getApplicationContext();

    @Override
    public void start(Stage stage) throws Exception {
        // 绑定关闭事件，关闭时退出 SpringBoot 上下文
        stage.setOnCloseRequest(event -> SpringApplication.exit(applicationContext));

        Label label = new Label("欢迎使用 JavaFX!");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 300);
        stage.setTitle("JavaFX 示例");
        stage.setScene(scene);
        stage.show();
    }

}
