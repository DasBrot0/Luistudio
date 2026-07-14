package com.luistudio.reservas.service.email;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailOutboxScheduler {

    private final EmailDispatchService emailDispatchService;

    public EmailOutboxScheduler(EmailDispatchService emailDispatchService) {
        this.emailDispatchService = emailDispatchService;
    }

    @Scheduled(fixedDelay = 7000)
    public void run() {
        emailDispatchService.processPendingEmails();
    }
}
