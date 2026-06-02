package com.luistudio.reservas.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("America/Lima");

    private AppTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalTime nowTime() {
        return LocalTime.now(ZONE);
    }

    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(ZONE);
    }
}
