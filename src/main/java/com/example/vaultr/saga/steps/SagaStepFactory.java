package com.example.vaultr.saga.steps;

import com.example.vaultr.entities.SagaStep;
import com.example.vaultr.enums.SagaStepType;
import org.springframework.stereotype.Component;

import java.util.List;
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

     public static final List<SagaStepType> steps = List.of(
            SagaStepType.DEBIT_SOURCE_WALLET,
            SagaStepType.CREDIT_DESTINATION_WALLET,
            SagaStepType.UPDATE_TRANSACTION_STATUS
    );
}
