package com.stan.stancore.extended.event.publishers.service;

import com.google.gson.Gson;
import com.systemspecs.remita.vending.extended.dto.NotificationDTO;
import com.systemspecs.remita.vending.extended.event.EventTopic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class TransactionNotificationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public TransactionNotificationEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(NotificationDTO event) {
        try {
            if (Objects.nonNull(event)) {
                kafkaTemplate.send(EventTopic.PAYMENT_NOTIFICATION_TOPIC, new Gson().toJson(event));
                log.info("Vending Transaction Notification event published this event: {}", event);
            } else {
                log.warn("Vending Transaction Notification attempted to publish a null event");
            }
        } catch (Exception e) {
            log.error("Error occurred while publishing vending notification event {}", e.getMessage());
        }
    }
}
