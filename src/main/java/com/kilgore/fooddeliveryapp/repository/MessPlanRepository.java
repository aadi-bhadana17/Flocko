package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.MessPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessPlanRepository extends JpaRepository<MessPlan,Long> {
}
