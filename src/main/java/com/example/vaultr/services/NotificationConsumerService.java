package com.example.vaultr.services;

import com.example.vaultr.dto.NotificationPayloadDTO;
import com.example.vaultr.entities.Notification;
import com.example.vaultr.repositories.NotificationRepository;
import com.example.vaultr.utils.IdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationConsumerService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationConsumerService(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "wallet-notifications", groupId = "vaultr-notifications-group")
    public void consumeWalletEvent(String payloadString, @Header(KafkaHeaders.RECEIVED_KEY) String kafkaKey) {
        log.info("RAW PAYLOAD RECEIVED: {}", payloadString);
        log.info("Received Kafka event with Key: {}", kafkaKey);

        try {
            NotificationPayloadDTO payload = objectMapper.readValue(payloadString, NotificationPayloadDTO.class);

            Notification notification = Notification.builder()
                    .id(IdGenerator.generateId())
                    .userId(payload.getUserId())
                    .transactionId(payload.getTransactionId())
                    .eventType(payload.getEventType())
                    .message(payload.getMessage())
                    .build();

            notificationRepository.save(notification);
            log.info("Notification saved successfully for User {}", payload.getUserId());

        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate Kafka message ignored for Transaction ID: {}", kafkaKey);
        } catch (Exception e) {
            log.error("Failed to process notification for Transaction ID: {}", kafkaKey, e);
            throw new RuntimeException("Notification processing failed", e);
        }
    }
}