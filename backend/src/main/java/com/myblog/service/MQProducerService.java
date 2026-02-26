package com.myblog.service;

import com.myblog.config.RabbitMQConfig;
import com.myblog.dto.mq.CommentNotificationMessage;
import com.myblog.dto.mq.LogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * MQ 消息生产者服务
 * 
 * 职责：将业务消息发送到 RabbitMQ 交换机
 * 
 * 面试亮点：
 * - 统一消息发送入口，方便管理和监控
 * - 每条消息生成唯一 messageId，支持消费端幂等去重
 * - 消息序列化使用 Jackson2Json（在 RabbitMQConfig 配置）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MQProducerService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送评论通知消息
     * 
     * @param message 评论通知消息
     */
    public void sendCommentNotification(CommentNotificationMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        
        log.info("[MQ生产者] 发送评论通知消息: messageId={}, articleId={}, commenter={}", 
                message.getMessageId(), message.getArticleId(), message.getCommenterName());
        
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COMMENT_EXCHANGE,
                    RabbitMQConfig.COMMENT_ROUTING_KEY,
                    message
            );
            log.info("[MQ生产者] 评论通知消息发送成功: messageId={}", message.getMessageId());
        } catch (Exception e) {
            log.error("[MQ生产者] 评论通知消息发送失败: messageId={}", message.getMessageId(), e);
        }
    }

    /**
     * 发送操作日志消息
     * 
     * @param message 操作日志消息
     */
    public void sendLogMessage(LogMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        
        log.debug("[MQ生产者] 发送操作日志消息: messageId={}, module={}, operation={}", 
                message.getMessageId(), message.getModule(), message.getDescription());
        
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.LOG_EXCHANGE,
                    RabbitMQConfig.LOG_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            log.error("[MQ生产者] 操作日志消息发送失败: messageId={}", message.getMessageId(), e);
        }
    }
}
