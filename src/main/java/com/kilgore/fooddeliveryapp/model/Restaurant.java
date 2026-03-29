package com.kilgore.fooddeliveryapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ManyToOne(cascade = CascadeType.PERSIST)
    private User owner;

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

    @OneToMany(mappedBy = "restaurant")
    private List<Review> reviews;

    @OneToMany(mappedBy = "restaurant")
    private List<Order> orders;

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
    @ManyToMany(mappedBy = "favourites")
    private Collection<User> users;

}
