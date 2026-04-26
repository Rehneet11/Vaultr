package com.example.vaultr.services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WalletServiceSender implements IWalletServiceSender {

    private Map<String,Double> balances = new HashMap<>();
    private Map<String,Double> lockedAmount = new HashMap<>();



    @Override
    public boolean prepare(String userId, double amount) {
        double balance = balances.getOrDefault(userId,100.0);
        if(balance>=amount){
            lockedAmount.put(userId, amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean commit(String userId, double amount) {
        double locked = lockedAmount.getOrDefault(userId,50.0);
        balances.put(userId,balances.getOrDefault(userId,100.0)-locked);
        lockedAmount.remove(userId);
        return true;
    }

    @Override
    public void rollback(String userId) {
        lockedAmount.remove(userId);
    }
}
