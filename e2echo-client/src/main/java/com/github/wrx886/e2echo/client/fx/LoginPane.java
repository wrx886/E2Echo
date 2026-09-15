package com.github.wrx886.e2echo.client.fx;

import com.github.wrx886.e2echo.client.common.BaseUrlStore;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.result.ResultCodeEnum;
import com.github.wrx886.e2echo.ecc.Ecc;
import com.github.wrx886.e2echo.ecc.util.EccUtil;
import java.io.File;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.Getter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 登入界面。
 *
 * <p>由 {@link MainApplication} 放入登入窗口中显示，在 Spring 容器启动之前运行，用于收集服务器
 * 地址与 ECC 密钥对（公钥、私钥）并完成登入，因此不能使用容器中的任何 Bean。界面本身只依赖
 * JavaFX 与 JDK，网络请求使用 {@link WebClient} 这类不需要容器即可使用的工具类。</p>
 *
 * <p>登入成功后，服务器地址写入 {@link BaseUrlStore}、密钥对写入 {@link Ecc}，随后执行登入成功
 * 回调（由调用方决定如何关闭窗口）；登入结果通过 {@link #isLoggedIn()} 读取。</p>
 */
public class LoginPane extends VBox {

    /**
     * 允许的客户端与服务端时间偏差，单位毫秒。
     */
    private static final long MAX_TIMESTAMP_DIFF = 5 * 1000L;

    /**
     * 保存登入信息时默认使用的文件名。
     */
    private static final String LOGIN_FILE_NAME = "e2echo-login.json";

    /**
     * JSON 序列化器，用于读写登入信息文件。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 登入成功后的回调，由调用方提供。
     */
    private final Runnable onLoginSuccess;

    /**
     * 是否已登入成功。
     */
    @Getter
    private boolean loggedIn;

    /**
     * 服务器地址输入框。
     */
    private final TextField baseUrlField = new TextField();

    /**
     * 公钥输入框，内容为 RAW HEX 格式的 secp256k1 公钥，长度较大所以使用多行输入。
     */
    private final TextArea publicKeyField = new TextArea();

    /**
     * 私钥输入框，内容为 RAW HEX 格式的 secp256k1 私钥，长度较大所以使用多行输入。
     */
    private final TextArea privateKeyField = new TextArea();

    /**
     * 构建登入界面。
     *
     * @param onLoginSuccess 登入成功后的回调，例如关闭登入窗口
     */
    public LoginPane(Runnable onLoginSuccess) {

        super(18);
        this.onLoginSuccess = onLoginSuccess;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));

        Label title = new Label("客户端登入");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        configureInputs();

        Button loginBtn = new Button("登入");
        loginBtn.setDefaultButton(true);
        loginBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 24 6 24;");
        loginBtn.setOnAction(e -> onLogin());

        Button generateBtn = new Button("生成");
        generateBtn.setOnAction(e -> onGenerate());

        Button saveBtn = new Button("保存");
        saveBtn.setOnAction(e -> onSave());

        Button loadBtn = new Button("加载");
        loadBtn.setOnAction(e -> onLoad());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.add(new Label("服务器地址："), 0, 0);
        grid.add(baseUrlField, 1, 0);
        grid.add(new Label("公钥："), 0, 1);
        grid.add(publicKeyField, 1, 1);
        grid.add(new Label("私钥："), 0, 2);
        grid.add(privateKeyField, 1, 2);

        // 标签列不允许被压缩（Label 宽度小于首选宽度时会省略成 "..."），
        // 输入框列吸收窗口拉伸与压缩带来的宽度变化
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        inputColumn.setMinWidth(0);
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);

        // 两个密钥输入框占据窗口纵向的剩余空间，窗口拉伸时同步变大
        RowConstraints urlRow = new RowConstraints();
        RowConstraints keyRow = new RowConstraints();
        keyRow.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(urlRow, keyRow, keyRow);

        HBox buttons = new HBox(12, loginBtn, generateBtn, saveBtn, loadBtn);
        buttons.setAlignment(Pos.CENTER);

        getChildren().addAll(title, grid, buttons);
        VBox.setVgrow(grid, Priority.ALWAYS);
    }

    /**
     * 设置三个输入框的占位提示与通用样式。
     */
    private void configureInputs() {

        baseUrlField.setPromptText("请输入服务器地址，例如 http://localhost:8080");

        configureKeyInput(publicKeyField, "请输入公钥（RAW HEX 格式的 secp256k1 公钥）");
        configureKeyInput(privateKeyField, "请输入私钥（RAW HEX 格式的 secp256k1 私钥）");
    }

    /**
     * 设置密钥输入框的通用样式。
     *
     * @param keyField   密钥输入框
     * @param promptText 占位提示文本
     */
    private void configureKeyInput(TextArea keyField, String promptText) {
        keyField.setPromptText(promptText);
        keyField.setPrefRowCount(3);
        keyField.setWrapText(true);
    }

    /**
     * 读取三个输入框中的内容，并校验三者都不为空。
     *
     * @return 输入框中的登入信息
     * @throws E2EchoException 服务器地址、公钥或私钥为空
     */
    private LoginInfo readInputs() {

        LoginInfo loginInfo = new LoginInfo(
                baseUrlField.getText().trim(),
                publicKeyField.getText().trim(),
                privateKeyField.getText().trim()
        );
        if (loginInfo.baseUrl().isEmpty()
                || loginInfo.publicKey().isEmpty()
                || loginInfo.privateKey().isEmpty()) {
            throw new E2EchoException("服务器地址、公钥、私钥均不能为空！");
        }
        return loginInfo;
    }

    /**
     * 登入：校验输入内容，保存密钥对，并确认服务器可用。
     *
     * <p>流程为：读取并校验三个输入框 → 用 {@link Ecc#store(String, String)} 校验密钥对是否匹配
     * 并保存 → 请求服务器 {@code /timestamp} 接口，检查响应状态与两端的时间偏差 → 把服务器地址
     * 写入 {@link BaseUrlStore}，最后执行登入成功回调。</p>
     *
     * <p>可预期的错误统一以 {@link E2EchoException} 抛出，由全局异常处理器弹出提示。请求在 FX
     * 线程上同步执行，此时登入窗口还未进入主界面，短暂阻塞可以接受。</p>
     */
    private void onLogin() {

        LoginInfo loginInfo = readInputs();

        // 密钥对：用私钥签名再用公钥验签，只有匹配的密钥对才会保存成功
        try {
            Ecc.store(loginInfo.publicKey(), loginInfo.privateKey());
        } catch (Exception e) {
            throw new E2EchoException("密钥对错误！");
        }

        // 验证服务器：拉取服务器时间戳
        Result<String> result;
        try {
            result = WebClient.builder()
                    .baseUrl(loginInfo.baseUrl())
                    .build()
                    .get()
                    .uri("timestamp")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<String>>() {
                    })
                    .block();
        } catch (WebClientException e) {
            // 地址不可达、服务未启动等情况给出可读提示，而不是统一提示 FAIL
            throw new E2EchoException("无法连接服务器！");
        }

        // 服务器状态
        if (result == null || !ResultCodeEnum.OK.getCode().equals(result.code())) {
            throw new E2EchoException("服务器状态异常！");
        }

        // 时间误差：偏差过大时后续消息会因时间戳校验被服务端拒绝，这里提前拦截
        long serverTimestamp;
        try {
            serverTimestamp = Long.parseLong(result.data());
        } catch (NumberFormatException e) {
            throw new E2EchoException("服务器状态异常！");
        }
        if (Math.abs(System.currentTimeMillis() - serverTimestamp) > MAX_TIMESTAMP_DIFF) {
            throw new E2EchoException("客户端与服务端时间差距过大！");
        }

        // 登入成功：记录服务器地址供容器启动后构造 WebClient 使用，并把结果交给调用方
        BaseUrlStore.setBaseUrl(loginInfo.baseUrl());
        loggedIn = true;
        onLoginSuccess.run();
    }

    /**
     * 登入信息：服务器地址与 ECC 密钥对，同时也是保存到 JSON 文件中的结构。
     *
     * @param baseUrl    服务器地址
     * @param publicKey  公钥
     * @param privateKey 私钥
     */
    private record LoginInfo(String baseUrl, String publicKey, String privateKey) {
    }

    /**
     * 生成：随机生成一对新的 ECC 密钥对并填充到公钥、私钥输入框。
     */
    private void onGenerate() {
        EccUtil.KeyPairHex keyPairHex;
        try {
            keyPairHex = Ecc.generateKeyPair();
        } catch (Exception e) {
            throw new E2EchoException("密钥对生成失败！");
        }
        privateKeyField.setText(keyPairHex.privateKeyHex());
        publicKeyField.setText(keyPairHex.publicKeyHex());
    }

    /**
     * 保存：把当前输入的服务器地址与密钥对写入 JSON 文件。
     *
     * <p>先校验三个输入框非空，再弹出文件选择器由用户决定保存的目录与文件名，用户取消时不写入
     * 任何内容。文件内容形如：</p>
     *
     * <pre>
     * {
     *   "baseUrl" : "http://localhost:8080",
     *   "publicKey" : "04...",
     *   "privateKey" : "12..."
     * }
     * </pre>
     */
    private void onSave() {

        LoginInfo loginInfo = readInputs();

        File file = chooseSaveFile();
        if (file == null) {
            // 用户取消选择
            return;
        }

        writeLoginInfo(file, loginInfo);
    }

    /**
     * 把登入信息以 JSON 格式写入文件。
     *
     * @param file      目标文件
     * @param loginInfo 登入信息
     * @throws E2EchoException 写入失败
     */
    private void writeLoginInfo(File file, LoginInfo loginInfo) {

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, loginInfo);
        } catch (JacksonException e) {
            throw new E2EchoException("保存失败！");
        }
    }

    /**
     * 弹出文件选择器，让用户决定保存的目录与文件名。
     *
     * @return 用户选择的文件，取消选择时返回 {@code null}
     */
    private File chooseSaveFile() {

        FileChooser fileChooser = newFileChooser("保存登入信息");
        fileChooser.setInitialFileName(LOGIN_FILE_NAME);

        File file = fileChooser.showSaveDialog(owner());
        if (file == null) {
            return null;
        }

        // 部分平台的文件选择器不会自动补上扩展名，这里按扩展名过滤器的约定补一下
        String fileName = file.getName();
        return fileName.toLowerCase(Locale.ROOT).endsWith(".json")
                ? file
                : new File(file.getParentFile(), fileName + ".json");
    }

    /**
     * 弹出文件选择器，让用户选择要加载的文件。
     *
     * @return 用户选择的文件，取消选择时返回 {@code null}
     */
    private File chooseOpenFile() {
        return newFileChooser("加载登入信息").showOpenDialog(owner());
    }

    /**
     * 构造只允许选择 JSON 文件的文件选择器。
     *
     * @param title 对话框标题
     * @return 文件选择器
     */
    private FileChooser newFileChooser(String title) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        return fileChooser;
    }

    /**
     * 获取登入窗口，作为文件选择器的父窗口。
     *
     * @return 登入窗口，界面不在窗口中时返回 {@code null}
     */
    private Window owner() {
        return getScene() == null ? null : getScene().getWindow();
    }

    /**
     * 加载：从 JSON 文件读取服务器地址与密钥对，并填充到输入框。
     *
     * <p>读取的是 {@link #onSave()} 写出的文件，只负责把内容填进界面；密钥对是否匹配、服务器是否
     * 可用，等用户点击登入时再校验。用户取消选择时不改动当前界面内容。</p>
     */
    private void onLoad() {

        File file = chooseOpenFile();
        if (file == null) {
            // 用户取消选择
            return;
        }

        fillInputs(readLoginInfo(file));
    }

    /**
     * 从 JSON 文件读取登入信息。
     *
     * @param file 目标文件
     * @return 文件中的登入信息
     * @throws E2EchoException 文件格式错误或内容不完整
     */
    private LoginInfo readLoginInfo(File file) {

        LoginInfo loginInfo;
        try {
            loginInfo = objectMapper.readValue(file, LoginInfo.class);
        } catch (JacksonException e) {
            throw new E2EchoException("加载失败！");
        }

        // 文件可能被手工改动过，缺失的字段在这里拦下来，避免把空值填进输入框
        if (loginInfo == null
                || isBlank(loginInfo.baseUrl())
                || isBlank(loginInfo.publicKey())
                || isBlank(loginInfo.privateKey())) {
            throw new E2EchoException("文件内容不完整！");
        }
        return loginInfo;
    }

    /**
     * 把登入信息填充到三个输入框。
     *
     * @param loginInfo 登入信息
     */
    private void fillInputs(LoginInfo loginInfo) {
        baseUrlField.setText(loginInfo.baseUrl());
        publicKeyField.setText(loginInfo.publicKey());
        privateKeyField.setText(loginInfo.privateKey());
    }

    /**
     * 判断字符串是否为 {@code null} 或空白。
     *
     * @param value 待判断的字符串
     * @return 为 {@code null} 或只包含空白字符时返回 {@code true}
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
