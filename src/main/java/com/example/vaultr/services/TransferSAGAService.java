package com.example.vaultr.services;

import com.example.vaultr.dto.NotificationPayloadDTO;
import com.example.vaultr.dto.TransferRequestDTO;
import com.example.vaultr.dto.TransferResponseDTO;
import com.example.vaultr.entities.OutboxEvent;
import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.EventStatus;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.enums.TransactionType;
import com.example.vaultr.repositories.OutboxEventRepository;
import com.example.vaultr.saga.ISagaOrchestrator;
import com.example.vaultr.saga.SAGAContext;
import com.example.vaultr.saga.steps.SagaStepFactory;
import com.example.vaultr.utils.IdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TransferSAGAService implements ITransferSAGAService {

    private final ISagaOrchestrator sagaOrchestrator;
    private final ITransactionService transactionService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TransferSAGAService(ISagaOrchestrator sagaOrchestrator, ITransactionService transactionService,
            OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.transactionService = transactionService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "databaseExecution", fallbackMethod = "fallbackSaga")
    @Retry(name = "databaseRetry")
    public TransferResponseDTO initiateTransfer(TransferRequestDTO requestDTO) throws Exception {
        long start = System.currentTimeMillis();
        String sourceWalletId = requestDTO.getSourceWalletId();
        String destinationWalletId = requestDTO.getDestinationWalletId();
        BigDecimal amount = requestDTO.getAmount();
        Transaction transaction = transactionService.createTransaction(sourceWalletId, destinationWalletId, amount);
        log.info("Initiating Transfer with Id {}", transaction.getId());

        SAGAContext context = SAGAContext.builder()
                .context(Map.ofEntries(
                        Map.entry("transactionId", transaction.getId()),
                        Map.entry("sourceWalletId", sourceWalletId),
                        Map.entry("destinationWalletId", destinationWalletId),
                        Map.entry("amount", amount)))
                .build();

        String sagaInstanceId = sagaOrchestrator.startSaga(context);
        log.info("Saga Instance created with id {}", sagaInstanceId);

        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);
        executeTransfer(sagaInstanceId);
        log.info("Initiate Transaction step took {} ms", System.currentTimeMillis() - start);
        return TransferResponseDTO.builder()
                .status(transaction.getStatus().toString())
                .transactionId(transaction.getId())
                .sourceWalletId(transaction.getSourceWalletId())
                .destinationWalletId(transaction.getDestinationWalletId())
                .amount(transaction.getAmount())
                .createdAt(transaction.createdAt)
                .build();
    }

    @Override
    public void executeTransfer(String sagaInstanceId) throws Exception {
        long start = System.currentTimeMillis();
        log.info("Executing SAGA with id {} ", sagaInstanceId);
        System.out.println("Testing Retry for Resilience4j");
        try {
            List<SagaStepType> TransferMoneySteps = SagaStepFactory.steps;
            for (SagaStepType step : TransferMoneySteps) {
                boolean success = sagaOrchestrator.executeStep(sagaInstanceId, step.toString());
                if (!success) {
                    sagaOrchestrator.markSagaFailed(sagaInstanceId);
                    transactionService.updateTransactionStatus(sagaInstanceId, TransactionStatus.FAILED);
                    log.error(" Step Failed {} ", step.toString());
                    return;
                }
            }
            sagaOrchestrator.markSagaComplete(sagaInstanceId);
            log.info("Saga Completed with Id {} ", sagaInstanceId);
            transactionService.updateTransactionStatus(sagaInstanceId, TransactionStatus.COMPLETED);
            Transaction transaction = transactionService.getTransactionsBySagaInstanceId(sagaInstanceId);

            NotificationPayloadDTO payloadReceiving = NotificationPayloadDTO.builder()
                    .userId(transaction.getDestinationWalletId())
                    .transactionId(transaction.getId())
                    .eventType("TRANSFER_RECEIVED")
                    .message("You received a transfer of ₹" + transaction.getAmount())
                    .build();

            String jsonPayloadReceiving = objectMapper.writeValueAsString(payloadReceiving);

            OutboxEvent outboxEventReceived = OutboxEvent.builder()
                    .id(IdGenerator.generateId())
                    .transactionType(TransactionType.TRANSFER)
                    .transactionId(transaction.getId())
                    .eventType("TRANSFER_RECEIVED")
                    .payload(jsonPayloadReceiving)
                    .status(EventStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEventReceived);

            NotificationPayloadDTO payloadSending = NotificationPayloadDTO.builder()
                    .userId(transaction.getSourceWalletId())
                    .transactionId(transaction.getId())
                    .eventType("TRANSFER_SENT")
                    .message("You Sent a transfer of ₹" + transaction.getAmount())
                    .build();

            String jsonPayloadSending = objectMapper.writeValueAsString(payloadSending);

            OutboxEvent outboxEventSent = OutboxEvent.builder()
                    .id(IdGenerator.generateId())
                    .transactionType(TransactionType.TRANSFER)
                    .transactionId(transaction.getId())
                    .eventType("TRANSFER_SENT")
                    .payload(jsonPayloadSending)
                    .status(EventStatus.PENDING)
                    .build();
            log.info("Execute Transaction step took {} ms", System.currentTimeMillis() - start);
            outboxEventRepository.save(outboxEventSent);
            log.info("Outbox event saved successfully for Transaction {}", transaction.getId());
        } catch (Exception e) {
            log.error("Saga Failed with Id {} , with error {}", sagaInstanceId, e.getMessage());
            sagaOrchestrator.markSagaFailed(sagaInstanceId);
            throw new RuntimeException("Failed to finalize SAGA and save outbox event", e);
        }

    }

    public String fallbackSaga(TransferRequestDTO requestDTO, Throwable throwable) {
        System.err.println("SAGA FAILED AFTER RETRIES OR CIRCUIT OPEN: " + throwable.getMessage());
        throw new RuntimeException("Service currently degraded. Please try again later.");
    }
}
