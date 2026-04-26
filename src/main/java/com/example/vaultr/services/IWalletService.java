package com.example.vaultr.services;

public interface IWalletService {
    boolean debit(String userId, double amount);
    boolean credit(String userId, double amount);
    boolean refund(String userId, double amount);
}
