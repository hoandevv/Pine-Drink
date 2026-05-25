package com.hoandev.pinedrink.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    private final RabbitMqProperties properties;

    // ──────────────────────────────────────────────
    // 1. Topic Exchanges
    // ──────────────────────────────────────────────

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(properties.domainEvents().exchange());
    }

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(properties.email().exchange());
    }

    @Bean
    public TopicExchange backgroundJobExchange() {
        return new TopicExchange(properties.backgroundJob().exchange());
    }

    // ──────────────────────────────────────────────
    // 2. Domain Events — Queues & Bindings
    // ──────────────────────────────────────────────

    @Bean
    public Queue domainEventQueue() {
        return QueueBuilder.durable(properties.domainEvents().queue())
                .build();
    }

    @Bean
    public Queue domainEventRetryQueue() {
        return QueueBuilder.durable(properties.domainEvents().retryQueue())
                .withArgument("x-message-ttl", properties.domainEvents().retryTtlMs())
                .withArgument("x-dead-letter-exchange", properties.domainEvents().exchange())
                .withArgument("x-dead-letter-routing-key", properties.domainEvents().routingKey())
                .build();
    }

    @Bean
    public Queue domainEventDlq() {
        return QueueBuilder.durable(properties.domainEvents().dlq()).build();
    }

    @Bean
    public Binding domainEventBinding() {
        return BindingBuilder.bind(domainEventQueue())
                .to(domainEventExchange())
                .with(properties.domainEvents().routingKey());
    }

    @Bean
    public Binding domainEventRetryBinding() {
        return BindingBuilder.bind(domainEventRetryQueue())
                .to(domainEventExchange())
                .with(properties.domainEvents().retryRoutingKey());
    }

    @Bean
    public Binding domainEventDlqBinding() {
        return BindingBuilder.bind(domainEventDlq())
                .to(domainEventExchange())
                .with(properties.domainEvents().dlqRoutingKey());
    }

    // ──────────────────────────────────────────────
    // 3. Email — Queues & Bindings
    // ──────────────────────────────────────────────

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(properties.email().queue()).build();
    }

    @Bean
    public Queue emailRetryQueue() {
        return QueueBuilder.durable(properties.email().retryQueue())
                .withArgument("x-message-ttl", properties.email().retryTtlMs())
                .withArgument("x-dead-letter-exchange", properties.email().exchange())
                .withArgument("x-dead-letter-routing-key", properties.email().routingKey())
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(properties.email().dlq()).build();
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(emailExchange())
                .with(properties.email().routingKey());
    }

    @Bean
    public Binding emailRetryBinding() {
        return BindingBuilder.bind(emailRetryQueue())
                .to(emailExchange())
                .with(properties.email().retryRoutingKey());
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq())
                .to(emailExchange())
                .with(properties.email().dlqRoutingKey());
    }

    // ──────────────────────────────────────────────
    // 4. Background Job — Queues & Bindings
    // ──────────────────────────────────────────────

    @Bean
    public Queue backgroundJobQueue() {
        return QueueBuilder.durable(properties.backgroundJob().queue()).build();
    }

    @Bean
    public Queue backgroundJobRetryQueue() {
        return QueueBuilder.durable(properties.backgroundJob().retryQueue())
                .withArgument("x-message-ttl", properties.backgroundJob().retryTtlMs())
                .withArgument("x-dead-letter-exchange", properties.backgroundJob().exchange())
                .withArgument("x-dead-letter-routing-key", properties.backgroundJob().routingKey())
                .build();
    }

    @Bean
    public Queue backgroundJobDlq() {
        return QueueBuilder.durable(properties.backgroundJob().dlq()).build();
    }

    @Bean
    public Binding backgroundJobBinding() {
        return BindingBuilder.bind(backgroundJobQueue())
                .to(backgroundJobExchange())
                .with(properties.backgroundJob().routingKey());
    }

    @Bean
    public Binding backgroundJobRetryBinding() {
        return BindingBuilder.bind(backgroundJobRetryQueue())
                .to(backgroundJobExchange())
                .with(properties.backgroundJob().retryRoutingKey());
    }

    @Bean
    public Binding backgroundJobDlqBinding() {
        return BindingBuilder.bind(backgroundJobDlq())
                .to(backgroundJobExchange())
                .with(properties.backgroundJob().dlqRoutingKey());
    }

    // ──────────────────────────────────────────────
    // 5. Message Converter & RabbitTemplate
    // ──────────────────────────────────────────────

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setObservationEnabled(true);
        return template;
    }
}
