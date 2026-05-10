package com.example.vaultr.repositories;

import com.example.vaultr.entities.Transaction;
import com.example.vaultr.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findBySourceWalletId(String sourceWalletId);

    List<Transaction> findByDestinationWalletId(String destinationWalletId);

    @Query("Select t From Transaction t where t.sourceWalletId = :walletId OR t.destinationWalletId = :walletId")
    List<Transaction> findByWalletId(@Param("walletId") String walletId);

    List<Transaction> findByStatus(TransactionStatus status);

    Transaction findBySagaInstanceId(String sagaInstanceId);
}
