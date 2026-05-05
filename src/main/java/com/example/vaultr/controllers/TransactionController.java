package com.example.vaultr.controllers;

import com.example.vaultr.dto.TransferRequestDTO;
import com.example.vaultr.dto.TransferResponseDTO;
import com.example.vaultr.entities.Transaction;
import com.example.vaultr.services.ITransactionService;
import com.example.vaultr.services.ITransferSAGAService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/transactions")
public class TransactionController {
    private final ITransactionService transactionService;
    private final ITransferSAGAService transferSAGAService;

    public TransactionController(ITransactionService transactionService, ITransferSAGAService transferSAGAService) {
        this.transactionService = transactionService;
        this.transferSAGAService = transferSAGAService;
    }

    @PostMapping
    public ResponseEntity<TransferResponseDTO> createTransaction(@RequestBody TransferRequestDTO transferRequestDTO) throws Exception {
        Long sagaInstanceId= transferSAGAService.initiateTransfer(transferRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                TransferResponseDTO.builder()
                        .sagaInstanceId(sagaInstanceId)
                        .build()
        );
    }
}
