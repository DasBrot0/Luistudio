package com.luistudio.reservas.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class CalendarUtils {

    private CalendarUtils() {
    }

    public static String googleCalendarLink(
        String title,
        String description,
        String location,
        LocalDate date,
        LocalTime start,
        LocalTime end
    ) {
        OffsetDateTime startDateTime = date.atTime(start).atOffset(ZoneOffset.ofHours(-5));
        OffsetDateTime endDateTime = date.atTime(end).atOffset(ZoneOffset.ofHours(-5));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String dates = startDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(formatter)
            + "/"
            + endDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(formatter);

        return "https://calendar.google.com/calendar/r/eventedit?text="
            + encode(title)
            + "&details=" + encode(description)
            + "&location=" + encode(location)
            + "&dates=" + encode(dates);
    }

    public static String createIcs(
        String title,
        String description,
        String location,
        LocalDate date,
        LocalTime start,
        LocalTime end
    ) {
        OffsetDateTime startDateTime = date.atTime(start).atOffset(ZoneOffset.ofHours(-5));
        OffsetDateTime endDateTime = date.atTime(end).atOffset(ZoneOffset.ofHours(-5));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

        return "BEGIN:VCALENDAR\n"
            + "VERSION:2.0\n"
            + "PRODID:-//Luistudio//Reservas//ES\n"
            + "BEGIN:VEVENT\n"
            + "UID:" + System.currentTimeMillis() + "@luistudio\n"
            + "DTSTAMP:" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).format(formatter) + "\n"
            + "DTSTART:" + startDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(formatter) + "\n"
            + "DTEND:" + endDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(formatter) + "\n"
            + "SUMMARY:" + sanitize(title) + "\n"
            + "DESCRIPTION:" + sanitize(description) + "\n"
            + "LOCATION:" + sanitize(location) + "\n"
            + "END:VEVENT\n"
            + "END:VCALENDAR\n";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String sanitize(String value) {
        return value.replace("\n", " ").replace(",", "\\,").replace(";", "\\;");
    }
}
