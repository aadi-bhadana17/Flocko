package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.MessPlanSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessPlanSlotRepository extends JpaRepository<MessPlanSlot, Long> {
}
