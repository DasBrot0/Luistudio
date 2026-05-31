package com.luistudio.reservas.service.booking.command;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingReminderScheduler {

    private final SendUpcomingReservationReminderCommand upcomingReminderCommand;
    private final SendEndingSoonReservationReminderCommand endingSoonReminderCommand;

    public BookingReminderScheduler(
        SendUpcomingReservationReminderCommand upcomingReminderCommand,
        SendEndingSoonReservationReminderCommand endingSoonReminderCommand
    ) {
        this.upcomingReminderCommand = upcomingReminderCommand;
        this.endingSoonReminderCommand = endingSoonReminderCommand;
    }

    @Scheduled(fixedDelay = 300000)
    public void runUpcomingReminderCommand() {
        upcomingReminderCommand.execute();
    }

    @Scheduled(fixedDelay = 300000)
    public void runEndingSoonReminderCommand() {
        endingSoonReminderCommand.execute();
    }
}
