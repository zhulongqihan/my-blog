package com.myblog.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志消息 DTO
 * 
 * AOP切面拦截到操作后，将日志信息发送到MQ
 * 由消费者异步写入数据库，避免阻塞业务线程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID（幂等去重） */
    private String messageId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 操作类型 */
    private String operationType;

    /** 模块名称 */
    private String module;

    /** 操作描述 */
    private String description;

    /** 方法名 */
    private String method;

    /** 请求URL */
    private String requestUrl;

    /** 请求方法 (GET/POST/PUT/DELETE) */
    private String requestMethod;

    /** 请求参数 */
    private String requestParams;

    /** 响应结果 */
    private String responseResult;

    /** IP地址 */
    private String ipAddress;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 执行时间（毫秒） */
    private Integer executionTime;

    /** 状态（1成功 0失败） */
    private Integer status;

    /** 错误信息 */
    private String errorMessage;

    /** 操作时间 */
    private LocalDateTime operationTime;
}
