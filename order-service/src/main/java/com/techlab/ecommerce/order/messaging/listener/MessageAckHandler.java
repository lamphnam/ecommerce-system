package com.techlab.ecommerce.order.messaging.listener;

import com.rabbitmq.client.Channel;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Shared ack/nack policy for the four saga listeners.
 *
 * <p>Listeners use manual acknowledgement
 * ({@code spring.rabbitmq.listener.simple.acknowledge-mode: manual}) so the
 * broker is told about success or failure only after the handler's
 * {@code @Transactional} method commits or fails:
 *
 * <ul>
 *   <li><b>success</b> — {@code basicAck}, message removed.</li>
 *   <li><b>{@link DataIntegrityViolationException}</b> from the idempotency
 *       insert — duplicate delivery, the original processing already happened
 *       on a previous delivery; {@code basicAck} and skip.</li>
 *   <li><b>any other exception</b> — {@code basicNack(requeue=false)} so the
 *       broker routes the message via {@code x-dead-letter-exchange} into the
 *       service's DLQ. The Spring retry interceptor is intentionally not used
 *       for saga events: a poison message goes straight to the DLQ for human
 *       investigation, which is safer than silently retrying a state machine
 *       transition that may have partially side-effected.</li>
 * </ul>
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
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Duplicate {} eventId={}; ack and skip",
                    routingKey, event.getEventId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle {} eventId={} orderId={}: {}",
                    routingKey, event.getEventId(), describePayload(event), e.getMessage(), e);
            // requeue=false → broker routes via x-dead-letter-exchange to the DLQ.
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private static <T> String describePayload(EventEnvelope<T> event) {
        T payload = event.getPayload();
        return payload != null ? payload.toString() : "<null>";
    }
}
