package com.kilgore.fooddeliveryapp.ordering.repository;

import com.kilgore.fooddeliveryapp.ordering.model.SharedCart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedCartRepository extends JpaRepository<SharedCart,Long> {
    SharedCart findByHostUserId(Long hostUserId);

    SharedCart findByJoinCode(String joinCode);
}
