package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    public User findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.employedAt = :restaurantId")
    public List<User> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    User findByPendingPaypalOrderId(String orderId);
}
