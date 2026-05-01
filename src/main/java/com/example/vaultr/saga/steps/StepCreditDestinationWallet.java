package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.Wallet;
import com.example.vaultr.repositories.WalletRepository;
import com.example.vaultr.saga.SAGAContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StepCreditDestinationWallet implements SAGAStep{
    private final WalletRepository walletRepository;

    public StepCreditDestinationWallet(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public boolean execute(SAGAContext context) throws Exception {
        Long destinationWalletId = context.getLong("destinationWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByIdWithLock(destinationWalletId)
                .orElseThrow(()-> new Exception("Cannot Find Wallet"));

        context.addContext("DestinationWalletBalanceBeforeCredit",wallet.getBalance());

        wallet.creditAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterCredit",wallet.getBalance());

        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SAGAContext context) throws Exception {
        Long destinationWalletId = context.getLong("destinationWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByIdWithLock(destinationWalletId)
                .orElseThrow(()-> new Exception("Cannot Find Wallet"));

        wallet.debitAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterCreditCompensation",wallet.getBalance());

        return true;
    }

    @Override
    public String getStepName() {
        return "StepCreditDestinationWallet";
    }
}
