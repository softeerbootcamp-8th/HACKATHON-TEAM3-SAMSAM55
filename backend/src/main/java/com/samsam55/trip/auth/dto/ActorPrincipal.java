package com.samsam55.trip.auth.dto;

public record ActorPrincipal(
        ActorType actorType,
        Long userId,
        Long participantId,
        Long tripId
) {

    public enum ActorType {
        HOST,
        PARTICIPANT
    }

    public static ActorPrincipal ofHost(Long userId) {
        return new ActorPrincipal(ActorType.HOST, userId, null, null);
    }

    public static ActorPrincipal ofParticipant(ParticipantPrincipal principal) {
        return new ActorPrincipal(
                ActorType.PARTICIPANT,
                null,
                principal.participantId(),
                principal.tripId()
        );
    }
}
