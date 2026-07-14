package com.luistudio.reservas.dto.room;

import java.util.List;

public record IntelligentRoomSearchResponse(RoomSearchIntent intent, List<Recommendation> recommendations, String message) {
    public IntelligentRoomSearchResponse(RoomSearchIntent intent, List<Recommendation> recommendations) {
        this(intent, recommendations, null);
    }

    public record Recommendation(RoomResponse room, int score, List<String> reasons) {
    }
}
