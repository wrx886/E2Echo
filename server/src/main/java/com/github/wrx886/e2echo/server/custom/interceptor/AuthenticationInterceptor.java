package com.github.wrx886.e2echo.server.custom.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.wrx886.e2echo.server.common.LoginHold;
import com.github.wrx886.e2echo.server.model.entity.User;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.util.JwtUtils;
import com.github.wrx886.e2echo.server.web.service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 获取 token
        String accessToken = request.getHeader("access-token");
        if (accessToken == null) {
            // 返回用户未登入
            throw new E2EchoException(ResultCodeEnum.LOGIN_AUTH);
        }
        // 校验通过，则继续执行，否则就会抛出异常
        Claims claims = jwtUtils.parseLoginUserToken(accessToken);
        // 获取用户
        User user = userService.getById(claims.get("userId", Long.class));
        if (user == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_USER_DELETED);
        }
        // 存放数据到 LoginUserHolder
        LoginHold.setUser(user);
        // 返回 true，继续完成请求
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 由于 SpringMVC 使用线程池技术，需要对线程进行复用，所以处理完毕后，需要清空 LoginUserHolder
        // 在处理完请求之后，清空 LoginUserHolder 的值
        LoginHold.remove();
    }
}
