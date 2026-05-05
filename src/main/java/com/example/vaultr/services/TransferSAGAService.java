package com.example.vaultr.services;

import com.example.vaultr.dto.TransferRequestDTO;
import com.example.vaultr.entities.SagaInstance;
import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.saga.ISagaOrchestrator;
import com.example.vaultr.saga.SAGAContext;
import com.example.vaultr.saga.steps.SagaStepFactory;
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

    public TransferSAGAService(ISagaOrchestrator sagaOrchestrator, ITransactionService transactionService) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.transactionService = transactionService;
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
        }
        catch (Exception e){
            log.error("Saga Failed with Id {} , with error {}", sagaInstanceId,e.getMessage());
            sagaOrchestrator.markSagaFailed(sagaInstanceId);
        }
    }
}
