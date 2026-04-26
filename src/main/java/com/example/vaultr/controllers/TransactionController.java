package com.example.vaultr.controllers;

import com.example.vaultr.coordinators.TwoPhaseCoordinator;
import com.example.vaultr.dto.TransactionRequestDTO;
import com.example.vaultr.dto.TransactionResponseDTO;
import com.example.vaultr.orchestrators.PaymentOrchestrator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/transaction")
public class TransactionController {
    private final TwoPhaseCoordinator twoPhaseCoordinator;
    private final PaymentOrchestrator paymentOrchestrator;

    public TransactionController(TwoPhaseCoordinator twoPhaseCoordinator, PaymentOrchestrator paymentOrchestrator) {
        this.twoPhaseCoordinator = twoPhaseCoordinator;
        this.paymentOrchestrator = paymentOrchestrator;
    }

    @PostMapping("/pc")
    public TransactionResponseDTO doTransactionPC(@RequestBody TransactionRequestDTO transactionRequestDTO){
        return twoPhaseCoordinator.doTransaction(transactionRequestDTO);
    }

    @PostMapping("/orchsaga")
    public TransactionResponseDTO doTransactionOrchSaga(@RequestBody TransactionRequestDTO transactionRequestDTO){
        return paymentOrchestrator.doTransaction(transactionRequestDTO);
    }
}
