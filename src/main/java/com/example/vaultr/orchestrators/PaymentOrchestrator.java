package com.example.vaultr.orchestrators;

import com.example.vaultr.dto.TransactionRequestDTO;
import com.example.vaultr.dto.TransactionResponseDTO;
import com.example.vaultr.services.IWalletService;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrchestrator {
    private final IWalletService walletService;


    public PaymentOrchestrator(IWalletService walletService) {
        this.walletService = walletService;
    }

    public TransactionResponseDTO doTransaction(TransactionRequestDTO requestDTO){
        String senderId= requestDTO.getSenderId();
        String receiverId= requestDTO.getReceiverId();
        double amount = requestDTO.getAmount();
        if(walletService.debit(senderId,amount)){
            if(walletService.credit(receiverId,amount)){
                return TransactionResponseDTO.builder().status(true).build();
            }
            else {
                walletService.refund(senderId,amount);
            }
        }
        return TransactionResponseDTO.builder().status(false).build();
    }

}
