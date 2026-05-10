package com.example.vaultr.services;

import com.example.vaultr.dto.TransferRequestDTO;
import com.example.vaultr.entities.Transaction;

import java.math.BigDecimal;

public interface ITransferSAGAService {
    String initiateTransfer(TransferRequestDTO requestDTO) throws Exception;
    void executeTransfer(String sagaInstanceId) throws Exception;
}
