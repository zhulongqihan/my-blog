package com.myblog.exception;

import com.myblog.common.constant.ErrorCode;
import com.myblog.common.exception.BusinessException;
import com.myblog.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 设计目标：统一异常处理，避免异常信息泄露，提供友好的错误提示
 * 
 * 处理策略：
 * 1. 业务异常：返回业务错误码和提示信息
 * 2. 参数校验异常：返回具体的校验失败信息
 * 3. 系统异常：记录日志，返回通用错误提示
 * 4. 安全异常：返回401/403状态码
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常处理
     * 场景：主动抛出的业务异常，如资源不存在、权限不足等
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 参数校验异常处理（@Valid）
     * 场景：使用@Valid注解校验DTO时，校验失败抛出的异常
     * 设计亮点：收集所有校验失败的字段，一次性返回给前端
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", errorMessage);
        return Result.paramError(errorMessage);
    }
    
    /**
     * 参数绑定异常处理
     * 场景：表单提交时参数绑定失败
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败：{}", errorMessage);
        return Result.paramError(errorMessage);
    }
    
    /**
     * 参数类型不匹配异常
     * 场景：URL参数类型错误，如传入字符串但期望数字
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 类型错误", e.getName());
        log.warn("参数类型不匹配：{}", message);
        return Result.paramError(message);
    }
    
    /**
     * 认证失败异常
     * 场景：用户名或密码错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败：{}", e.getMessage());
        return Result.error(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), 
                           ErrorCode.USERNAME_OR_PASSWORD_ERROR.getMessage());
    }
    
    /**
     * 权限不足异常
     * 场景：用户访问无权限的资源
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return Result.forbidden("权限不足，无法访问该资源");
    }
    
    /**
     * 空指针异常
     * 场景：代码中出现空指针（应该避免，但作为兜底处理）
     * 设计意图：避免空指针异常信息泄露给前端，统一返回友好提示
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.error("系统异常，请联系管理员");
    }
    
    /**
     * 非法参数异常
     * 场景：参数值不合法
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数：{}", e.getMessage());
        return Result.paramError(e.getMessage());
    }
    
    /**
     * 通用异常处理
     * 场景：所有未被上述方法捕获的异常
     * 设计意图：兜底处理，避免异常信息泄露，记录完整日志便于排查
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        // 生产环境不返回具体异常信息，避免信息泄露
        return Result.error("系统繁忙，请稍后再试");
    }
}
