package com.qm.common;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 交换机
    public static final String EXCHANGE = "qm.events";

    // 队列
    public static final String QUEUE_ARCHIVER_MESSAGE = "qm.archiver.message";
    public static final String QUEUE_NOTIFY = "qm.notify";
    public static final String QUEUE_TIMEOUT_REVIEW = "qm.timeout.review";
    // 延迟队列（无人消费，TTL 到期后死信转发到 QUEUE_TIMEOUT_REVIEW）
    public static final String QUEUE_DELAY_REVIEW_REMIND = "qm.delay.review-remind";
    public static final String QUEUE_DELAY_REVIEW_ESCALATE = "qm.delay.review-escalate";

    // 路由键
    public static final String RK_ARCHIVER_MESSAGE = "qm.archiver.message";
    public static final String RK_NOTIFY = "qm.notify";
    public static final String RK_TIMEOUT_REVIEW = "qm.timeout.review";
    public static final String RK_DELAY_REVIEW_REMIND = "qm.delay.review-remind";
    public static final String RK_DELAY_REVIEW_ESCALATE = "qm.delay.review-escalate";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue archiverMessageQueue() {
        return new Queue(QUEUE_ARCHIVER_MESSAGE, true);
    }

    @Bean
    public Queue notifyQueue() {
        return new Queue(QUEUE_NOTIFY, true);
    }

    @Bean
    public Binding archiverMessageBinding() {
        return BindingBuilder.bind(archiverMessageQueue())
            .to(exchange()).with(RK_ARCHIVER_MESSAGE);
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue())
            .to(exchange()).with(RK_NOTIFY);
    }

    // ========== 评审超时升级（TTL + 死信转发实现延迟消息，默认 48h 提醒 / 72h 升级） ==========

    @Value("${qm.timeout.review-remind-ms:172800000}")
    private long reviewRemindMs;

    @Value("${qm.timeout.review-escalate-ms:259200000}")
    private long reviewEscalateMs;

    @Bean
    public Queue reviewRemindDelayQueue() {
        return QueueBuilder.durable(QUEUE_DELAY_REVIEW_REMIND)
            .withArgument("x-message-ttl", reviewRemindMs)
            .withArgument("x-dead-letter-exchange", EXCHANGE)
            .withArgument("x-dead-letter-routing-key", RK_TIMEOUT_REVIEW)
            .build();
    }

    @Bean
    public Queue reviewEscalateDelayQueue() {
        return QueueBuilder.durable(QUEUE_DELAY_REVIEW_ESCALATE)
            .withArgument("x-message-ttl", reviewEscalateMs)
            .withArgument("x-dead-letter-exchange", EXCHANGE)
            .withArgument("x-dead-letter-routing-key", RK_TIMEOUT_REVIEW)
            .build();
    }

    @Bean
    public Queue timeoutReviewQueue() {
        return new Queue(QUEUE_TIMEOUT_REVIEW, true);
    }

    @Bean
    public Binding reviewRemindDelayBinding() {
        return BindingBuilder.bind(reviewRemindDelayQueue())
            .to(exchange()).with(RK_DELAY_REVIEW_REMIND);
    }

    @Bean
    public Binding reviewEscalateDelayBinding() {
        return BindingBuilder.bind(reviewEscalateDelayQueue())
            .to(exchange()).with(RK_DELAY_REVIEW_ESCALATE);
    }

    @Bean
    public Binding timeoutReviewBinding() {
        return BindingBuilder.bind(timeoutReviewQueue())
            .to(exchange()).with(RK_TIMEOUT_REVIEW);
    }
}
