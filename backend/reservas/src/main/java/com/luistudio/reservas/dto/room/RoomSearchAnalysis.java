package com.luistudio.reservas.dto.room;

import java.util.List;

/** Intención estructurada y afinidad semántica sugerida; el backend conserva la decisión final. */
public record RoomSearchAnalysis(
    RoomSearchIntent intent,
    List<CandidateMatch> candidateMatches,
    ProximityPreference proximityPreference
) {
    public RoomSearchAnalysis(RoomSearchIntent intent, List<CandidateMatch> candidateMatches) {
        this(intent, candidateMatches, new ProximityPreference(ProximityMode.NONE, 0L));
    }

    public record CandidateMatch(Long roomId, int relevanceScore, String reason, boolean excluded) {
        public CandidateMatch(Long roomId, int relevanceScore, String reason) {
            this(roomId, relevanceScore, reason, false);
        }
    }

    public record ProximityPreference(ProximityMode mode, Long referenceRoomId) {
    }

    public enum ProximityMode {
        NONE,
        NEAR,
        FAR
    }
}
