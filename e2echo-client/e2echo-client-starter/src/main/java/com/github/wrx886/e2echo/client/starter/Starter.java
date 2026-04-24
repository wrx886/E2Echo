package com.github.wrx886.e2echo.client.starter;

import javafx.application.Application;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.github.wrx886.e2echo.client")
public class Starter {

    private static boolean test;

    @Value("${test:false}")
    public void setTest(boolean testValue) {
        test = testValue;
    }

    public static void main(String[] args) {
        SpringApplication.run(Starter.class, args);
        if (!test) {
            Application.launch(Main.class, args);
        }
    }
}
