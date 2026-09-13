package com.github.wrx886.e2echo.client.interceptor;

import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * 登录校验拦截器。
 *
 * <p>拦截除 {@code /auth/**} 以外的全部请求，要求请求所属会话的 ID 与
 * {@link AuthService#getSessionId()} 记录的已认证会话一致。</p>
 *
 * <p>会话不一致时直接向响应写入统一的 {@link Result} JSON（HTTP 状态码为 200）并终止请求，
 * 不经过控制器与全局异常处理器。由于响应在拦截器中直接写出，首页、静态资源等由不同
 * HandlerMapping 处理的请求都能得到一致的失败返回。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 认证业务逻辑，用于获取已认证的会话 ID。
     */
    private final AuthService authService;

    /**
     * JSON 序列化器，用于输出认证失败的统一响应体。
     */
    private final ObjectMapper objectMapper;

    /**
     * 校验当前请求所属的会话是否已通过认证。
     *
     * <p>认证失败时把 HTTP 状态码设置为 200，并以 {@code application/json;charset=UTF-8} 写入
     * {@link Result#fail(String)} 的失败结果。</p>
     *
     * @param request  当前请求
     * @param response 当前响应，认证失败时写入失败结果
     * @param handler  被调用的处理器
     * @return 认证通过返回 {@code true}，请求继续交给后续拦截器与处理器处理；认证失败返回
     *         {@code false}，请求在此终止
     * @throws Exception 写入响应失败
     */
    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (request.getSession().getId().equals(authService.getSessionId())) {
            return true;
        } else {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail("认证失败")));
            return false;
        }
    }

}
