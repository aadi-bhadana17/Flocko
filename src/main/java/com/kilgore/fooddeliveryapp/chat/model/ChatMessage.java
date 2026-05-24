package com.kilgore.fooddeliveryapp.chat.model;

import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.EAGER)
    private Order order;
    @ManyToOne
    private User sender;

    private String message;
    private LocalDateTime timestamp;
}
