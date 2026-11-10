package com.campusmart.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE    = "order.exchange";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final String CLOSE_QUEUE = "order.close.queue";
    public static final String RK_DELAY    = "order.delay";
    public static final String RK_CLOSE    = "order.close";

    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .ttl(30 * 60 * 1000)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(RK_CLOSE)
                .build();
    }

    @Bean
    public Queue orderCloseQueue() {
        return QueueBuilder.durable(CLOSE_QUEUE).build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderExchange()).with(RK_DELAY);
    }

    @Bean
    public Binding closeBinding() {
        return BindingBuilder.bind(orderCloseQueue()).to(orderExchange()).with(RK_CLOSE);
    }
}
