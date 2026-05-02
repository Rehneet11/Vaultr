package com.example.vaultr.repositories;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findBySourceWalletId(Long sourceWalletId);
    List<Transaction> findByDestinationWalletId(Long destinationWalletId);

    @Query("Select t From Transaction where t.sourceWalletId = :walletId OR t.destinationWalletId = :walletId")
    List<Transaction> findByWalletId(@Param("walletId") Long walletId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findBySagaInstanceId(Long sagaInstanceId);
}
