package com.github.wrx886.e2echo.client.gui.controller.impl;

import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;

import javafx.application.Platform;

public class GuiControllerImpl implements GuiController {

    /**
     * 刷新
     */
    @Override
    public void flushAsync() {
        Platform.runLater(() -> {
            // 执行刷新操作
        });
    }

}
