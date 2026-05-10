package com.example.vaultr.controllers;


import com.example.vaultr.annotations.Idempotent;
import com.example.vaultr.dto.TransactionResponseDTO;
import com.example.vaultr.dto.TransferRequestDTO;
import com.example.vaultr.dto.TransferResponseDTO;
import com.example.vaultr.services.ITransactionService;
import com.example.vaultr.services.ITransferSAGAService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;
import java.util.List;

@RestController
@RequestMapping("api/transactions")
public class TransactionController {
    private final ITransactionService transactionService;
    private final ITransferSAGAService transferSAGAService;

    public TransactionController(ITransactionService transactionService, ITransferSAGAService transferSAGAService) {
        this.transactionService = transactionService;
        this.transferSAGAService = transferSAGAService;
    }

    @Idempotent
    @PostMapping
    @RateLimiter(name = "transactionApi")
    public ResponseEntity<TransferResponseDTO> createTransaction(@Valid @RequestBody TransferRequestDTO transferRequestDTO) throws Exception {
        TransferResponseDTO responseDTO = transferSAGAService.initiateTransfer(transferRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                responseDTO
        );
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(@PathVariable String transactionId) throws Exception{
        return ResponseEntity.status(HttpStatus.FOUND).body(transactionService.getTransactionByTransactionId(transactionId));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<Transaction>> getTransactionsByWalletId(@PathVariable String walletId) throws Exception {
        return ResponseEntity.ok(transactionService.getTransactionsByWalletId(walletId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Transaction>> getTransactionsByStatus(@PathVariable TransactionStatus status) throws Exception {
        return ResponseEntity.ok(transactionService.getTransactionsByStatus(status));
    }

    @GetMapping("/saga/{sagaInstanceId}")
    public ResponseEntity<Transaction> getTransactionsBySagaInstanceId(@PathVariable String sagaInstanceId) throws Exception {
        return ResponseEntity.ok(transactionService.getTransactionsBySagaInstanceId(sagaInstanceId));
    }

    @GetMapping("/wallet/source/{walletId}")
    public ResponseEntity<List<Transaction>> getTransactionsBySourceWalletId(@PathVariable String walletId) throws Exception {
        return ResponseEntity.ok(transactionService.getTransactionsBySourceWalletId(walletId));
    }

    @GetMapping("/wallet/destination/{walletId}")
    public ResponseEntity<List<Transaction>> getTransactionsByDestinationWalletId(@PathVariable String walletId) throws Exception {
        return ResponseEntity.ok(transactionService.getTransactionsByDestinationWalletId(walletId));
    }
    
}
