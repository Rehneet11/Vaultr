package com.example.vaultr.entities;

import com.example.vaultr.exceptions.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "Wallet")
public class Wallet extends BaseEntity{

    @Column(name = "user_id", nullable = false,updatable = false)
    private Long userId;

    @Column(name = "balance",nullable = false)
    @Builder.Default
    private BigDecimal balance=BigDecimal.ZERO;

    @Column(name = "is_Active",nullable = false)
    private Boolean isActive;

    public boolean hasEnoughBalance(BigDecimal amount){
        return balance.compareTo(amount)>=0;
    }

    public void debitAmount(BigDecimal amount) {
        if(!hasEnoughBalance(amount)){
            throw new InsufficientBalanceException("Insufficient balance. Available: " + balance + ", Requested: " + amount);
        }
        balance=balance.subtract(amount);
    }

    public void creditAmount(BigDecimal amount){
        balance=balance.add(amount);
    }

}
