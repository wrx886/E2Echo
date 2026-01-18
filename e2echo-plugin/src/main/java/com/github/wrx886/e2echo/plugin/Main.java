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
        SpringApplication.run(Main.class, args);
        Application.launch(GuiMain.class, args);
    }

}
