package com.example.vaultr.saga.steps;

import com.example.vaultr.saga.SAGAContext;

public interface ISagaStep {
    boolean execute(SAGAContext context) throws Exception;
    boolean compensate(SAGAContext context) throws Exception;
    String getStepName();
}
