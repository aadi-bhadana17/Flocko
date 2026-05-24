package com.kilgore.fooddeliveryapp.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"categoryName", "restaurant_id"})
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    private String categoryName;

    @ManyToOne
    @JsonIgnore
    private Restaurant restaurant;

    private String description;
    private Integer displayOrder;

    @EqualsAndHashCode.Include
    @ManyToMany
    @JoinTable(
            name = "addons_for_categories",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "addon_id")
    )
    private Set<Addon> availableAddons =  new HashSet<>();

    public void addAddon(Addon addon) {
        this.availableAddons.add(addon);
        addon.getCategories().add(this);
    }
    public void removeAddon(Addon addon) {
        this.availableAddons.remove(addon);
        addon.getCategories().remove(this);
    }
}

