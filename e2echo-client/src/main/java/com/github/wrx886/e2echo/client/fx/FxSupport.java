package com.github.wrx886.e2echo.client.fx;

import java.util.Objects;

import com.github.wrx886.e2echo.client.exception.E2EchoException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

/**
 * JavaFX 界面公共支持。
 *
 * <p>集中处理各个窗口共用的部分：加载中文字体、挂载全局样式表、安装全局异常处理器，以及创建带
 * 统一字体的弹窗。</p>
 *
 * <p>本类只依赖 JavaFX 与 JDK，在 Spring 容器启动前后都可以使用。</p>
 */
@Slf4j
public final class FxSupport {

    /**
     * 界面字体资源路径。
     */
    private static final String FONT_RESOURCE = "/font/NotoSerifSC/SubsetOTF/SC/NotoSerifSC-Light.otf";

    /**
     * 全局样式表路径，界面字体在其中定义。
     */
    private static final String STYLESHEET = "/css/global-styles.css";

    /**
     * 私有构造方法，防止外部实例化工具类。
     */
    private FxSupport() {
    }

    /**
     * 初始化界面公共部分：加载字体并安装全局异常处理器。
     *
     * <p>需要在 FX 应用线程上调用，例如各个 {@code Application#start} 的开头。</p>
     */
    public static void init() {
        loadFont();
        installExceptionHandler();
    }

    /**
     * 加载界面字体。
     *
     * <p>只负责把字体文件注册到 JavaFX，字体族由全局样式表引用。</p>
     */
    public static void loadFont() {
        Font.loadFonts(FxSupport.class.getResourceAsStream(FONT_RESOURCE), -1);
    }

    /**
     * 获取全局样式表的地址。
     *
     * @return 样式表地址
     */
    public static String stylesheet() {
        return Objects.requireNonNull(FxSupport.class.getResource(STYLESHEET)).toExternalForm();
    }

    /**
     * 创建带全局字体的弹窗。
     *
     * <p>弹窗拥有独立的场景图，不会继承窗口上的样式，需要单独挂上全局样式表。</p>
     *
     * @param type    弹窗类型
     * @param message 提示内容
     * @return 弹窗
     */
    public static Alert alert(Alert.AlertType type, String message) {

        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误" : "提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(stylesheet());
        return alert;
    }

    /**
     * 安装全局异常处理器。
     *
     * <p>注册为所有线程的默认未捕获异常处理器：控制器里抛出的异常由 Spring 的全局异常处理器返回
     * 统一的失败结果，不会走到这里；其他线程（界面事件、通知长连接、后台任务等）未捕获的异常都会
     * 进入本类的处理逻辑，弹窗提示用户。</p>
     *
     * <p>需要在 FX 应用线程上调用（例如各个 {@code Application#start} 的开头），这样设置默认处理器
     * 之后，FX 应用线程自己抛出的异常也能被捕获。</p>
     */
    public static void installExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(FxSupport::handleUncaughtException);
    }

    /**
     * 处理未捕获的异常。
     *
     * <p>业务异常 {@link E2EchoException} 的信息是给用户看的，直接展示；其他异常属于未预期的
     * 错误，界面上只提示 {@code FAIL}，堆栈打印到标准错误输出，便于在控制台排查。</p>
     *
     * @param thread    抛出异常的线程
     * @param throwable 未捕获的异常
     */
    private static void handleUncaughtException(Thread thread, Throwable throwable) {

        // 未预期的异常保留堆栈，控制台是排查它们的唯一线索
        if (!(throwable instanceof E2EchoException)) {
            throwable.printStackTrace(System.err);
        }

        String message = throwable instanceof E2EchoException && throwable.getMessage() != null
                ? throwable.getMessage()
                : "FAIL";

        // 弹窗只能在 FX 应用线程上创建，而异常可能来自任意线程，所以这里切回 FX 应用线程
        if (Platform.isFxApplicationThread()) {
            showError(message);
            return;
        }
        try {
            Platform.runLater(() -> showError(message));
        } catch (Exception e) {
            // FX 工具箱尚未启动或已经退出（例如容器启动阶段的线程），此时无法弹窗
            log.error("无法在 FX 应用线程上弹出异常提示", e);
        }
    }

    /**
     * 在 FX 应用线程上弹出异常提示。
     *
     * <p>弹窗自身出错时只记录日志，不再往外抛：异常处理器里再抛异常会再次进入处理器，可能造成
     * 无限弹窗。</p>
     *
     * @param message 提示内容
     */
    private static void showError(String message) {
        try {
            // 用 show() 而不是 showAndWait()：异常可能发生在嵌套事件循环或布局过程中，
            // 这些场景下 showAndWait() 会抛 IllegalStateException
            alert(Alert.AlertType.ERROR, message).show();
        } catch (Exception e) {
            log.error("弹出异常提示失败", e);
        }
    }
}
