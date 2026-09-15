package com.github.wrx886.e2echo.client.fx;

import java.util.Objects;

import com.github.wrx886.e2echo.client.exception.E2EchoException;

import javafx.scene.control.Alert;
import javafx.scene.text.Font;

/**
 * JavaFX 界面公共支持。
 *
 * <p>集中处理各个窗口共用的部分：加载中文字体、挂载全局样式表、安装全局异常处理器，以及创建带
 * 统一字体的弹窗。</p>
 *
 * <p>本类只依赖 JavaFX 与 JDK，在 Spring 容器启动前后都可以使用。</p>
 */
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
     * 安装 FX 全局异常处理器。
     *
     * <p>需要在 FX 应用线程上调用，安装后界面事件中抛出的未捕获异常都会进入本类的处理逻辑。</p>
     */
    public static void installExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(FxSupport::handleUncaughtException);
    }

    /**
     * 处理 FX 应用线程上未捕获的异常。
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

        // 用 show() 而不是 showAndWait()：异常可能发生在嵌套事件循环或布局过程中，
        // 这些场景下 showAndWait() 会抛 IllegalStateException
        alert(Alert.AlertType.ERROR, message).show();
    }
}
