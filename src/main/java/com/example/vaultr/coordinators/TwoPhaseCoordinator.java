package com.example.vaultr.coordinators;


import com.example.vaultr.dto.TransactionRequestDTO;
import com.example.vaultr.dto.TransactionResponseDTO;
import com.example.vaultr.services.IWalletServiceReceiver;
import com.example.vaultr.services.IWalletServiceSender;
import org.springframework.stereotype.Service;

@Service
public class TwoPhaseCoordinator{
    private final IWalletServiceSender sender;
    private final IWalletServiceReceiver receiver;

    public TwoPhaseCoordinator(IWalletServiceSender sender, IWalletServiceReceiver receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }
    public TransactionResponseDTO doTransaction(TransactionRequestDTO requestDTO){
        String senderId =requestDTO.getSenderId();
        String receiverId = requestDTO.getReceiverId();
        double amount = requestDTO.getAmount();
        if(sender.prepare(senderId,amount) && receiver.prepare(receiverId,amount)){
            if(sender.commit(senderId,amount) && receiver.commit(receiverId,amount)){
                return TransactionResponseDTO.builder().status(true).build();
            }
            else{
                sender.rollback(senderId);
                receiver.rollback(receiverId);
            }
        }
        return TransactionResponseDTO.builder().status(false).build();
    }

}
