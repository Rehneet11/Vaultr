package com.example.vaultr.entities;

import com.example.vaultr.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_instance")
public class SagaInstance extends BaseEntity{
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status=SagaStatus.STARTED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context",columnDefinition = "jsonb")
    private String context;

    @Column(name = "current_step", nullable = false)
    private String currentStep;
}
