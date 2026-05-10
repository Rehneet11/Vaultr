package com.example.vaultr.repositories;

import com.example.vaultr.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent,String> {

    @Query(value = "Select * from outbox_event where status='PENDING' Order By created_at ASC LIMIT 50 FOR UPDATE SKIP LOCKED",nativeQuery = true)
    List<OutboxEvent> findPendingEventsForProcessing();
}
