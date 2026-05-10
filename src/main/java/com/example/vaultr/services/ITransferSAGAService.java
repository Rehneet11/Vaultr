package com.example.vaultr.services;

import com.example.vaultr.dto.TransferRequestDTO;
import com.example.vaultr.dto.TransferResponseDTO;
import com.example.vaultr.entities.Transaction;

import java.math.BigDecimal;

public interface ITransferSAGAService {
    TransferResponseDTO initiateTransfer(TransferRequestDTO requestDTO) throws Exception;
    void executeTransfer(String sagaInstanceId) throws Exception;
}
