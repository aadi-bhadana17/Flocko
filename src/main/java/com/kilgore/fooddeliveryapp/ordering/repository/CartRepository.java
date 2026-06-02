package com.kilgore.fooddeliveryapp.ordering.repository;

import com.kilgore.fooddeliveryapp.ordering.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart,Long> {

    Cart findByUserIdAndSharedCartIsNull(Long userId);

    Cart findByUserId(Long userId);
}
