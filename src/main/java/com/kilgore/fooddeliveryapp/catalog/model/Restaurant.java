package com.kilgore.fooddeliveryapp.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kilgore.fooddeliveryapp.common.enums.KitchenLoadIndicator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantId;
    private String restaurantName;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    private String restaurantDescription;

    @Enumerated(EnumType.STRING)
    private CuisineType cuisineType;

    @Embedded
    private RestaurantAddress address;

    @Embedded
    private ContactInformation contactInformation;

    private LocalTime openingTime;
    private LocalTime closingTime;
    private BigDecimal avgRating;
    private Long totalReviews;

    @ElementCollection
    @CollectionTable(
            name = "restaurant_review_ids",
            joinColumns = @JoinColumn(name = "restaurant_id")
    )
    @Column(name = "review_id")
    private List<Long> reviewIds;

    @ElementCollection
    @CollectionTable(
            name = "restaurant_order_ids",
            joinColumns = @JoinColumn(name = "restaurant_id")
    )
    @Column(name = "order_id")
    private List<Long> orderIds;

    @Column(length = 1000)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> images;

    private LocalDate registrationDate;
    private boolean open;

    private RestaurantStatus restaurantStatus;
    private KitchenLoadIndicator kitchenStatus = KitchenLoadIndicator.LOW;

    @JsonIgnore
    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<Food> foods;

    @OneToMany(mappedBy = "restaurant")
    private List<Category> categories;

    @ElementCollection
    @CollectionTable(name = "user_favourites", joinColumns = @JoinColumn(name = "restaurant_id"))
    @Column(name = "user_id")
    private Collection<Long> favouriteUserIds;

}
