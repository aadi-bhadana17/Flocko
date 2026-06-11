package com.kilgore.fooddeliveryapp.ordering.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @Column(name = "food_food_id", nullable = false)
    private Long foodId;
    private int quantity;
    private BigDecimal priceAtOrder;
    private BigDecimal itemTotal;

    @ElementCollection
    @CollectionTable(name = "order_item_addons", joinColumns = @JoinColumn(name = "order_item_id"))
    @Column(name = "addon_id")
    private List<Long> addonIds = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
