package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.repositories.TransactionRepository;
import com.example.vaultr.saga.SAGAContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StepUpdateTransactionStatusI implements ISagaStep {
    private final TransactionRepository transactionRepository;

    public StepUpdateTransactionStatusI(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public boolean execute(SAGAContext context) throws Exception {
        long start = System.currentTimeMillis();
        String transactionId = context.getString("transactionId");
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new com.example.vaultr.exceptions.ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        context.addContext("originalTransactionStatus",transaction.getStatus());

        if(
                context.getTransactionStatusEnum("TransactionStatusAfterCreditSuccess")== TransactionStatus.COMPLETED
                && context.getTransactionStatusEnum("TransactionStatusAfterDebitSuccess")==TransactionStatus.COMPLETED
        ){
            transaction.setStatus(TransactionStatus.COMPLETED);
        }
        else{
            transaction.setStatus(TransactionStatus.FAILED);
        }

        transactionRepository.save(transaction);
        context.addContext("TransactionStatusAfterTransactionUpdation", transaction.getStatus());
        log.info("Execute Update Status step took {} ms", System.currentTimeMillis() - start);
        return true;

    }

    @Override
    public boolean compensate(SAGAContext context) throws Exception {
        long start = System.currentTimeMillis();
        String transactionId = context.getString("transactionId");
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new com.example.vaultr.exceptions.ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        TransactionStatus status = context.getTransactionStatusEnum("originalTransactionStatus");
        transaction.setStatus(status);

        transactionRepository.save(transaction);
        context.addContext("TransactionStatusAfterTransactionCompensation", transaction.getStatus());
        log.info("Compensate Execute Status step took {} ms", System.currentTimeMillis() - start);
        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepType.UPDATE_TRANSACTION_STATUS.toString();
    }
}
