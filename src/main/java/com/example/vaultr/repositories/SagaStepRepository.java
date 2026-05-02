package com.example.vaultr.repositories;

import com.example.vaultr.entities.SagaStep;
import com.example.vaultr.enums.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SagaStepRepository extends JpaRepository<SagaStep,Long> {
    List<SagaStep> findBySagaInstanceId(Long sagaInstanceId);

    @Query("SELECT s from SagaStep s where s.sagaInstanceId = :sagaInstanceId AND s.status = :status")
    List<SagaStep> findStepsBySagaInstanceIdAndStatus(@Param("sagaInstanceId") Long sagaInstanceId, @Param("status") SagaStatus status);

    @Query("SELECT s from SagaStep s where s.sagaInstanceId = :sagaInstanceId AND s.status IN ('COMPLETED', 'COMPENSATED')")
    List<SagaStep> findCompletedOrCompensatedStepsBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId);
}
