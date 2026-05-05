package com.example.vaultr.services;


import com.example.vaultr.dto.CreateWalletRequestDTO;
import com.example.vaultr.dto.CreditWalletRequestDTO;
import com.example.vaultr.dto.DebitWalletRequestDTO;
import com.example.vaultr.entities.Wallet;
import com.example.vaultr.repositories.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class WalletService implements IWalletService{
    private final WalletRepository walletRepository;
    private final UserService userService;

    public WalletService(WalletRepository walletRepository, UserService userService) {
        this.walletRepository = walletRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public Wallet createWallet(CreateWalletRequestDTO requestDTO) throws Exception {
        Long userId= requestDTO.getUserId();
        if(userService.getUserById(userId)==null){
            throw new Exception("User Not Found");
        }
        if (walletRepository.findByUserId(userId).isPresent()){
            throw new Exception("Wallet Already Exists with this user Id");
        }
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .isActive(true)
                .build();
        Wallet createdWallet = walletRepository.save(wallet);
        log.info("Wallet Created with Id {}",createdWallet.getId());
        return wallet;
    }

    @Override
    public Wallet getWalletByUserId(Long userId) throws Exception {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new Exception("Cannot Find Wallet"));
    }

    @Override
    public Wallet getWalletById(Long walletId) throws Exception {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new Exception("Cannot Find Wallet"));
    }

    @Override
    @Transactional
    public Wallet creditMoneyToWallet(CreditWalletRequestDTO requestDTO) throws Exception {
        Long userId=requestDTO.getUserId();
        BigDecimal amount = requestDTO.getAmount();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new Exception("Cannot Find Wallet"));
        wallet.creditAmount(amount);
        walletRepository.save(wallet);
        return wallet;
    }

    @Override
    @Transactional
    public Wallet debitMoneyFromWallet(DebitWalletRequestDTO requestDTO) throws Exception {
        Long userId=requestDTO.getUserId();
        BigDecimal amount = requestDTO.getAmount();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new Exception("Cannot Find Wallet"));
        if(amount==null || amount.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Amount Must be positive");
        }
        if(amount.compareTo(wallet.getBalance())>0){
            throw new IllegalArgumentException("Insufficient Balance");
        }
        wallet.debitAmount(amount);
        walletRepository.save(wallet);
        return wallet;
    }

    @Override
    public BigDecimal getWalletBalance(Long walletId) throws Exception {
        Wallet wallet =getWalletById(walletId);
        return wallet.getBalance();
    }


}
