package com.github.wrx886.e2echo.client.test.common.controller.srv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.ecc.store.EccKeyStore;
import com.github.wrx886.e2echo.client.srv.store.WebUrlStore;
import com.github.wrx886.e2echo.client.starter.Main.Starter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = { Starter.class })
@ActiveProfiles("test")
public class AliasControllerTest {

    @Autowired
    private WebUrlStore webUrlStore;

    @Autowired
    private EccKeyStore eccKeyStore;

    @Autowired
    private AliasController aliasController;

    @BeforeEach
    public void init() {
        eccKeyStore.set(
                "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5",
                "6869bee95e46cc8b28cd9c167285ba68a6143b63f1ba371331085c9eae355bba");
        webUrlStore.setWebUrl("http://localhost:8080");
    }

    @Test
    public void putTest() {
        // 创建别名
        aliasController.put(eccKeyStore.get().getPublicKeyHex(), "用户");
        // 获取别名
        log.info(aliasController.get(eccKeyStore.get().getPublicKeyHex()));
    }

}
