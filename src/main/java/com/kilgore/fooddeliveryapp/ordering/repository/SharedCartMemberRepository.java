package com.kilgore.fooddeliveryapp.ordering.repository;

import com.kilgore.fooddeliveryapp.ordering.model.SharedCartMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SharedCartMemberRepository extends JpaRepository<SharedCartMember, Long> {

    @Query("SELECT mem FROM SharedCartMember mem " +
            "WHERE mem.sharedCart.joinCode = :joinCode " +
            "AND mem.userId = :userId " +
            "AND mem.isActive = true")
    SharedCartMember findByUserIdAndJoinCode(@Param("joinCode") String joinCode, @Param("userId") Long userId);

    @Query("SELECT mem FROM SharedCartMember mem " +
            "WHERE mem.userId = :userId " +
            "AND mem.isActive = true " +
            "AND mem.sharedCart.isActive = true")
    SharedCartMember findActiveMemberByUserId(@Param("userId") Long userId);

}
