package com.luistudio.reservas.service.booking.command;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingReminderScheduler {

    private final BookingReminderCommandManager commandManager;
    private final SendUpcomingReservationReminderCommand upcomingCommand;
    private final SendEndingSoonReservationReminderCommand endingSoonCommand;

    public BookingReminderScheduler(
        BookingReminderCommandManager commandManager,
        SendUpcomingReservationReminderCommand upcomingCommand,
        SendEndingSoonReservationReminderCommand endingSoonCommand
    ) {
        this.commandManager = commandManager;
        this.upcomingCommand = upcomingCommand;
        this.endingSoonCommand = endingSoonCommand;
    }

    @Scheduled(fixedDelay = 300000)
    public void run() {
        commandManager.agregarComando(upcomingCommand);
        commandManager.agregarComando(endingSoonCommand);
        commandManager.ejecutarPendientes();
    }
}
