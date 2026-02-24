package com.github.wrx886.e2echo.client.srv.store;

import java.util.List;
import org.java_websocket.enums.ReadyState;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.srv.service.MessageService;
import com.github.wrx886.e2echo.client.srv.service.SessionService;
import com.github.wrx886.e2echo.client.srv.socket.MessageWebSocketClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MessageWebSocketClientStore {

    private final WebUrlStore webUrlStore;
    private MessageWebSocketClient client;
    private boolean closed = false;
    private Thread monitorThread;

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
            // 初始化
            init();
            // 创建监控线程
            monitorThread = new Thread(() -> {
                while (!closed) {
                    try {
                        getClient();
                    } catch (Exception e) {
                    }
                    try {
                        Thread.sleep(1000L * 5 * 60);
                    } catch (Exception e) {
                    }
                }
            });
            monitorThread.start();
        }

        // 连接
        if (!client.isOpen()) {
            try {
                client.reconnectBlocking();
                if (!ReadyState.OPEN.equals(client.getReadyState())) {
                    throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_CONNECT_FAIL);
                }
                init();
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

    // 初始化
    private synchronized void init() {
        // 获取服务
        MessageService messageService = BeanProvider.getBean(MessageService.class);
        SessionService sessionService = BeanProvider.getBean(SessionService.class);
        // 接收消息
        messageService.receiveMessage();
        // 订阅消息
        messageService.subscribeOne();
        // 获取会话
        List<Session> sessions = sessionService.listSession();
        for (Session session : sessions) {
            if (session.getGroup() && session.getGroupEnabled()) {
                // 订阅群聊
                messageService.subscribeGroup(session.getPublicKeyHex());
            }
        }
    }

}
