package com.github.wrx886.e2echo.client.gui.controller.impl;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class GuiControllerImpl implements GuiController {

    /**
     * 刷新
     */
    @Override
    public void flushAsync() {
        try {
            Platform.runLater(() -> {
                // 执行刷新操作
            });
        } catch (Throwable t) {
            log.error(null, t);
        }
    }

}
