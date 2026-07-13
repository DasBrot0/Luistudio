package com.luistudio.reservas.dto.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CampusMapResponse(OffsetDateTime generatedAt, int refreshAfterSeconds, List<Campus> campuses) {
    public record Campus(String code, String name, Coordinate center, double defaultZoom, List<Pavilion> pavilions) {}
    public record Coordinate(BigDecimal latitude, BigDecimal longitude) {}
    public record Pavilion(Long id, String code, String name, BigDecimal latitude, BigDecimal longitude,
                           String aggregateStatus, Summary summary, List<Location> locations) {}
    public record Summary(long free, long occupied, long maintenance, long total) {}
    public record Location(String name, List<Room> rooms) {}
    public record Room(Long id, String code, String name, String venue, Integer capacity, String status,
                       boolean withinSchedule, boolean reservableNow) {}
}
