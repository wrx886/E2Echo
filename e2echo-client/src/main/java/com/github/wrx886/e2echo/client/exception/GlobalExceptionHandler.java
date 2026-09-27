package com.github.wrx886.e2echo.client.exception;

import com.github.wrx886.e2echo.client.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，统一将异常转换为 {@link Result} 返回。
 *
 * <p>返回体固定为 {@link Result} 的 JSON，HTTP 状态码统一为 200，调用方通过返回体中的
 * {@code code} 判断请求成功与否。</p>
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
     * 处理 Spring MVC 抛出的框架异常，返回以异常信息为提示的失败结果。
     *
     * <p>覆盖请求方法不支持、媒体类型不支持、参数缺失或类型不匹配、请求体不可读、静态资源不
     * 存在等异常，避免这类可预期的请求错误被当作系统异常记录堆栈。</p>
     *
     * @param ex 框架异常
     * @return 失败结果，message 为异常信息
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class, MissingPathVariableException.class, MissingServletRequestParameterException.class, MissingServletRequestPartException.class, ServletRequestBindingException.class, HandlerMethodValidationException.class, NoHandlerFoundException.class, NoResourceFoundException.class, AsyncRequestTimeoutException.class, ErrorResponseException.class, MaxUploadSizeExceededException.class, ConversionNotSupportedException.class, TypeMismatchException.class, HttpMessageNotReadableException.class, HttpMessageNotWritableException.class, MethodValidationException.class, AsyncRequestNotUsableException.class})
    public Result<Void> handleHttpException(Exception ex) {
        return Result.fail(ex.getMessage());
    }

    /**
     * 处理 {@code @RequestBody} 参数校验失败。
     *
     * @param e 请求体参数校验异常
     * @return 失败结果，message 为各字段的校验提示，用 {@code ; } 拼接
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(message);
    }

    /**
     * 处理 {@code @RequestParam}、{@code @PathVariable} 等参数校验失败。
     *
     * @param e 参数校验异常
     * @return 失败结果，message 为各参数的校验提示，用 {@code ; } 拼接
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(message);
    }

    /**
     * 处理表单、查询参数绑定失败。
     *
     * @param e 绑定异常
     * @return 失败结果，message 为各字段的校验提示，用 {@code ; } 拼接
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(message);
    }

}
