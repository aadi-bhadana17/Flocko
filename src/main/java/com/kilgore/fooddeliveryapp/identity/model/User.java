package com.kilgore.fooddeliveryapp.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    @Enumerated(EnumType.STRING)
    private UserRole role;

    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isOnline;

    private LocalDateTime restrictedUntil;
    private String restrictionReason;

    private Long employedAt;
    @Column(nullable = false)
    private boolean isTempPassword = false;

    private BigDecimal walletBalance = BigDecimal.ZERO;
    private BigDecimal pendingDepositAmount;
    private String pendingPaypalOrderId;
    @Version
    private Long version = 0L;

    @ElementCollection
    @CollectionTable(
            name = "user_favourite_restaurants",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "restaurant_id")
    private List<Long> favouriteRestaurantIds = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses =  new ArrayList<>();

    @ElementCollection
    private Set<Long> ownedRestaurantIds = new HashSet<>();
}
