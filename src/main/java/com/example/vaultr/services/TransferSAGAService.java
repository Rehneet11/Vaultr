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
    private final NotificationPublisherService notificationPublisherService;

    public TransferSAGAService(ISagaOrchestrator sagaOrchestrator, ITransactionService transactionService,
                               OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper, NotificationPublisherService notificationPublisherService) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.transactionService = transactionService;
        this.outboxEventRepository = outboxEventRepository;
        this.notificationPublisherService = notificationPublisherService;
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
        if(transaction==null){
            return TransferResponseDTO.builder().status("Sender and Receiver must be different").build();
        }
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
        Transaction transaction = transactionService.getTransactionsBySagaInstanceId(sagaInstanceId);
        try {
            List<SagaStepType> TransferMoneySteps = SagaStepFactory.steps;
            for (SagaStepType step : TransferMoneySteps) {
                boolean success = sagaOrchestrator.executeStep(sagaInstanceId, step.toString());
                if (!success) {
                    sagaOrchestrator.markSagaFailed(sagaInstanceId);
                    transactionService.updateTransactionStatus(sagaInstanceId, TransactionStatus.FAILED);
                    log.error(" Step Failed {} ", step.toString());
                    notificationPublisherService.addNotification(transaction.getId(),TransactionType.TRANSFER,transaction.getSourceWalletId(),transaction.getAmount(),"TRANSFER_FAILED","TRANSFER FAILED OF ₹");
                    return;
                }
            }
            sagaOrchestrator.markSagaComplete(sagaInstanceId);
            log.info("Saga Completed with Id {} ", sagaInstanceId);
            transactionService.updateTransactionStatus(sagaInstanceId, TransactionStatus.COMPLETED);

            notificationPublisherService.addNotification(transaction.getId(),TransactionType.TRANSFER,transaction.getDestinationWalletId(),transaction.getAmount(),"TRANSFER_RECEIVED","You received a Transfer of ₹");

            notificationPublisherService.addNotification(transaction.getId(),TransactionType.TRANSFER,transaction.getSourceWalletId(),transaction.getAmount(),"TRANSFER_SENT","You sent a Transfer of ₹");

        } catch (Exception e) {
            log.error("Saga Failed with Id {} , with error {}", sagaInstanceId, e.getMessage());
            sagaOrchestrator.markSagaFailed(sagaInstanceId);
            notificationPublisherService.addNotification(transaction.getId(),TransactionType.TRANSFER,transaction.getSourceWalletId(),transaction.getAmount(),"TRANSFER_FAILED","TRANSFER FAILED OF ₹");
            throw new RuntimeException("Failed to finalize SAGA and save outbox event", e);
        }

    }

    public TransferResponseDTO fallbackSaga(TransferRequestDTO requestDTO, Throwable throwable) {
        System.err.println("SAGA FAILED AFTER RETRIES OR CIRCUIT OPEN: " + throwable.getMessage());
        throw new RuntimeException("Service currently degraded. Please try again later.");
    }
}
