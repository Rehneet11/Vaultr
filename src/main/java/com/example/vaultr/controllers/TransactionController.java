package com.example.vaultr.controllers;

import com.example.vaultr.coordinators.TwoPhaseCoordinator;
import com.example.vaultr.dto.TransactionRequestDTO;
import com.example.vaultr.dto.TransactionResponseDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/transaction")
public class TransactionController {
    private final TwoPhaseCoordinator twoPhaseCoordinator;

    public TransactionController(TwoPhaseCoordinator twoPhaseCoordinator) {
        this.twoPhaseCoordinator = twoPhaseCoordinator;
    }

    @PostMapping("")
    public TransactionResponseDTO doTransaction(@RequestBody TransactionRequestDTO transactionRequestDTO){
        return twoPhaseCoordinator.doTransaction(transactionRequestDTO);
    }
}
