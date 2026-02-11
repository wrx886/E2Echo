package com.github.wrx886.e2echo.client.srv.store;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.srv.service.SessionService;
import com.github.wrx886.e2echo.client.srv.socket.MessageWebSocketClient;

@Component
public class MessageWebSocketClientStore {

    private ExecutorService executorService;
    private final WebUrlStore webUrlStore;
    private MessageWebSocketClient client;
    private boolean closed = false;

    // 构造函数
    public MessageWebSocketClientStore(WebUrlStore webUrlStore, ExecutorService executorService) {
        this.webUrlStore = webUrlStore;
        this.executorService = executorService;
    }

    /**
     * 现取现用
     * 
     * @return
     */
    public synchronized MessageWebSocketClient getClient() {
        // 已经关闭
        if (closed) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_CLIENT_CLOSED);
        }

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

            // 创建一个线程，用于不断检测连接状态
            executorService.submit(() -> {
                SessionService sessionService = BeanProvider.getBean(SessionService.class);
                while (!closed) {
                    if (!client.isOpen()) {
                        // 重连
                        this.getClient();
                        // 订阅消息
                        List<Session> sessions = sessionService.list();
                        // 休眠
                        try {
                            Thread.sleep(1000L * 60 * 10); // 10 分钟
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
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
            closed = true;
            client = null;
        }
    }

}
