package com.kilgore.fooddeliveryapp.ordering.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne
    @JsonIgnore
    private Cart cart;

    @Column(name = "food_food_id", nullable = false)
    private Long foodId;

    private int quantity;
    private BigDecimal priceAtAddition;

    @ElementCollection
    @CollectionTable(name = "cart_item_addons", joinColumns = @JoinColumn(name = "cart_item_id"))
    @Column(name = "addon_id")
    private List<Long> addonIds = new ArrayList<>();

    private BigDecimal itemTotal;

    private LocalDateTime addedTime;

}
