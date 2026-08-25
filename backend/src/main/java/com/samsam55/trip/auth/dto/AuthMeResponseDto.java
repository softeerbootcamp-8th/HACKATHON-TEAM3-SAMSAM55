package com.samsam55.trip.auth.dto;

public record AuthMeResponseDto(String actorType, Long userId, Long participantId, Long tripId) {

    public static AuthMeResponseDto from(ActorPrincipal principal) {
        return new AuthMeResponseDto(
                principal.actorType().name(),
                principal.userId(),
                principal.participantId(),
                principal.tripId()
        );
    }

    public static AuthMeResponseDto ofHost(Long userId) {
        return new AuthMeResponseDto("HOST", userId, null, null);
    }

    public static AuthMeResponseDto ofParticipant(ParticipantPrincipal principal) {
        return new AuthMeResponseDto("PARTICIPANT", null, principal.participantId(), principal.tripId());
    }
}
