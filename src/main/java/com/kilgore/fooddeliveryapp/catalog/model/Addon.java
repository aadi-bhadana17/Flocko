package com.kilgore.fooddeliveryapp.catalog.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)// well as we know, that this annotation from lombok override methods like
// equals(Object o) and hasCode(), so by default it includes all the fields of entity
// but when we do, onlyExplicitlyIncluded = true, that says - ignore everything except the one I mark with @EqualsAndHashCode.Include
public class Addon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long addonId;

    private String addonName;

    @ManyToOne
    private Restaurant restaurant;

    @ManyToMany(mappedBy = "availableAddons")
    private Set<Category> categories = new HashSet<>();

    private BigDecimal price;
    private boolean available;

}