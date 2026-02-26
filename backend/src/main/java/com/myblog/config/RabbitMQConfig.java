package com.myblog.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * RabbitMQ 配置类
 * 
 * 架构设计：
 * 1. 评论通知交换机 → 评论通知队列（发送邮件通知）
 * 2. 操作日志交换机 → 操作日志队列（异步写入DB）
 * 3. 死信交换机 → 死信队列（兜底处理失败消息）
 * 
 * 面试亮点：
 * - Direct Exchange 精确路由
 * - 死信队列 (DLX) 处理消费失败消息
 * - Jackson2Json 序列化替代默认 Java 序列化
 * - 手动ACK确保消息可靠消费
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 交换机名称 ====================
    public static final String COMMENT_EXCHANGE = "blog.comment.exchange";
    public static final String LOG_EXCHANGE = "blog.log.exchange";
    public static final String DLX_EXCHANGE = "blog.dlx.exchange";

    // ==================== 队列名称 ====================
    public static final String COMMENT_NOTIFICATION_QUEUE = "blog.comment.notification.queue";
    public static final String LOG_QUEUE = "blog.log.queue";
    public static final String DLX_QUEUE = "blog.dlx.queue";

    // ==================== 路由键 ====================
    public static final String COMMENT_ROUTING_KEY = "comment.notification";
    public static final String LOG_ROUTING_KEY = "log.operation";
    public static final String DLX_ROUTING_KEY = "dlx.#";

    // ==================== 消息转换器 ====================

    /**
     * 使用 Jackson2Json 消息转换器
     * 替代默认的 Java 序列化，支持跨语言、可读性更好
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * 配置 RabbitTemplate，使用 JSON 消息转换器
     * 开启 publisher confirm 和 return 回调
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        
        // 消息发送到交换机确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("[MQ] 消息发送到交换机失败: " + cause);
            }
        });
        
        // 消息从交换机路由到队列失败的回调
        template.setReturnsCallback(returned -> {
            System.err.println("[MQ] 消息路由到队列失败: exchange=" + returned.getExchange()
                    + ", routingKey=" + returned.getRoutingKey()
                    + ", replyText=" + returned.getReplyText());
        });
        
        // 设置mandatory=true，消息无法路由时触发ReturnCallback
        template.setMandatory(true);
        
        return template;
    }

    // ==================== 死信交换机和队列（先定义，其他队列引用） ====================

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(DLX_ROUTING_KEY);
    }

    // ==================== 评论通知交换机和队列 ====================

    @Bean
    public DirectExchange commentExchange() {
        return new DirectExchange(COMMENT_EXCHANGE, true, false);
    }

    /**
     * 评论通知队列
     * - 持久化
     * - 绑定死信交换机（消费失败的消息转入 DLX）
     * - 消息 TTL 30秒（超时未消费也转入 DLX）
     */
    @Bean
    public Queue commentNotificationQueue() {
        return QueueBuilder.durable(COMMENT_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlx.comment")
                .withArgument("x-message-ttl", 30000) // 30秒 TTL
                .build();
    }

    @Bean
    public Binding commentBinding() {
        return BindingBuilder.bind(commentNotificationQueue())
                .to(commentExchange())
                .with(COMMENT_ROUTING_KEY);
    }

    // ==================== 操作日志交换机和队列 ====================

    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(LOG_EXCHANGE, true, false);
    }

    /**
     * 操作日志队列
     * - 持久化
     * - 绑定死信交换机
     */
    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlx.log")
                .build();
    }

    @Bean
    public Binding logBinding() {
        return BindingBuilder.bind(logQueue())
                .to(logExchange())
                .with(LOG_ROUTING_KEY);
    }
}
