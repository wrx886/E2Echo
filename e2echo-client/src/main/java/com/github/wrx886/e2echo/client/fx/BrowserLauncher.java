package com.github.wrx886.e2echo.client.fx;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javafx.application.HostServices;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统浏览器启动器。
 *
 * <p>优先调用各操作系统自带的命令行工具打开网页：Windows 使用 {@code rundll32}，macOS 使用
 * {@code open}，其他系统使用 {@code xdg-open}。这些工具由系统或桌面环境直接提供，不依赖 JavaFX
 * 的本地实现，也不要求 Linux 必须运行在 GNOME 桌面上。</p>
 *
 * <p>命令不存在、启动失败或退出码非零时，回落到 {@link HostServices#showDocument(String)}。
 * 命令在后台进程中执行，不会阻塞 FX 应用线程。</p>
 */
@Slf4j
public final class BrowserLauncher {

    /**
     * 私有构造方法，防止外部实例化工具类。
     */
    private BrowserLauncher() {
    }

    /**
     * 使用系统默认浏览器打开网页。
     *
     * @param url          网页地址
     * @param hostServices JavaFX 宿主服务，作为系统命令不可用时的兜底
     */
    public static void open(String url, HostServices hostServices) {

        List<String> command = command(url);
        try {
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            process.onExit().thenAccept(finished -> {
                if (finished.exitValue() != 0) {
                    log.warn("系统浏览器命令 {} 退出码为 {}，回落到 HostServices",
                            command.get(0), finished.exitValue());
                    fallback(url, hostServices);
                }
            });
        } catch (IOException e) {
            // 命令不存在（例如精简的 Linux 环境没有 xdg-open）时直接兜底
            log.warn("无法启动系统浏览器命令 {}，回落到 HostServices", command.get(0), e);
            fallback(url, hostServices);
        }
    }

    /**
     * 根据当前操作系统选择打开网页的命令。
     *
     * @param url 网页地址
     * @return 命令及参数
     */
    static List<String> command(String url) {

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.startsWith("windows")) {
            // rundll32 直接调用 ShellExecute，不依赖 cmd.exe，也不需要额外转义 URL
            return List.of("rundll32", "url.dll,FileProtocolHandler", url);
        }
        if (osName.startsWith("mac") || osName.startsWith("darwin")) {
            return List.of("open", url);
        }
        // xdg-open 是 freedesktop.org 提供的标准入口，GNOME、KDE 等桌面环境都有实现
        return List.of("xdg-open", url);
    }

    /**
     * 回落到 JavaFX 宿主服务打开网页。
     *
     * @param url          网页地址
     * @param hostServices JavaFX 宿主服务
     */
    private static void fallback(String url, HostServices hostServices) {

        // showDocument 需要在 FX 应用线程上调用，而命令退出回调运行在后台线程
        if (Platform.isFxApplicationThread()) {
            showDocument(url, hostServices);
            return;
        }
        try {
            Platform.runLater(() -> showDocument(url, hostServices));
        } catch (IllegalStateException e) {
            // FX 工具包已经退出（例如程序正在关闭），此时无法再弹出浏览器
            log.error("无法在 FX 应用线程上打开网页：{}", url, e);
        }
    }

    /**
     * 调用宿主服务打开网页。
     *
     * @param url          网页地址
     * @param hostServices JavaFX 宿主服务
     */
    private static void showDocument(String url, HostServices hostServices) {

        try {
            hostServices.showDocument(url);
        } catch (RuntimeException e) {
            log.error("使用 HostServices 打开网页失败：{}", url, e);
        }
    }

}
