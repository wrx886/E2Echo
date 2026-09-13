package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 登录认证接口。
 *
 * <p>接收客户端启动时生成的一次性 auth 票据，校验通过后当前会话即被视为已登录，并重定向到
 * 站点首页。</p>
 */
@Controller
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * 认证业务逻辑。
     */
    private final AuthService authService;

    /**
     * 使用一次性 auth 票据完成登录。
     *
     * @param auth    客户端启动时生成的一次性 auth 票据
     * @param session 当前 HTTP 会话，校验通过后其 ID 会被记录为已认证会话
     * @return 重定向到首页的视图名
     * @throws E2EchoException 票据错误或已超过有效期
     */
    @GetMapping("{auth}")
    public String auth(@PathVariable String auth, HttpSession session) {
        authService.auth(auth, session.getId());
        return "redirect:/";
    }

}
