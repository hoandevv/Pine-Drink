# RabbitMQ Report Export Flow

## Muc tieu

Tai lieu nay mo ta cach RabbitMQ duoc su dung trong luong xuat bao cao bat dong bo cua Pine Drink.
Day la luong de giai thich nhat vi co day du cac thanh phan: API, DB job, publisher, exchange, queue, listener, worker.

## Tong quan luong

```text
Client
  -> ReportJobController
  -> ReportJobServiceImpl
  -> DB: ExportRequest(PENDING)
  -> afterCommit()
  -> RabbitTemplate.convertAndSend(exchange, routingKey, event)
  -> RabbitMQ report.exchange
  -> report.queue
  -> ReportExportListener
  -> ReportExportService.export(jobId)
  -> JasperReportService generate PDF
  -> Storage
  -> DB: ExportRequest(COMPLETED/FAILED)
```

Y tuong chinh:

- API khong generate PDF truc tiep trong request.
- API chi tao job trong DB, sau do dua message vao RabbitMQ.
- Worker phia sau nhan message, lay `jobId`, xu ly export PDF.
- HTTP request nhanh hon, tranh timeout, de retry, de scale consumer.

## Thanh phan chinh

| Thanh phan | Vai tro |
| --- | --- |
| `ReportJobController` | Nhan request tao job export report |
| `ReportJobServiceImpl` | Tao DB job, publish event sau khi commit |
| `RabbitMqEventPublisher` | Adapter gui message vao RabbitMQ |
| `report.exchange` | Exchange nhan message report |
| `report.queue` | Queue chua job export report |
| `ReportExportListener` | Consumer lang nghe queue report |
| `ReportExportService` | Xu ly nghiep vu export that su |
| `JasperReportService` | Generate PDF tu Jasper template |

## Cau hinh queue

File: `src/main/resources/application.yaml`

```yaml
app:
  rabbitmq:
    report:
      exchange: ${APP_RABBITMQ_REPORT_EXCHANGE:pine-drink.report.exchange}
      queue: ${APP_RABBITMQ_REPORT_QUEUE:pine-drink.report.queue}
      retry-queue: ${APP_RABBITMQ_REPORT_RETRY_QUEUE:pine-drink.report.retry.queue}
      dlq: ${APP_RABBITMQ_REPORT_DLQ:pine-drink.report.dlq}
      routing-key: ${APP_RABBITMQ_REPORT_ROUTING_KEY:pine-drink.report.export}
      retry-routing-key: ${APP_RABBITMQ_REPORT_RETRY_ROUTING_KEY:pine-drink.report.retry}
      dlq-routing-key: ${APP_RABBITMQ_REPORT_DLQ_ROUTING_KEY:pine-drink.report.failed}
      retry-ttl-ms: ${APP_RABBITMQ_REPORT_RETRY_TTL_MS:60000}
      concurrent-consumers: ${APP_RABBITMQ_REPORT_CONCURRENT_CONSUMERS:1}
      max-consumers: ${APP_RABBITMQ_REPORT_MAX_CONSUMERS:2}
      prefetch: ${APP_RABBITMQ_REPORT_PREFETCH:1}
```

Giai thich nhanh:

- `exchange`: noi publisher gui message vao.
- `queue`: noi consumer lay message ra xu ly.
- `routing-key`: khoa de RabbitMQ route message tu exchange vao queue.
- `retry-queue`: queue dung de cho truoc khi retry.
- `dlq`: dead letter queue, luu message loi cuoi.
- `concurrent-consumers`: so consumer ban dau.
- `max-consumers`: so consumer toi da.
- `prefetch`: so message moi consumer lay truoc; report de `1` vi generate PDF la tac vu nang.

## Khai bao exchange, queue, binding

File: `src/main/java/com/hoandev/pinedrink/configuration/RabbitMqConfig.java`

```java
@Bean
public TopicExchange reportExchange() {
    return new TopicExchange(properties.report().exchange());
}

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
```

Y nghia:

- `reportExchange()` tao topic exchange cho report.
- `reportQueue()` tao queue chinh.
- `reportBinding()` noi queue vao exchange bang routing key.
- Message gui vao dung exchange va routing key se duoc dua vao queue.

## Payload message

File: `src/main/java/com/hoandev/pinedrink/queue/event/report/ReportExportRequestedEvent.java`

```java
public record ReportExportRequestedEvent(
        String eventId,
        String jobId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
```

Message chi chua `jobId`, khong chua toan bo data bao cao.

Ly do:

- Message nhe.
- Consumer tu load lai du lieu moi nhat tu DB.
- Retry an toan hon.
- Khong bi stale data neu thong tin report thay doi sau khi request tao job.

## Publish message

File: `src/main/java/com/hoandev/pinedrink/service/impl/ReportJobServiceImpl.java`

Vi du logic publish:

```java
ReportExportRequestedEvent event = new ReportExportRequestedEvent(
        UUID.randomUUID().toString(),
        job.getId(),
        LocalDateTime.now()
);

eventPublisher.publish(
        event,
        report.exchange(),
        report.routingKey()
);
```

Trong project, publish duoc dat sau khi transaction commit:

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        eventPublisher.publish(event, report.exchange(), report.routingKey());
    }
});
```

Ly do phai publish sau commit:

- Service tao job trong DB.
- Neu publish truoc commit, consumer co the nhan message qua nhanh.
- Consumer load `jobId` nhung DB chua commit job -> loi khong tim thay job.
- `afterCommit()` dam bao job da ton tai trong DB truoc khi message duoc gui.

## Publisher adapter

File: `src/main/java/com/hoandev/pinedrink/queue/publisher/RabbitMqEventPublisher.java`

```java
rabbitTemplate.convertAndSend(exchange, routingKey, event);
```

File: `src/main/java/com/hoandev/pinedrink/configuration/RabbitMqConfig.java`

```java
@Bean
public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
}

@Bean
public RabbitTemplate rabbitTemplate(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter
) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    template.setObservationEnabled(true);
    return template;
}
```

Y nghia:

- `RabbitTemplate` la client de gui message vao RabbitMQ.
- `Jackson2JsonMessageConverter` chuyen Java object thanh JSON message.
- Code service khong can biet chi tiet protocol cua RabbitMQ.

## Consume message

File: `src/main/java/com/hoandev/pinedrink/queue/listener/ReportExportListener.java`

```java
@RabbitListener(
        queues = "${app.rabbitmq.report.queue}",
        containerFactory = "reportListenerContainerFactory"
)
public void handleReportExportRequested(ReportExportRequestedEvent event) {
    log.info("Received report export event: eventId={}, jobId={}", event.eventId(), event.jobId());

    try {
        reportExportService.export(event.jobId());
    } catch (Exception e) {
        log.error("Report export listener handled failure without message retry: eventId={}, jobId={}",
                event.eventId(), event.jobId(), e);
    }
}
```

Y nghia:

- `@RabbitListener` dang ky consumer cho queue report.
- Khi co message, Spring AMQP tu deserialize JSON ve `ReportExportRequestedEvent`.
- Listener chi goi service; nghiep vu export nam trong `ReportExportService`.
- Cach tach nay giup queue layer chi dong vai tro adapter.

## Consumer concurrency

File: `src/main/java/com/hoandev/pinedrink/configuration/RabbitMqConfig.java`

```java
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
```

Y nghia:

- `concurrentConsumers = 1`: mac dinh co 1 worker xu ly report.
- `maxConsumers = 2`: khi tai cao co the tang toi da 2 worker.
- `prefetch = 1`: moi worker chi lay 1 job tai mot thoi diem.

Voi export PDF, `prefetch = 1` hop ly vi moi job co the nang, neu consumer lay nhieu message cung luc thi de qua tai RAM/CPU.

## Tai sao khong export truc tiep trong API

Neu export truc tiep:

```text
Client -> API -> generate PDF -> save file -> response
```

Van de:

- Request lau.
- De timeout.
- Neu nhieu user export cung luc, API thread bi chiem.
- Kho retry neu loi giua chung.

Dung RabbitMQ:

```text
Client -> API -> create job -> queue -> worker export
```

Loi ich:

- API phan hoi nhanh.
- Worker xu ly nen.
- Co the tang so consumer khi can.
- Co the theo doi status job trong DB.
- Neu loi, job co status `FAILED`, de debug va retry.

## Cach noi khi bao ve

Co the trinh bay ngan gon:

```text
Trong luong export report, em khong cho API sinh PDF truc tiep.
API chi tao mot ExportRequest trong DB voi trang thai PENDING.
Sau khi transaction commit thanh cong, service publish mot event gom eventId va jobId vao RabbitMQ.
RabbitMQ route message qua report exchange va routing key vao report queue.
ReportExportListener lang nghe queue nay, nhan jobId, load job tu DB, generate PDF bang Jasper, luu file, sau do cap nhat trang thai job thanh COMPLETED hoac FAILED.
Cach nay giup tach tac vu nang ra background, request nhanh hon, tranh timeout va de scale worker.
```

## Cac queue RabbitMQ chinh trong project

| Queue group | Muc dich |
| --- | --- |
| `email` | Gui OTP/register/reset password email bat dong bo |
| `domain-events` | Xu ly domain event noi bo, hien dang dung cho geocoding |
| `report` | Export report PDF bat dong bo |
| `order-expiry` | Xu ly don hang het han bang delayed message |
| `realtime` | Publish event realtime cho notification/chat/webhook/audit |

## Ket luan

RabbitMQ trong project duoc dung de tach cac tac vu lau hoac nen khoi HTTP request.
Voi report export, RabbitMQ dong vai tro hang doi trung gian: API tao job va publish event, listener xu ly job phia sau.
Thiet ke nay giup he thong on dinh hon, de giai thich hon va de mo rong khi luong job tang.
