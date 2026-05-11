package com.example.vaultr.saga;

import com.example.vaultr.entities.SagaInstance;
import com.example.vaultr.enums.SagaStatus;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.repositories.SagaInstanceRepository;
import com.example.vaultr.services.ITransactionService;
import com.example.vaultr.services.ITransferSAGAService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class SagaReconciler {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final ITransferSAGAService transferSAGAService;
    private final ISagaOrchestrator sagaOrchestrator;
    private final ITransactionService transactionService;

    private static final int MAX_RETRIES = 5;
    private static final int STALE_MINUTES = 5;

    public SagaReconciler(SagaInstanceRepository sagaInstanceRepository, ITransferSAGAService transferSAGAService, ISagaOrchestrator sagaOrchestrator, ITransactionService transactionService) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.transferSAGAService = transferSAGAService;
        this.sagaOrchestrator = sagaOrchestrator;
        this.transactionService = transactionService;
    }

    @Scheduled(fixedDelay = 60000)
    public void reconcileSagas() {
        log.info("Starting Saga Reconciliation Scan...");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);

        reconcileInProgressSagas(threshold);
        reconcileCompensatingSagas(threshold);
    }

    private void reconcileInProgressSagas(LocalDateTime threshold) {
        List<SagaInstance> staleSagas = sagaInstanceRepository.findByStatusAndUpdatedAtBeforeAndRetryCountLessThan(SagaStatus.PROCESSING, threshold, MAX_RETRIES);
        staleSagas.addAll(sagaInstanceRepository.findByStatusAndUpdatedAtBeforeAndRetryCountLessThan(SagaStatus.STARTED, threshold, MAX_RETRIES));

        for (SagaInstance saga : staleSagas) {
            try {
                log.warn("Reconciling Stale PROGRESSING Saga: {}", saga.getId());
                incrementRetryCount(saga);
                transferSAGAService.executeTransfer(saga.getId());
            } catch (Exception e) {
                log.error("Failed to reconcile PROGRESSING Saga {}: {}", saga.getId(), e.getMessage());
                handleTerminalFailure(saga);
            }
        }
    }

    private void reconcileCompensatingSagas(LocalDateTime threshold) {
        List<SagaInstance> staleSagas = sagaInstanceRepository.findByStatusAndUpdatedAtBeforeAndRetryCountLessThan(SagaStatus.COMPENSATING, threshold, MAX_RETRIES);

        for (SagaInstance saga : staleSagas) {
            try {
                log.warn("Reconciling Stale COMPENSATING Saga: {}", saga.getId());
                incrementRetryCount(saga);
                sagaOrchestrator.compensateSaga(saga.getId());
                transactionService.updateTransactionStatus(saga.getId(), TransactionStatus.FAILED);
            } catch (Exception e) {
                log.error("Failed to reconcile COMPENSATING Saga {}: {}", saga.getId(), e.getMessage());
                handleTerminalFailure(saga);
            }
        }
    }

    private void incrementRetryCount(SagaInstance saga) {
        saga.setRetryCount(saga.getRetryCount() + 1);
        sagaInstanceRepository.save(saga);
    }

    private void handleTerminalFailure(SagaInstance saga) {
        if (saga.getRetryCount() >= MAX_RETRIES) {
            log.error("CRITICAL: Saga {} has reached max retries. Escalating to TERMINAL_FAILURE. Manual intervention required.", saga.getId());
            saga.setStatus(SagaStatus.TERMINAL_FAILURE);
            sagaInstanceRepository.save(saga);
        }
    }
}
