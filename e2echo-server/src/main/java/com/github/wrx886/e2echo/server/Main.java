package com.github.wrx886.e2echo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        System.out.println(SpringBootVersion.getVersion());
        SpringApplication.run(Main.class, args);
    }
}