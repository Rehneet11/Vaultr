package com.example.vaultr.dto;

import lombok.*;

import com.example.vaultr.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDTO {
    private String transactionId;
    private String sourceWalletId;
    private String destinationWalletId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private String status;
}
