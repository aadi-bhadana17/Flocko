package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.order.orderId = :orderId ORDER BY m.timestamp ASC")
     List<ChatMessage> getMessages(Long orderId);
}
