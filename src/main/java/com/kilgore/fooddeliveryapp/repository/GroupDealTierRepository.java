package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.GroupDealTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupDealTierRepository extends JpaRepository<GroupDealTier, Long> {
}
