package com.example.vaultr.services;

import com.example.vaultr.dto.NotificationPayloadDTO;
import com.example.vaultr.entities.OutboxEvent;
import com.example.vaultr.enums.EventStatus;
import com.example.vaultr.enums.TransactionType;
import com.example.vaultr.repositories.OutboxEventRepository;
import com.example.vaultr.utils.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class NotificationPublisherService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public NotificationPublisherService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository =outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void addNotification(String transactionId, TransactionType transactionType, String userId, BigDecimal amount, String eventType, String message) throws JsonProcessingException {
        NotificationPayloadDTO payloadReceiving = NotificationPayloadDTO.builder()
                .userId(userId)
                .transactionId(transactionId)
                .eventType(eventType)
                .message(message+ amount)
                .build();

        String jsonPayloadReceiving = objectMapper.writeValueAsString(payloadReceiving);

        OutboxEvent outboxEventReceived = OutboxEvent.builder()
                .id(IdGenerator.generateId())
                .transactionType(transactionType)
                .transactionId(transactionId)
                .eventType(eventType)
                .payload(jsonPayloadReceiving)
                .status(EventStatus.PENDING)
                .build();

        outboxEventRepository.save(outboxEventReceived);
    }
}
