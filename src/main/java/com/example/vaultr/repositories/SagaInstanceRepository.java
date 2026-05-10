package com.example.vaultr.repositories;

import com.example.vaultr.entities.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.vaultr.enums.SagaStatus;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, String> {
    List<SagaInstance> findByStatusAndUpdatedAtBeforeAndRetryCountLessThan(SagaStatus status, LocalDateTime threshold, Integer maxRetries);
}
