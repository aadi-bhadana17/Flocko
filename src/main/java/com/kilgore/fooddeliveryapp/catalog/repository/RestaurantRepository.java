package com.kilgore.fooddeliveryapp.catalog.repository;

import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Restaurant findRestaurantByRestaurantNameAndAddress_City(String name, String city);

    @Query("""
            select distinct r
            from Restaurant r
            left join fetch r.categories c
            left join fetch c.availableAddons
            where r.restaurantId = :restaurantId
            """)
    Optional<Restaurant> findMenuById(@Param("restaurantId") Long restaurantId);

    List<Restaurant> findAllByOwnerUserId(@Param("ownerId") Long ownerId);

    List<Restaurant> findByOwnerUserId(Long userId);
}
