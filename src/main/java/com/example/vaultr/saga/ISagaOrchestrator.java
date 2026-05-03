package com.example.vaultr.saga;

import com.example.vaultr.entities.SagaInstance;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ISagaOrchestrator {
    Long startSaga(SAGAContext context);
    void markSagaComplete(Long sagaInstanceId) throws Exception;
    void markSagaFailed(Long sagaInstanceId) throws Exception;
    SagaInstance getSagaInstance(Long sagaInstanceId) throws Exception;
    boolean executeStep(Long sagaInstanceId, String stepName) throws Exception;
    boolean compensateStep(Long sagaInstanceId, String stepName) throws Exception;
    void compensateSaga(Long sagaInstanceId) throws Exception;
}
