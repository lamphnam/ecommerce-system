package com.techlab.ecommerce.order.messaging.listener;

import com.rabbitmq.client.Channel;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.order.messaging.payload.inbound.InventoryFailedPayload;
import com.techlab.ecommerce.order.service.OrderSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedListener {

    private final OrderSagaService sagaService;
    private final MessageAckHandler ackHandler;

    @RabbitListener(queues = QueueNames.ORDER_INVENTORY_FAILED)
    public void onMessage(EventEnvelope<InventoryFailedPayload> event,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.debug("Received {} eventId={}", RoutingKeys.INVENTORY_FAILED, event.getEventId());
        ackHandler.handle(event, channel, deliveryTag, RoutingKeys.INVENTORY_FAILED,
                sagaService::handleInventoryFailed);
    }
}
