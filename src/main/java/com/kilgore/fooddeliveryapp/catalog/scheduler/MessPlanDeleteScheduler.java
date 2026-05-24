package com.kilgore.fooddeliveryapp.catalog.scheduler;

import com.kilgore.fooddeliveryapp.catalog.model.MessPlan;
import com.kilgore.fooddeliveryapp.catalog.repository.MessPlanRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MessPlanDeleteScheduler {

    private final MessPlanRepository messPlanRepository;

    public MessPlanDeleteScheduler(MessPlanRepository messPlanRepository) {
        this.messPlanRepository = messPlanRepository;
    }

    @Scheduled(fixedRate = 86_400_000) // This method will run once a day to check for mess plans that are scheduled for deletion
    public void schedule() {
        LocalDateTime now = LocalDateTime.now();

        List<MessPlan> plans = messPlanRepository.findPlansDueForDeletion(now);

        plans.forEach(messPlan -> messPlan.setActive(false));
                        // Mark the mess plan as inactive instead of deleting it

        messPlanRepository.saveAll(plans);
    }

}
