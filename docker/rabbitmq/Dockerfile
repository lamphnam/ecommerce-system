FROM rabbitmq:3.13-management-alpine

# Pin to the same minor version as the broker. The plugin file name pattern is
# rabbitmq_delayed_message_exchange-<plugin-version>.ez. Plugin releases:
#   https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases
ARG PLUGIN_VERSION=3.13.0

USER root
RUN apk add --no-cache curl && \
    curl -fL --retry 3 \
      "https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v${PLUGIN_VERSION}/rabbitmq_delayed_message_exchange-${PLUGIN_VERSION}.ez" \
      -o "/opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-${PLUGIN_VERSION}.ez" && \
    chown rabbitmq:rabbitmq "/opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-${PLUGIN_VERSION}.ez" && \
    chmod 644 "/opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-${PLUGIN_VERSION}.ez"

COPY enabled_plugins /etc/rabbitmq/enabled_plugins
RUN chown rabbitmq:rabbitmq /etc/rabbitmq/enabled_plugins && \
    chmod 644 /etc/rabbitmq/enabled_plugins

USER rabbitmq
