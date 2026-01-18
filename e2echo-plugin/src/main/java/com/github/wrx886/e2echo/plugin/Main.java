package com.github.wrx886.e2echo.plugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.github.wrx886.e2echo.plugin.gui.GuiMain;

import javafx.application.Application;

@CrossOrigin(origins = "*")
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        // 设置 web 端口
        System.setProperty("server.port", isPortAvailable(35010) ? "35010" : "0");

        SpringApplication.run(Main.class, args);
        Application.launch(GuiMain.class, args);
    }

    private static boolean isPortAvailable(int port) {
        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
