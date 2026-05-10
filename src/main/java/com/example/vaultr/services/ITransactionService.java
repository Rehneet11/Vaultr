package com.example.vaultr.services;

import com.example.vaultr.dto.TransactionResponseDTO;
import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;

public interface ITransactionService {
    Transaction getTransactionById(String id) throws Exception;

    TransactionResponseDTO getTransactionByTransactionId(String id) throws Exception;
    List<Transaction> getTransactionsByWalletId(String walletId) throws Exception;

    List<Transaction> getTransactionsByStatus(TransactionStatus status) throws Exception;

    Transaction getTransactionsBySagaInstanceId(String sagaInstanceId) throws Exception;

    List<Transaction> getTransactionsBySourceWalletId(String walletId) throws Exception;

    List<Transaction> getTransactionsByDestinationWalletId(String walletId) throws Exception;

    Transaction createTransaction(String sourceWalletId, String destinationWalletId, BigDecimal amount);

    void updateTransactionWithSagaInstanceId(String id, String sagaInstanceId) throws Exception;

    void updateTransactionStatus(String sagaInstanceId, TransactionStatus status) throws Exception;

    String createDepositTransaction(String userId, BigDecimal amount) throws Exception;

    String createWithdrawTransaction(String userId, BigDecimal amount) throws Exception;

    void updateTransactionStatusWithTransactionId(String transactionId, TransactionStatus status) throws Exception;
}
