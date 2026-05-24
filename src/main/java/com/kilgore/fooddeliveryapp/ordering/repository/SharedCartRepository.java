package com.kilgore.fooddeliveryapp.ordering.repository;

import com.kilgore.fooddeliveryapp.ordering.model.SharedCart;
import com.kilgore.fooddeliveryapp.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedCartRepository extends JpaRepository<SharedCart,Long> {
    SharedCart findByHost(User user);

    SharedCart findByJoinCode(String joinCode);
}
