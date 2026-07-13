package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.RoomSearchIntent;

public interface RoomIntentInterpreter {
    RoomSearchIntent interpret(String naturalLanguageQuery);
}
