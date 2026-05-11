package com.example.vaultr.controllers;

import com.example.vaultr.annotations.Idempotent;
import com.example.vaultr.dto.ApiConcurrentRequestErrorResponseDTO;
import com.example.vaultr.dto.ApiErrorResponseDTO;
import com.example.vaultr.dto.ApiValidationErrorResponseDTO;
import com.example.vaultr.dto.TransactionResponseDTO;
import com.example.vaultr.dto.TransferRequestDTO;
import com.example.vaultr.dto.TransferResponseDTO;
import com.example.vaultr.services.ITransactionService;
import com.example.vaultr.services.ITransferSAGAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;
import java.util.List;

@RestController
@RequestMapping("/api/transactions") // Fixed: Added leading slash
@Tag(name = "Transactions", description = "Distributed P2P SAGA Transfer API")
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
    @Operation(
            summary = "Initiate a P2P Transfer",
            description = "Triggers a distributed SAGA transaction to transfer funds between two sharded wallets. Requires an Idempotency-Key header."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer Initiated Successfully"),

            @ApiResponse(responseCode = "400", description = "Invalid Request or Validation Error",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "425", description = "Concurrent Request Detected",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiConcurrentRequestErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Insufficient Balance",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "429", description = "Rate Limit Exceeded",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<TransferResponseDTO> createTransaction(
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "Client-generated UUID v4 to prevent duplicate transfers", required = true)
            String idempotencyKey,

            @Valid @RequestBody
            @Parameter(description = "Transfer request payload", required = true)
            TransferRequestDTO transferRequestDTO) throws Exception {

        TransferResponseDTO responseDTO = transferSAGAService.initiateTransfer(transferRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Get Transaction by ID",
            description = "Retrieves the details of a specific transaction using its globally unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction Found"),
            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @Parameter(description = "The unique transaction ID", required = true)
            @PathVariable String transactionId) throws Exception {

        return ResponseEntity.ok(transactionService.getTransactionByTransactionId(transactionId));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(
            summary = "Get All Transactions for a Wallet",
            description = "Retrieves a history of all transactions (both inbound and outbound) associated with a specific wallet ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions Retrieved Successfully")
    })
    public ResponseEntity<List<Transaction>> getTransactionsByWalletId(
            @Parameter(description = "The unique wallet ID (User ID)", required = true)
            @PathVariable String walletId) throws Exception {

        return ResponseEntity.ok(transactionService.getTransactionsByWalletId(walletId));
    }

    @GetMapping("/wallet/source/{walletId}")
    @Operation(
            summary = "Get Outbound Transactions",
            description = "Retrieves a list of all transactions where the specified wallet acted as the sender (source)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions Retrieved Successfully")
    })
    public ResponseEntity<List<Transaction>> getTransactionsBySourceWalletId(
            @Parameter(description = "The source wallet ID", required = true)
            @PathVariable String walletId) throws Exception {

        return ResponseEntity.ok(transactionService.getTransactionsBySourceWalletId(walletId));
    }

    @GetMapping("/wallet/destination/{walletId}")
    @Operation(
            summary = "Get Inbound Transactions",
            description = "Retrieves a list of all transactions where the specified wallet acted as the receiver (destination)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions Retrieved Successfully")
    })
    public ResponseEntity<List<Transaction>> getTransactionsByDestinationWalletId(
            @Parameter(description = "The destination wallet ID", required = true)
            @PathVariable String walletId) throws Exception {

        return ResponseEntity.ok(transactionService.getTransactionsByDestinationWalletId(walletId));
    }
}
