package com.example.vaultr.controllers;

import com.example.vaultr.dto.ApiErrorResponseDTO;
import com.example.vaultr.dto.ApiValidationErrorResponseDTO;
import com.example.vaultr.dto.CreateWalletRequestDTO;
import com.example.vaultr.dto.CreditWalletRequestDTO;
import com.example.vaultr.dto.DebitWalletRequestDTO;
import com.example.vaultr.entities.Wallet;
import com.example.vaultr.services.IWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallets")
@Slf4j
@RateLimiter(name = "api")
@Tag(name = "Wallets", description = "Core Ledger & Wallet Balance Operations")
public class WalletController {
    private final IWalletService walletService;

    public WalletController(IWalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Wallet Details",
            description = "Retrieves the full wallet object, including metadata and sharding information, using the unique Wallet ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet Found"),
            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "429", description = "Rate Limit Exceeded",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Wallet> getWalletById(
            @Parameter(description = "The unique wallet ID", required = true)
            @PathVariable String id) throws Exception {
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/balance/{id}")
    @Operation(
            summary = "Check Wallet Balance",
            description = "Returns the current decimal balance of the wallet. This performs a read-consistent check against the sharded database."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance Retrieved"),
            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<BigDecimal> getWalletBalanceById(
            @Parameter(description = "The unique wallet ID", required = true)
            @PathVariable String id) throws Exception {
        BigDecimal balance = walletService.getWalletBalance(id);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/user/{id}")
    @Operation(
            summary = "Get Wallet by User ID",
            description = "Finds the wallet associated with a specific user. In Vaultr, Wallet ID and User ID are typically mapped 1:1 to optimize sharding lookups."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet Found"),
            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Wallet> getWalletByUserId(
            @Parameter(description = "The unique User ID", required = true)
            @PathVariable String id) throws Exception {
        Wallet wallet = walletService.getWalletByUserId(id);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/debit_wallet")
    @Operation(
            summary = "Direct Wallet Debit",
            description = "Directly deducts funds from a wallet. Used for internal ledger adjustments or withdrawals. Uses Pessimistic Locking to prevent race conditions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Debit Successful"),
            @ApiResponse(responseCode = "422", description = "Insufficient Balance",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation Error",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Wallet> debitWallet(
            @Parameter(description = "Debit request details", required = true)
            @Valid @RequestBody DebitWalletRequestDTO requestDTO) throws Exception {
        Wallet wallet = walletService.debitMoneyFromWallet(requestDTO);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/credit_wallet")
    @Operation(
            summary = "Direct Wallet Credit",
            description = "Directly adds funds to a wallet. Used for deposits or incoming top-ups."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit Successful"),
            @ApiResponse(responseCode = "400", description = "Validation Error",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Wallet> creditWallet(
            @Parameter(description = "Credit request details", required = true)
            @Valid @RequestBody CreditWalletRequestDTO requestDTO) throws Exception {
        Wallet wallet = walletService.creditMoneyToWallet(requestDTO);
        return ResponseEntity.ok(wallet);
    }
}
