package com.techlab.ecommerce.payment.messaging.listener;

import com.rabbitmq.client.Channel;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.payment.exception.DuplicateProcessedEventException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Manual ack/nack policy for Payment Service consumers:
 * success → ack; duplicate processed_events marker → ack and skip; unexpected
 * exception → nack(requeue=false) so RabbitMQ routes the message to the DLQ.
 */
@Slf4j
@Component
public class MessageAckHandler {

    public <T> void handle(EventEnvelope<T> event,
                           Channel channel,
                           long deliveryTag,
                           String routingKey,
                           Consumer<EventEnvelope<T>> handler) throws IOException {
        try {
            handler.accept(event);
            channel.basicAck(deliveryTag, false);
        } catch (DuplicateProcessedEventException duplicate) {
            log.info("Duplicate {} eventId={}; ack and skip", routingKey, eventId(event));
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle {} eventId={} payload={}: {}",
                    routingKey, eventId(event), describePayload(event), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private static <T> String eventId(EventEnvelope<T> event) {
        return event != null ? event.getEventId() : "<null>";
    }

    private static <T> String describePayload(EventEnvelope<T> event) {
        return event != null && event.getPayload() != null ? event.getPayload().toString() : "<null>";
    }
}
