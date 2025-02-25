package com.github.wrx886.e2echo.client.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.BaseWebSocketClient;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.common.WebSocketResult;
import com.github.wrx886.e2echo.client.model.api.MessageApiVo;
import com.github.wrx886.e2echo.client.model.api.ReceiveMessageApiVo;
import com.github.wrx886.e2echo.client.model.enums.MessageApiType;

// 消息处理函数
@Component
public class MessageWebSocketApi extends BaseWebSocketClient {

    // 初始化 URI （空 URI）
    private static final URI initUri;

    static {
        try {
            initUri = new URI("");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new E2Echoxception(null, e.getMessage());
        }
    }

    // 构造函数
    public MessageWebSocketApi() {
        super(initUri);
    }

    // 设置 baseUrl
    public void setBaseUrl(String baseUrl) {
        try {
            if (!this.isOpen()) {
                super.uri = new URI(baseUrl.replaceFirst("^(http|https):", "ws:")).resolve("/server/message");
            } else {
                throw new E2Echoxception(null, "不允许在开启时修改 url");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2Echoxception(null, e.getMessage());
        }
    }

    // 登入业务（发送）
    public void login(String accessToken) {
        // 确保连接开启
        ensureOpen();

        // 发送数据
        sendMessage("login", accessToken);
    }

    // 登入业务（接收）
    public void login(WebSocketResult<?> result) {
        // 不需要处理
    }

    // 发送消息（发送）
    public void send(MessageApiVo messageApiVo) {
        // 确保连接开启
        ensureOpen();

        // 发送消息
        sendMessage("send", messageApiVo);
    }

    // 发送消息（接收）
    public void send(WebSocketResult<?> result) {
        // 不需要处理
    }

    // 接收消息（发送）
    public void receive(ReceiveMessageApiVo receiveMessageApiVo) {
        // 确保连接开启
        ensureOpen();

        // 发送请求
        sendMessage("receive", receiveMessageApiVo);
    }

    // 接收消息（接收）
    // 需要手动绑定处理方案
    public void receive(WebSocketResult<?> result) {
        throw new UnsupportedOperationException("Unimplemented method 'receive'");
    }

    // 配置自动接收（发送）
    // 这里需要注意，如果配置为 GROUP，则可以接收所有消息
    public void registryAutoReceive(MessageApiType messageApiType) {
        // 确保连接开启
        ensureOpen();

        // 发送请求
        sendMessage("registryAutoReceive", messageApiType);
    }

    // 配置自动接收（接收）
    public void registryAutoReceive(WebSocketResult<?> result) {
        // 不需要处理
    }

    // 取消配置自动接收（发送）
    public void cancelAutoReceive() {
        // 确保连接开启
        ensureOpen();

        // 发送请求
        sendMessage("cancelAutoReceive");
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

    // 确保业务开启
    private synchronized void ensureOpen() {
        try {
            if (!isOpen()) {
                connectBlocking();
            }
            if (!isOpen()) {
                throw new E2Echoxception(null, "连接失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2Echoxception(null, e.getMessage());
        }
    }

}
