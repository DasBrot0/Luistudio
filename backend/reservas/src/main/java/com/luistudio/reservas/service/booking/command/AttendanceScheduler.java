package com.luistudio.reservas.service.booking.command;

import com.luistudio.reservas.service.AttendanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceScheduler {

    private final AttendanceService attendanceService;

    public AttendanceScheduler(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Scheduled(fixedDelay = 300_000)
    public void run() {
        attendanceService.processMissedBookings();
    }
}
