package com.example.vaultr.services;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.repositories.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class TransactionService implements ITransactionService{
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction getTransactionById(Long id) throws Exception {
        return transactionRepository.findById(id)
                .orElseThrow(()-> new Exception("Transaction Not Found"));
    }

    @Override
    public List<Transaction> getTransactionsByWalletId(Long walletId) throws Exception {
        List<Transaction> transactions =  transactionRepository.findByWalletId(walletId);
        if(transactions.isEmpty()){
            throw new Exception ("No Transactions Found");
        }
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) throws Exception {
        List<Transaction> transactions =  transactionRepository.findByStatus(status);
        if(transactions.isEmpty()){
            throw new Exception ("No Transactions Found");
        }
        return transactions;
    }

    @Override
    public Transaction getTransactionsBySagaInstanceId(Long sagaInstanceId) throws Exception {
        Transaction transactions =  transactionRepository.findBySagaInstanceId(sagaInstanceId);
        if(transactions==null){
            throw new Exception ("No Transactions Found");
        }
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionsBySourceWalletId(Long walletId) throws Exception {
        List<Transaction> transactions =  transactionRepository.findBySourceWalletId(walletId);
        if(transactions.isEmpty()){
            throw new Exception ("No Transactions Found");
        }
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionsByDestinationWalletId(Long walletId) throws Exception{
        List<Transaction> transactions =  transactionRepository.findByDestinationWalletId(walletId);
        if(transactions.isEmpty()){
            throw new Exception ("No Transactions Found");
        }
        return transactions;
    }

    @Override
    @Transactional
    public Transaction createTransaction(Long sourceWalletId, Long destinationWalletId, BigDecimal amount) {
        log.info("Creating Transaction with sourceWalletId {} DestinationWallet Id {} amount {}", sourceWalletId, destinationWalletId, amount);
        Transaction transaction = Transaction.builder()
                .sourceWalletId(sourceWalletId)
                .destinationWalletId(destinationWalletId)
                .amount(amount)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction Created with Id {}",savedTransaction.getId());
        return savedTransaction;
    }

    @Override
    public void updateTransactionWithSagaInstanceId(Long id, Long sagaInstanceId) throws Exception {
        Transaction transaction =getTransactionById(id);
        transaction.setSagaInstanceId(sagaInstanceId);
        transactionRepository.save(transaction);
        log.info("Transaction Updated with id {} ans SagaInstanceId {}",id,sagaInstanceId);
    }

    @Override
    @Transactional
    public void updateTransactionStatus(Long sagaInstanceId, TransactionStatus status) throws Exception {
        Transaction transaction = transactionRepository.findBySagaInstanceId(sagaInstanceId);
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }


}
