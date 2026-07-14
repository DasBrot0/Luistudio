package com.luistudio.reservas.service.booking.command;

import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AvailabilitySubscriptionScheduler {

    private final AvailabilitySubscriptionService subscriptionService;

    public AvailabilitySubscriptionScheduler(AvailabilitySubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedDelay = 7000)
    public void run() {
        subscriptionService.processNewlyAvailableRooms();
    }
}
