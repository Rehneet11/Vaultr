package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.Wallet;
import com.example.vaultr.repositories.WalletRepository;
import com.example.vaultr.saga.SAGAContext;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

public class StepDebitSourceWallet implements SAGAStep{
    private final WalletRepository walletRepository;

    public StepDebitSourceWallet(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public boolean execute(SAGAContext context) throws Exception {
        Long sourceWalletId = context.getLong("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByIdWithLock(sourceWalletId)
                .orElseThrow(()-> new Exception("Cannot Find Wallet"));

        if(!wallet.hasEnoughBalance(amount)){
            return false;
        }

        context.addContext("DestinationWalletBalanceBeforeDebit",wallet.getBalance());

        wallet.debitAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterDebit",wallet.getBalance());

        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SAGAContext context) throws Exception {
        Long sourceWalletId = context.getLong("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByIdWithLock(sourceWalletId)
                .orElseThrow(()-> new Exception("Cannot Find Wallet"));

        wallet.creditAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterDebitCompensation",wallet.getBalance());

        return true;
    }

    @Override
    public String getStepName() {
        return "StepDebitSourceWallet";
    }
}
