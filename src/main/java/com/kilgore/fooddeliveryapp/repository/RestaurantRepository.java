package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Restaurant findRestaurantByRestaurantNameAndAddress_City(String name, String city);

    Optional<Object> findRestaurantByRestaurantName(String name);

    @Query("""
            select distinct r
            from Restaurant r
            left join fetch r.categories c
            left join fetch c.availableAddons
            where r.restaurantId = :restaurantId
            """)
    Optional<Restaurant> findMenuById(@Param("restaurantId") Long restaurantId);
}
