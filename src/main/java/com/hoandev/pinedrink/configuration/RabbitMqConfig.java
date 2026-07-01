package com.hoandev.pinedrink.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    private final RabbitMqProperties properties;

    public RabbitMqConfig(RabbitMqProperties properties) {
        this.properties = properties;
    }

    @Value("${app.rabbitmq.queue.geocoding:pine-drink.geocoding.queue}")
    private String geocodingQueueName;

    // ──────────────────────────────────────────────
    // 1. Topic Exchanges
    // ──────────────────────────────────────────────

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(properties.domainEvents().exchange());
    }

    @Bean
    public TopicExchange realtimeEventExchange() {
        return new TopicExchange(properties.realtime().exchange(), true, false);
    }

    @Bean
    public TopicExchange realtimeDeadLetterExchange() {
        return new TopicExchange(properties.realtime().dlx(), true, false);
    }

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(properties.email().exchange());
    }

    @Bean
    public TopicExchange backgroundJobExchange() {
        return new TopicExchange(properties.backgroundJob().exchange());
    }

    @Bean
    public TopicExchange reportExchange() {
        return new TopicExchange(properties.report().exchange());
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

    @Bean
    public Queue realtimeNotificationQueue() {
        return realtimeQueue(properties.realtime().notificationQueue());
    }

    @Bean
    public Queue realtimeAuditQueue() {
        return realtimeQueue(properties.realtime().auditQueue());
    }

    @Bean
    public Queue realtimeChatQueue() {
        return realtimeQueue(properties.realtime().chatQueue());
    }

    @Bean
    public Queue realtimeWebhookQueue() {
        return realtimeQueue(properties.realtime().webhookQueue());
    }

    @Bean
    public Queue realtimeDlq() {
        return QueueBuilder.durable(properties.realtime().dlq()).build();
    }

    @Bean
    public Binding realtimeNotificationOrderBinding() {
        return BindingBuilder.bind(realtimeNotificationQueue()).to(realtimeEventExchange()).with("order.*");
    }

    @Bean
    public Binding realtimeNotificationPaymentBinding() {
        return BindingBuilder.bind(realtimeNotificationQueue()).to(realtimeEventExchange()).with("payment.*");
    }

    @Bean
    public Binding realtimeNotificationBinding() {
        return BindingBuilder.bind(realtimeNotificationQueue()).to(realtimeEventExchange()).with("notification.*");
    }

    @Bean
    public Binding realtimeAuditBinding() {
        return BindingBuilder.bind(realtimeAuditQueue()).to(realtimeEventExchange()).with("#");
    }

    @Bean
    public Binding realtimeChatBinding() {
        return BindingBuilder.bind(realtimeChatQueue()).to(realtimeEventExchange()).with("chat.*");
    }

    @Bean
    public Binding realtimeWebhookOrderBinding() {
        return BindingBuilder.bind(realtimeWebhookQueue()).to(realtimeEventExchange()).with("order.*");
    }

    @Bean
    public Binding realtimeWebhookPaymentBinding() {
        return BindingBuilder.bind(realtimeWebhookQueue()).to(realtimeEventExchange()).with("payment.*");
    }

    @Bean
    public Binding realtimeDlqBinding() {
        return BindingBuilder.bind(realtimeDlq()).to(realtimeDeadLetterExchange()).with("failed");
    }

    @Bean
    public Queue geocodingQueue() {
        return QueueBuilder.durable(geocodingQueueName).build();
    }

    @Bean
    public Binding geocodingBinding() {
        return BindingBuilder.bind(geocodingQueue())
                .to(domainEventExchange())
                .with(properties.domainEvents().routingKey());
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
    // 5. Report — Queues & Bindings
    // ──────────────────────────────────────────────

    @Bean
    public Queue reportQueue() {
        return QueueBuilder.durable(properties.report().queue()).build();
    }

    @Bean
    public Queue reportRetryQueue() {
        return QueueBuilder.durable(properties.report().retryQueue())
                .withArgument("x-message-ttl", properties.report().retryTtlMs())
                .withArgument("x-dead-letter-exchange", properties.report().exchange())
                .withArgument("x-dead-letter-routing-key", properties.report().routingKey())
                .build();
    }

    @Bean
    public Queue reportDlq() {
        return QueueBuilder.durable(properties.report().dlq()).build();
    }

    @Bean
    public Binding reportBinding() {
        return BindingBuilder.bind(reportQueue())
                .to(reportExchange())
                .with(properties.report().routingKey());
    }

    @Bean
    public Binding reportRetryBinding() {
        return BindingBuilder.bind(reportRetryQueue())
                .to(reportExchange())
                .with(properties.report().retryRoutingKey());
    }

    @Bean
    public Binding reportDlqBinding() {
        return BindingBuilder.bind(reportDlq())
                .to(reportExchange())
                .with(properties.report().dlqRoutingKey());
    }

    // ──────────────────────────────────────────────
    // 6. Order Expiry — Delayed Message Exchange
    // ──────────────────────────────────────────────

    @Bean
    public DirectExchange orderExpireExchange() {
        return ExchangeBuilder
                .directExchange(properties.orderExpiry().exchange())
                .delayed()
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderExpireQueue() {
        return QueueBuilder.durable(properties.orderExpiry().queue())
                .withArgument("x-dead-letter-exchange", properties.orderExpiry().exchange())
                .withArgument("x-dead-letter-routing-key", "order.expire.dlq")
                .build();
    }

    @Bean
    public Binding orderExpireBinding() {
        return BindingBuilder.bind(orderExpireQueue())
                .to(orderExpireExchange())
                .with(properties.orderExpiry().routingKey());
    }

    // ──────────────────────────────────────────────
    // 7. Message Converter & RabbitTemplate
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

    /**
     * Factory tạo container listener được tinh chỉnh cho các background job như xuất báo cáo.
     * <p>
     * Factory này áp dụng concurrency và prefetch từ {@code app.rabbitmq.background-job}.
     * Thông điệp xuất báo cáo dùng factory này để quá trình sinh PDF lâu không kéo quá nhiều
     * job về cùng một instance ứng dụng.
     *
     * @param configurer bộ cấu hình của Spring Boot dùng để áp dụng cấu hình listener chung
     * @param connectionFactory connection factory RabbitMQ do Spring Boot quản lý
     * @return factory tạo container listener cho các consumer background-job
     */
    @Bean
    public SimpleRabbitListenerContainerFactory backgroundJobListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        var backgroundJob = properties.backgroundJob();
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setConcurrentConsumers(backgroundJob.concurrentConsumers());
        factory.setMaxConcurrentConsumers(backgroundJob.maxConsumers());
        factory.setPrefetchCount(backgroundJob.prefetch());
        return factory;
    }

    /**
     * Factory riêng cho report export để job sinh file nặng không chiếm consumer background-job.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory reportListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        var report = properties.report();
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setConcurrentConsumers(report.concurrentConsumers());
        factory.setMaxConcurrentConsumers(report.maxConsumers());
        factory.setPrefetchCount(report.prefetch());
        return factory;
    }

    private Queue realtimeQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", properties.realtime().dlx())
                .withArgument("x-dead-letter-routing-key", "failed")
                .build();
    }
}
