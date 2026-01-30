package com.github.wrx886.e2echo.client.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.ecc.dialog.LoginDialog;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Main extends Application {

    public final ApplicationContext applicationContext = BeanProvider.getApplicationContext();

    @Override
    public void start(Stage stage) throws Exception {
        // 绑定关闭事件，关闭时退出 SpringBoot 上下文
        stage.setOnCloseRequest(event -> SpringApplication.exit(applicationContext));

        // 全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof E2EchoException e) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("提示");
                alert.setHeaderText(null);
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            } else {
                log.error("", throwable);
                SpringApplication.exit(applicationContext);
                System.exit(1);
            }
        });

        // 使用主题
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // 登录弹窗
        String publicKeyHex = LoginDialog.dialog();
        if(publicKeyHex == null) {
            // 退出程序
            SpringApplication.exit(applicationContext);
            System.exit(0);
        }

        stage.setTitle("E2Echo");
        stage.setWidth(800);
        stage.setHeight(600);
        stage.show();
    }

    @SpringBootApplication
    @ComponentScan("com.github.wrx886.e2echo.client")
    public final static class Starter {
        public static void main(String[] args) {
            SpringApplication.run(Starter.class, args);
            Application.launch(Main.class, args);
        }
    }

}
