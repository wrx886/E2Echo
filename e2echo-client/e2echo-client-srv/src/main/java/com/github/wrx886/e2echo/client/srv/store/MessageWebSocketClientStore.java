package com.github.wrx886.e2echo.client.srv.store;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.srv.socket.MessageWebSocketClient;

@Component
public class MessageWebSocketClientStore {

    private final WebUrlStore webUrlStore;
    private MessageWebSocketClient client;

    // 构造函数
    public MessageWebSocketClientStore(WebUrlStore webUrlStore) {
        this.webUrlStore = webUrlStore;
    }

    /**
     * 现取现用
     * 
     * @return
     */
    public synchronized MessageWebSocketClient getClient() {
        // 创建一个新对象
        if (client == null) {
            String url = webUrlStore.getWebUrl();
            if (url == null || url.isBlank()) {
                throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEB_URL_IS_EMPTY);
            }
            // 去掉开头的 http 或 https
            url = url.replaceFirst("^https?://", "");
            // 创建客户端
            try {
                client = new MessageWebSocketClient("ws://" + url + "/server/message");
            } catch (Exception e) {
                throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_CONNECT_FAIL);
            }
        }

        // 连接
        if (!client.isOpen()) {
            try {
                client.reconnectBlocking();
            } catch (InterruptedException e) {
                throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_CONNECT_FAIL);
            }
        }

        return client;
    }

    // 关闭
    public synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

}
