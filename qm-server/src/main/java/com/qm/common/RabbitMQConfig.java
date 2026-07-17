package com.qm.common;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 交换机
    public static final String EXCHANGE = "qm.events";

    // 队列
    public static final String QUEUE_ARCHIVER_MESSAGE = "qm.archiver.message";
    public static final String QUEUE_NOTIFY = "qm.notify";

    // 路由键
    public static final String RK_ARCHIVER_MESSAGE = "qm.archiver.message";
    public static final String RK_NOTIFY = "qm.notify";

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
}
