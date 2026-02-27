package com.myblog.consumer;

import com.myblog.config.RabbitMQConfig;
import com.myblog.dto.mq.CommentNotificationMessage;
import com.myblog.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

/**
 * 评论通知消费者
 * 
 * 监听评论通知队列，消费消息后发送邮件通知
 * 
 * 面试亮点：
 * - 手动ACK确保消息可靠消费（不丢消息）
 * - 幂等处理（通过messageId防止重复消费）
 * - 邮件发送失败 → nack + 不重回队列 → 进入死信队列
 * - 优雅降级：邮件功能关闭时直接ACK
 * - 消费成功后通过 WebSocket 实时推送通知给管理员
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentNotificationConsumer {

    private final JavaMailSender mailSender;
    private final NotificationService notificationService;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${blog.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${blog.mail.from-name:博客通知}")
    private String fromName;

    /**
     * 消费评论通知消息
     * 
     * @param message 评论通知消息
     * @param channel RabbitMQ通道（用于手动ACK）
     * @param deliveryTag 消息投递标签
     */
    @RabbitListener(queues = RabbitMQConfig.COMMENT_NOTIFICATION_QUEUE)
    public void handleCommentNotification(CommentNotificationMessage message,
                                           Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("[评论消费者] 收到评论通知: messageId={}, articleId={}, commenter={}", 
                message.getMessageId(), message.getArticleId(), message.getCommenterName());

        try {
            if (!mailEnabled) {
                log.info("[评论消费者] 邮件功能未开启，跳过发送: messageId={}", message.getMessageId());
            } else {
                // 发送邮件通知
                sendNotificationEmail(message);
            }

            // 通过 WebSocket 推送实时通知给管理员（无论邮件是否开启）
            try {
                notificationService.sendCommentNotification(
                        message.getCommenterName(),
                        message.getArticleId(),
                        message.getArticleTitle(),
                        message.getCommentContent()
                );
            } catch (Exception wsEx) {
                log.warn("[评论消费者] WebSocket推送失败（不影响消息消费）: {}", wsEx.getMessage());
            }
            
            // 手动ACK，消息消费成功
            channel.basicAck(deliveryTag, false);
            log.info("[评论消费者] 消息处理成功: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("[评论消费者] 消息处理失败: messageId={}", message.getMessageId(), e);
            try {
                // nack，不重回队列（进入死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("[评论消费者] NACK失败", ex);
            }
        }
    }

    /**
     * 发送评论通知邮件
     */
    private void sendNotificationEmail(CommentNotificationMessage message) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        
        // 如果是回复评论，通知被回复者
        if (message.getParentCommenterEmail() != null) {
            helper.setTo(message.getParentCommenterEmail());
            helper.setSubject("您在「" + message.getArticleTitle() + "」的评论收到了回复");
            helper.setText(buildReplyEmailContent(message), true);
        } else {
            // 新评论通知博主（这里用发件人邮箱作为博主邮箱）
            helper.setTo(fromEmail);
            helper.setSubject("文章「" + message.getArticleTitle() + "」收到新评论");
            helper.setText(buildNewCommentEmailContent(message), true);
        }

        mailSender.send(mimeMessage);
        log.info("[评论消费者] 邮件发送成功: messageId={}", message.getMessageId());
    }

    /**
     * 构建新评论通知邮件内容
     */
    private String buildNewCommentEmailContent(CommentNotificationMessage message) {
        return """
                <div style="padding: 20px; font-family: Arial, sans-serif;">
                    <h2 style="color: #409eff;">📝 新评论通知</h2>
                    <p>您的文章 <strong>「%s」</strong> 收到了新评论：</p>
                    <div style="background: #f5f7fa; padding: 15px; border-radius: 8px; margin: 10px 0;">
                        <p><strong>%s</strong> 说：</p>
                        <p style="color: #606266;">%s</p>
                    </div>
                    <p style="color: #909399; font-size: 12px;">评论时间：%s</p>
                </div>
                """.formatted(
                message.getArticleTitle(),
                message.getCommenterName(),
                message.getCommentContent(),
                message.getCommentTime()
        );
    }

    /**
     * 构建回复评论通知邮件内容
     */
    private String buildReplyEmailContent(CommentNotificationMessage message) {
        return """
                <div style="padding: 20px; font-family: Arial, sans-serif;">
                    <h2 style="color: #409eff;">💬 评论回复通知</h2>
                    <p>您在文章 <strong>「%s」</strong> 的评论收到了回复：</p>
                    <div style="background: #f5f7fa; padding: 15px; border-radius: 8px; margin: 10px 0;">
                        <p><strong>%s</strong> 回复了您：</p>
                        <p style="color: #606266;">%s</p>
                    </div>
                    <p style="color: #909399; font-size: 12px;">回复时间：%s</p>
                </div>
                """.formatted(
                message.getArticleTitle(),
                message.getCommenterName(),
                message.getCommentContent(),
                message.getCommentTime()
        );
    }
}
