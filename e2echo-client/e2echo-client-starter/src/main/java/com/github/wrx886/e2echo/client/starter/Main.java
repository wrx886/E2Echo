package com.github.wrx886.e2echo.client.starter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.ecc.dialog.LoginDialog;
import com.github.wrx886.e2echo.client.srv.dialog.WebUrlDialog;
import com.github.wrx886.e2echo.client.srv.store.MessageWebSocketClientStore;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Main extends Application {

    public final ApplicationContext applicationContext = BeanProvider.getApplicationContext();
    private final WebUrlDialog webUrlDialog = BeanProvider.getBean(WebUrlDialog.class);
    private final MessageWebSocketClientStore webSocketClientStore = BeanProvider.getBean(MessageWebSocketClientStore.class);

    @Override
    public void start(Stage stage) throws Exception {
        // 绑定关闭事件，关闭时退出 SpringBoot 上下文
        stage.setOnCloseRequest(event -> {
            throw new RuntimeException("Exit");
        });

        // 全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                if (throwable instanceof E2EchoException e) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("提示");
                    alert.setHeaderText(null);
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                } else {
                    log.error("", throwable);
                    webSocketClientStore.close();
                    SpringApplication.exit(applicationContext);
                    System.exit(1);
                }
            } catch (Throwable t) {
                // 降级处理
                t.printStackTrace();
            }
        });

        // 使用主题
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // 登录弹窗
        String publicKeyHex = LoginDialog.dialog();
        if (publicKeyHex == null) {
            // 退出程序
            throw new RuntimeException("Exit");
        }

        // 获取 Web URL
        if (!webUrlDialog.dialog()) {
            // 退出程序
            throw new RuntimeException("Exit");
        }

        stage.setTitle("E2Echo");
        stage.setWidth(800);
        stage.setHeight(600);
        stage.setResizable(false);
        stage.show();
    }

    @SpringBootApplication
    @ComponentScan("com.github.wrx886.e2echo.client")
    public final static class Starter {

        private static boolean test;

        @Value("${test:false}")
        public void setTest(boolean testValue) {
            test = testValue;
        }

        public static void main(String[] args) {
            SpringApplication.run(Starter.class, args);
            if (!test) {
                Application.launch(Main.class, args);
            }
        }
    }

}
