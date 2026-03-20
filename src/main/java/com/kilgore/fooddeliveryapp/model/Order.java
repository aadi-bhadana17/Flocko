package com.kilgore.fooddeliveryapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant;

    private BigDecimal totalPrice;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Address deliveryAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    private int totalQuantity;

    @Enumerated(EnumType.STRING)
    private OrderType orderType = OrderType.REGULAR;
    private LocalDateTime scheduledAt;
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private boolean isSpecial = false;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<ChatMessage>  chatMessages = new ArrayList<>();
}

