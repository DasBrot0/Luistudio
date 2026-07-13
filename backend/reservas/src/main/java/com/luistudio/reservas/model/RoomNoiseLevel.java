package com.luistudio.reservas.model;

/** Nivel de ruido habitual de una sala, de menor a mayor. */
public enum RoomNoiseLevel {
    BAJO,
    MEDIO,
    ALTO;

    public boolean satisfies(RoomNoiseLevel maximumRequested) {
        return ordinal() <= maximumRequested.ordinal();
    }
}
