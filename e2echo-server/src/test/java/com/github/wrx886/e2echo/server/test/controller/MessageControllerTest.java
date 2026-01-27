package com.github.wrx886.e2echo.server.test.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.server.controller.MessageController;
import com.github.wrx886.e2echo.server.test.util.EccMessageTestUtil;
import com.github.wrx886.e2echo.server.util.EccUtil.KeyPairHex;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class MessageControllerTest {

    private final KeyPairHex keyPairHex = new KeyPairHex(
            "040cf52f0991cba1b7414af7a93551c763ce5294a97c80d76fbf1d6c21c36905a1cafa974939e1ce07cf33de6b1587f74be889287829e0a804324a007bfaa54731",
            "040cf52f0991cba1b7414af7a93551c763ce5294a97c80d76fbf1d6c21c36905a1cafa974939e1ce07cf33de6b1587f74be889287829e0a804324a007bfaa54731");

    @Test
    public void testSendOne() {
        log.info(EccMessageTestUtil.generateKeyPair().toString());
    }

}
