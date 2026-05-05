package com.example.vaultr.services;

import com.example.vaultr.dto.CreateWalletRequestDTO;
import com.example.vaultr.dto.CreditWalletRequestDTO;
import com.example.vaultr.dto.DebitWalletRequestDTO;
import com.example.vaultr.entities.Wallet;

import java.math.BigDecimal;

public interface IWalletService {
    Wallet createWallet(CreateWalletRequestDTO requestDTO) throws Exception;
    Wallet getWalletByUserId(Long userId) throws Exception;
    Wallet getWalletById(Long walletId) throws Exception;
    Wallet creditMoneyToWallet(CreditWalletRequestDTO requestDTO) throws Exception;
    Wallet debitMoneyFromWallet(DebitWalletRequestDTO requestDTO) throws Exception;
    BigDecimal getWalletBalance(Long walletId) throws Exception;
}
