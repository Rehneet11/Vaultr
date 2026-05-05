package com.example.vaultr.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {
    private Long sourceWalletId;
    private Long destinationWalletId;
    private BigDecimal amount;
}
