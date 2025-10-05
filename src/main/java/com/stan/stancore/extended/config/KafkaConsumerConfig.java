package com.stan.stancore.extended.config;

import com.systemspecs.remita.vending.extended.dto.NotificationDTO;
import com.systemspecs.remita.vending.extended.event.EventGroup;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final VendingCoreProperties properties;

    private Map<String, Object> commonConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Disable auto-commit
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getOffSetConfig()); // Start from the earliest message
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getMaxPollRecords()); // Process one record at a time
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, properties.getMaxPollInterval()); // 5 minutes interval
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000); // Consumer session timeout
        return configProps;
    }

    @Bean
    public ConsumerFactory<String, NotificationDTO> consumerFactory() {
        Map<String, Object> configProps = commonConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, EventGroup.THIRD_PARTY_PAYMENT_NOTIFICATION_GROUP);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class.getName());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class.getName());
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
            configProps,
            new ErrorHandlingDeserializer<>(new StringDeserializer()),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(NotificationDTO.class))
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // Manual commit mode
        factory.setConcurrency(properties.getConcurrency()); // Single-threaded consumption to ensure order
        return factory;
    }
}
