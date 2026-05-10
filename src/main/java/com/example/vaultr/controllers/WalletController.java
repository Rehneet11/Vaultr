package com.example.vaultr.controllers;

import com.example.vaultr.dto.CreateWalletRequestDTO;
import com.example.vaultr.dto.CreditWalletRequestDTO;
import com.example.vaultr.dto.DebitWalletRequestDTO;
import com.example.vaultr.entities.Wallet;
import com.example.vaultr.services.IWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/wallets")
@Slf4j
@RateLimiter(name = "api")
public class WalletController {
    private final IWalletService walletService;

    public WalletController(IWalletService walletService) {
        this.walletService = walletService;
    }


    @GetMapping("/{id}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable String id) throws Exception {
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/balance/{id}")
    public ResponseEntity<BigDecimal> getWalletBalanceById(@PathVariable String id) throws Exception {
        BigDecimal balance= walletService.getWalletBalance(id);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<Wallet> getWalletByUserId(@PathVariable String id) throws Exception{
        Wallet wallet = walletService.getWalletByUserId(id);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/debit_wallet")
    public ResponseEntity<Wallet> debitWallet(@Valid @RequestBody DebitWalletRequestDTO requestDTO) throws Exception {
        Wallet wallet = walletService.debitMoneyFromWallet(requestDTO);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/credit_wallet")
    public ResponseEntity<Wallet> creditWallet(@Valid @RequestBody CreditWalletRequestDTO requestDTO) throws Exception{
        Wallet wallet = walletService.creditMoneyToWallet(requestDTO);
        return ResponseEntity.ok(wallet);
    }
}
