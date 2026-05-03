package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.SagaStep;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SagaStepFactory {
    private final Map<String, ISagaStep> sagaStepMap;

    public SagaStepFactory(Map<String, ISagaStep> sagaStepMap) {
        this.sagaStepMap = sagaStepMap;
    }

    public ISagaStep getSagaStep(String stepName){
        return sagaStepMap.get(stepName);
    }
}
