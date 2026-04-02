package com.kilgore.fooddeliveryapp.scheduler;

import com.kilgore.fooddeliveryapp.model.GroupDeal;
import com.kilgore.fooddeliveryapp.model.GroupDealParticipation;
import com.kilgore.fooddeliveryapp.model.GroupDealStatus;
import com.kilgore.fooddeliveryapp.repository.GroupDealParticipationRepository;
import com.kilgore.fooddeliveryapp.repository.GroupDealRepository;
import com.kilgore.fooddeliveryapp.service.GroupDealOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroupDealScheduler {

    private final GroupDealRepository groupDealRepository;
    private final GroupDealParticipationRepository groupDealParticipationRepository;
    private final GroupDealOrderService  groupDealOrderService;

    public GroupDealScheduler(GroupDealRepository groupDealRepository, GroupDealParticipationRepository groupDealParticipationRepository, GroupDealOrderService groupDealOrderService) {
        this.groupDealRepository = groupDealRepository;
        this.groupDealParticipationRepository = groupDealParticipationRepository;
        this.groupDealOrderService = groupDealOrderService;
    }

    @Scheduled(fixedRate = 300000)
    public void handleVotingExpiry() {
        // This method will fetch the deals whose voting period is over and
        // then update their status to EXPIRED or move them to the next phase (e.g., CONFIRMATION)
        // based on your business logic = at least, 50% vote required

        LocalDateTime now = LocalDateTime.now();

        List<GroupDeal> deals = groupDealRepository.findDealsByStatusAndEndTimeBefore(GroupDealStatus.VOTING, now);
        deals.forEach(deal -> {
            Integer currPar = groupDealParticipationRepository.getTotalParticipantsByDeal(deal.getDealId());
            if(currPar == null || currPar < deal.getTargetParticipation() * 0.5){
                deal.setStatus(GroupDealStatus.EXPIRED);
            } else {
                deal.setStatus(GroupDealStatus.CONFIRMATION_WINDOW);
                deal.setConfirmationWindowEndTime(now.plusMinutes(30)); // Set confirmation window to 30 minutes
            }
        });

        groupDealRepository.saveAll(deals);
    }

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void handleConfirmationWindowExpiry() {
        LocalDateTime now = LocalDateTime.now();

        List<GroupDeal> deals = groupDealRepository
                .findDealsByStatusAndConfirmationWindowEndTimeBefore(
                        GroupDealStatus.CONFIRMATION_WINDOW, now
                );

        deals.forEach(deal -> {
            Integer currPar = groupDealParticipationRepository
                    .getTotalParticipantsByDeal(deal.getDealId());

            if (currPar == null || currPar < deal.getTargetParticipation() * 0.5) {
                groupDealOrderService.expireDeal(deal.getDealId(), currPar);
            } else {
                groupDealOrderService.processGroupDeal(deal.getDealId(), currPar);
            }
        });
    }
    
}
