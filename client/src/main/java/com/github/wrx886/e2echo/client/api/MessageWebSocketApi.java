package com.github.wrx886.e2echo.client.api;

import java.net.URI;
import java.util.HashMap;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.BaseWebSocketClient;
import com.github.wrx886.e2echo.client.common.WebSocketClientMethod;
import com.github.wrx886.e2echo.client.common.WebSocketResult;
import com.github.wrx886.e2echo.client.model.api.MessageApiVo;
import com.github.wrx886.e2echo.client.model.api.ReceiveMessageApiVo;
import com.github.wrx886.e2echo.client.model.enums.MessageApiType;

// 消息处理函数
@Component
public class MessageWebSocketApi {

    // WebSocket 对象
    private BaseWebSocketClient client;

    // URI
    private URI uri;

    // 命令 -> 方法
    private final HashMap<String, WebSocketClientMethod> command2method = new HashMap<>();

    // 绑定方法
    public synchronized void bind(String command, WebSocketClientMethod method) {
        command2method.put(command, method);
    }

    // 构造函数
    public MessageWebSocketApi() {
        // 这里负责绑定方法
        bind("login", this::login);
        bind("send", this::send);
        bind("receive", this::receive);
        bind("registryAutoReceive", this::registryAutoReceive);
        bind("cancelAutoReceive", this::cancelAutoReceive);
        bind("autoReceive", this::autoReceive);
    }

    // 确保业务开启
    private synchronized void ensureOpen() {
        try {
            // 创建或重建
            if (client == null || (client != null && !client.isOpen())) {
                client = new BaseWebSocketClient(uri);
                command2method.forEach((command, method) -> {
                    client.bindCommandMethod(command, method);
                });
                client.connectBlocking();
            }

            // 验证
            if (!client.isOpen()) {
                throw new RuntimeException("WebSocket 连接失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // 设置 baseUrl
    public synchronized void setBaseUrl(String baseUrl) {
        try {
            if (client == null || !client.isOpen()) {
                uri = new URI(baseUrl.replaceFirst("^(http|https):", "ws:")).resolve("/server/message");
            } else {
                throw new RuntimeException("不允许在开启时修改 url");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // 登入业务（发送）
    public synchronized void login(String accessToken) {
        // 确保连接开启
        ensureOpen();

        // 发送数据
        client.sendMessage("login", accessToken);
    }

    // 登入业务（接收）
    public void login(WebSocketResult<?> result) {
        // 不需要处理
    }

    // 发送消息（发送）
    public synchronized void send(MessageApiVo messageApiVo) {
        // 确保连接开启
        ensureOpen();

        // 发送消息
        client.sendMessage("send", messageApiVo);
    }

    // 发送消息（接收）
    public void send(WebSocketResult<?> result) {
        // 不需要处理
    }

    // 接收消息（发送）
    public synchronized void receive(ReceiveMessageApiVo receiveMessageApiVo) {
        // 确保连接开启
        ensureOpen();

        // 发送请求
        client.sendMessage("receive", receiveMessageApiVo);
    }

    // 接收消息（接收）
    // 需要手动绑定处理方案
    public void receive(WebSocketResult<?> result) {
        throw new UnsupportedOperationException("Unimplemented method 'receive'");
    }

    // 配置自动接收（发送）
    // 这里需要注意，如果配置为 GROUP，则可以接收所有消息
    public synchronized void registryAutoReceive(MessageApiType messageApiType) {
        // 确保连接开启
        ensureOpen();

        // 发送请求
        client.sendMessage("registryAutoReceive", messageApiType);
    }

    // 配置自动接收（接收）
    public void registryAutoReceive(WebSocketResult<?> result) {
        // 不需要处理
    }

    // 取消配置自动接收（发送）
    public synchronized void cancelAutoReceive() {
        // 确保连接开启
        ensureOpen();

        // 发送请求
        client.sendMessage("cancelAutoReceive");
    }

    // 取消配置自动接收（接收）
    public void cancelAutoReceive(WebSocketResult<?> result) {
        // 不需要处理
    }

    // 自动接收（接收）
    // 需要手动绑定处理方案
    public void autoReceive(WebSocketResult<?> result) {
        throw new UnsupportedOperationException("Unimplemented method 'autoReceive'");
    }

}
