package com.github.wrx886.e2echo.server.test.client;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.model.socket.WebSocketRequest;
import com.github.wrx886.e2echo.server.model.socket.WebSocketResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketTestClient extends WebSocketClient implements AutoCloseable {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    // 存储请求的特有处理方法
    private ConcurrentHashMap<String, WebSocketTestClientMethod> requestId2Method = new ConcurrentHashMap<>();

    // 存储方法通用的处理方法
    private ConcurrentHashMap<String, WebSocketTestClientMethod> command2Method = new ConcurrentHashMap<>();

    // 构造函数，url格式：：ws://127.0.0.1:8080/server/socket
    public WebSocketTestClient(String url) throws Exception {
        super(new URI(url));
        this.connectBlocking();
        if (ReadyState.OPEN != this.getReadyState()) {
            throw new Exception("连接失败");
        }
    }

    // 处理接收到的请求
    @Override
    public final void onMessage(String message) {
        try {
            // 将 message 转为 WebSocketResult
            WebSocketResult<?> result = objectMapper.readValue(message, WebSocketResult.class);

            // 表示请求是否被处理
            boolean[] handle = new boolean[1];
            handle[0] = false;

            // 使用请求特有方法处理
            requestId2Method.compute(result.getId(), (id, method) -> {
                if (method != null) {
                    method.handle(result);
                    handle[0] = true;
                }
                // 完成后清空绑定
                return null;
            });

            // 处理完成
            if (handle[0] == true) {
                return;
            }

            // 命令特有的方法进行处理
            command2Method.compute(result.getCommand(), (command, method) -> {
                if (method != null) {
                    method.handle(result);
                    handle[0] = true;
                }
                // 完成后保持绑定
                return method;
            });

            // 处理完成
            if (handle[0] == true) {
                return;
            }

            // 根据命令调用反射中的通用方法进行处理
            this.getClass()
                    .getMethod(result.getCommand(), WebSocketResult.class)
                    .invoke(this, result);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // 发送请求
    public final <E> void sendMessage(String command, E data, WebSocketTestClientMethod method) throws Exception {
        // 创建请求
        WebSocketRequest<E> request = new WebSocketRequest<>();
        request.setCommand(command);
        request.setData(data);
        request.setId(UUID.randomUUID().toString());

        // 发送请求
        this.send(objectMapper.writeValueAsString(request));

        // 绑定处理方法
        if (method != null) {
            requestId2Method.put(request.getId(), method);
        }
    }

    // 发送请求
    public final void sendMessgae(String command, WebSocketTestClientMethod method) throws Exception {
        sendMessage(command, null, method);
    }

    // 发送请求
    public final <E> void sendMessage(String command, E data) throws Exception {
        sendMessage(command, data, null);
    }

    // 发送请求
    public final void sendMessage(String command) throws Exception {
        sendMessage(command, null);
    }

    // 绑定命令特有处理方法
    public final void bindCommandMethod(String command, WebSocketTestClientMethod method) {
        command2Method.put(command, method);
    }

    // 解绑命令特有方法
    public final void unbindCommandMethod(String command) {
        command2Method.remove(command);
    }

    // WebSocket 关闭时调用
    @Override
    public final void onClose(int code, String reason, boolean remote) {
        log.info("连接 {} 已关闭", this.getURI());
    }

    // WebSocket 错误时调用
    @Override
    public final void onError(Exception ex) {
        log.error("连接 {} 错误", this.getURI(), ex);
    }

    // WebSocket 开启时调用
    @Override
    public final void onOpen(ServerHandshake handshakedata) {
        log.info("连接 {} 已开启", this.getURI());
    }

}
