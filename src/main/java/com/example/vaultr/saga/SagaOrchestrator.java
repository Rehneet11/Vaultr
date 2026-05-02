package com.example.vaultr.saga;

import com.example.vaultr.entities.SagaInstance;

public interface SagaOrchestrator {
    Long startSaga(SAGAContext context);
    void markSagaComplete(Long sagaInstanceId);
    void markSagaFailed(Long sagaInstanceId);
    SagaInstance getSagaInstance(Long sagaInstanceId);
    boolean executeStep(Long sagaInstanceId, String stepName);
    boolean compensateStep(Long sagaInstanceId, String stepName);
    void compensateSaga(Long sagaInstanceId);
}
