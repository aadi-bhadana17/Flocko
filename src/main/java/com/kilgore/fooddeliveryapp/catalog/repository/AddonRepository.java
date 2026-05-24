package com.kilgore.fooddeliveryapp.catalog.repository;

import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddonRepository extends JpaRepository<Addon, Long> {

     List<Addon> findAllByRestaurant_RestaurantId(Long restaurantId);
}
