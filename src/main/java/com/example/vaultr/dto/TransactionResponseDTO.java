package com.example.vaultr.dto;

import com.example.vaultr.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    private TransactionStatus status;
    private String sourceWalletId;
    private String destinationWalletId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
