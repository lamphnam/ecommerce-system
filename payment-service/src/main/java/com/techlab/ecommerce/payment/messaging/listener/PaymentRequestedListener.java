package com.techlab.ecommerce.payment.messaging.listener;

import com.rabbitmq.client.Channel;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.payment.messaging.payload.inbound.PaymentRequestedPayload;
import com.techlab.ecommerce.payment.service.PaymentSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PaymentRequestedListener {

    private final PaymentSagaService sagaService;
    private final MessageAckHandler ackHandler;

    @RabbitListener(queues = QueueNames.PAYMENT_PROCESS)
    public void onPaymentRequested(EventEnvelope<PaymentRequestedPayload> event,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        ackHandler.handle(event, channel, deliveryTag, RoutingKeys.PAYMENT_REQUESTED, sagaService::handlePaymentRequested);
    }
}
