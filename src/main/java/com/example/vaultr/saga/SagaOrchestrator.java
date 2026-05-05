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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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
    public Long startSaga(SAGAContext context) {
        try {
            String contextJSON = objectMapper.writeValueAsString(context);

            SagaInstance sagaInstance = SagaInstance.builder()
                    .context(contextJSON)
                    .status(SagaStatus.STARTED)
                    .build();

            SagaInstance savedSagaInstance = sagaInstanceRepository.save(sagaInstance);

            return savedSagaInstance.getId();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    @Transactional
    public void markSagaComplete(Long sagaInstanceId) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new Exception("Cannot Find Saga Instance"));
        sagaInstance.markAsCompleted();
        sagaInstanceRepository.save(sagaInstance);

    }

    @Override
    @Transactional
    public void markSagaFailed(Long sagaInstanceId) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new Exception("Cannot Find Saga"));
        sagaInstance.markAsFailed();
        sagaInstanceRepository.save(sagaInstance);
        compensateSaga(sagaInstanceId);
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) throws Exception {
        return sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(()->new Exception("Cannot find Saga Instance"));
    }

    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new Exception("Cannot find Saga Instance"));

        ISagaStep step = sagaStepFactory.getSagaStep(stepName);
        if(step==null) throw new Exception("Cannot find Saga Step");

        SagaStep sagaStepEntity = sagaStepRepository.findByStepNameAndSagaInstanceIdAndStatus(stepName,sagaInstanceId,StepStatus.PENDING)
                .orElse(SagaStep.builder()
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

            if(result){
                sagaStepEntity.markAsCompleted();
                sagaStepRepository.save(sagaStepEntity);
                sagaInstance.setCurrentStep(stepName);
                sagaInstance.markAsProcessing();
                sagaInstanceRepository.save(sagaInstance);
                System.out.println("Step Successful " + stepName );
                return true;
            }
            else {
                sagaStepEntity.markAsFailed();
                sagaStepRepository.save(sagaStepEntity);
                System.out.println("Step Failed " + stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepEntity.markAsFailed();
            sagaStepRepository.save(sagaStepEntity);
            System.out.println("Step Failed " + stepName);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensateStep(Long sagaInstanceId, String stepName) throws Exception{
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new Exception("Cannot find Saga Instance"));

        ISagaStep step = sagaStepFactory.getSagaStep(stepName);
        if(step==null) throw new Exception("Cannot Find SAGA Step");

        SagaStep sagaStepEntity = sagaStepRepository.findByStepNameAndSagaInstanceIdAndStatus(stepName,sagaInstanceId,StepStatus.COMPLETED)
                .orElse(null);

        if (sagaStepEntity==null){
            System.out.println("Step not Found "+ stepName);
            return true;
        }

        try{
            SAGAContext sagaContext = objectMapper.readValue(sagaInstance.getContext(),SAGAContext.class);

            sagaStepEntity.markAsCompensating();
            sagaStepRepository.save(sagaStepEntity);

            boolean result = step.compensate(sagaContext);

            if(result){
                sagaStepEntity.markAsCompensated();
                sagaStepRepository.save(sagaStepEntity);
                System.out.println("Step Compensated " + stepName );
                return true;
            }
            else {
                sagaStepEntity.markAsFailed();
                sagaStepRepository.save(sagaStepEntity);
                System.out.println("Step Failed " + stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepEntity.markAsFailed();
            sagaStepRepository.save(sagaStepEntity);
            System.out.println("Step Failed " + stepName);
            return false;
        }

    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(()-> new Exception("Cannot find Saga"));

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
            System.out.println("Saga Failed for Saga Instance Id " + sagaInstanceId);
        }


    }
}
