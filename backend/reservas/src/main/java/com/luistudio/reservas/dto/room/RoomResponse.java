package com.luistudio.reservas.dto.room;

public record RoomResponse(
    Long id,
    String code,
    String name,
    Integer capacity,
    String location,
    String status,
    String pabellonCode
) {
}
