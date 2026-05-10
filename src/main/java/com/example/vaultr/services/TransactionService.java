package com.example.vaultr.services;

import com.example.vaultr.dto.TransactionResponseDTO;
import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.enums.TransactionType;
import com.example.vaultr.exceptions.ResourceNotFoundException;
import com.example.vaultr.repositories.TransactionRepository;
import com.example.vaultr.utils.IdGenerator;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class TransactionService implements ITransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionResponseDTO getTransactionByTransactionId(String id) {
        Transaction transaction= transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
        return TransactionResponseDTO.builder()
                .sourceWalletId(transaction.getSourceWalletId())
                .destinationWalletId(transaction.getDestinationWalletId())
                .amount(transaction.getAmount())
                .createdAt(transaction.createdAt)
                .build();
    }

    @Override
    public Transaction getTransactionById(String id) throws Exception {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));

    }

    @Override
    public List<Transaction> getTransactionsByWalletId(String walletId) {
        List<Transaction> transactions = transactionRepository.findByWalletId(walletId);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found for wallet ID: " + walletId);
        }
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        List<Transaction> transactions = transactionRepository.findByStatus(status);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found with status: " + status);
        }
        return transactions;
    }

    @Override
    public Transaction getTransactionsBySagaInstanceId(String sagaInstanceId) {
        Transaction transactions = transactionRepository.findBySagaInstanceId(sagaInstanceId);
        if (transactions == null) {
            throw new ResourceNotFoundException("No transaction found for saga instance ID: " + sagaInstanceId);
        }
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionsBySourceWalletId(String walletId) {
        List<Transaction> transactions = transactionRepository.findBySourceWalletId(walletId);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found for source wallet ID: " + walletId);
        }
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionsByDestinationWalletId(String walletId) {
        List<Transaction> transactions = transactionRepository.findByDestinationWalletId(walletId);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found for destination wallet ID: " + walletId);
        }
        return transactions;
    }

    @Override
    @Transactional
    public Transaction createTransaction(String sourceWalletId, String destinationWalletId, BigDecimal amount) {
        log.info("Creating Transaction with sourceWalletId {} DestinationWallet Id {} amount {}", sourceWalletId,
                destinationWalletId, amount);
        Transaction transaction = Transaction.builder()
                .id(IdGenerator.generateId())
                .sourceWalletId(sourceWalletId)
                .destinationWalletId(destinationWalletId)
                .amount(amount)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction Created with Id {}", savedTransaction.getId());
        return savedTransaction;
    }

    @Override
    public void updateTransactionWithSagaInstanceId(String id, String sagaInstanceId) throws Exception {
        Transaction transaction = getTransactionById(id);
        transaction.setSagaInstanceId(sagaInstanceId);
        transactionRepository.save(transaction);
        log.info("Transaction Updated with id {} ans SagaInstanceId {}", id, sagaInstanceId);
    }

    @Override
    @Transactional
    public void updateTransactionStatus(String sagaInstanceId, TransactionStatus status) throws Exception {
        Transaction transaction = transactionRepository.findBySagaInstanceId(sagaInstanceId);
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public String createDepositTransaction(String userId, BigDecimal amount) throws Exception {
        log.info("Creating Deposit Transaction with userId {} amount {}", userId, amount);
        Transaction transaction = Transaction.builder()
                .id(IdGenerator.generateId())
                .sourceWalletId(userId)
                .destinationWalletId(userId)
                .transactionType(TransactionType.DEPOSIT)
                .amount(amount)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Deposit Transaction Created with Id {}", savedTransaction.getId());
        return savedTransaction.getId();
    }

    @Override
    @Transactional
    public String createWithdrawTransaction(String userId, BigDecimal amount) throws Exception {
        log.info("Creating Withdrawal Transaction with userId {} amount {}", userId, amount);
        Transaction transaction = Transaction.builder()
                .id(IdGenerator.generateId())
                .sourceWalletId(userId)
                .destinationWalletId(userId)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(amount)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Withdrawal Transaction Created with Id {}", savedTransaction.getId());
        return savedTransaction.getId();
    }

    @Override
    @Transactional
    public void updateTransactionStatusWithTransactionId(String transactionId, TransactionStatus status)
            throws Exception {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        transaction.setStatus(status);
        log.info("Updating Transaction Status with Id {} and status {}", transaction.getId(), status.toString());
        transactionRepository.save(transaction);
    }

}
