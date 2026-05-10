package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.Wallet;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.repositories.WalletRepository;
import com.example.vaultr.saga.SAGAContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StepDebitSourceWalletI implements ISagaStep {
    private final WalletRepository walletRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public StepDebitSourceWalletI(WalletRepository walletRepository, RedisTemplate<String, Object> redisTemplate) {
        this.walletRepository = walletRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean execute(SAGAContext context) throws Exception {
        long start = System.currentTimeMillis();
        String sagaInstanceId = context.getString("transactionId"); // Using transactionId as saga identifier since it maps 1:1
        String idempotencyKey = "saga:step:execute:" + getStepName() + ":" + sagaInstanceId;

        if (redisTemplate.hasKey(idempotencyKey)) {
            return true; // Already executed
        }

        String sourceWalletId = context.getString("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByUserIdWithLock(sourceWalletId)
                .orElseThrow(() -> new com.example.vaultr.exceptions.ResourceNotFoundException(
                        "Wallet not found for user ID: " + sourceWalletId));

        context.addContext("SourceWalletBalanceBeforeDebit", wallet.getBalance());

        wallet.debitAmount(amount);
        walletRepository.save(wallet);

        context.addContext("SourceWalletBalanceAfterDebit", wallet.getBalance());
        context.addContext("TransactionStatusAfterDebitSuccess", TransactionStatus.COMPLETED);

        redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", 24, TimeUnit.HOURS);
        log.info("Execute Debit step took {} ms", System.currentTimeMillis() - start);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean compensate(SAGAContext context) throws Exception {
        long start = System.currentTimeMillis();
        String sagaInstanceId = context.getString("transactionId");
        String idempotencyKey = "saga:step:compensate:" + getStepName() + ":" + sagaInstanceId;

        if (redisTemplate.hasKey(idempotencyKey)) {
            return true; // Already compensated
        }

        String sourceWalletId = context.getString("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByUserIdWithLock(sourceWalletId)
                .orElseThrow(() -> new com.example.vaultr.exceptions.ResourceNotFoundException(
                        "Wallet not found for user ID: " + sourceWalletId));

        wallet.creditAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterDebitCompensation", wallet.getBalance());
        context.addContext("TransactionStatusAfterDebitCompensated", TransactionStatus.FAILED);

        redisTemplate.opsForValue().set(idempotencyKey, "COMPENSATED", 24, TimeUnit.HOURS);
        log.info("Compensate Debit step took {} ms", System.currentTimeMillis() - start);
        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepType.DEBIT_SOURCE_WALLET.toString();
    }
}
