package com.example.vaultr.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    private String sourceWalletId;
    private String destinationWalletId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
