package com.myblog.common.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONUtil;
import com.myblog.common.annotation.Log;
import com.myblog.entity.OperationLog;
import com.myblog.entity.User;
import com.myblog.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面
 * 设计目标：自动记录标记了@Log注解的方法的操作日志
 * 
 * 工作流程：
 * 1. 拦截标记了@Log注解的方法
 * 2. 记录请求开始时间
 * 3. 执行目标方法
 * 4. 记录请求结束时间，计算耗时
 * 5. 收集操作信息（用户、IP、浏览器等）
 * 6. 异步保存日志到数据库
 * 
 * 设计亮点：
 * - 使用环绕通知，可以记录方法执行前后的信息
 * - 异步保存日志，不阻塞主业务
 * - 自动解析User-Agent，获取浏览器和操作系统信息
 * - 异常情况也会记录日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {
    
    private final OperationLogService operationLogService;
    
    /**
     * 环绕通知：拦截标记了@Log注解的方法
     * 
     * @param joinPoint 切入点
     * @param logAnnotation @Log注解
     * @return 方法执行结果
     */
    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 构建日志对象
        OperationLog operationLog = new OperationLog();
        
        try {
            // 获取请求信息
            HttpServletRequest request = getHttpServletRequest();
            if (request != null) {
                // 设置请求信息
                operationLog.setRequestUrl(request.getRequestURI());
                operationLog.setRequestMethod(request.getMethod());
                operationLog.setIpAddress(getClientIp(request));
                
                // 解析User-Agent
                String userAgentStr = request.getHeader("User-Agent");
                if (StrUtil.isNotBlank(userAgentStr)) {
                    UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
                    operationLog.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
                    operationLog.setOs(userAgent.getOs().getName());
                }
            }
            
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                operationLog.setUserId(user.getId());
                operationLog.setUsername(user.getUsername());
            }
            
            // 设置注解信息
            operationLog.setModule(logAnnotation.module());
            operationLog.setOperationType(logAnnotation.operationType());
            operationLog.setDescription(logAnnotation.description());
            
            // 设置方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = signature.getName();
            operationLog.setMethod(className + "." + methodName);
            
            // 保存请求参数
            if (logAnnotation.saveRequestParams()) {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    // 过滤掉HttpServletRequest等不需要序列化的参数
                    String params = Arrays.stream(args)
                            .filter(arg -> !(arg instanceof HttpServletRequest))
                            .filter(arg -> !(arg instanceof HttpServletResponse))
                            .filter(arg -> !(arg instanceof UserDetails))
                            .map(this::toJsonString)
                            .collect(Collectors.joining(", "));
                    operationLog.setRequestParams(params);
                }
            }
            
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 保存响应结果
            if (logAnnotation.saveResponseResult() && result != null) {
                operationLog.setResponseResult(toJsonString(result));
            }
            
            // 设置成功状态
            operationLog.setStatus(1);
            
            return result;
            
        } catch (Throwable e) {
            // 设置失败状态
            operationLog.setStatus(0);
            operationLog.setErrorMsg(e.getMessage());
            throw e;
            
        } finally {
            // 计算执行耗时
            long endTime = System.currentTimeMillis();
            operationLog.setExecutionTime((int) (endTime - startTime));
            operationLog.setCreatedAt(LocalDateTime.now());
            
            // 异步保存日志
            operationLogService.saveLog(operationLog);
        }
    }
    
    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * 获取客户端IP地址
     * 支持通过代理获取真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 对象转JSON字符串
     * 设计意图：统一处理序列化异常，避免日志记录失败
     */
    private String toJsonString(Object obj) {
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            try {
                return obj.toString();
            } catch (Exception ex) {
                return obj.getClass().getSimpleName() + "(serialization failed)";
            }
        }
    }
}
