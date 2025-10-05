package com.stan.stancore.extended.service.impl;

import com.google.gson.Gson;
import com.systemspecs.remita.vending.extended.event.EventTopic;
import com.systemspecs.remita.vending.extended.util.MailMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class BalanceNotificationPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishEmailEvent(MailMessageRequest event) {
        try {
            if (Objects.nonNull(event)) {
                String json = new Gson().toJson(event); // Serialize to JSON
                CompletableFuture<SendResult<String, String>> send = kafkaTemplate.send(EventTopic.NOTIFICATION_EMAIL_TOPIC, json).completable();
                send.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error publishing email notification event", ex);
                    } else {
                        log.info("Email notification event published to topic: {}", EventTopic.NOTIFICATION_EMAIL_TOPIC);
                        log.debug("Email notification payload: {}", json);
                    }
                });
            } else {
                log.warn("Email Notification attempted to publish a null event");
            }
        } catch (Exception e) {
            log.error("Error occurred while publishing Email notification event", e);
        }
    }
}
