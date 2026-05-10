package com.example.vaultr.saga;

import com.example.vaultr.entities.SagaInstance;
import com.example.vaultr.entities.SagaStep;
import com.example.vaultr.enums.SagaStatus;
import com.example.vaultr.enums.StepStatus;
import com.example.vaultr.repositories.SagaInstanceRepository;
import com.example.vaultr.repositories.SagaStepRepository;
import com.example.vaultr.repositories.TransactionRepository;
import com.example.vaultr.saga.steps.ISagaStep;
import com.example.vaultr.saga.steps.SagaStepFactory;
import com.example.vaultr.exceptions.ResourceNotFoundException;
import com.example.vaultr.exceptions.SagaExecutionException;
import com.example.vaultr.utils.IdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SagaOrchestrator implements ISagaOrchestrator{

    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepFactory sagaStepFactory;
    private final SagaStepRepository sagaStepRepository;

    public SagaOrchestrator(ObjectMapper objectMapper, SagaInstanceRepository sagaInstanceRepository, SagaStepFactory sagaStepFactory, SagaStepRepository sagaStepRepository, TransactionRepository transactionRepository) {
        this.objectMapper = objectMapper;
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepFactory = sagaStepFactory;
        this.sagaStepRepository = sagaStepRepository;
    }

    @Override
    @Transactional
    public String startSaga(SAGAContext context) {
        try {
            String contextJSON = objectMapper.writeValueAsString(context);

            SagaInstance sagaInstance = SagaInstance.builder()
                    .id(IdGenerator.generateId())
                    .context(contextJSON)
                    .status(SagaStatus.STARTED)
                    .build();

            SagaInstance savedSagaInstance = sagaInstanceRepository.save(sagaInstance);

            return savedSagaInstance.getId();

        } catch (Exception e) {
            throw new SagaExecutionException("Failed to start saga", e);
        }

    }

    @Override
    @Transactional
    public void markSagaComplete(String sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new ResourceNotFoundException("Saga Instance not found with ID: " + sagaInstanceId));
        sagaInstance.markAsCompleted();
        sagaInstanceRepository.save(sagaInstance);

    }

    @Override
    @Transactional
    public void markSagaFailed(String sagaInstanceId) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new ResourceNotFoundException("Saga Instance not found with ID: " + sagaInstanceId));
        sagaInstance.markAsFailed();
        sagaInstanceRepository.save(sagaInstance);
        compensateSaga(sagaInstanceId);
    }

    @Override
    public SagaInstance getSagaInstance(String sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(()-> new ResourceNotFoundException("Saga Instance not found with ID: " + sagaInstanceId));
    }

    @Override
    @Transactional
    public boolean executeStep(String sagaInstanceId, String stepName) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new ResourceNotFoundException("Saga Instance not found with ID: " + sagaInstanceId));

        ISagaStep step = sagaStepFactory.getSagaStep(stepName);
        if(step==null) throw new SagaExecutionException("Saga step not found: " + stepName);

        SagaStep sagaStepEntity = sagaStepRepository.findByStepNameAndSagaInstanceIdAndStatus(stepName,sagaInstanceId,StepStatus.PENDING)
                .orElse(SagaStep.builder()
                        .id(IdGenerator.generateId())
                        .stepName(stepName)
                        .sagaInstanceId(sagaInstanceId)
                        .status(StepStatus.PENDING)
                        .build());

        if (sagaStepEntity.getId()==null){
            sagaStepRepository.save(sagaStepEntity);
        }

        try{
            SAGAContext sagaContext = objectMapper.readValue(sagaInstance.getContext(),SAGAContext.class);

            sagaStepEntity.markAsProcessing();
            sagaStepRepository.save(sagaStepEntity);

            boolean result = step.execute(sagaContext);

            String updatedContextJson = objectMapper.writeValueAsString(sagaContext);
            sagaInstance.setContext(updatedContextJson);

            if(result){
                sagaStepEntity.markAsCompleted();
                sagaStepRepository.save(sagaStepEntity);
                sagaInstance.setCurrentStep(stepName);
                sagaInstance.markAsProcessing();
                sagaInstanceRepository.save(sagaInstance);
                log.info("Step Successful: {}", stepName);
                return true;
            }
            else {
                sagaStepEntity.markAsFailed();
                sagaStepRepository.save(sagaStepEntity);
                sagaInstanceRepository.save(sagaInstance);
                log.warn("Step Failed: {}", stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepEntity.markAsFailed();
            sagaStepRepository.save(sagaStepEntity);
            log.error("Step Failed: {} — {}", stepName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensateStep(String sagaInstanceId, String stepName) throws Exception{
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new ResourceNotFoundException("Saga Instance not found with ID: " + sagaInstanceId));

        ISagaStep step = sagaStepFactory.getSagaStep(stepName);
        if(step==null) throw new SagaExecutionException("Saga step not found: " + stepName);

        SagaStep sagaStepEntity = sagaStepRepository.findByStepNameAndSagaInstanceIdAndStatus(stepName,sagaInstanceId,StepStatus.COMPLETED)
                .orElse(null);

        if (sagaStepEntity==null){
            log.info("Step not found for compensation, skipping: {}", stepName);
            return true;
        }

        try{
            SAGAContext sagaContext = objectMapper.readValue(sagaInstance.getContext(),SAGAContext.class);

            sagaStepEntity.markAsCompensating();
            sagaStepRepository.save(sagaStepEntity);

            boolean result = step.compensate(sagaContext);

            // Persist context after compensation step too
            String updatedContextJson = objectMapper.writeValueAsString(sagaContext);
            sagaInstance.setContext(updatedContextJson);
            sagaInstanceRepository.save(sagaInstance);

            if(result){
                sagaStepEntity.markAsCompensated();
                sagaStepRepository.save(sagaStepEntity);
                log.info("Step Compensated: {}", stepName);
                return true;
            }
            else {
                sagaStepEntity.markAsFailed();
                sagaStepRepository.save(sagaStepEntity);
                log.warn("Compensation Failed: {}", stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepEntity.markAsFailed();
            sagaStepRepository.save(sagaStepEntity);
            log.error("Compensation Failed: {} — {}", stepName, e.getMessage(), e);
            return false;
        }

    }

    @Override
    @Transactional
    public void compensateSaga(String sagaInstanceId) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new ResourceNotFoundException("Saga Instance not found with ID: " + sagaInstanceId));

        sagaInstance.markAsCompensating();
        sagaInstanceRepository.save(sagaInstance);

        List<SagaStep> completedSagaSteps = sagaStepRepository.findStepsBySagaInstanceIdAndStatus(sagaInstanceId,StepStatus.COMPLETED);

        boolean completeCompensation= true;
        for(SagaStep step : completedSagaSteps){
            boolean compensated = this.compensateStep(sagaInstanceId,step.getStepName());
            if(!compensated){
                completeCompensation=false;
            }
        }

        if (completeCompensation){
            sagaInstance.markAsCompensated();
            sagaInstanceRepository.save(sagaInstance);
        }
        else{
            log.error("Saga compensation incomplete for Saga Instance Id: {}", sagaInstanceId);
        }


    }
}
