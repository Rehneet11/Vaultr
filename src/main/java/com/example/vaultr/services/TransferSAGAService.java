package com.example.vaultr.services;

import com.example.vaultr.dto.NotificationPayloadDTO;
import com.example.vaultr.dto.TransferRequestDTO;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class TransferSAGAService implements ITransferSAGAService{

    private final ISagaOrchestrator sagaOrchestrator;
    private final ITransactionService transactionService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TransferSAGAService(ISagaOrchestrator sagaOrchestrator, ITransactionService transactionService, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.transactionService = transactionService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Long initiateTransfer(TransferRequestDTO requestDTO) throws Exception {
        Long sourceWalletId = requestDTO.getSourceWalletId();
        Long destinationWalletId= requestDTO.getDestinationWalletId();
        BigDecimal amount  = requestDTO.getAmount();
        Transaction transaction = transactionService.createTransaction(sourceWalletId,destinationWalletId,amount);
        log.info("Initiating Transfer with Id {}",transaction.getId());

        SAGAContext context = SAGAContext.builder()
                .context(Map.ofEntries(
                        Map.entry("transactionId",transaction.getId()),
                        Map.entry("sourceWalletId",sourceWalletId),
                        Map.entry("destinationWalletId",destinationWalletId),
                        Map.entry("amount",amount)
                ))
                .build();

        Long sagaInstanceId = sagaOrchestrator.startSaga(context);
        log.info("Saga Instance created with id {}",sagaInstanceId);

        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(),sagaInstanceId);
        executeTransfer(sagaInstanceId);
        return sagaInstanceId;
    }

    @Override
    @Transactional
    public void executeTransfer(Long sagaInstanceId) throws Exception {
        log.info("Executing SAGA with id {} ", sagaInstanceId);

        try{
            List<SagaStepType> TransferMoneySteps = SagaStepFactory.steps;
            for (SagaStepType step : TransferMoneySteps){
                boolean success = sagaOrchestrator.executeStep(sagaInstanceId,step.toString());
                if (!success){
                    sagaOrchestrator.markSagaFailed(sagaInstanceId);
                    transactionService.updateTransactionStatus(sagaInstanceId,TransactionStatus.FAILED);
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
                    .transactionType(TransactionType.TRANSFER)
                    .transactionId(transaction.getId())
                    .eventType("TRANSFER_SENT")
                    .payload(jsonPayloadSending)
                    .status(EventStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEventSent);
            log.info("Outbox event saved successfully for Transaction {}", transaction.getId());
        }
        catch (Exception e){
            log.error("Saga Failed with Id {} , with error {}", sagaInstanceId,e.getMessage());
            sagaOrchestrator.markSagaFailed(sagaInstanceId);
            throw new RuntimeException("Failed to finalize SAGA and save outbox event", e);
        }
    }
}
