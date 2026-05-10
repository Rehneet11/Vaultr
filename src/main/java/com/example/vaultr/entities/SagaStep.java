package com.example.vaultr.entities;
import com.example.vaultr.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_step")
@Getter
@Setter
public class SagaStep extends BaseEntity{
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    public String id;

    @Column(name = "saga_instance_id",nullable = false)
    private String sagaInstanceId;

    @Column(name = "step_name",nullable = false)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "varchar(50)")
    private StepStatus status=StepStatus.STARTED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_data",columnDefinition = "json")
    private String StepData;

    @Column(name = "error_message")
    private String errorMessage;

    public void markAsCompleted(){
        this.status =StepStatus.COMPLETED;
    }

    public void markAsFailed(){
        this.status =StepStatus.FAILED;
    }

    public void markAsCompensating(){
        this.status =StepStatus.COMPENSATING;
    }

    public void markAsProcessing(){
        this.status =StepStatus.PROCESSING;
    }

    public void markAsStarted(){
        this.status =StepStatus.STARTED;
    }

    public void markAsPending(){
        this.status =StepStatus.PENDING;
    }

    public void markAsCompensated(){
        this.status =StepStatus.COMPENSATED;
    }
}
