package com.example.vaultr.config;

import com.example.vaultr.saga.steps.ISagaStep;
import com.example.vaultr.enums.SagaStepType;
import com.example.vaultr.saga.steps.StepCreditDestinationWalletI;
import com.example.vaultr.saga.steps.StepDebitSourceWalletI;
import com.example.vaultr.saga.steps.StepUpdateTransactionStatusI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SagaConfiguration {

    @Bean
    public Map<String, ISagaStep> sagaStepMap(
            StepCreditDestinationWalletI creditDestinationWallet,
            StepDebitSourceWalletI debitSourceWallet,
            StepUpdateTransactionStatusI updateTransactionStatus
    ){
        Map<String, ISagaStep> map = new HashMap<>();
        map.put(SagaStepType.CREDIT_DESTINATION_WALLET.toString(),creditDestinationWallet);
        map.put(SagaStepType.DEBIT_SOURCE_WALLET.toString(),debitSourceWallet);
        map.put(SagaStepType.UPDATE_TRANSACTION_STATUS.toString(),updateTransactionStatus);
        return map;
    }
}
