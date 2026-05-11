package com.example.vaultr.services;

import com.example.vaultr.dto.CreditWalletRequestDTO;
import com.example.vaultr.dto.DebitWalletRequestDTO;
import com.example.vaultr.entities.User;
import com.example.vaultr.entities.Wallet;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.enums.TransactionType;
import com.example.vaultr.exceptions.DuplicateResourceException;
import com.example.vaultr.exceptions.InsufficientBalanceException;
import com.example.vaultr.exceptions.ResourceNotFoundException;
import com.example.vaultr.repositories.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Service
@Slf4j
public class WalletService implements IWalletService {
    private final WalletRepository walletRepository;
    private final ITransactionService transactionService;
    private final ObjectMapper objectMapper;
    private final NotificationPublisherService notificationPublisherService;


    public WalletService(WalletRepository walletRepository, ITransactionService transactionService, ObjectMapper objectMapper, NotificationPublisherService notificationPublisherService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.objectMapper = objectMapper;
        this.notificationPublisherService = notificationPublisherService;
    }

    @Override
    @Transactional
    public Wallet createWallet(User user) throws Exception {
        String userId = user.id;
        ;
        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new DuplicateResourceException("Wallet already exists for user ID: " + userId);
        }
        Wallet wallet = Wallet.builder()
                .id(userId)
                .userId(userId)
                .isActive(true)
                .build();
        Wallet createdWallet = walletRepository.save(wallet);
        log.info("Wallet Created with Id {}", createdWallet.getId());
        return wallet;
    }

    @Override
    public Wallet getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user ID: " + userId));
    }

    @Override
    public Wallet getWalletById(String walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with ID: " + walletId));
    }

    @Override
    @Transactional
    public Wallet creditMoneyToWallet(CreditWalletRequestDTO requestDTO) throws Exception {
        String userId = requestDTO.getUserId();
        BigDecimal amount = requestDTO.getAmount();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user ID: " + userId));
        String transactionId = transactionService.createDepositTransaction(userId, amount);
        wallet.creditAmount(amount);
        walletRepository.save(wallet);
        transactionService.updateTransactionStatusWithTransactionId(transactionId, TransactionStatus.COMPLETED);
        notificationPublisherService.addNotification(transactionId,TransactionType.DEPOSIT,userId,amount,"WALLET_CREDITED","You credited funds of ₹");
        return wallet;
    }

    @Override
    @Transactional
    public Wallet debitMoneyFromWallet(DebitWalletRequestDTO requestDTO) throws Exception {
        String userId = requestDTO.getUserId();
        BigDecimal amount = requestDTO.getAmount();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user ID: " + userId));
        String transactionId = transactionService.createWithdrawTransaction(userId, amount);
        if (amount.compareTo(wallet.getBalance()) > 0) {
            transactionService.updateTransactionStatusWithTransactionId(transactionId, TransactionStatus.FAILED);
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + wallet.getBalance() + ", Requested: " + amount);
        }
        wallet.debitAmount(amount);
        walletRepository.save(wallet);
        transactionService.updateTransactionStatusWithTransactionId(transactionId, TransactionStatus.COMPLETED);
        notificationPublisherService.addNotification(transactionId,TransactionType.WITHDRAWAL,userId,amount,"WALLET_DEBITED","You did withdrawal funds of ₹");
        return wallet;
    }

    @Override
    public BigDecimal getWalletBalance(String walletId) {
        Wallet wallet = getWalletById(walletId);
        return wallet.getBalance();
    }

}
