package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.RoomSearchAnalysis;
import com.luistudio.reservas.dto.room.RoomSearchCandidate;
import java.util.List;

public interface RoomIntentInterpreter {
    RoomSearchAnalysis interpret(String naturalLanguageQuery, List<RoomSearchCandidate> candidates);
}
