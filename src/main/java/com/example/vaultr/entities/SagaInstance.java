package com.example.vaultr.entities;

import com.example.vaultr.enums.SagaStatus;
import com.example.vaultr.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "saga_instance")
public class SagaInstance extends BaseEntity{
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    public String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(50)")
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "json")
    private String context;

    @Column(name = "current_step")
    private String currentStep;

    public void markAsCompleted(){
        this.status=SagaStatus.COMPLETED;
    }

    public void markAsFailed(){
        this.status =SagaStatus.FAILED;
    }

    public void markAsCompensating(){
        this.status =SagaStatus.COMPENSATING;
    }

    public void markAsProcessing(){
        this.status =SagaStatus.PROCESSING;
    }

    public void markAsStarted(){
        this.status =SagaStatus.STARTED;
    }

    public void markAsPending(){
        this.status =SagaStatus.PENDING;
    }

    public void markAsCompensated(){
        this.status =SagaStatus.COMPENSATED;
    }
}
