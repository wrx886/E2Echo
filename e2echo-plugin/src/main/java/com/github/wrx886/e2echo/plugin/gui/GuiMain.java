package com.github.wrx886.e2echo.plugin.gui;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import com.github.wrx886.e2echo.plugin.gui.common.BeanProvider;
import com.github.wrx886.e2echo.plugin.gui.common.SceneRouter;
import com.github.wrx886.e2echo.plugin.gui.scene.LoginScene;
import com.github.wrx886.e2echo.plugin.gui.scene.MainScene;
import com.github.wrx886.e2echo.plugin.result.E2EchoException;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

// GUI 主程序
@Slf4j
public class GuiMain extends Application {

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

        // 场景路由
        SceneRouter sceneRouter = new SceneRouter(stage);
        sceneRouter.register(new LoginScene(sceneRouter));
        sceneRouter.register(new MainScene(sceneRouter, getHostServices()));
        sceneRouter.push(LoginScene.class);

        stage.setTitle("E2Echo 插件");
        stage.setWidth(320);
        stage.setHeight(240);
        stage.show();
    }

}
