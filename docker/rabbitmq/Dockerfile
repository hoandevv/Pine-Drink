FROM rabbitmq:3-management-alpine

ADD https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.13.0/rabbitmq_delayed_message_exchange-3.13.0.ez /opt/rabbitmq/plugins/
RUN chown rabbitmq:rabbitmq /opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-3.13.0.ez
RUN rabbitmq-plugins enable --offline rabbitmq_stomp rabbitmq_web_stomp rabbitmq_delayed_message_exchange
