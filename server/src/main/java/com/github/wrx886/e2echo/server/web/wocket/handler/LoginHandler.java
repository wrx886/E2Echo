package com.github.wrx886.e2echo.server.web.wocket.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.WebSocketSession;

import com.github.wrx886.e2echo.server.common.LoginHold;
import com.github.wrx886.e2echo.server.model.entity.User;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.web.service.LoginService;
import com.github.wrx886.e2echo.server.web.service.UserService;
import com.github.wrx886.e2echo.server.web.wocket.request.WebSocketRequest;

public class LoginHandler extends BaseWebSocketHandler {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginService loginService;

    // 登入业务
    public void login(WebSocketSession session, String accessToken) {
        loginService.loginByToken(session.getId(), accessToken);
    }

    // 获取用户信息
    public User info() {
        return LoginHold.getUser();
    }

    // 连接结束时，需要删除映射关系
    @Override
    protected void afterClose(WebSocketSession session) {
        loginService.getUserSessionMap().removeSessionIdAndUserId(session.getId());
    }

    // 前置拦截器
    @Override
    protected void beforeHandler(WebSocketSession session, WebSocketRequest<?> webSocketRequest, Object param2) {
        // 特例：登入
        if ("login".equals(webSocketRequest.getCommand())) {
            return;
        }

        // 判断用户是否登入
        Long userId = loginService.getUserSessionMap().getUserIdBySessionId(session.getId());
        if (userId == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 获取用户
        User user = userService.getById(userId);
        if (user == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_USER_DELETED);
        }

        // 将用户放入 LoginHolder
        LoginHold.setUser(user);
    }

    // 后处理
    @Override
    protected void afterHandler(WebSocketSession session, WebSocketRequest<?> webSocketRequest, Object param2) {
        // 清空 LoginHold
        LoginHold.remove();
    }

}
