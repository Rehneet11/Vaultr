package com.example.vaultr.dto;


import lombok.*;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitWalletRequestDTO {
    private Long userId;
    private BigDecimal amount;

}
