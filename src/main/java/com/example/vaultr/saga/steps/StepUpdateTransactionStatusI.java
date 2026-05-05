package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.repositories.TransactionRepository;
import com.example.vaultr.saga.SAGAContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StepUpdateTransactionStatusI implements ISagaStep {
    private final TransactionRepository transactionRepository;

    public StepUpdateTransactionStatusI(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public boolean execute(SAGAContext context) throws Exception {
        Long transactionId = context.getLong("transactionId");
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new Exception("Transaction Cannot be found"));

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

        return true;

    }

    @Override
    public boolean compensate(SAGAContext context) throws Exception {
        Long transactionId = context.getLong("transactionId");
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new Exception("Transaction Cannot be found"));

        TransactionStatus status = context.getTransactionStatusEnum("originalTransactionStatus");
        transaction.setStatus(status);

        transactionRepository.save(transaction);
        context.addContext("TransactionStatusAfterTransactionCompensation", transaction.getStatus());

        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepType.UPDATE_TRANSACTION_STATUS.toString();
    }
}
