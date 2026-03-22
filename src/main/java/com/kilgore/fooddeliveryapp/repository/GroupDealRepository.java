package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.GroupDeal;
import com.kilgore.fooddeliveryapp.model.GroupDealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupDealRepository extends JpaRepository<GroupDeal, Long> {

    @Query("SELECT gd FROM GroupDeal gd WHERE gd.restaurant.restaurantId = :restaurantId AND gd.status IN :statuses")
    List<GroupDeal> getActiveDealsForRestaurant(@Param("restaurantId") Long restaurantId, @Param("statuses") List<GroupDealStatus> statuses);

    @Query("SELECT gd FROM GroupDeal gd WHERE gd.restaurant.restaurantId = :restaurantId")
    List<GroupDeal> getAllDealsForRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT gd FROM GroupDeal gd WHERE gd.restaurant.restaurantId = :restaurantId AND gd.dealName != :dealName AND gd.status IN :statuses")
    GroupDeal getActiveDealForRestaurantAndByName(@Param("restaurantId") Long restaurantId, @Param("dealName") String dealName, @Param("statuses") List<GroupDealStatus> statuses);


    @Query("SELECT gd FROM GroupDeal gd WHERE gd.status = :status AND (gd.endTime <= :now)")
    List<GroupDeal> findDealsByStatusAndEndTimeBefore(@Param("status") GroupDealStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT gd FROM GroupDeal gd WHERE gd.status = :status AND (gd.confirmationWindowEndTime <= :now)")
    List<GroupDeal> findDealsByStatusAndConfirmationWindowEndTimeBefore(@Param("status") GroupDealStatus status, @Param("now") LocalDateTime now);

}
