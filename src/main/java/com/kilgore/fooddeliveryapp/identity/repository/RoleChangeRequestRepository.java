package com.kilgore.fooddeliveryapp.identity.repository;

import com.kilgore.fooddeliveryapp.identity.model.RoleChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleChangeRequestRepository extends JpaRepository<RoleChangeRequest, Long> {

}
