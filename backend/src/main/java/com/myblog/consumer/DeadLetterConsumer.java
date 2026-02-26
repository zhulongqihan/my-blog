package com.myblog.consumer;

import com.myblog.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 * 
 * 处理所有消费失败进入死信队列的消息
 * 
 * 面试亮点：
 * - 死信队列是兜底方案，防止消息永久丢失
 * - 可以在这里做告警通知、人工补偿等
 * - 记录详细日志方便排查问题
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    /**
     * 消费死信消息
     * 
     * 进入死信队列的场景：
     * 1. 消费者 nack 且不重回队列
     * 2. 消息 TTL 超时
     * 3. 队列达到最大长度
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleDeadLetter(Message message,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.warn("[死信消费者] 收到死信消息: exchange={}, routingKey={}, body={}", 
                message.getMessageProperties().getReceivedExchange(),
                message.getMessageProperties().getReceivedRoutingKey(),
                new String(message.getBody()));

        try {
            // 记录死信详情（生产中可以写入DB或发送告警）
            log.warn("[死信消费者] 死信详情: messageId={}, consumerQueue={}, headers={}", 
                    message.getMessageProperties().getMessageId(),
                    message.getMessageProperties().getConsumerQueue(),
                    message.getMessageProperties().getHeaders());

            // ACK确认（防止死信消息在死信队列中无限循环）
            channel.basicAck(deliveryTag, false);
            log.warn("[死信消费者] 死信消息已记录并ACK");

        } catch (Exception e) {
            log.error("[死信消费者] 处理死信失败", e);
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("[死信消费者] ACK失败", ex);
            }
        }
    }
}
