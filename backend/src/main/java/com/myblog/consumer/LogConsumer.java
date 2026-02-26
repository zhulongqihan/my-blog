package com.myblog.consumer;

import com.myblog.config.RabbitMQConfig;
import com.myblog.dto.mq.LogMessage;
import com.myblog.entity.OperationLog;
import com.myblog.repository.OperationLogRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 操作日志消费者
 * 
 * 监听日志队列，异步将操作日志写入数据库
 * 
 * 面试亮点：
 * - 日志写入从同步改为MQ异步，彻底解耦业务线程
 * - 手动ACK保证消息不丢失
 * - 消费失败进入死信队列，可人工排查
 * - 对比之前的 @Async 方案：MQ 更可靠（持久化+ACK），重启不丢消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogConsumer {

    private final OperationLogRepository operationLogRepository;

    /**
     * 消费操作日志消息
     */
    @RabbitListener(queues = RabbitMQConfig.LOG_QUEUE)
    public void handleLogMessage(LogMessage message,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.debug("[日志消费者] 收到日志消息: messageId={}, module={}", 
                message.getMessageId(), message.getModule());

        try {
            // 将 MQ 消息转换为 OperationLog 实体并保存
            OperationLog operationLog = convertToEntity(message);
            operationLogRepository.save(operationLog);
            
            // 手动ACK
            channel.basicAck(deliveryTag, false);
            log.debug("[日志消费者] 日志保存成功: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("[日志消费者] 日志保存失败: messageId={}", message.getMessageId(), e);
            try {
                // nack，不重回队列，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("[日志消费者] NACK失败", ex);
            }
        }
    }

    /**
     * LogMessage DTO → OperationLog Entity
     */
    private OperationLog convertToEntity(LogMessage message) {
        OperationLog log = new OperationLog();
        log.setUserId(message.getUserId());
        log.setUsername(message.getUsername());
        log.setOperationType(message.getOperationType());
        log.setModule(message.getModule());
        log.setDescription(message.getDescription());
        log.setMethod(message.getMethod());
        log.setRequestUrl(message.getRequestUrl());
        log.setRequestMethod(message.getRequestMethod());
        log.setRequestParams(message.getRequestParams());
        log.setResponseResult(message.getResponseResult());
        log.setIpAddress(message.getIpAddress());
        log.setBrowser(message.getBrowser());
        log.setOs(message.getOs());
        log.setExecutionTime(message.getExecutionTime());
        log.setStatus(message.getStatus());
        log.setErrorMsg(message.getErrorMessage());
        log.setCreatedAt(message.getOperationTime());
        return log;
    }
}
