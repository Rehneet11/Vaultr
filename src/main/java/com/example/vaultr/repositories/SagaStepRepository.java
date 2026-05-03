package com.example.vaultr.repositories;

import com.example.vaultr.entities.SagaStep;
import com.example.vaultr.enums.SagaStatus;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep,Long> {
    List<SagaStep> findBySagaInstanceId(Long sagaInstanceId);

    @Query("SELECT s from SagaStep s where s.sagaInstanceId = :sagaInstanceId AND s.status = :status")
    List<SagaStep> findStepsBySagaInstanceIdAndStatus(@Param("sagaInstanceId") Long sagaInstanceId, @Param("status") StepStatus status);

    @Query("SELECT s from SagaStep s where s.sagaInstanceId = :sagaInstanceId AND s.status IN ('COMPLETED', 'COMPENSATED')")
    List<SagaStep> findCompletedOrCompensatedStepsBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId);

    @Query("SELECT s from SagaStep s where s.stepName = :stepName AND s.sagaInstanceId = :sgaInstanceId AND s.status = :status")
    Optional<SagaStep> findByStepNameAndSagaInstanceIdAndStatus(@Param("stepName") String stepName, @Param("sagaInstanceId") Long sagaInstanceId, @Param("status") StepStatus status);

}
