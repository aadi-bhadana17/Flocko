package com.kilgore.fooddeliveryapp.catalog.scheduler;

import com.kilgore.fooddeliveryapp.catalog.model.MealType;
import com.kilgore.fooddeliveryapp.catalog.model.MessPlanSlot;
import com.kilgore.fooddeliveryapp.catalog.model.MessSubscription;
import com.kilgore.fooddeliveryapp.catalog.repository.MessSubscriptionRepository;
import com.kilgore.fooddeliveryapp.ordering.service.MessOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Component
public class MessSubscriptionScheduler {

    private final MessSubscriptionRepository messSubscriptionRepository;
    private final MessOrderService messOrderService;

    public MessSubscriptionScheduler(MessSubscriptionRepository messSubscriptionRepository, MessOrderService messOrderService) {
        this.messSubscriptionRepository = messSubscriptionRepository;

        this.messOrderService = messOrderService;
    }

    //----------------------------------------------Schedulers----------------------------------------------------------

    @Scheduled(cron = "0 0 7 * * *") // format - sec min hour day-of-month month day-of-week
    private void scheduleBreakFast() {
        scheduleOrderForSubscribers(MealType.BREAKFAST);
    }

    @Scheduled(cron = "0 0 12 * * *")
    private void scheduleLunch() {
        scheduleOrderForSubscribers(MealType.LUNCH);
    }

    @Scheduled(cron = "0 0 16 * * *")
    private void scheduleSnacks() {
        scheduleOrderForSubscribers(MealType.SNACKS);
    }

    @Scheduled(cron = "0 0 19 * * *")
    private void scheduleDinner() {
        scheduleOrderForSubscribers(MealType.DINNER);
    }

    //-----------------------------------------Schedule and Place Orders------------------------------------------------

    public void scheduleOrderForSubscribers(MealType mealType) {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        List<MessSubscription> subscriptionList = messSubscriptionRepository.findActiveSubscriptionsByDayAndMealType(
                today,
                dayOfWeek, // current day of week
                mealType // meal-type according to the time from schedulers
        );

        subscriptionList.forEach(subscription -> {
            MessPlanSlot slot = messOrderService.processSubscription(subscription, dayOfWeek, mealType);
            messOrderService.placeOrder(slot.getFoodItems(), subscription.getUserId(), subscription);
        });
    }

    //-----------------------------------------Subscriptions Status Handling--------------------------------------------

    @Scheduled(cron = "0 05 12 * * *")
    public void settingSubscriptionInActive() {
        LocalDate today = LocalDate.now();

        List<MessSubscription> subscriptionList = messSubscriptionRepository.findActiveSubscriptionsByCurrentDate(today);

        subscriptionList.forEach(sub -> sub.setActive(false));
        messSubscriptionRepository.saveAll(subscriptionList);
    }

}
