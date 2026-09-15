package com.github.wrx886.e2echo.client.fx;

import com.github.wrx886.e2echo.client.api.TimestampApi;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.service.AuthService;
import com.github.wrx886.e2echo.ecc.Ecc;

import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.core.env.Environment;

/**
 * 主界面。
 *
 * <p>由 {@link MainApplication} 放入主窗口中显示，运行在 Spring 容器启动之后，展示当前用户的公钥，
 * 并提供三个操作：打开网页、测试与服务端的连通性、退出程序。界面自身不做业务判断，具体逻辑都
 * 委托给容器中的 Bean（例如 {@link AuthService}、{@link TimestampApi}），需要的 Bean 由构造方法从
 * 外部传入。</p>
 */
public class MainPane extends VBox {

    /**
     * 复制按钮的宽度，固定住可以避免按钮文字在“复制”和“已复制”之间切换时界面抖动。
     */
    private static final double COPY_BUTTON_WIDTH = 70;

    /**
     * 宿主服务，用于调用系统默认浏览器打开网页。
     */
    private final HostServices hostServices;

    /**
     * 内嵌 Web 容器的运行环境，用于读取实际端口。
     */
    private final Environment environment;

    /**
     * 认证业务逻辑，用于生成一次性 auth 票据。
     */
    private final AuthService authService;

    /**
     * 时间戳接口，用于测试与服务端的连通性。
     */
    private final TimestampApi timestampApi;

    /**
     * 构建主界面。
     *
     * @param onExit       退出动作
     * @param hostServices 宿主服务
     * @param environment  内嵌 Web 容器的运行环境
     * @param authService  认证业务逻辑
     * @param timestampApi 时间戳接口
     */
    public MainPane(Runnable onExit, HostServices hostServices, Environment environment,
                    AuthService authService, TimestampApi timestampApi) {

        super(18);
        this.hostServices = hostServices;
        this.environment = environment;
        this.authService = authService;
        this.timestampApi = timestampApi;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));

        // 公钥：只读展示，长度较大所以用多行输入框，旁边放一个复制按钮
        Label publicKeyLabel = new Label("公钥：");
        publicKeyLabel.setMinWidth(Region.USE_PREF_SIZE);

        TextArea publicKeyArea = new TextArea(publicKey());
        publicKeyArea.setEditable(false);
        publicKeyArea.setWrapText(true);
        publicKeyArea.setPrefRowCount(4);
        publicKeyArea.setMaxWidth(Double.MAX_VALUE);
        // TextArea 的最小宽度默认等于首选宽度，会把同一行的标签和按钮挤扁，
        // 这里放开最小宽度让它吸收窗口宽度的变化
        publicKeyArea.setMinWidth(0);
        HBox.setHgrow(publicKeyArea, Priority.ALWAYS);

        Button copyBtn = new Button("复制");
        copyBtn.setPrefWidth(COPY_BUTTON_WIDTH);
        copyBtn.setMinWidth(Region.USE_PREF_SIZE);
        copyBtn.setOnAction(e -> onCopyPublicKey(publicKeyArea.getText(), copyBtn));

        HBox publicKeyRow = new HBox(10, publicKeyLabel, publicKeyArea, copyBtn);
        publicKeyRow.setAlignment(Pos.CENTER);
        publicKeyRow.setMaxWidth(Double.MAX_VALUE);

        Button openWebBtn = new Button("打开网页");
        openWebBtn.setDefaultButton(true);
        openWebBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 24 6 24;");
        openWebBtn.setOnAction(e -> onOpenWeb());

        Button testBtn = new Button("测试");
        testBtn.setOnAction(e -> onTest());

        Button exitBtn = new Button("退出");
        exitBtn.setOnAction(e -> onExit.run());

        // 三个按钮等分窗口宽度，窗口变宽时按钮同步变宽
        GridPane buttons = new GridPane();
        buttons.setHgap(12);
        buttons.setMaxWidth(Double.MAX_VALUE);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 3);
            column.setHgrow(Priority.ALWAYS);
            buttons.getColumnConstraints().add(column);
        }

        for (Button button : new Button[]{openWebBtn, testBtn, exitBtn}) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        buttons.add(openWebBtn, 0, 0);
        buttons.add(testBtn, 1, 0);
        buttons.add(exitBtn, 2, 0);

        getChildren().addAll(publicKeyRow, buttons);
    }

    /**
     * 获取当前登入用户的公钥。
     *
     * @return RAW HEX 格式的 secp256k1 公钥，未保存密钥对时返回空串
     */
    private String publicKey() {
        String publicKey = Ecc.getPublicKey();
        return publicKey == null ? "" : publicKey;
    }

    /**
     * 复制公钥：写入系统剪贴板，并短暂把按钮文字改成“已复制”作为反馈。
     *
     * @param publicKey 公钥
     * @param copyBtn   复制按钮
     */
    private void onCopyPublicKey(String publicKey, Button copyBtn) {

        ClipboardContent content = new ClipboardContent();
        content.putString(publicKey);
        Clipboard.getSystemClipboard().setContent(content);

        copyBtn.setText("已复制");
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> copyBtn.setText("复制"));
        pause.play();
    }

    /**
     * 打开网页：在系统默认浏览器中打开登录地址。
     *
     * <p>登录地址为 {@code http://localhost:{端口}/auth/{票据}}，其中端口是内嵌 Web 容器的真实端口
     * （配置为 0 时为随机端口），票据是每次点击都重新生成的一次性 auth 票据。浏览器访问该地址后
     * 由服务端完成认证，并自动跳转到站点首页。</p>
     */
    private void onOpenWeb() {

        String port = environment.getProperty("local.server.port");
        String auth = authService.newAndGetAuth();

        hostServices.showDocument("http://localhost:" + port + "/auth/" + auth);
    }

    /**
     * 测试：调用服务端时间戳接口，确认与服务端的连通性并展示两端的时间偏差。
     *
     * <p>请求失败时 {@link TimestampApi#timestamp()} 抛出 {@link E2EchoException}，由全局异常处理器
     * 弹出提示，这里只处理请求成功的情况。</p>
     */
    private void onTest() {

        String timestamp = timestampApi.timestamp();

        long diff;
        try {
            diff = Math.abs(System.currentTimeMillis() - Long.parseLong(timestamp));
        } catch (NumberFormatException e) {
            throw new E2EchoException("服务器时间戳格式错误！");
        }

        FxSupport.alert(Alert.AlertType.INFORMATION, "连通正常，与服务端时间相差 " + diff + " 毫秒").show();
    }

}
