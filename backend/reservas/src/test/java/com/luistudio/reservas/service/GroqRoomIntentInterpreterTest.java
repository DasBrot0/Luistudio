package com.luistudio.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.dto.room.RoomSearchCandidate;
import com.luistudio.reservas.dto.room.RoomSearchAnalysis;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GroqRoomIntentInterpreterTest {

    @Test
    void sendsPublicCandidateMetadataWithoutScheduleOrPersonalData() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        GroqRoomIntentInterpreter interpreter = new GroqRoomIntentInterpreter(
            RestClient.builder(), objectMapper, "test-key", "test-model", 2
        );
        RoomSearchCandidate candidate = new RoomSearchCandidate(
            7L, "CDM-FUTBOL", "Cancha fútbol", "Cancha para partidos", "Mayorazgo", "Centro Deportivo",
            "Zona Sur", -12.0596826, -76.9421069, 14, "ALTO", false, "GENERAL", Set.of("arcos"), Set.of("fútbol"),
            Set.of("comedor cercano"), Set.of("ruta accesible")
        );

        Map<String, Object> body = interpreter.requestBody("quiero jugar fútbol", List.of(candidate));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        JsonNode sent = objectMapper.readTree(messages.get(1).get("content"));

        assertThat(sent.path("query").asText()).isEqualTo("quiero jugar fútbol");
        assertThat(sent.path("activeRooms").path(0).path("name").asText()).isEqualTo("Cancha fútbol");
        assertThat(sent.path("activeRooms").path(0).path("nearbyServices").toString()).contains("comedor cercano");
        assertThat(sent.path("activeRooms").path(0).path("latitude").asDouble()).isEqualTo(-12.0596826);
        assertThat(sent.has("date")).isFalse();
        assertThat(sent.has("start")).isFalse();
        assertThat(sent.has("end")).isFalse();
        assertThat(sent.toString()).doesNotContain("email", "session", "cookie");
    }

    @Test
    void parsesNegativeConstraintsAndSpatialPreference() throws Exception {
        GroqRoomIntentInterpreter interpreter = new GroqRoomIntentInterpreter(
            RestClient.builder(), new ObjectMapper(), "test-key", "test-model", 2
        );
        String content = """
            {"roomType":"ESTUDIO_INDIVIDUAL","minimumCapacity":1,"maximumNoise":"BAJO",
             "requiresConcentration":true,"requiredEquipment":[],
             "candidateMatches":[
               {"roomId":10,"relevanceScore":0,"reason":"Edificio excluido","excluded":true},
               {"roomId":20,"relevanceScore":25,"reason":"Alternativa compatible","excluded":false}
             ],
             "proximityPreference":{"mode":"FAR","referenceRoomId":10}}
            """;
        String response = new ObjectMapper().writeValueAsString(Map.of(
            "choices", List.of(Map.of("message", Map.of("content", content)))
        ));

        RoomSearchAnalysis analysis = interpreter.parseIntent(response);

        assertThat(analysis.candidateMatches().getFirst().excluded()).isTrue();
        assertThat(analysis.proximityPreference().mode()).isEqualTo(RoomSearchAnalysis.ProximityMode.FAR);
        assertThat(analysis.proximityPreference().referenceRoomId()).isEqualTo(10L);
    }
}
