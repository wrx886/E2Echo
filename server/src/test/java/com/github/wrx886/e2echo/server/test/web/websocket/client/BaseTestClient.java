package com.github.wrx886.e2echo.server.test.web.websocket.client;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.web.wocket.request.WebSocketRequest;
import com.github.wrx886.e2echo.server.web.wocket.request.WebSocketResult;

public class BaseTestClient extends WebSocketClient {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    // 存储请求的特有处理方法
    private ConcurrentHashMap<String, TestClientMethod> requestId2Method = new ConcurrentHashMap<>();

    // 存储方法通用的处理方法
    private ConcurrentHashMap<String, TestClientMethod> command2Method = new ConcurrentHashMap<>();

    public BaseTestClient(URI uri) {
        super(uri);
    }

    // 处理接收到的请求
    @Override
    public final void onMessage(String message) {
        try {
            System.out.println(message);
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
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    // 发送请求
    public final <E> void sendMessage(String command, E data, TestClientMethod method) throws Exception {
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
    public final void sendMessgae(String command, TestClientMethod method) throws Exception {
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
    public final void bindCommandMethod(String command, TestClientMethod method) {
        command2Method.put(command, method);
    }

    // 解绑命令特有方法
    public final void unbindCommandMethod(String command) {
        command2Method.remove(command);
    }

    @Override
    public final void onClose(int arg0, String arg1, boolean arg2) {
    }

    @Override
    public final void onError(Exception arg0) {
    }

    @Override
    public final void onOpen(ServerHandshake arg0) {
    }

}
