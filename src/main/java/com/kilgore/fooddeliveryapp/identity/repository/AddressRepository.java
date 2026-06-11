package com.kilgore.fooddeliveryapp.identity.repository;

import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address,Long> {

    @Query("SELECT ad FROM Address ad " +
            "WHERE ad.user.userId = :userId " +
            "AND ad.isDefault = true"
    )
    Address getDefaultAddressByUserId(@Param("userId") Long userId);
}
