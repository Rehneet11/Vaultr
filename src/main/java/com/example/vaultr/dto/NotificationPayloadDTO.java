package com.example.vaultr.dto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayloadDTO {
    public String userId;
    public String transactionId;
    public String eventType;
    public String message;
}
