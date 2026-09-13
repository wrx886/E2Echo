package com.github.wrx886.e2echo.server.exception;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wrx886.e2echo.server.result.Result;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器，统一将异常转换为 {@link Result} 返回。
 *
 * <p>按异常来源分三层处理：业务异常 {@link E2EchoException} 直接返回其携带的信息；请求本身不合法
 * 引发的异常由 {@link #handleHttpException(Exception)} 返回异常自带的提示；其余未预期的异常由
 * {@link #handleException(Exception)} 记录日志后返回统一的失败结果，不把内部细节暴露给调用方。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，返回异常携带的具体信息。
     *
     * @param e 业务异常
     * @return 失败结果，message 为异常信息
     */
    @ExceptionHandler(E2EchoException.class)
    public Result<Void> handleE2EchoException(E2EchoException e) {
        return Result.fail(e.getMessage());
    }

    /**
     * 处理其他未捕获的异常，返回统一的失败结果。
     *
     * @param e 未捕获的异常
     * @return 失败结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail();
    }

    /**
     * 处理请求本身不合法引发的异常，返回异常携带的具体信息。
     *
     * <p>这些异常由 Spring MVC 在进入业务逻辑之前抛出，覆盖以下场景：请求方法或媒体类型不支持、
     * 缺少路径变量或请求参数、请求参数类型不匹配、请求体无法解析或无法写出、参数校验不通过、
     * 找不到处理方法或静态资源、异步请求超时或连接已不可用、上传文件超过大小限制等。它们说明调用方
     * 的请求有问题，而不是服务端出了故障，因此这里不记录日志。</p>
     *
     * <p>单独拦截的意义在于提示信息：这些异常自带的原因（例如缺少了哪个参数、参数类型不对）对
     * 调用方很有价值，若落到 {@link #handleException(Exception)} 的兜底分支，客户端只会收到没有
     * 信息量的「FAIL」，排查起来要困难得多。</p>
     *
     * @param ex 请求处理过程中抛出的异常
     * @return 失败结果，message 为异常信息
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class, MissingPathVariableException.class, MissingServletRequestParameterException.class, MissingServletRequestPartException.class, ServletRequestBindingException.class, MethodArgumentNotValidException.class, HandlerMethodValidationException.class, NoHandlerFoundException.class, NoResourceFoundException.class, AsyncRequestTimeoutException.class, ErrorResponseException.class, MaxUploadSizeExceededException.class, ConversionNotSupportedException.class, TypeMismatchException.class, HttpMessageNotReadableException.class, HttpMessageNotWritableException.class, MethodValidationException.class, AsyncRequestNotUsableException.class})
    public Result<Void> handleHttpException(Exception ex) {
        return Result.fail(ex.getMessage());
    }

}
