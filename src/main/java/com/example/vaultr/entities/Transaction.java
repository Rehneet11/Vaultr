package com.example.vaultr.entities;

import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
@Entity
@Table(name = "transaction")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Transaction extends BaseEntity{
    @Column(name = "source_wallet_id",nullable = false)
    private Long sourceWalletId;

    @Column(name = "destination_wallet_id",nullable = false)
    private Long destinationWalletId;

    @Column(name = "amount",nullable = false)
    private BigDecimal amount;

    @Column(name = "saga_instance_id",nullable = false)
    private Long sagaInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name="transaction_type",nullable = false)
    private TransactionType transactionType=TransactionType.TRANSFER;
}
