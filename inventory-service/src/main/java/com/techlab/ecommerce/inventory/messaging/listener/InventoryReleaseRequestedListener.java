package com.techlab.ecommerce.inventory.messaging.listener;

import com.rabbitmq.client.Channel;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.InventoryReleaseRequestedPayload;
import com.techlab.ecommerce.inventory.service.InventorySagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InventoryReleaseRequestedListener {

    private final InventorySagaService sagaService;
    private final MessageAckHandler ackHandler;

    @RabbitListener(queues = QueueNames.INVENTORY_RELEASE)
    public void onInventoryReleaseRequested(EventEnvelope<InventoryReleaseRequestedPayload> event,
                                            Channel channel,
                                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        ackHandler.handle(event, channel, deliveryTag,
                RoutingKeys.INVENTORY_RELEASE_REQUESTED, sagaService::handleInventoryReleaseRequested);
    }
}
