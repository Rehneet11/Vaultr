package com.example.vaultr.services;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;

public interface ITransactionService {
    Transaction getTransactionById(Long id) throws Exception;
    List<Transaction> getTransactionsByWalletId(Long walletId) throws Exception;
    List<Transaction> getTransactionsByStatus(TransactionStatus status) throws Exception;
    Transaction getTransactionsBySagaInstanceId(Long sagaInstanceId) throws Exception;
    List<Transaction> getTransactionsBySourceWalletId(Long walletId) throws Exception;
    List<Transaction> getTransactionsByDestinationWalletId(Long walletId) throws Exception;
    Transaction createTransaction(Long sourceWalletId, Long destinationWalletId, BigDecimal amount);
    void updateTransactionWithSagaInstanceId(Long id, Long sagaInstanceId) throws Exception;
    void updateTransactionStatus(Long sagaInstanceId, TransactionStatus status) throws Exception;
}
