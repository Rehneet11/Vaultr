package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.Wallet;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.repositories.WalletRepository;
import com.example.vaultr.saga.SAGAContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StepDebitSourceWalletI implements ISagaStep {
    private final WalletRepository walletRepository;

    public StepDebitSourceWalletI(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public boolean execute(SAGAContext context) throws Exception {
        Long sourceWalletId = context.getLong("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByUserIdWithLock(sourceWalletId)
                .orElseThrow(()-> new Exception("Cannot Find Wallet"));

        context.addContext("SourceWalletBalanceBeforeDebit",wallet.getBalance());

        wallet.debitAmount(amount);
        walletRepository.save(wallet);

        context.addContext("SourceWalletBalanceAfterDebit",wallet.getBalance());
        context.addContext("TransactionStatusAfterDebitSuccess", TransactionStatus.COMPLETED);

        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SAGAContext context) throws Exception {
        Long sourceWalletId = context.getLong("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByUserIdWithLock(sourceWalletId)
                .orElseThrow(()-> new Exception("Cannot Find Wallet"));

        wallet.creditAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterDebitCompensation",wallet.getBalance());
        context.addContext("TransactionStatusAfterDebitCompensated",TransactionStatus.FAILED);

        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepType.DEBIT_SOURCE_WALLET.toString();
    }
}
