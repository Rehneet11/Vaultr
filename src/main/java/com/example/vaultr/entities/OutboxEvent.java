package com.example.vaultr.entities;

import com.example.vaultr.enums.EventStatus;
import com.example.vaultr.enums.TransactionStatus;
import com.example.vaultr.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbox_event")
public class OutboxEvent extends BaseEntity{
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    public String id;

    @Enumerated(EnumType.STRING)
    @Column(name="transaction_type",nullable = false)
    private TransactionType transactionType;

    @Column(name="transaction_id",nullable = false)
    private String transactionId;

    @Column(name = "event_type",nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

}
