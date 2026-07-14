package com.luistudio.reservas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.dto.room.RoomSearchIntent;
import com.luistudio.reservas.dto.room.RoomSearchAnalysis;
import com.luistudio.reservas.dto.room.RoomSearchCandidate;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.RoomNoiseLevel;
import com.luistudio.reservas.model.RoomType;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adaptador de Groq: extrae criterios y estima afinidad sobre el catálogo público de salas activas. */
@Slf4j
@Component
public class GroqRoomIntentInterpreter implements RoomIntentInterpreter {
    private static final String SYSTEM_PROMPT = """
        Eres un analizador de búsqueda de espacios universitarios. Convierte el pedido del usuario a los criterios
        solicitados y evalúa la afinidad semántica de cada sala activa usando su nombre, descripción,
        actividades, equipamiento, servicios cercanos, accesibilidad y coordenadas. Usa ESTUDIO_INDIVIDUAL para una persona,
        ESTUDIO_GRUPAL para dos o más personas estudiando, REUNION, PRESENTACION o GENERAL. Infiere la capacidad
        mínima solicitada (usa 1 si no se indica). Para deporte o actividades distintas a estudio/reunión usa GENERAL.
        maximumNoise es BAJO, MEDIO o ALTO. requiredEquipment contiene nombres cortos y normalizados en español.
        Interpreta afirmaciones y negaciones de forma literal. "No quiero en el edificio M", "evita M" o "excepto M"
        excluyen las salas de M. "No necesito proyector" significa que el proyector no es obligatorio: no excluyas una
        sala por tenerlo. "No me importa el ruido" usa ALTO; "no quiero ruido" usa BAJO. "Sin escaleras" favorece
        accesibilidad, pero solo excluye una sala si sus datos contradicen expresamente el pedido. Para cada candidato,
        excluded es true solo cuando viola una restricción negativa explícita; nunca excluyas por preferencias blandas.
        Devuelve cada roomId recibido una sola vez, con relevanceScore entre 0 y 30 y una razón breve y concreta.
        Si el usuario pide estar cerca o lejos de un edificio/espacio identificable en el catálogo, devuelve NEAR o FAR
        y usa como referenceRoomId una sala de ese edificio; el backend calculará la distancia con las coordenadas.
        Para "cerca del comedor" u otro servicio sin punto geográfico exacto, usa afinidad semántica y proximity NONE.
        No inventes identificadores ni datos. Responde solo con JSON.
        """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GroqRoomIntentInterpreter(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${app.intelligent-search.groq.api-key:}") String apiKey,
        @Value("${app.intelligent-search.groq.model:openai/gpt-oss-20b}") String model,
        @Value("${app.intelligent-search.groq.timeout-seconds:12}") long timeoutSeconds
    ) {
        this.restClient = restClientBuilder
            .baseUrl("https://api.groq.com/openai/v1")
            .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
                setReadTimeout(Duration.ofSeconds(timeoutSeconds));
            }})
            .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public RoomSearchAnalysis interpret(String naturalLanguageQuery, List<RoomSearchCandidate> candidates) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "La búsqueda inteligente no está configurada");
        }

        try {
            String response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(naturalLanguageQuery, candidates))
                .retrieve()
                .body(String.class);
            return parseIntent(response);
        } catch (RestClientException | JsonProcessingException | IllegalArgumentException ex) {
            log.warn("groq_intent_interpretation_failed type={}", ex.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo interpretar la búsqueda. Intenta nuevamente.");
        }
    }

    Map<String, Object> requestBody(String query, List<RoomSearchCandidate> candidates) throws JsonProcessingException {
        String userContent = objectMapper.writeValueAsString(Map.of("query", query, "activeRooms", candidates));
        return Map.of(
            "model", model,
            "temperature", 0,
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userContent)
            ),
            "response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of("name", "room_search_intent", "strict", true, "schema", jsonSchema())
            )
        );
    }

    private Map<String, Object> jsonSchema() {
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("roomType", "minimumCapacity", "maximumNoise", "requiresConcentration", "requiredEquipment", "candidateMatches", "proximityPreference"),
            "properties", Map.of(
                "roomType", Map.of("type", "string", "enum", List.of("ESTUDIO_INDIVIDUAL", "ESTUDIO_GRUPAL", "REUNION", "PRESENTACION", "GENERAL")),
                "minimumCapacity", Map.of("type", "integer", "minimum", 1, "maximum", 100),
                "maximumNoise", Map.of("type", "string", "enum", List.of("BAJO", "MEDIO", "ALTO")),
                "requiresConcentration", Map.of("type", "boolean"),
                "requiredEquipment", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 10),
                "candidateMatches", Map.of(
                    "type", "array",
                    "maxItems", 200,
                    "items", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("roomId", "relevanceScore", "reason", "excluded"),
                        "properties", Map.of(
                            "roomId", Map.of("type", "integer", "minimum", 1),
                            "relevanceScore", Map.of("type", "integer", "minimum", 0, "maximum", 30),
                            "reason", Map.of("type", "string", "maxLength", 200),
                            "excluded", Map.of("type", "boolean")
                        )
                    )
                ),
                "proximityPreference", Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "required", List.of("mode", "referenceRoomId"),
                    "properties", Map.of(
                        "mode", Map.of("type", "string", "enum", List.of("NONE", "NEAR", "FAR")),
                        "referenceRoomId", Map.of("type", "integer", "minimum", 0)
                    )
                )
            )
        );
    }

    RoomSearchAnalysis parseIntent(String response) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual()) {
            throw new JsonProcessingException("Groq did not return an intent") {};
        }
        JsonNode intent = objectMapper.readTree(content.asText());
        Set<String> equipment = new LinkedHashSet<>();
        intent.path("requiredEquipment").forEach(item -> equipment.add(item.asText().trim().toLowerCase()));
        RoomSearchIntent parsedIntent = new RoomSearchIntent(
            RoomType.valueOf(intent.path("roomType").asText()),
            intent.path("minimumCapacity").asInt(),
            RoomNoiseLevel.valueOf(intent.path("maximumNoise").asText()),
            intent.path("requiresConcentration").asBoolean(),
            equipment
        );
        List<RoomSearchAnalysis.CandidateMatch> matches = new java.util.ArrayList<>();
        intent.path("candidateMatches").forEach(item -> matches.add(new RoomSearchAnalysis.CandidateMatch(
            item.path("roomId").asLong(),
            Math.max(0, Math.min(30, item.path("relevanceScore").asInt())),
            item.path("reason").asText("").trim(),
            item.path("excluded").asBoolean(false)
        )));
        JsonNode proximity = intent.path("proximityPreference");
        RoomSearchAnalysis.ProximityMode proximityMode = RoomSearchAnalysis.ProximityMode.valueOf(
            proximity.path("mode").asText("NONE")
        );
        return new RoomSearchAnalysis(
            parsedIntent,
            List.copyOf(matches),
            new RoomSearchAnalysis.ProximityPreference(proximityMode, proximity.path("referenceRoomId").asLong(0))
        );
    }
}
