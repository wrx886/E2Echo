package com.github.wrx886.e2echo.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.github.wrx886.e2echo.client.gui.GuiApp;
import javafx.application.Application;

@SpringBootApplication
public class ClientStarter {

    public static void main(String[] args) {
        SpringApplication.run(ClientStarter.class, args);
        Application.launch(GuiApp.class);
    }

}