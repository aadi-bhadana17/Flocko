package com.kilgore.fooddeliveryapp.deals.repository;

import com.kilgore.fooddeliveryapp.deals.model.GroupDeal;
import com.kilgore.fooddeliveryapp.deals.model.GroupDealParticipation;
import com.kilgore.fooddeliveryapp.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupDealParticipationRepository extends JpaRepository<GroupDealParticipation, Long> {

    @Query("SELECT SUM(gdp.quantity) FROM GroupDealParticipation gdp WHERE gdp.groupDeal.dealId = :dealId AND gdp.isConfirmed = true ")
    Integer getTotalParticipantsByDeal(@Param("dealId") Long dealId);

    @Query("SELECT gcd FROM GroupDealParticipation gcd WHERE gcd.groupDeal.dealId = :dealId AND gcd.isConfirmed = true")
    List<GroupDealParticipation> findActiveParticipantsByDeal(@Param("dealId") Long dealId);

    List<GroupDealParticipation> findGroupDealParticipationsByGroupDeal(GroupDeal deal);

    List<GroupDealParticipation> findByUserIdAndGroupDeal(Long userId, GroupDeal deal);
}
