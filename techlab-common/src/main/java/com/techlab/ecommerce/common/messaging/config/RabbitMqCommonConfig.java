package com.techlab.ecommerce.common.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
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
 *
 * <h2>Cross-service type compatibility</h2>
 * The {@link Jackson2JsonMessageConverter} is configured with
 * {@link TypePrecedence#INFERRED}: deserialization uses the listener method's
 * declared parameter type (e.g. {@code EventEnvelope<InventoryReservedPayload>})
 * instead of the {@code __TypeId__} header injected by the producer. This lets
 * each service keep its own copy of a payload DTO under its own package without
 * the consumer side trying to load a class that lives in the producer's module.
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

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(TypePrecedence.INFERRED);
        // Permit any payload class — we trust the broker boundary and rely on the
        // listener parameter type for binding instead of the producer's class name.
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);

        return converter;
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
