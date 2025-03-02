package com.github.wrx886.e2echo.client.gui;

import com.github.wrx886.e2echo.client.gui.common.SceneRouter;
import com.github.wrx886.e2echo.client.gui.scene.login.LoginScene;
import com.github.wrx886.e2echo.client.gui.scene.main.MainScene;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class GuiApp extends Application {

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
        primaryStage.setWidth(1000);
        primaryStage.setHeight(750);

        // 显示界面
        primaryStage.show();
    }

}
