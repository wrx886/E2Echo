package com.github.wrx886.e2echo.client.exception;

/**
 * 业务异常。
 *
 * <p>用于表示可预期的业务错误，其信息会由全局异常处理器直接返回给调用方。</p>
 */
public class E2EchoException extends RuntimeException {

    /**
     * 使用异常信息构造业务异常。
     *
     * @param message 异常信息
     */
    public E2EchoException(String message) {
        super(message);
    }
}
