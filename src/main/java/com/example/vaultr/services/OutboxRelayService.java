package com.example.vaultr.services;

import com.example.vaultr.entities.OutboxEvent;
import com.example.vaultr.enums.EventStatus;
import com.example.vaultr.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class OutboxRelayService implements IOutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processOutboxEvents() {

        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEventsForProcessing();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox Relay woke up. Found {} pending events to process.", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {

                String messageKey = String.valueOf(event.getTransactionId());
                String topic = "wallet-notifications";

                kafkaTemplate.send(topic, messageKey, event.getPayload()).get();

                event.setStatus(EventStatus.COMPLETED);

                log.info("Successfully published event for Transaction ID: {}", messageKey);

            } catch (Exception e) {
                log.error("Failed to publish event {} to Kafka. Will retry.", event.getId(), e);
            }
        }
    }
}
