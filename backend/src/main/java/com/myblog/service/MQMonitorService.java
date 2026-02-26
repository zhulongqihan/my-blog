package com.myblog.service;

import com.myblog.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MQ 监控服务
 * 
 * 提供 RabbitMQ 运行状态监控数据
 * 通过 RabbitMQ Management HTTP API 获取队列信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MQMonitorService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 获取 MQ 综合状态
     */
    public Map<String, Object> getMQStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // 连接状态
        stats.put("connected", isConnected());
        
        // 队列信息
        List<Map<String, Object>> queues = new ArrayList<>();
        queues.add(getQueueInfo(RabbitMQConfig.COMMENT_NOTIFICATION_QUEUE, "评论通知队列"));
        queues.add(getQueueInfo(RabbitMQConfig.LOG_QUEUE, "操作日志队列"));
        queues.add(getQueueInfo(RabbitMQConfig.DLX_QUEUE, "死信队列"));
        stats.put("queues", queues);
        
        // 交换机信息
        List<Map<String, Object>> exchanges = new ArrayList<>();
        exchanges.add(buildExchangeInfo(RabbitMQConfig.COMMENT_EXCHANGE, "direct", "评论通知交换机"));
        exchanges.add(buildExchangeInfo(RabbitMQConfig.LOG_EXCHANGE, "direct", "操作日志交换机"));
        exchanges.add(buildExchangeInfo(RabbitMQConfig.DLX_EXCHANGE, "topic", "死信交换机"));
        stats.put("exchanges", exchanges);
        
        return stats;
    }

    /**
     * 获取单个队列信息
     */
    private Map<String, Object> getQueueInfo(String queueName, String description) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", queueName);
        info.put("description", description);
        
        try {
            // 获取队列中的消息数量
            var properties = rabbitTemplate.execute(channel -> {
                return channel.queueDeclarePassive(queueName);
            });
            
            if (properties != null) {
                info.put("messageCount", properties.getMessageCount());
                info.put("consumerCount", properties.getConsumerCount());
                info.put("status", "running");
            } else {
                info.put("messageCount", 0);
                info.put("consumerCount", 0);
                info.put("status", "unknown");
            }
        } catch (Exception e) {
            log.warn("获取队列信息失败: {}", queueName, e);
            info.put("messageCount", -1);
            info.put("consumerCount", -1);
            info.put("status", "error");
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    /**
     * 构建交换机信息
     */
    private Map<String, Object> buildExchangeInfo(String name, String type, String description) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", name);
        info.put("type", type);
        info.put("description", description);
        return info;
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        try {
            var connectionFactory = rabbitTemplate.getConnectionFactory();
            if (connectionFactory != null) {
                var connection = connectionFactory.createConnection();
                boolean open = connection.isOpen();
                return open;
            }
            return false;
        } catch (Exception e) {
            log.warn("检查RabbitMQ连接失败", e);
            return false;
        }
    }

    /**
     * 发送测试消息到指定队列
     */
    public void sendTestMessage(String queueName) {
        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("type", "test");
        testMessage.put("content", "MQ测试消息");
        testMessage.put("timestamp", System.currentTimeMillis());

        switch (queueName) {
            case RabbitMQConfig.COMMENT_NOTIFICATION_QUEUE:
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.COMMENT_EXCHANGE,
                        RabbitMQConfig.COMMENT_ROUTING_KEY,
                        testMessage
                );
                break;
            case RabbitMQConfig.LOG_QUEUE:
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.LOG_EXCHANGE,
                        RabbitMQConfig.LOG_ROUTING_KEY,
                        testMessage
                );
                break;
            default:
                throw new IllegalArgumentException("不支持的队列: " + queueName);
        }
        log.info("[MQ监控] 发送测试消息到队列: {}", queueName);
    }
}
