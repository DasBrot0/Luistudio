package com.luistudio.reservas.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RoomCatalogTranslator {

    private static final Map<String, String> CAMPUS_TO_ES = Map.of(
        "Monterrico", "Monterrico",
        "Mayorazgo", "Mayorazgo"
    );

    private static final Map<String, String> VENUE_TO_ES = Map.of(
        "University Wellness Center", "Centro Bienestar Universitario",
        "Cruz del Sur Sports Center", "Centro Deportivo Cruz del Sur",
        "Mayorazgo Sports Center", "Centro Deportivo Mayorazgo"
    );

    private static final Map<String, String> RESOURCE_TO_ES = Map.ofEntries(
        Map.entry("Basketball Full Court", "Basket cancha completa"),
        Map.entry("Basketball Half Court", "Basket media cancha"),
        Map.entry("Fronton Court", "Campo fronton"),
        Map.entry("Padel Court", "Campo padel"),
        Map.entry("Tennis Court", "Campo tenis"),
        Map.entry("Soccer Field", "Cancha Futbol"),
        Map.entry("Volleyball Court", "Cancha Voley"),
        Map.entry("Swimming Pool", "Piscina"),
        Map.entry("Multiuse Field", "Campo multiuso"),
        Map.entry("Study Cubicles", "Cubiculos"),
        Map.entry("Table Football", "Fulbito de Mesa"),
        Map.entry("Dance Room", "Sala de Baile"),
        Map.entry("Screening Room", "Sala visionado"),
        Map.entry("Table Tennis", "Tennis de Mesa"),
        Map.entry("Cruz del Sur Field - Five-a-side Soccer", "Campo Cruz del Sur - Fulbito")
    );

    public String campusToEs(String campusEn) {
        return CAMPUS_TO_ES.getOrDefault(campusEn, campusEn);
    }

    public String venueToEs(String venueEn) {
        return VENUE_TO_ES.getOrDefault(venueEn, venueEn);
    }

    public String resourceToEs(String resourceEn) {
        return RESOURCE_TO_ES.getOrDefault(resourceEn, resourceEn);
    }
}
