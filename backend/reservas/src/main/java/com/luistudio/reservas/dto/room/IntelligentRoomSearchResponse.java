package com.luistudio.reservas.dto.room;

import java.util.List;

public record IntelligentRoomSearchResponse(RoomSearchIntent intent, List<Recommendation> recommendations) {
    public record Recommendation(RoomResponse room, int score, List<String> reasons) {
    }
}
