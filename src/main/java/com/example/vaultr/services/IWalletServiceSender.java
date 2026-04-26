package com.example.vaultr.services;

public interface IWalletServiceSender {
    boolean prepare(String userId, double amount);
    boolean commit(String userId, double amount);
    void rollback(String userId);
}
