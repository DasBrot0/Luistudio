package com.luistudio.reservas.util;

import java.net.URLEncoder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class CalendarUtils {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

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
        String dates = toUtcCalendarInstant(date, start)
            + "/"
            + toUtcCalendarInstant(date, end);

        return "https://calendar.google.com/calendar/render?action=TEMPLATE&text="
            + encode(title)
            + "&details=" + encode(description)
            + "&location=" + encode(location)
            + "&dates=" + dates;
    }

    public static String createIcs(
        String uid,
        String title,
        String description,
        String location,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate date,
        LocalTime start,
        LocalTime end
    ) {
        String geo = latitude == null || longitude == null
            ? ""
            : "GEO:" + latitude.stripTrailingZeros().toPlainString() + ";"
                + longitude.stripTrailingZeros().toPlainString() + "\r\n";

        return "BEGIN:VCALENDAR\r\n"
            + "VERSION:2.0\r\n"
            + "PRODID:-//Luistudio//Reservas//ES\r\n"
            + "CALSCALE:GREGORIAN\r\n"
            + "METHOD:PUBLISH\r\n"
            + "BEGIN:VEVENT\r\n"
            + "UID:" + sanitize(uid) + "@luistudio\r\n"
            + "DTSTAMP:" + OffsetDateTime.now(java.time.ZoneOffset.UTC).format(UTC_FORMATTER) + "\r\n"
            + "DTSTART:" + toUtcCalendarInstant(date, start) + "\r\n"
            + "DTEND:" + toUtcCalendarInstant(date, end) + "\r\n"
            + "SUMMARY:" + sanitize(title) + "\r\n"
            + "DESCRIPTION:" + sanitize(description) + "\r\n"
            + "LOCATION:" + sanitize(location) + "\r\n"
            + geo
            + "END:VEVENT\r\n"
            + "END:VCALENDAR\r\n";
    }

    private static String toUtcCalendarInstant(LocalDate date, LocalTime time) {
        ZonedDateTime limaDateTime = date.atTime(time).atZone(LIMA_ZONE);
        return limaDateTime.withZoneSameInstant(java.time.ZoneOffset.UTC).format(UTC_FORMATTER);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String sanitize(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\r\n", "\\n")
            .replace("\r", "\\n")
            .replace("\n", "\\n")
            .replace(",", "\\,")
            .replace(";", "\\;");
    }
}
