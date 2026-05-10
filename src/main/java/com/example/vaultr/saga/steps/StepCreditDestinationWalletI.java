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
public class StepCreditDestinationWalletI implements ISagaStep {
    private final WalletRepository walletRepository;

    public StepCreditDestinationWalletI(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public boolean execute(SAGAContext context) throws Exception {
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

        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SAGAContext context) throws Exception {
        String destinationWalletId = context.getString("destinationWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        Wallet wallet = walletRepository.findByUserIdWithLock(destinationWalletId)
                .orElseThrow(() -> new com.example.vaultr.exceptions.ResourceNotFoundException(
                        "Wallet not found for user ID: " + destinationWalletId));

        wallet.debitAmount(amount);
        walletRepository.save(wallet);

        context.addContext("DestinationWalletBalanceAfterCreditCompensation", wallet.getBalance());
        context.addContext("TransactionStatusAfterCreditCompensated", TransactionStatus.FAILED);

        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepType.CREDIT_DESTINATION_WALLET.toString();
    }
}
