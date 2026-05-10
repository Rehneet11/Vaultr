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
public class Transaction extends BaseEntity {
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    public String id;

    @Column(name = "source_wallet_id", nullable = false)
    private String sourceWalletId;

    @Column(name = "destination_wallet_id", nullable = false)
    private String destinationWalletId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "saga_instance_id")
    private String sagaInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType = TransactionType.TRANSFER;
}
