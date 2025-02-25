package com.github.wrx886.e2echo.client.common;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseWebSocketClient extends WebSocketClient {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    // 存储请求的特有处理方法
    private ConcurrentHashMap<String, WebSocketClientMethod> requestId2Method = new ConcurrentHashMap<>();

    // 存储方法通用的处理方法
    private ConcurrentHashMap<String, WebSocketClientMethod> command2Method = new ConcurrentHashMap<>();

    public BaseWebSocketClient(URI uri) {
        super(uri);
    }

    // 处理接收到的请求
    // 如果使用函数接收，则函数有且只有一个参数：WebSocketResult<?>
    @Override
    public final void onMessage(String message) {
        try {
            log.info(message);
            // 将 message 转为 WebSocketResult
            WebSocketResult<?> result = objectMapper.readValue(message, WebSocketResult.class);

            // 返回码错误
            if (!CodeEnum.OK.getCode().equals(result.getCode())) {
                // 解除请求 ID 对应的绑定
                if (result.getId() == null) {
                    requestId2Method.remove(result.getId());
                }

                throw new E2Echoxception(result.getCode(), result.getMessage());
            }

            // 表示请求是否被处理
            boolean[] handle = new boolean[1];
            handle[0] = false;
            Exception[] catchException = new Exception[1];

            // 使用请求特有方法处理
            requestId2Method.compute(result.getId(), (id, method) -> {
                if (method != null) {
                    try {
                        method.handle(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        catchException[0] = e;
                    }
                    handle[0] = true;
                }
                // 完成后清空绑定
                return null;
            });

            // 异常处理
            if (catchException[0] != null) {
                throw catchException[0];
            }

            // 处理完成
            if (handle[0] == true) {
                return;
            }

            // 命令特有的方法进行处理
            command2Method.compute(result.getCommand(), (command, method) -> {
                if (method != null) {
                    try {
                        method.handle(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        catchException[0] = e;
                    }
                    handle[0] = true;
                }
                // 完成后保持绑定
                return method;
            });

            // 异常处理
            if (catchException[0] != null) {
                throw catchException[0];
            }

            // 处理完成
            if (handle[0] == true) {
                return;
            }

            // 根据命令调用反射中的通用方法进行处理
            this.getClass()
                    .getMethod(result.getCommand(), WebSocketResult.class)
                    .invoke(this, result);
        } catch (E2Echoxception e) {
            e.printStackTrace();
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new E2Echoxception(null, t.getMessage());
        }
    }

    // 发送请求
    public final <E> void sendMessage(String command, E data, WebSocketClientMethod method) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2Echoxception(null, e.getMessage());
        }
    }

    // 发送请求
    public final void sendMessgae(String command, WebSocketClientMethod method) {
        sendMessage(command, null, method);
    }

    // 发送请求
    public final <E> void sendMessage(String command, E data) {
        sendMessage(command, data, null);
    }

    // 发送请求
    public final void sendMessage(String command) {
        sendMessage(command, null);
    }

    // 绑定命令特有处理方法
    public final void bindCommandMethod(String command, WebSocketClientMethod method) {
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
