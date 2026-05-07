package com.example.vaultr.dto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayloadDTO {
    public Long userId;
    public Long transactionId;
    public String eventType;
    public String message;
}
