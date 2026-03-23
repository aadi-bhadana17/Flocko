package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.GroupDeal;
import com.kilgore.fooddeliveryapp.model.GroupDealParticipation;
import com.kilgore.fooddeliveryapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupDealParticipationRepository extends JpaRepository<GroupDealParticipation, Long> {

    @Query("SELECT SUM(gdp.quantity) FROM GroupDealParticipation gdp WHERE gdp.groupDeal.dealId = :dealId AND gdp.isConfirmed = true ")
    Integer getTotalParticipantsByDeal(@Param("dealId") Long dealId);

    List<GroupDealParticipation> findByUserAndGroupDeal(User user, GroupDeal groupDeal);

    List<GroupDealParticipation> findByGroupDeal(GroupDeal deal);

    @Query("SELECT gcd FROM GroupDealParticipation gcd WHERE gcd.groupDeal.dealId = :dealId AND gcd.isConfirmed = true")
    List<GroupDealParticipation> findActiveParticipantsByDeal(@Param("dealId") Long dealId);

    List<GroupDealParticipation> findGroupDealParticipationsByGroupDeal(GroupDeal deal);
}
