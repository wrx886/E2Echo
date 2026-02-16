package com.github.wrx886.e2echo.client.srv.dialog;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.store.JsonStore;
import com.github.wrx886.e2echo.client.srv.feign.PingFeign;
import com.github.wrx886.e2echo.client.srv.store.WebUrlStore;

import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public final class WebUrlDialog {

    private final WebUrlStore webUrlStore;
    private final PingFeign pingFeign;
    private final JsonStore jsonStore;

    public boolean dialog() {
        TextInputDialog textInputDialog = new TextInputDialog(jsonStore.getWebUrl());
        textInputDialog.setTitle("请输入 Web URL");
        textInputDialog.setHeaderText(null);
        textInputDialog.setContentText("Web URL:");
        textInputDialog.setResultConverter(dialogButton -> {
            if (ButtonType.OK.equals(dialogButton)) {
                // 对 URL 进行验证并设置到 Store
                String url = textInputDialog.getEditor().getText();
                if (url == null || url.isEmpty()) {
                    throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEB_URL_IS_EMPTY);
                }
                try {
                    webUrlStore.setWebUrl(url);
                    pingFeign.ping();
                } catch (Exception e) {
                    webUrlStore.setWebUrl(null);
                    throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEB_URL_NOT_AVAILABLE);
                }
                // 返回
                return url;
            }
            return null;
        });

        // 显示
        textInputDialog.showAndWait();
        jsonStore.setWebUrl(webUrlStore.getWebUrl());
        return webUrlStore.getWebUrl() != null;
    }

}
