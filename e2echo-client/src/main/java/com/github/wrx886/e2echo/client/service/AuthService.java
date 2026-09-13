package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.util.IdUtil;
import lombok.Getter;
import org.springframework.stereotype.Service;

/**
 * 登录认证业务逻辑。
 *
 * <p>客户端启动时生成一次性 auth 票据，调用方携带该票据访问 {@code /auth/{auth}} 后，票据被
 * 校验并与当前 HTTP 会话绑定；绑定的会话 ID 是后续请求判断是否已登录的依据。</p>
 *
 * <p>票据只能使用一次，且有效期为 60 秒（依据票据 ID 中记录的时间戳判断），校验通过后票据
 * 立即失效；校验失败时抛出异常，由全局异常处理器转换为失败响应。</p>
 */
@Service
public class AuthService {

    /**
     * 当前待校验的一次性 auth 票据，为 {@code null} 表示没有待认证的票据。
     */
    private String auth;

    /**
     * 已通过认证的会话 ID，为 {@code null} 表示尚无会话通过认证。
     */
    @Getter
    private volatile String sessionId;

    /**
     * 校验一次性 auth 票据并绑定会话。
     *
     * <p>票据必须与 {@link #newAndGetAuth()} 生成的一致，且未超过 60 秒有效期；校验通过后把
     * {@link #sessionId} 记录为传入的会话 ID，并清空票据使其只能使用一次。</p>
     *
     * @param auth      待校验的一次性票据
     * @param sessionId 使用该票据的 HTTP 会话 ID
     * @throws E2EchoException 票据与当前待校验票据不一致，或已超过 60 秒有效期
     */
    public synchronized void auth(String auth, String sessionId) {
        // 验证 auth，超时时间：60s
        if (this.auth != null && this.auth.equals(auth) && Math.abs(System.currentTimeMillis() - IdUtil.getTimestampFromId(auth)) < 60 * 1000L) {
            this.sessionId = sessionId;
            this.auth = null;
        } else {
            throw new E2EchoException("auth 错误或超时！");
        }
    }

    /**
     * 生成新的一次性 auth 票据，并清空上一轮的登录状态。
     *
     * <p>生成票据的同时把 {@link #sessionId} 置空，即新票据生成后旧会话立即失去登录状态。</p>
     *
     * @return 新生成的一次性 auth 票据
     */
    public synchronized String newAndGetAuth() {
        auth = IdUtil.newId();
        this.sessionId = null;
        return auth;
    }

}
