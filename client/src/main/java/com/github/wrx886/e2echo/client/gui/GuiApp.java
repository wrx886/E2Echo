package com.github.wrx886.e2echo.client.gui;

import java.io.File;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.gui.common.SceneRouter;
import com.github.wrx886.e2echo.client.gui.scene.login.LoginScene;
import com.github.wrx886.e2echo.client.gui.scene.main.MainScene;
import com.github.wrx886.e2echo.client.store.MessageStore;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class GuiApp extends Application {

    private ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);

    private MessageStore messageStore = BeanProvider.getBean(MessageStore.class);

    // 场景路由
    private SceneRouter sceneRouter;

    @Override
    public void init() throws Exception {
        // 定义全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("出现一个错误");
                alert.setContentText(throwable.getMessage());
                alert.showAndWait();
            });
        });

        try {
            // 读取上次刷新时间
            MessageStore jsonMessageStore = objectMapper.readValue(new File("./messageStore.json"),
                    MessageStore.class);
            BeanUtils.copyProperties(jsonMessageStore, messageStore);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 构建场景路由
        sceneRouter = new SceneRouter(primaryStage);

        // 注册场景
        sceneRouter.register(new LoginScene(sceneRouter));
        sceneRouter.register(new MainScene(sceneRouter));

        // 放入默认页面
        sceneRouter.push(LoginScene.class);

        // 设置界面大小
        primaryStage.setTitle("E2Echo");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);

        // 显示界面
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        try {
            objectMapper.writeValue(new File("./messageStore.json"), messageStore);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
