package com.luistudio.reservas.util;

import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class RoomLocationFormatter {

    private RoomLocationFormatter() {
    }

    public static String format(RoomEntity room) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, room.getUbicacion());

        PabellonEntity building = room.getPabellon();
        if (building != null) {
            addIfPresent(parts, building.getNombre());
            if (building.getCampus() != null) {
                addIfPresent(parts, "Campus " + building.getCampus().getNombre());
            }
            if (building.getLatitude() != null && building.getLongitude() != null) {
                parts.add("(" + coordinate(building.getLatitude()) + ", "
                    + coordinate(building.getLongitude()) + ")");
            }
        }

        return String.join(", ", parts);
    }

    public static BigDecimal latitude(RoomEntity room) {
        return room.getPabellon() == null ? null : room.getPabellon().getLatitude();
    }

    public static BigDecimal longitude(RoomEntity room) {
        return room.getPabellon() == null ? null : room.getPabellon().getLongitude();
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private static String coordinate(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
