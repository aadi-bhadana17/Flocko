package com.kilgore.fooddeliveryapp.ordering.repository;

import com.kilgore.fooddeliveryapp.ordering.model.Order;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE o.restaurantId = :restaurantId " +
            "AND o.createdAt > :since " +
            "AND oi.foodId = :foodId")
    int countFoodQuantityInLastHour(@Param("restaurantId") Long restaurantId,
                                    @Param("since") LocalDateTime since,
                                    @Param("foodId") Long foodId);

    @Query("SELECT o FROM Order o " +
            "WHERE o.orderType = com.kilgore.fooddeliveryapp.ordering.model.OrderType.PRE_ORDER " +
            "AND o.scheduledAt < :now AND o.orderStatus = :status")
    List<Order> findDuePreOrders(@Param("now") LocalDateTime now, @Param("status") OrderStatus status);

    boolean existsByUserIdAndRestaurantIdAndCreatedAtAfter(Long userId, Long restaurantId, LocalDateTime createdAt);

    List<Order> findByUserId(Long userId);

    List<Order> findByRestaurantId(Long restaurantId);

    @Query("SELECT COUNT(o)" +
            "FROM Order o " +
            "WHERE o.restaurantId = :restaurantId " +
            "AND o.orderStatus IN" +
            "(com.kilgore.fooddeliveryapp.ordering.model.OrderStatus.PREPARING, " +
            "com.kilgore.fooddeliveryapp.ordering.model.OrderStatus.CONFIRMED)")
    long countCurrentOrdersByRestaurantId(@Param("restaurantId") Long restaurantId);
}
