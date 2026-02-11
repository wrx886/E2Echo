package com.github.wrx886.e2echo.client.starter;

import java.io.File;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.store.JsonStore;
import com.github.wrx886.e2echo.client.ecc.dialog.LoginDialog;
import com.github.wrx886.e2echo.client.gui.MainScene;
import com.github.wrx886.e2echo.client.srv.dialog.WebUrlDialog;
import com.github.wrx886.e2echo.client.srv.store.MessageWebSocketClientStore;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Main extends Application {

    private final ApplicationContext applicationContext = BeanProvider.getApplicationContext();
    private final WebUrlDialog webUrlDialog = BeanProvider.getBean(WebUrlDialog.class);
    private final MessageWebSocketClientStore webSocketClientStore = BeanProvider
            .getBean(MessageWebSocketClientStore.class);
    private final JsonStore jsonStore = BeanProvider.getBean(JsonStore.class);
    private final ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);
    private final EccController eccController = BeanProvider.getBean(EccController.class);

    @Override
    public void start(Stage stage) throws Exception {
        // 绑定关闭事件，关闭时退出 SpringBoot 上下文
        stage.setOnCloseRequest(event -> {
            throw new RuntimeException("Exit");
        });

        // 全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof E2EchoException e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("提示");
                    alert.setHeaderText(null);
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            } else {
                log.error("", throwable);

                try {
                    webSocketClientStore.close();
                } catch (Throwable t) {
                }

                // 保存到 json 文件
                try {
                    File dir = new File("./JsonStore");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    objectMapper.writeValue(new File("./JsonStore/" + eccController.getPublicKey() + ".json"),
                            jsonStore);
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                SpringApplication.exit(applicationContext);
                System.exit(1);
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

        // 读取对应的 json
        File file = new File("./JsonStore/" + publicKeyHex + ".json");
        if (file.exists()) {
            JsonStore jsonStoreRead = objectMapper.readValue(file, JsonStore.class);
            BeanUtils.copyProperties(jsonStoreRead, jsonStore);
        }

        // 获取 Web URL
        if (!webUrlDialog.dialog()) {
            // 退出程序
            throw new RuntimeException("Exit");
        }

        // 启动 WebSocket
        Platform.runLater(() -> {
            webSocketClientStore.getClient();
        });

        // 设置场景
        stage.setScene(new MainScene());

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
