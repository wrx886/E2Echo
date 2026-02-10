package com.github.wrx886.e2echo.client.test.srv.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.client.starter.Main.Starter;

@SpringBootTest(classes = Starter.class)
@ActiveProfiles("test")
public class MessageServiceTest {

    @Test
    public void testSendOne() {

    }

}
