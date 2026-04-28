package com.techlab.ecommerce.common.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Auto-imported RabbitMQ defaults: JSON message conversion (with java.time support),
 * publisher confirms, and a sane default {@link RabbitTemplate}.
 *
 * <p>Each service declares its own queues / exchanges / bindings in its own RabbitMqConfig.
 * This class exists to avoid duplicating the converter and template setup six times.
 *
 * <p>Gated on {@link ConnectionFactory} so unit tests that exclude
 * {@code RabbitAutoConfiguration} are not forced to provide a stub connection factory.
 */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMqCommonConfig {

    @Bean
    @Primary
    public MessageConverter jacksonAmqpMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public RabbitTemplate techlabRabbitTemplate(ConnectionFactory connectionFactory,
                                                MessageConverter jacksonAmqpMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonAmqpMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
