package com.github.wrx886.e2echo.client.test.service;

import java.io.FileWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.wrx886.e2echo.client.model.entity.File;
import com.github.wrx886.e2echo.client.service.FileService;
import com.github.wrx886.e2echo.client.store.LoginUserStore;
import com.github.wrx886.e2echo.client.test.config.LoginConfig;

@SpringBootTest
public class FileServiceTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private LoginConfig loginConfig;

    @Autowired
    private LoginUserStore loginUserStore;

    // 模拟登入
    @BeforeEach
    public void login() {
        loginUserStore.setAccessToken(loginConfig.getAccessToken());
        loginUserStore.setBaseUrl(loginConfig.getBaseUrl());
        loginUserStore.setId(loginConfig.getOwnerId());
        loginUserStore.setPrivateKey(loginConfig.getPrivateKey());
        loginUserStore.setPublicKey(loginConfig.getPublicKey());
    }

    @Test
    public void test() throws Exception {
        // 创建示例文件
        String filePath = "./test.txt";
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write("Hello, World!");
        }

        // 上传文件
        File file = fileService.upload(filePath);
        System.out.println(file);

        // 下载文件
        System.out.println(fileService.download(file));

    }

}
