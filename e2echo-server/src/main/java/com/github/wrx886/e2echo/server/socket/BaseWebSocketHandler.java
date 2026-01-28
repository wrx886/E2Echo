package com.github.wrx886.e2echo.server.socket;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.model.socket.WebSocketRequest;
import com.github.wrx886.e2echo.server.model.socket.WebSocketResult;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseWebSocketHandler extends TextWebSocketHandler {

    private static ObjectMapper objectMapper = new ObjectMapper();

    /*
     * 这里要求子 Handler 中的处理函数必须满足以下条件：
     * 1. 不能重名
     * 2. 可以有 0 个参数
     * 3. 如果有一个参数，则必须是 WebSocketSession 类型
     * 4. 如果有两个参数，则第一个必须是 WebSocketSession 类型
     * 5. 最多允许两个参数
     * 6. 返回值（或异常）会自动放入 WebSocketResult，在函数中只需要返回 WebSocketResult 中 data 的数据或没有返回值
     */
    @Override
    public final void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        WebSocketRequest<?> webSocketRequest = new WebSocketRequest<>();
        try {
            // 类型转换
            if (message instanceof TextMessage tm) {
                try {
                    // 转为 JSON
                    webSocketRequest = objectMapper.readValue(tm.getPayload(), WebSocketRequest.class);
                } catch (Exception e) {
                    // 请求格式错误
                    throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_FARMAT_ERROR);
                }
            } else {
                // 请求格式错误
                throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_FARMAT_ERROR);
            }

            // 判断请求的 id 是否为空
            if (webSocketRequest.getId() == null || webSocketRequest.getId().isBlank()) {
                throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_ID_EMPTY);
            }

            // 判断请求命令是否为空
            if (webSocketRequest.getCommand() == null || webSocketRequest.getCommand().isBlank()) {
                throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_COMMAND_EMPTY);
            }

            // 获取本类方法名
            Set<String> superMethodNames = Arrays.stream(BaseWebSocketHandler.class.getMethods())
                    .map(new Function<Method, String>() {
                        @Override
                        public String apply(Method t) {
                            return t.getName();
                        }
                    }).collect(Collectors.toSet());

            // 获取类方法
            List<Method> methods = Arrays.stream(this.getClass().getMethods()).filter(new Predicate<Method>() {
                @Override
                public boolean test(Method method) {
                    return !superMethodNames.contains(method.getName());
                }
            }).toList();

            // 提取命令并反射为函数
            Method method = null;
            for (Method m : methods) {
                if (m.getName().equals(webSocketRequest.getCommand())) {
                    if (method == null) {
                        method = m;
                    } else {
                        // 重定义
                        throw new RuntimeException("WebSocket method redefined!");
                    }
                }
            }

            // 命令不存在
            if (method == null) {
                throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_NOT_FOUND);
            }

            // 参数校验
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length > 2) {
                System.out.println(parameterTypes.length);
                throw new RuntimeException("The number of Websocket method parameter must lower and equal 2!");
            }

            // 要求第一个参数（如果有）必须是 Session
            if (parameterTypes.length > 0 && !parameterTypes[0].equals(WebSocketSession.class)) {
                throw new RuntimeException(
                        "Socket method first paramter must be %s!".formatted(WebSocketSession.class));
            }

            // 运行函数
            Object response;
            if (parameterTypes.length == 2) {
                Object param2;
                if (parameterTypes[1].equals(String.class)) {
                    if (webSocketRequest.getData() instanceof String s) {
                        param2 = s;
                    } else {
                        throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_PARAM_ERROR);
                    }
                } else {
                    try {
                        param2 = objectMapper.readValue(objectMapper.writeValueAsString(webSocketRequest.getData()),
                                parameterTypes[1]);
                    } catch (Exception e) {
                        throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_PARAM_ERROR);
                    }
                }

                try {
                    // 两个参数
                    beforeHandler(session, webSocketRequest, param2);
                    response = method.invoke(this, session, param2);
                } finally {
                    afterHandler(session, webSocketRequest, param2);
                }
            } else {
                try {
                    // 一个参数或没有参数
                    beforeHandler(session, webSocketRequest, null);
                    if (parameterTypes.length == 0) {
                        response = method.invoke(this);
                    } else {
                        response = method.invoke(this, session);
                    }
                } finally {
                    afterHandler(session, webSocketRequest, null);
                }
            }

            // 构建响应字符串
            String resultString = objectMapper.writeValueAsString(
                    WebSocketResult.ok(webSocketRequest.getId(), webSocketRequest.getCommand(), response));

            // 发送相应（这里临时扩大发送容量，以防止发送失败）
            int textMessageSizeLimit = session.getTextMessageSizeLimit();
            if (resultString.getBytes().length + Byte.MAX_VALUE > textMessageSizeLimit) {
                session.setTextMessageSizeLimit(resultString.getBytes().length + Byte.MAX_VALUE);
            }
            session.sendMessage(new TextMessage(resultString));
            session.setTextMessageSizeLimit(textMessageSizeLimit);
        } catch (E2EchoException e) {
            session.sendMessage(
                    new TextMessage(objectMapper.writeValueAsString(
                            WebSocketResult.build(webSocketRequest.getId(), webSocketRequest.getCommand(),
                                    e.getResultCodeEnum()))));
        } catch (Throwable t) {
            log.error("", t.getMessage());
            session.sendMessage(
                    new TextMessage(objectMapper.writeValueAsString(WebSocketResult.fail(
                            webSocketRequest.getId(), webSocketRequest.getCommand()))));
        }
    }

    @Override
    public final void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 添加 Session 到映射
        addSession(session);

        afterEstablished(session);
    }

    @Override
    public final void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 删除 Session 映射
        removeSession(session.getId());

        afterClose(session);
    }

    // ----------------------------------------------------------------

    // 连接开启后处理
    protected void afterEstablished(WebSocketSession session) {
    }

    // 连接关闭后处理
    protected void afterClose(WebSocketSession session) {
    }

    // 消息处理前处理
    protected void beforeHandler(WebSocketSession session, WebSocketRequest<?> webSocketRequest, Object param2) {
    }

    // 消息处理后处理
    protected void afterHandler(WebSocketSession session, WebSocketRequest<?> webSocketRequest, Object param2) {
    }

    // ----------------------------------------------------------------

    // 将 SessionId 转为 Session
    private static final ConcurrentHashMap<String, WebSocketSession> sessionId2Session = new ConcurrentHashMap<>();

    // 根据 Session ID 获取 Session
    public static final WebSocketSession getSessionBySessionId(String sessionId) {
        return sessionId2Session.get(sessionId);
    }

    // 添加 Session
    private static final void addSession(WebSocketSession session) {
        sessionId2Session.put(session.getId(), session);
    }

    // 删除 Session
    private static final void removeSession(String sessionId) {
        sessionId2Session.remove(sessionId);
    }

    // 发送消息到客户端
    public static final <E> void sendMessage(WebSocketSession session, String command, E data) throws Exception {
        session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(WebSocketResult.ok(UUID.randomUUID().toString(), command, data))));
    }

    // 发送消息到客户端
    public static final <E> void sendMessage(String sessionId, String command, E data) throws Exception {
        sendMessage(getSessionBySessionId(sessionId), command, data);
    }
}
