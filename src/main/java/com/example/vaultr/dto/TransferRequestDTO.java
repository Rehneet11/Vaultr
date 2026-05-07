package com.example.vaultr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {
    @NotNull(message = "Source wallet ID is required")
    private Long sourceWalletId;

    @NotNull(message = "Destination wallet ID is required")
    private Long destinationWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}
