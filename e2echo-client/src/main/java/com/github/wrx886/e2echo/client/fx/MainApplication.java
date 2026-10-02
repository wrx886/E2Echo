package com.github.wrx886.e2echo.client.fx;

import com.github.wrx886.e2echo.client.ClientApplication;
import com.github.wrx886.e2echo.client.api.TimestampApi;
import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.ContextClosedEventHandler;
import com.github.wrx886.e2echo.client.enums.SysParamEnum;
import com.github.wrx886.e2echo.client.service.AuthService;

import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.service.SysParamService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;

/**
 * 客户端主程序。
 *
 * <p>一个进程只能有一套 JavaFX 会话（{@code Application.launch} 只能调用一次，工具箱退出后也无法
 * 重新启动），所以整个客户端只保留本类这一个 {@link Application}，由它统一安排启动流程：</p>
 *
 * <ol>
 *     <li>弹出登入窗口并等待用户登入；</li>
 *     <li>登入成功后启动 Spring 容器，容器从
 *         {@link com.github.wrx886.e2echo.client.common.BaseUrlStore} 与
 *         {@link com.github.wrx886.e2echo.ecc.Ecc} 读取登入信息；</li>
 *     <li>打开主窗口，并负责退出程序。</li>
 * </ol>
 */
@Slf4j
public class MainApplication extends Application {

    /**
     * 登入窗口的初始宽度。
     */
    private static final int LOGIN_WIDTH = 520;

    /**
     * 登入窗口的初始高度。
     */
    private static final int LOGIN_HEIGHT = 460;

    /**
     * 主窗口的初始宽度。
     */
    private static final int MAIN_WIDTH = 480;

    /**
     * 主窗口的初始高度。
     */
    private static final int MAIN_HEIGHT = 240;

    /**
     * 启动客户端：先登入，再启动容器，最后打开主界面。
     *
     * @param stage 主窗口，由 JavaFX 启动器创建
     */
    @Override
    public void start(Stage stage) {

        FxSupport.init();

        // 登入窗口关闭、主窗口打开之前，进程会短暂处于“没有窗口”的状态，
        // 隐式退出会在这时结束 JavaFX，所以关闭它，统一由 exit() 结束程序
        Platform.setImplicitExit(false);

        // 登入：窗口关闭后通过 isLoggedIn() 判断用户是否完成登入
        Stage loginStage = new Stage();
        LoginPane loginPane = new LoginPane(loginStage::close);
        configure(loginStage, "E2Echo 客户端登入", loginPane, LOGIN_WIDTH, LOGIN_HEIGHT);
        loginStage.showAndWait();
        if (!loginPane.isLoggedIn()) {
            // 用户没有完成登入，直接结束程序
            Platform.exit();
            return;
        }

        // 启动容器：服务器地址与密钥对已由登入界面准备好
        // 会话 cookie 名按登入用户派生（cookie 不区分端口，随机名又会在浏览器里越积越多）
        ClientApplication.configureSessionCookieName();
        SpringApplication.run(ClientApplication.class, getParameters().getRaw().toArray(String[]::new));

        // 主界面：需要的容器 Bean 在这里取出来交给界面，界面本身不再依赖 BeanProvider
        MainPane mainPane = new MainPane(this::exit, getHostServices(),
                BeanProvider.getBean(Environment.class),
                BeanProvider.getBean(AuthService.class),
                BeanProvider.getBean(TimestampApi.class));
        configure(stage, "E2Echo 客户端", mainPane, MAIN_WIDTH, MAIN_HEIGHT);

        // 隐式退出已关闭，直接关闭主窗口也要结束容器，否则进程会留在后台
        stage.setOnCloseRequest(e -> exit());
        stage.show();

        // 占用当前用户：放在窗口显示之后（界面已经出来，出错再弹窗）；
        // 释放占用的清理任务注册到容器关闭事件上，命令行关闭也能执行
        Platform.runLater(() -> {
            SysParamService sysParamService = BeanProvider.getBean(SysParamService.class);
            MessageService messageService = BeanProvider.getBean(MessageService.class);
            ContextClosedEventHandler contextClosedEventHandler = BeanProvider.getBean(ContextClosedEventHandler.class);
            try {
                sysParamService.putIfAbsent(SysParamEnum.IS_USED, Boolean.TRUE.toString());
                contextClosedEventHandler.addCleanTask(() -> {
                    // 移除对当前用户的占用（注册在占用成功之后，失败退出时不会误删别人的占用）
                    sysParamService.remove(SysParamEnum.IS_USED);
                });
                // 确认用户可用之后才建立通知通道
                messageService.connectNotice(); // 这里要确保允许登入才可以拉拉取消息
            } catch (Exception e) {
                log.error("占用登入状态失败，可能是该用户已在运行。", e);
                FxSupport.alert(Alert.AlertType.ERROR, "该用户已登入！").showAndWait();
                exit();
            }
        });
    }

    /**
     * 退出程序：结束 JavaFX 并关闭 Spring 容器。
     *
     * <p>容器中的 Web 线程是非守护线程，只结束 JavaFX 的话进程会继续存活，所以还需要关闭容器，
     * 让 {@code main} 返回后进程正常结束。</p>
     */
    private void exit() {
        Platform.exit();
        SpringApplication.exit(BeanProvider.getApplicationContext());
    }

    /**
     * 设置窗口的标题、场景、初始尺寸与全局样式表。
     *
     * <p>场景的尺寸只是窗口的初始尺寸，窗口显示时 JavaFX 会按场景的首选尺寸重新计算，窗口管理器
     * 也可能介入，所以这里把尺寸显式设置到窗口上：窗口的宽高一旦被显式设置，就会优先于场景尺寸
     * 生效。</p>
     *
     * <p>界面按固定尺寸设计（例如公钥展示区的高度），因此窗口不允许调整大小，避免窗口被改小后
     * 内容被裁掉。</p>
     *
     * @param window 窗口
     * @param title  窗口标题
     * @param root   界面根节点
     * @param width  窗口宽度
     * @param height 窗口高度
     */
    private void configure(Stage window, String title, Parent root, double width, double height) {

        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(FxSupport.stylesheet());

        // 不允许调整大小：窗口尺寸完全由这里的配置决定
        window.setResizable(false);
        window.setTitle(title);
        window.setScene(scene);
        window.setWidth(width);
        window.setHeight(height);
    }

}
