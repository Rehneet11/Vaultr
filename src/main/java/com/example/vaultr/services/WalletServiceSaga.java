package com.example.vaultr.services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WalletServiceSaga implements IWalletService{
    Map<String, Double> balances = new HashMap<>();

    @Override
    public boolean debit(String userId, double amount) {
        if(userId.isEmpty() || balances.getOrDefault(userId,100.0)<amount) return false;
        balances.put(userId,balances.getOrDefault(userId,100.0)-amount);
        return true;
    }

    @Override
    public boolean credit(String userId, double amount) {
        if(userId.isEmpty()) return false;
        balances.put(userId,balances.getOrDefault(userId,100.0)+amount);
        return true;
    }

    @Override
    public boolean refund(String userId, double amount) {
        balances.put(userId,balances.getOrDefault(userId,100.0)+amount);
        return true;
    }
}
