package com.example.vaultr.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"transaction_id", "event_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    public String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String message;

}