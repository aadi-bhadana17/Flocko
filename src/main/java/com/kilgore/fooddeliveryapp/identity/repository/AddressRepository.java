package com.kilgore.fooddeliveryapp.identity.repository;

import com.kilgore.fooddeliveryapp.identity.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address,Long> {
}
