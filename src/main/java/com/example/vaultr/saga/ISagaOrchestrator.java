package com.example.vaultr.saga;

import com.example.vaultr.entities.SagaInstance;

public interface ISagaOrchestrator {
    String startSaga(SAGAContext context);
    void markSagaComplete(String sagaInstanceId) throws Exception;
    void markSagaFailed(String sagaInstanceId) throws Exception;
    SagaInstance getSagaInstance(String sagaInstanceId) throws Exception;
    boolean executeStep(String sagaInstanceId, String stepName) throws Exception;
    boolean compensateStep(String sagaInstanceId, String stepName) throws Exception;
    void compensateSaga(String sagaInstanceId) throws Exception;
}
