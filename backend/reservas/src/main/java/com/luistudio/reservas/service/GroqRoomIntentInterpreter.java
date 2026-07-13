package com.luistudio.reservas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.dto.room.RoomSearchIntent;
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

/** Adaptador de Groq: transforma texto en criterios, nunca recibe ni devuelve salas. */
@Slf4j
@Component
public class GroqRoomIntentInterpreter implements RoomIntentInterpreter {
    private static final String SYSTEM_PROMPT = """
        Eres un extractor de intención para reservas de espacios universitarios. No recomiendes salas.
        Convierte el pedido del usuario a los criterios solicitados. Usa ESTUDIO_INDIVIDUAL para una persona,
        ESTUDIO_GRUPAL para dos o más personas estudiando, REUNION, PRESENTACION o GENERAL. Infiere la capacidad
        mínima solicitada (usa 1 si no se indica). maximumNoise es BAJO, MEDIO o ALTO. requiredEquipment contiene
        nombres cortos y normalizados en español, por ejemplo proyector, pizarra, computadora. Responde solo con JSON.
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
    public RoomSearchIntent interpret(String naturalLanguageQuery) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "La búsqueda inteligente no está configurada");
        }

        try {
            String response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(naturalLanguageQuery))
                .retrieve()
                .body(String.class);
            return parseIntent(response);
        } catch (RestClientException | JsonProcessingException | IllegalArgumentException ex) {
            log.warn("groq_intent_interpretation_failed type={}", ex.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo interpretar la búsqueda. Intenta nuevamente.");
        }
    }

    private Map<String, Object> requestBody(String query) {
        return Map.of(
            "model", model,
            "temperature", 0,
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", query)
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
            "required", List.of("roomType", "minimumCapacity", "maximumNoise", "requiresConcentration", "requiredEquipment"),
            "properties", Map.of(
                "roomType", Map.of("type", "string", "enum", List.of("ESTUDIO_INDIVIDUAL", "ESTUDIO_GRUPAL", "REUNION", "PRESENTACION", "GENERAL")),
                "minimumCapacity", Map.of("type", "integer", "minimum", 1, "maximum", 100),
                "maximumNoise", Map.of("type", "string", "enum", List.of("BAJO", "MEDIO", "ALTO")),
                "requiresConcentration", Map.of("type", "boolean"),
                "requiredEquipment", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 10)
            )
        );
    }

    private RoomSearchIntent parseIntent(String response) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual()) {
            throw new JsonProcessingException("Groq did not return an intent") {};
        }
        JsonNode intent = objectMapper.readTree(content.asText());
        Set<String> equipment = new LinkedHashSet<>();
        intent.path("requiredEquipment").forEach(item -> equipment.add(item.asText().trim().toLowerCase()));
        return new RoomSearchIntent(
            RoomType.valueOf(intent.path("roomType").asText()),
            intent.path("minimumCapacity").asInt(),
            RoomNoiseLevel.valueOf(intent.path("maximumNoise").asText()),
            intent.path("requiresConcentration").asBoolean(),
            equipment
        );
    }
}
