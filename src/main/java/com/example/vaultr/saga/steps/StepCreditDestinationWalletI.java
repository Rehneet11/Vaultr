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
public class StepCreditDestinationWalletI implements ISagaStep {
    private final WalletRepository walletRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public StepCreditDestinationWalletI(WalletRepository walletRepository, RedisTemplate<String, Object> redisTemplate) {
        this.walletRepository = walletRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean execute(SAGAContext context) throws Exception {
        long start = System.currentTimeMillis();
        String sagaInstanceId = context.getString("transactionId");
        String idempotencyKey = "saga:step:execute:" + getStepName() + ":" + sagaInstanceId;

        if (redisTemplate.hasKey(idempotencyKey)) {
            return true; // Already executed
        }

        String destinationWalletId = context.getString("destinationWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByUserIdWithLock(destinationWalletId)
                .orElseThrow(() -> new com.example.vaultr.exceptions.ResourceNotFoundException(
                        "Wallet not found for user ID: " + destinationWalletId));

        context.addContext("DestinationWalletBalanceBeforeCredit", wallet.getBalance());

        wallet.creditAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterCredit", wallet.getBalance());
        context.addContext("TransactionStatusAfterCreditSuccess", TransactionStatus.COMPLETED);

        redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", 24, TimeUnit.HOURS);
        log.info("Execute Credit step took {} ms", System.currentTimeMillis() - start);
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

        String destinationWalletId = context.getString("destinationWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByUserIdWithLock(destinationWalletId)
                .orElseThrow(() -> new com.example.vaultr.exceptions.ResourceNotFoundException(
                        "Wallet not found for user ID: " + destinationWalletId));

        wallet.debitAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterCreditCompensation", wallet.getBalance());
        context.addContext("TransactionStatusAfterCreditCompensated", TransactionStatus.FAILED);

        redisTemplate.opsForValue().set(idempotencyKey, "COMPENSATED", 24, TimeUnit.HOURS);
        log.info("Compensate Credit step took {} ms", System.currentTimeMillis() - start);
        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepType.CREDIT_DESTINATION_WALLET.toString();
    }
}
