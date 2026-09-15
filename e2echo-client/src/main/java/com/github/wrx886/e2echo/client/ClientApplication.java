package com.github.wrx886.e2echo.client;

import com.github.wrx886.e2echo.client.fx.MainApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 客户端启动类。
 *
 * <p>一个进程只能有一套 JavaFX 会话（{@code Application.launch} 只能调用一次，工具箱退出后也无法
 * 重新启动），因此这里只启动唯一的界面程序 {@link MainApplication}，由它负责登入、启动本容器、
 * 打开主界面的完整流程。</p>
 */
@SpringBootApplication
public class ClientApplication {

    /**
     * 客户端入口。
     *
     * @param args 命令行参数，原样交给 JavaFX 与后续的 Spring Boot
     */
    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }

}
